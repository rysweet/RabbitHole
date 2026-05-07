/*******************************************************************************
 * Copyright (c) 2006, 2015, Carnegie Mellon University. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Products derived from the software may not be called "Alice", nor may
 *    "Alice" appear in their name, without prior written permission of
 *    Carnegie Mellon University.
 *
 * 4. All advertising materials mentioning features or use of this software must
 *    display the following acknowledgement: "This product includes software
 *    developed by Carnegie Mellon University"
 *
 * 5. The gallery of art assets and animations provided with this software is
 *    contributed by Electronic Arts Inc. and may be used for personal,
 *    non-commercial, and academic use only. Redistributions of any program
 *    source code that utilizes The Sims 2 Assets must also retain the copyright
 *    notice, list of conditions and the disclaimer contained in
 *    The Alice 3.0 Art Gallery License.
 *
 * DISCLAIMER:
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 * ANY AND ALL EXPRESS, STATUTORY OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY,  FITNESS FOR A
 * PARTICULAR PURPOSE, TITLE, AND NON-INFRINGEMENT ARE DISCLAIMED. IN NO EVENT
 * SHALL THE AUTHORS, COPYRIGHT OWNERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING FROM OR OTHERWISE RELATING TO
 * THE USE OF OR OTHER DEALINGS WITH THE SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package edu.cmu.cs.dennisc.java.awt;

import edu.cmu.cs.dennisc.java.lang.SystemUtilities;
import edu.cmu.cs.dennisc.java.util.Maps;
import edu.cmu.cs.dennisc.java.util.logging.Logger;
import edu.cmu.cs.dennisc.map.MapToMap;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * @author Dennis Cosgrove
 */
public class FileDialogUtilities {
  public static final String SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY = "org.alice.eatme.saveDialogDiscoveryEvidenceDir";
  public static final String SAVE_DIALOG_DISCOVERY_TARGET_ARTIFACT = "desktop-save-dialog-discovery-target.json";

  private interface FileDialog {
    String getFile();

    void setFile(String filename);

    String getDirectory();

    void setDirectory(String path);

    void setFilenameFilter(FilenameFilter filenameFilter);

    void show();
  }

  private static class AwtFileDialog implements FileDialog {
    private final java.awt.FileDialog awtFileDialog;

    AwtFileDialog(Component root, String title, int mode) {
      if (root instanceof Frame frame) {
        awtFileDialog = new java.awt.FileDialog(frame, title, mode);
      } else if (root instanceof Dialog dialog) {
        awtFileDialog = new java.awt.FileDialog(dialog, title, mode);
      } else {
        awtFileDialog = new java.awt.FileDialog((Dialog) null, title, mode);
      }
    }

    @Override
    public String getFile() {
      return this.awtFileDialog.getFile();
    }

    @Override
    public void setFile(String filename) {
      this.awtFileDialog.setFile(filename);
    }

    @Override
    public String getDirectory() {
      return this.awtFileDialog.getDirectory();
    }

    @Override
    public void setDirectory(String path) {
      this.awtFileDialog.setDirectory(path);
    }

    @Override
    public void setFilenameFilter(FilenameFilter filenameFilter) {
      this.awtFileDialog.setFilenameFilter(filenameFilter);
    }

    @Override
    public void show() {
      this.awtFileDialog.setVisible(true);
    }
  }

  private static class SwingFileDialog implements FileDialog {
    private final JFileChooser jFileChooser = new JFileChooser();
    private final Component root;
    private final String title;
    private final int mode;
    private int result = JFileChooser.CANCEL_OPTION;

    SwingFileDialog(Component root, String title, int mode) {
      this.root = root;
      this.title = title;
      this.mode = mode;
    }

    @Override
    public String getFile() {
      if (this.result != JFileChooser.CANCEL_OPTION) {
        File file = this.jFileChooser.getSelectedFile();
        if (file != null) {
          return file.getName();
        } else {
          return null;
        }
      } else {
        return null;
      }
    }

    @Override
    public void setFile(String filename) {
      this.jFileChooser.setSelectedFile(new File(filename));
    }

    @Override
    public String getDirectory() {
      if (this.result != JFileChooser.CANCEL_OPTION) {
        File file = this.jFileChooser.getCurrentDirectory();
        if (file != null) {
          return file.getAbsolutePath();
        } else {
          return null;
        }
      } else {
        return null;
      }
    }

