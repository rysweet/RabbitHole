package org.lgna.project.io;

import edu.cmu.cs.dennisc.ui.prompt.EulaPromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.MessagePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.MessageSeverity;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptResult;
import edu.cmu.cs.dennisc.ui.prompt.UiPromptBoundary;
import edu.cmu.cs.dennisc.ui.prompt.UiPrompts;
import org.alice.serialization.tweedle.TweedleEncoderDecoder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lgna.project.Project;
import org.lgna.project.ast.AbstractDeclaration;
import org.lgna.project.ast.NamedUserType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Regression guard for the UI-prompt-boundary leak in the Tweedle serializer.
 *
 * <p>{@code ArgumentEncoder.getParameterLabel} used to call
 * {@code edu.cmu.cs.dennisc.javax.swing.option.Dialogs.showError(...)} directly
 * when it could not read a parameter label — a Swing dialog raised from
 * reusable serialization code. During headless encoding (the Phase 6 corpus
 * harness, batch export) that dialog threw a stream of AWT
 * {@code HeadlessException}s on the event queue. That violated the UI prompt
 * boundary contract (see {@code docs/concepts/ui-prompt-boundary.md}): reusable
 * library code must not open dialogs directly.
 *
 * <p>The serializer now routes the warning through
 * {@link UiPrompts#showMessage(MessagePromptRequest)}. The desktop IDE (with a
 * Swing boundary installed) still surfaces it; headless/library callers get the
 * silent {@code NonInteractiveUiPromptBoundary} default.
 *
 * <p>This test installs a <b>recording</b> boundary, encodes the checked-in
 * curriculum corpus (the exact scenario that used to spew), and asserts that
 * the "Unlabeled parameter" warning is delivered as an {@code ERROR}
 * {@link MessagePromptRequest} through the boundary — not as a direct Swing
 * dialog. Before the fix the message went straight to {@code Dialogs} and the
 * recording boundary would capture nothing, so this test fails on regression.
 *
 * <p>Headless: no JavaFX, no UI. Reads projects directly from the source tree.
 */
public class SerializerUnlabeledParameterPromptBoundaryTest {

  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final TweedleEncoderDecoder CODEC = new TweedleEncoderDecoder();
  private static final String UNLABELED_PARAMETER_TITLE = "Unlabeled parameter";

  private RecordingBoundary recording;
  private UiPromptBoundary previousBoundary;

  @Before
  public void installRecordingBoundary() {
    recording = new RecordingBoundary();
    previousBoundary = UiPrompts.install(recording);
  }

  @After
  public void restoreBoundary() {
    UiPrompts.install(previousBoundary);
  }

  @Test
  public void unlabeledParameterWarningIsRoutedThroughUiPromptBoundaryNotSwing() {
    List<Path> archives = corpusArchives();
    assertFalse("Curriculum corpus must not be empty", archives.isEmpty());

    boolean anyTypeEncoded = false;
    for (Path archive : archives) {
      Project project;
      try {
        project = IoUtilities.readProject(archive.toFile());
      } catch (Exception | LinkageError e) {
        // Unreadable projects (helper/gallery limitation) are irrelevant here.
        continue;
      }
      Set<NamedUserType> types = project.getNamedUserTypes();
      Set<AbstractDeclaration> terminals = new HashSet<>(types);
      for (NamedUserType type : types) {
        try {
          CODEC.encode(type, terminals);
          anyTypeEncoded = true;
        } catch (Exception | LinkageError e) {
          // Encode gaps are a separate concern; the label warning fires
          // (or not) before any such failure.
        }
      }
      // Stop as soon as the boundary has seen the warning we assert on — no
      // need to grind the whole corpus once the contract is demonstrated.
      if (recording.hasMessageWithTitle(UNLABELED_PARAMETER_TITLE)) {
        break;
      }
    }

    assertTrue("Harness must have encoded at least one type", anyTypeEncoded);
    assertTrue(
        "The serializer's 'Unlabeled parameter' warning must be delivered through "
            + "the UI prompt boundary (as an ERROR MessagePromptRequest), not via a "
            + "direct Swing dialog. Recorded messages: " + recording.messages,
        recording.hasMessageWithSeverityAndTitle(MessageSeverity.ERROR, UNLABELED_PARAMETER_TITLE));
  }

  private static List<Path> corpusArchives() {
    Path dir = starterProjectsDirectory();
    List<Path> archives = new ArrayList<>();
    try {
      Files.list(dir)
          .filter(p -> p.getFileName().toString().endsWith(".a3p"))
          .sorted(Comparator.comparing(p -> p.getFileName().toString()))
          .forEach(archives::add);
    } catch (Exception e) {
      fail("Unable to list curriculum corpus in " + dir + ": " + e.getMessage());
    }
    return archives;
  }

  private static Path starterProjectsDirectory() {
    Path current = Paths.get("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve("core/resources/src/application/resources/starter-projects");
      if (Files.isDirectory(candidate)) {
        return candidate;
      }
      current = current.getParent();
    }
    fail("Unable to find core/resources/src/application/resources/starter-projects from "
        + Paths.get("").toAbsolutePath());
    return null;
  }

  /** Boundary that records message requests instead of showing Swing dialogs. */
  private static final class RecordingBoundary implements UiPromptBoundary {
    final List<MessagePromptRequest> messages = new CopyOnWriteArrayList<>();

    boolean hasMessageWithTitle(String title) {
      return messages.stream().anyMatch(m -> title.equals(m.title()));
    }

    boolean hasMessageWithSeverityAndTitle(MessageSeverity severity, String title) {
      return messages.stream()
          .anyMatch(m -> severity == m.severity() && title.equals(m.title()));
    }

    @Override
    public ResourcePromptResult requestResourceLocation(ResourcePromptRequest request) {
      return ResourcePromptResult.noSelection();
    }

    @Override
    public void showMessage(MessagePromptRequest request) {
      messages.add(request);
    }

    @Override
    public boolean requestEulaAcceptance(EulaPromptRequest request) {
      return false;
    }
  }
}
