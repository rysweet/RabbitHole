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
package org.alice.stageide;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formdev.flatlaf.FlatLaf;
import edu.cmu.cs.dennisc.app.ApplicationRootInitializationException;
import edu.cmu.cs.dennisc.crash.CrashDetector;
import edu.cmu.cs.dennisc.java.awt.ConsistentMouseDragEventQueue;
import edu.cmu.cs.dennisc.java.io.TextFileUtilities;
import edu.cmu.cs.dennisc.java.lang.SystemUtilities;
import edu.cmu.cs.dennisc.java.util.logging.Logger;
import edu.cmu.cs.dennisc.javax.swing.UIManagerUtilities;
import edu.cmu.cs.dennisc.javax.swing.WindowStack;
import edu.cmu.cs.dennisc.render.gl.RendererNativeLibraryLoader;
import edu.wustl.lookingglass.utilities.memory.HeapWatchDog;
import javafx.application.Application;
import javafx.stage.Stage;
import org.lgna.croquet.ProcessTerminationRequestedException;
import org.lgna.croquet.ProcessTerminator;
import org.lgna.project.ProjectVersion;
import org.lgna.project.reflect.ClassInfo;
import org.lgna.project.reflect.ClassInfoManager;

import javax.swing.*;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.util.Locale;

/**
 * @author Dennis Cosgrove
 */
public class EntryPoint extends Application {
  private static final String MENU_BAR_UI_NAME = "MenuBarUI";

  private static HeapWatchDog heapMonitor;

  public static void main(final String[] args) {
    ProcessTerminator.Handler previous = ProcessTerminator.setHandler(System::exit);
    try {
      requireGraphicalEnvironmentForDesktopLaunch(GraphicsEnvironment.isHeadless());

      final CrashDetector crashDetector = new CrashDetector(EntryPoint.class);
      if (crashDetector.isPreviouslyOpenedButNotSucessfullyClosed()) {
        String propertyName = "org.alice.stageide.isCrashDetectionDesired";
        String isCrashDetectionDesiredText = System.getProperty(propertyName, "true");
        if ("true".equals(isCrashDetectionDesiredText.toLowerCase(Locale.ENGLISH))) {
          JOptionPane.showMessageDialog(null, "Alice did not successfully close last time.");
        }
      }
      crashDetector.open();
      String text = ProjectVersion.getCurrentVersionText()/* + " BETA" */;
      System.out.println("version: " + text);

      // This resources file is where all the theme colors are defined
      FlatLaf.registerCustomDefaultsSource("org.alice.stageide.themes");

      // TODO- create a setting somewhere? auto-determine from OS?
      Boolean useDarkMode = false;
      try {
        javax.swing.UIManager.setLookAndFeel((useDarkMode ? new com.formdev.flatlaf.FlatDarkLaf() : new com.formdev.flatlaf.FlatLightLaf()));
        com.formdev.flatlaf.FlatLaf.updateUI();
      } catch (UnsupportedLookAndFeelException updateFlatLafThemeException) {
        Logger.severe("Was unable to set look and feel theme: " + updateFlatLafThemeException.getMessage());
        updateFlatLafThemeException.printStackTrace();
      }

      // Initialize this on the main thread, before Swing or JavaFX, and before opening a project in args.
      try {
        RendererNativeLibraryLoader.initializeIfNecessary();
      } catch (ApplicationRootInitializationException e) {
        JOptionPane.showMessageDialog(null, e.getMessage(), "Application Root Error", JOptionPane.ERROR_MESSAGE);
        ProcessTerminator.requestExit(-1);
      }

      // Initialize Swing here to do it on the correct thread, outside of JavaFX
      SwingUtilities.invokeLater(() -> {
        if (SystemUtilities.isMac()) { //&& SystemUtilities.isPropertyTrue("apple.laf.useScreenMenuBar")) {
          final Object macMenuBarUI = UIManager.get(MENU_BAR_UI_NAME);
          if (macMenuBarUI != null) {
            UIManager.put(MENU_BAR_UI_NAME, macMenuBarUI);
          }
        }

        UIManagerUtilities.scaleFontIAppropriate();

        UIManager.put("ScrollBar.width", 13);
        UIManager.put("ScrollBar.incrementButtonGap", 0);
        UIManager.put("ScrollBar.decrementButtonGap", 0);

        ConsistentMouseDragEventQueue.pushIfAppropriate();

        LaunchConfiguration launchConfiguration = LaunchConfiguration.parse(args);

        JFrame rootFrame = WindowStack.getRootFrame();
        rootFrame.setLocation(launchConfiguration.getXLocation(), launchConfiguration.getYLocation());
        rootFrame.setSize(launchConfiguration.getWidth(), launchConfiguration.getHeight());

        if (launchConfiguration.isMaximizationDesired()) {
          rootFrame.setExtendedState(rootFrame.getExtendedState() | Frame.MAXIMIZED_BOTH);
        }
        if (launchConfiguration.getLocaleString() != null) {
          System.setProperty("org.alice.ide.locale", launchConfiguration.getLocaleString());
          String localeTest = System.getProperty("org.alice.ide.locale");
          System.out.println(localeTest);
        }

        loadClassInfos();
        StageIDE ide = new StageIDE(crashDetector);
        if (launchConfiguration.getProjectFile() != null) {
          if (launchConfiguration.getProjectFile().exists()) {
            ide.setProjectFileToLoadOnWindowOpened(launchConfiguration.getProjectFile());
          } else {
            Logger.warning("file does not exist:", launchConfiguration.getProjectFile());
          }
        }
        ide.initialize(args);
        ide.getDocumentFrame().getFrame().setVisible(true);
        heapMonitor = new HeapWatchDog();
      });
      // Call to initialize JavaFX
      launch(args);
    } catch (ProcessTerminationRequestedException request) {
      System.exit(request.getStatus());
    } finally {
      ProcessTerminator.setHandler(previous);
    }
  }

  static void requireGraphicalEnvironmentForDesktopLaunch(boolean isHeadless) {
    if (isHeadless) {
      throw new IllegalStateException("Alice desktop launch requires a graphical environment.");
    }
  }

  private static void loadClassInfos() {
    String json = TextFileUtilities.read(EntryPoint.class.getResourceAsStream("classinfos.json"));
    ObjectMapper mapper = new ObjectMapper();
    try {
      ClassInfoManager.addClassInfos(mapper.readValue(json, ClassInfo[].class));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
  }
}