    @Override
    public void setDirectory(String path) {
      if (path != null) {
        this.jFileChooser.setCurrentDirectory(new File(path));
      }
    }

    @Override
    public void setFilenameFilter(FilenameFilter filenameFilter) {
      //todo Wrap FilenameFilter in a FileFilter
    }

    @Override
    public void show() {
      //todo: use this.title
      this.result = JFileChooser.CANCEL_OPTION;
      if (mode == java.awt.FileDialog.LOAD) {
        this.result = this.jFileChooser.showOpenDialog(this.root);
      } else {
        this.result = this.jFileChooser.showSaveDialog(this.root);
      }

    }
  }

  private static MapToMap<Component, String, FileDialog> mapPathToSaveFileDialog = MapToMap.newInstance();
  private static Map<String, String> mapSecondaryKeyToPath = Maps.newHashMap();

  private static FileDialog createFileDialog(Component root, String title, int mode) {
    if (SystemUtilities.isLinux()) {
      return new SwingFileDialog(root, title, mode);
    } else {
      return new AwtFileDialog(root, title, mode);
    }
  }

  private static final String NULL_KEY = "null";

  public static File showSaveFileDialog(Component component, File directory, String filename, String extension) {
    String directoryPath = directory != null ? directory.getAbsolutePath() : null;
    FileDialog fileDialog;
    Component root = SwingUtilities.getRoot(component);
    recordSaveDialogDiscoveryTarget(component, directory, filename, extension, root);
    String secondaryKey;
    if (directoryPath != null) {
      secondaryKey = directoryPath;
    } else {
      secondaryKey = NULL_KEY;
    }
    MapToMap<Component, String, FileDialog> mapPathToFileDialog;
    mapPathToFileDialog = FileDialogUtilities.mapPathToSaveFileDialog;
    fileDialog = mapPathToFileDialog.get(component, secondaryKey);

    if (fileDialog == null) {
      fileDialog = createFileDialog(root, "Save...", java.awt.FileDialog.SAVE);
      mapPathToFileDialog.put(component, secondaryKey, fileDialog);
    }
    if (filename != null) {
      fileDialog.setFile(filename);
    }

    String path = FileDialogUtilities.mapSecondaryKeyToPath.get(secondaryKey);
    if (path == null) {
      path = directoryPath;
    }
    if (path != null) {
      fileDialog.setDirectory(path);
    }

    fileDialog.show();
    String fileName = fileDialog.getFile();
    if (fileName != null) {
      String requestedDirectoryPath = fileDialog.getDirectory();
      FileDialogUtilities.mapSecondaryKeyToPath.put(secondaryKey, requestedDirectoryPath);
      File directory1 = new File(requestedDirectoryPath);
      if (!fileName.endsWith("." + extension)) {
        fileName += "." + extension;
      }
      return new File(directory1, fileName);
    } else {
      return null;
    }
  }

  public static Path writeSaveDialogDiscoveryTarget(
      Path evidenceDir,
      Component component,
      File directory,
      String filename,
      String extension) throws IOException {
    return writeSaveDialogDiscoveryTarget(evidenceDir, component, directory, filename, extension, SwingUtilities.getRoot(component));
  }

