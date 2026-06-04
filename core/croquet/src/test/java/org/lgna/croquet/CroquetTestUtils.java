package org.lgna.croquet;

import edu.cmu.cs.dennisc.codec.BinaryDecoder;
import edu.cmu.cs.dennisc.codec.BinaryEncoder;
import org.lgna.croquet.history.UserActivity;

import javax.swing.DefaultButtonModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared test utilities for croquet unit tests.
 *
 * <p>Centralizes Swing listener removal helpers that decouple state objects
 * from the {@code Application.getActiveInstance()} dependency chain, and
 * provides a reusable {@link ItemCodec} for String-based tests.</p>
 */
public final class CroquetTestUtils {

  private CroquetTestUtils() {}

  private static final AtomicLong UUID_COUNTER = new AtomicLong();

  /**
   * Returns a deterministic UUID for test use. Avoids the {@code SecureRandom}
   * overhead of {@link UUID#randomUUID()} which is unnecessary in tests.
   */
  public static UUID nextTestUUID() {
    return new UUID(0L, UUID_COUNTER.incrementAndGet());
  }

  /**
   * Resets the Application singleton to null via reflection, allowing tests
   * to install a fresh instance regardless of prior test execution order.
   */
  public static void resetApplicationSingleton() {
    try {
      java.lang.reflect.Field field = Application.class.getDeclaredField("singleton");
      field.setAccessible(true);
      field.set(null, null);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Failed to reset Application singleton", e);
    }
  }

  public static synchronized Application<?> ensureTestApplication() {
    Application<?> active = Application.getActiveInstance();
    if (active == null || !(active instanceof HeadlessTestApplication)) {
      resetApplicationSingleton();
      new HeadlessTestApplication();
    }
    return Application.getActiveInstance();
  }

  private static final class HeadlessTestApplication extends Application<DocumentFrame> {
    @Override
    public DocumentFrame getDocumentFrame() {
      return null;
    }

    @Override
    protected Operation getAboutOperation() {
      return null;
    }

    @Override
    protected Operation getPreferencesOperation() {
      return null;
    }

    @Override
    protected void handleOpenFiles(List<File> files) {
    }

    @Override
    protected void handleWindowOpened(WindowEvent e) {
    }

    @Override
    public void handleQuit(UserActivity activity) {
      activity.finish();
    }

    @Override
    public String getApplicationSubPath() {
      return "test";
    }
  }

  /**
   * Removes all ItemListeners from a BooleanState's ButtonModel.
   * Prevents Application.getActiveInstance() calls during headless testing.
   */
  public static void removeItemListeners(BooleanState state) {
    DefaultButtonModel bm = (DefaultButtonModel) state.getImp().getSwingModel().getButtonModel();
    for (ItemListener il : bm.getItemListeners()) {
      bm.removeItemListener(il);
    }
  }

  /**
   * Removes all ChangeListeners from a BoundedNumberState's SpinnerModel.
   * Prevents Application.getActiveInstance() calls during headless testing.
   */
  public static void removeSpinnerChangeListeners(BoundedNumberState<?> state) {
    SpinnerNumberModel spinner = state.getSwingModel().getSpinnerModel();
    for (ChangeListener cl : spinner.getChangeListeners()) {
      spinner.removeChangeListener(cl);
    }
  }

  /**
   * Removes all DocumentListeners from a StringState's Document.
   * Prevents Application.getActiveInstance() calls during headless testing.
   */
  public static void removeDocumentListeners(StringState state) {
    Document doc = state.getSwingModel().getDocument();
    for (DocumentListener dl : ((AbstractDocument) doc).getDocumentListeners()) {
      doc.removeDocumentListener(dl);
    }
  }

  /**
   * Removes all ListSelectionListeners from a SingleSelectListState's model.
   * Prevents NullTrigger → Application.getActiveInstance() calls during headless testing.
   */
  public static void removeListSelectionListeners(SingleSelectListState<?, ?> state) {
    DefaultListSelectionModel lsm =
        (DefaultListSelectionModel) state.getSwingModel().getListSelectionModel();
    for (ListSelectionListener l : lsm.getListSelectionListeners()) {
      lsm.removeListSelectionListener(l);
    }
  }

  /**
   * Minimal {@link ItemCodec} for String values, shared across list-data
   * and list-state tests.
   */
  public static final ItemCodec<String> STRING_CODEC = new ItemCodec<String>() {
    @Override
    public Class<String> getValueClass() {
      return String.class;
    }

    @Override
    public String decodeValue(BinaryDecoder binaryDecoder) {
      return binaryDecoder.decodeString();
    }

    @Override
    public void encodeValue(BinaryEncoder binaryEncoder, String value) {
      binaryEncoder.encode(value);
    }

    @Override
    public void appendRepresentation(StringBuilder sb, String value) {
      sb.append(value);
    }
  };
}