  public static String escapeJson(String value) {
    StringBuilder escaped = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char ch = value.charAt(i);
      switch (ch) {
        case '\\' -> escaped.append("\\\\");
        case '"' -> escaped.append("\\\"");
        case '\b' -> escaped.append("\\b");
        case '\f' -> escaped.append("\\f");
        case '\n' -> escaped.append("\\n");
        case '\r' -> escaped.append("\\r");
        case '\t' -> escaped.append("\\t");
        default -> {
          if (ch < 0x20) {
            escaped.append(String.format("\\u%04x", (int) ch));
          } else {
            escaped.append(ch);
          }
        }
      }
    }
    return escaped.toString();
  }

  private static void recordSaveDialogDiscoveryTarget(
      Component component,
      File directory,
      String filename,
      String extension,
      Component root) {
    String evidenceDir = System.getProperty(SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY);
    if (evidenceDir == null || evidenceDir.isBlank()) {
      return;
    }
    try {
      writeSaveDialogDiscoveryTarget(Path.of(evidenceDir), component, directory, filename, extension, root);
    } catch (IOException | RuntimeException ex) {
      Logger.throwable(ex, "Save dialog discovery target evidence write failed: " + evidenceDir);
    }
  }

  private static Path writeSaveDialogDiscoveryTarget(
      Path evidenceDir,
      Component component,
      File directory,
      String filename,
      String extension,
      Component root) throws IOException {
    Files.createDirectories(evidenceDir);
    Path artifact = artifactPath(evidenceDir, SAVE_DIALOG_DISCOVERY_TARGET_ARTIFACT);
    Files.writeString(
        artifact,
        saveDialogDiscoveryJson(component, directory, filename, extension, root),
        StandardCharsets.UTF_8);
    if (!Files.isRegularFile(artifact) || Files.size(artifact) == 0) {
      throw new IOException("Save dialog discovery target artifact was not written: " + artifact);
    }
    return artifact;
  }

  private static String saveDialogDiscoveryJson(
      Component component,
      File directory,
      String filename,
      String extension,
    Component root) {
    String reason = saveDialogDiscoveryReason(component, root);
    String status = switch (reason) {
      case "target_resolved" -> "target_resolved";
      case "missing_owner_component", "headless_graphics_environment" -> "unsupported";
      default -> "blocked";
    };
    return "{\n"
        + "  \"schema_version\": \"eatme.alice-desktop-save-dialog-discovery-target/v1\",\n"
        + "  \"status\": \"" + status + "\",\n"
        + "  \"reason\": \"" + reason + "\",\n"
        + "  \"source\": \"edu.cmu.cs.dennisc.java.awt.FileDialogUtilities#showSaveFileDialog(Component,File,String,String)\",\n"
        + "  \"dialog_targets\": {\n"
        + "    \"desktop_frame\": \"org.lgna.croquet.DocumentFrame#showSaveFileDialog(File,String,String)\",\n"
        + "    \"native_chooser\": \"edu.cmu.cs.dennisc.java.awt.FileDialogUtilities#showSaveFileDialog(Component,File,String,String)\",\n"
        + "    \"dialog_implementation\": \"" + escapeJson(saveDialogImplementation()) + "\"\n"
        + "  },\n"
        + "  \"request\": {\n"
        + "    \"title\": \"Save...\",\n"
        + "    \"mode\": \"SAVE\",\n"
        + "    \"directory\": " + fileJson(directory) + ",\n"
        + "    \"filename\": " + stringJson(filename) + ",\n"
        + "    \"extension\": " + stringJson(extension) + "\n"
        + "  },\n"
        + "  \"owner_window\": {\n"
        + "    \"headless\": " + GraphicsEnvironment.isHeadless() + ",\n"
        + "    \"owner_component_class\": " + componentJson(component) + ",\n"
        + "    \"owner_displayable\": " + booleanJson(component == null ? null : component.isDisplayable()) + ",\n"
        + "    \"owner_showing\": " + booleanJson(component == null ? null : component.isShowing()) + ",\n"
        + "    \"root_component_class\": " + componentJson(root) + ",\n"
        + "    \"root_displayable\": " + booleanJson(root == null ? null : root.isDisplayable()) + ",\n"
        + "    \"root_showing\": " + booleanJson(root == null ? null : root.isShowing()) + "\n"
        + "  },\n"
        + "  \"reporting_summary\": \"" + escapeJson(reportingSummary(reason)) + "\",\n"
        + "  \"blocker\": {\n"
        + "    \"observed\": \"" + escapeJson(observed(reason)) + "\",\n"
        + "    \"required\": \"displayable Alice ProjectDocumentFrame root window before FileDialogUtilities.showSaveFileDialog displays the Save dialog\"\n"
        + "  },\n"
        + "  \"requiresNextEvidence\": [\n"
        + "    \"displayable Alice ProjectDocumentFrame root window at FileDialogUtilities.showSaveFileDialog\",\n"
        + "    \"Save dialog displayed result from FileDialogUtilities after FileDialog.show() returns\",\n"
        + "    \"selected Save path supplied by UI automation\"\n"
        + "  ],\n"
        + "  \"doesNotClaim\": [\n"
        + "    \"desktop Save menu item was clicked\",\n"
        + "    \"Save dialog displayed\",\n"
        + "    \"Save dialog controlled\",\n"
        + "    \"selected Save path supplied by UI automation\",\n"
        + "    \"saved file completed\",\n"
        + "    \"first-lesson completion\",\n"
        + "    \"visible rendering correctness\",\n"
        + "    \"grading\"\n"
        + "  ]\n"
        + "}\n";
  }

  private static String saveDialogDiscoveryReason(Component component, Component root) {
    if (component == null) {
      return "missing_owner_component";
    }
    if (root == null) {
      return "missing_dialog_root_window";
    }
    if (GraphicsEnvironment.isHeadless()) {
      return "headless_graphics_environment";
    }
    if (!root.isDisplayable()) {
      return "dialog_root_not_displayable";
    }
    return "target_resolved";
  }

  private static String saveDialogImplementation() {
    return SystemUtilities.isLinux()
        ? "edu.cmu.cs.dennisc.java.awt.FileDialogUtilities.SwingFileDialog"
        : "edu.cmu.cs.dennisc.java.awt.FileDialogUtilities.AwtFileDialog";
  }

  private static String reportingSummary(String reason) {
    return switch (reason) {
      case "missing_owner_component" -> "No owner Component was supplied, so Alice cannot discover or control a Save dialog from this seam.";
      case "missing_dialog_root_window" -> "A Save dialog owner Component was supplied, but SwingUtilities.getRoot(component) did not find a desktop window.";
      case "headless_graphics_environment" -> "The JVM is headless, so Alice cannot display or control a desktop Save dialog here.";
      case "dialog_root_not_displayable" -> "The Save dialog root window exists but is not displayable yet.";
      default -> "The Save dialog owner and root target were resolved before FileDialog.show(); dialog display and control still require separate evidence.";
    };
  }

  private static String observed(String reason) {
    return switch (reason) {
      case "missing_owner_component" -> "owner Component is null";
      case "missing_dialog_root_window" -> "SwingUtilities.getRoot(component) is null";
      case "headless_graphics_environment" -> "GraphicsEnvironment.isHeadless() is true";
      case "dialog_root_not_displayable" -> "root Component exists but root.isDisplayable() is false";
      default -> "owner Component and displayable root Component resolved";
    };
  }

  private static String fileJson(File file) {
    return file == null ? "null" : stringJson(file.getPath());
  }

  private static String stringJson(String value) {
    return value == null ? "null" : "\"" + escapeJson(value) + "\"";
  }

  private static String componentJson(Component component) {
    return component == null ? "null" : stringJson(component.getClass().getName());
  }

  private static String booleanJson(Boolean value) {
    return value == null ? "null" : value.toString();
  }

  private static Path artifactPath(Path evidenceDir, String artifactName) {
    Path artifact = evidenceDir.resolve(artifactName).normalize();
    if (!artifact.startsWith(evidenceDir.normalize())) {
      throw new IllegalArgumentException("Save dialog discovery artifact escapes evidence dir");
    }
    return artifact;
  }

  private static File showFileDialog(int mode, Component component, String title, File directory, String filename, FilenameFilter filenameFilter, String extensionToAddIfMissing) {
    if ((extensionToAddIfMissing != null) && (extensionToAddIfMissing.charAt(0) == '.')) {
      Logger.severe("removing leading . from", extensionToAddIfMissing);
      extensionToAddIfMissing = extensionToAddIfMissing.substring(1);
    }
    Component root = SwingUtilities.getRoot(component);
    FileDialog fileDialog = createFileDialog(root, title, mode);
    if (filename != null) {
      fileDialog.setFile(filename);
    }
    if ((directory != null) && directory.exists()) {
      fileDialog.setDirectory(directory.toString());
    }
    if (filenameFilter != null) {
      fileDialog.setFilenameFilter(filenameFilter);
    }
    fileDialog.show();
    String fileName = fileDialog.getFile();
    File rv;
    if (fileName != null) {
      String requestedDirectoryPath = fileDialog.getDirectory();
      File rvDirectory = new File(requestedDirectoryPath);
      if (mode == java.awt.FileDialog.SAVE) {
        if (!fileName.endsWith("." + extensionToAddIfMissing)) {
          fileName += "." + extensionToAddIfMissing;
        }
      }
      rv = new File(rvDirectory, fileName);
    } else {
      rv = null;
    }
    return rv;
  }

  public static File showOpenFileDialog(Component component, String title, File initialDirectory, String initialFilename, FilenameFilter filenameFilter) {
    return showFileDialog(java.awt.FileDialog.LOAD, component, title, initialDirectory, initialFilename, filenameFilter, null);
  }

  public static File showSaveFileDialog(Component component, String title, File initialDirectory, String initialFilename, FilenameFilter filenameFilter, String extensionToAddIfMissing) {
    return showFileDialog(java.awt.FileDialog.SAVE, component, title, initialDirectory, initialFilename, filenameFilter, extensionToAddIfMissing);
  }
}
