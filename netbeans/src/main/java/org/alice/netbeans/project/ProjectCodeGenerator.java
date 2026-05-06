/*******************************************************************************
 * Copyright (c) 2006, 2016, Carnegie Mellon University. All rights reserved.
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

package org.alice.netbeans.project;

import edu.cmu.cs.dennisc.java.io.TextFileUtilities;
import edu.cmu.cs.dennisc.java.util.Lists;
import edu.cmu.cs.dennisc.java.util.logging.Logger;
import org.lgna.project.Project;
import org.lgna.project.VersionNotSupportedException;
import org.lgna.project.ast.AbstractDeclaration;
import org.lgna.project.ast.AbstractPackage;
import org.lgna.project.ast.JavaCodeGenerator;
import org.lgna.project.ast.ManagementLevel;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.io.IoUtilities;
import org.lgna.project.resource.ResourcesTypeWrapper;
import org.lgna.story.SProgram;
import org.lgna.story.SScene;
import org.lgna.story.ast.JavaCodeUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.modules.editor.indent.api.Reformat;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.text.NbDocument;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import javax.lang.model.SourceVersion;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Dennis Cosgrove
 */
public class ProjectCodeGenerator {

  private static void progress(ProgressHandle progressHandle, String prefix, FileObject fileObject, int workUnit) {
    if (progressHandle != null) {
      progressHandle.progress(prefix + fileObject.getNameExt(), workUnit);
      //for testing progress
      //ThreadUtilities.sleep(1000);
    }
  }

  public static Collection<FileObject> generateCode(File aliceProjectFile, File javaSrcDirectory, ProgressHandle progressHandle) throws IOException, VersionNotSupportedException {
    return generateCode(aliceProjectFile, javaSrcDirectory, progressHandle, true);
  }

  static Collection<FileObject> generateCode(
      File aliceProjectFile,
      File javaSrcDirectory,
      ProgressHandle progressHandle,
      boolean formatGeneratedFiles) throws IOException, VersionNotSupportedException {
    Project aliceProject = IoUtilities.readProject(aliceProjectFile);
    JavaCodeGenerator.Builder javaCodeGeneratorBuilder = JavaCodeUtilities.createJavaCodeGeneratorBuilder();
    //JavaCodeGenerator.Builder javaCodeGeneratorBuilder = new JavaCodeGenerator.Builder().isLambdaSupported(true);

    List<FileObject> filesToOpen = Lists.newLinkedList();
    List<FileObject> fileObjectsToFormat = Lists.newLinkedList();
    Set<NamedUserType> namedUserTypes = aliceProject.getNamedUserTypes();
    final Set<org.lgna.common.Resource> resources = aliceProject.getResources();
    ResourcesTypeWrapper resourcesTypeWrapper = null;
    if (!resources.isEmpty()) {
      resourcesTypeWrapper = new ResourcesTypeWrapper(aliceProject.getResources());
      namedUserTypes.add(resourcesTypeWrapper.getType());
    }

    ensureGeneratedDestinationFilesAreAvailable(javaSrcDirectory, namedUserTypes, resources, resourcesTypeWrapper);

    if (!resources.isEmpty()) {
      FileObject javaSrcDirectoryFileObject = FileUtil.toFileObject(javaSrcDirectory);
      if (javaSrcDirectoryFileObject == null || !javaSrcDirectoryFileObject.isFolder()) {
        throw new IOException("Java source directory is not available: " + javaSrcDirectory);
      }
      for (org.lgna.common.Resource resource : resources) {
        final String dstPath = resourcesTypeWrapper.getResourcePathForResource(resource);
        FileObject f = FileUtil.createData(javaSrcDirectoryFileObject, dstPath);
        if (f == null) {
          throw new IOException("Unable to create resource file: " + dstPath);
        }

        FileLock lock = f.lock();
        try {
          try (OutputStream os = f.getOutputStream(lock)) {
            os.write(resource.getData());
          }
        } finally {
          lock.releaseLock();
        }
      }
    }

    if (progressHandle != null) {
      progressHandle.switchToDeterminate(namedUserTypes.size());
    }
    int createWorkUnit = 0;
    for (NamedUserType type : namedUserTypes) {
      File file = getJavaSourceFileForType(javaSrcDirectory, type);
      final NetbeansJavaCodeGenerator generator = new NetbeansJavaCodeGenerator(javaCodeGeneratorBuilder);
      type.process(generator);
      String code = generator.getText();
      boolean isMarkedForOpen = false;
      if (!type.isAssignableTo(SProgram.class)) {
        if (type.isAssignableTo(SScene.class)) {
          isMarkedForOpen = true;
        } else {
          for (UserMethod method : type.methods) {
            if (method.managementLevel.getValue() == ManagementLevel.NONE) {
              isMarkedForOpen = true;
              break;
            }
          }
        }
      }

      TextFileUtilities.write(file, code);
      FileObject fileObject = FileUtil.toFileObject(file);
      fileObjectsToFormat.add(fileObject);

      if (isMarkedForOpen) {
        filesToOpen.add(fileObject);
      }
      progress(progressHandle, "create: ", fileObject, createWorkUnit);
      createWorkUnit++;
    }

    FileObject fileObject = generateLauncher(javaSrcDirectory);
    filesToOpen.add(fileObject);
    progress(progressHandle, "create: ", fileObject, createWorkUnit);

    if (formatGeneratedFiles) {
      formatGeneratedFiles(fileObjectsToFormat, progressHandle);
    }
    return filesToOpen;
  }

  private static void ensureGeneratedDestinationFilesAreAvailable(
      File javaSrcDirectory,
      Set<NamedUserType> namedUserTypes,
      Set<org.lgna.common.Resource> resources,
      ResourcesTypeWrapper resourcesTypeWrapper) throws IOException {
    Path sourceRoot = javaSrcDirectory.getCanonicalFile().toPath();
    Set<String> generatedSourceNames = new HashSet<>();
    Set<Path> generatedOutputPaths = new HashSet<>();
    List<Path> existingPaths = new ArrayList<>();

    generatedSourceNames.add(LAUNCHER_FILE_NAME);
    addGeneratedOutputPath(generatedOutputPaths, new File(javaSrcDirectory, LAUNCHER_FILE_NAME), sourceRoot);

    if (resourcesTypeWrapper != null) {
      for (org.lgna.common.Resource resource : resources) {
        addGeneratedOutputPath(
            generatedOutputPaths,
            new File(javaSrcDirectory, resourcesTypeWrapper.getResourcePathForResource(resource)),
            sourceRoot);
      }
    }

    for (NamedUserType type : namedUserTypes) {
      File file = getJavaSourceFileForType(javaSrcDirectory, type);
      if (!generatedSourceNames.add(file.getName())) {
        throw new IOException("Duplicate generated Java source file: " + file.getName());
      }
      validateUserAuthoredJavaIdentifiers(type);
      addGeneratedOutputPath(generatedOutputPaths, file, sourceRoot);
    }

    for (Path generatedOutputPath : generatedOutputPaths) {
      if (generatedOutputPath.toFile().exists()) {
        existingPaths.add(generatedOutputPath);
      }
    }
    if (!existingPaths.isEmpty()) {
      throw new IOException("Generated destination already exists: " + existingPaths.get(0));
    }
  }

  private static void addGeneratedOutputPath(Set<Path> generatedOutputPaths, File file, Path sourceRoot) throws IOException {
    Path generatedOutputPath = file.getCanonicalFile().toPath();
    if (!generatedOutputPath.startsWith(sourceRoot)) {
      throw new IOException("Generated output path escapes source directory: " + generatedOutputPath);
    }
    if (!generatedOutputPaths.add(generatedOutputPath)) {
      throw new IOException("Duplicate generated output file: " + sourceRoot.relativize(generatedOutputPath));
    }
  }

  private static void validateUserAuthoredJavaIdentifiers(NamedUserType type) throws IOException {
    for (AbstractDeclaration declaration : type.createDeclarationSet()) {
      if (declaration.isUserAuthored()
          && !(declaration instanceof AbstractPackage)
          && (declaration.getNamePropertyIfItExists() != null)
          && (declaration.getName() != null)) {
        validateJavaIdentifier(
            declaration.getName(),
            "Unsafe Alice declaration name for Java source generation");
      }
    }
  }

  private static File getJavaSourceFileForType(File javaSrcDirectory, NamedUserType type) throws IOException {
    String typeName = type.getName();
    validateJavaIdentifier(typeName, "Unsafe Alice type name for Java source generation");

    File sourceRoot = javaSrcDirectory.getCanonicalFile();
    File file = new File(sourceRoot, typeName + ".java").getCanonicalFile();
    if (!file.toPath().startsWith(sourceRoot.toPath())) {
      throw new IOException("Generated Java source path escapes source directory: " + file);
    }
    return file;
  }

  private static void validateJavaIdentifier(String identifier, String description) throws IOException {
    if ((identifier == null) || !SourceVersion.isIdentifier(identifier) || SourceVersion.isKeyword(identifier)) {
      throw new IOException(description + ": " + identifier);
    }
  }

  private static void formatGeneratedFiles(List<FileObject> fileObjectsToFormat, ProgressHandle progressHandle) {
    if (progressHandle != null) {
      progressHandle.switchToDeterminate(fileObjectsToFormat.size());
    }
    int formatWorkUnit = 0;
    for (FileObject fileObjectToFormat : fileObjectsToFormat) {
      try {
        DataObject dobj = DataObject.find(fileObjectToFormat);
        EditorCookie ec = dobj.getCookie(EditorCookie.class);
        final StyledDocument doc = ec.openDocument();
        final Reformat rf = Reformat.get(doc);
        rf.lock();
        try {
          NbDocument.runAtomicAsUser(doc, new Runnable() {
            @Override
            public void run() {
              try {
                rf.reformat(0, doc.getLength());
              } catch (BadLocationException ble) {
                Logger.throwable(ble);
              }
            }
          });
        } finally {
          rf.unlock();
        }
        ec.saveDocument();
        progress(progressHandle, "format: ", fileObjectToFormat, formatWorkUnit);
      } catch (BadLocationException ble) {
        Logger.throwable(ble);
      } catch (IOException ioe) {
        Logger.throwable(ioe);
      }
      formatWorkUnit++;
    }
  }

  static FileObject generateLauncher(File javaSrcDirectory) {
    File file = new File(javaSrcDirectory, LAUNCHER_FILE_NAME);
    TextFileUtilities.write(file, LAUNCHER_FILE);
    return FileUtil.toFileObject(file);
  }

  private static final String LAUNCHER_FILE_NAME = "AliceJavaFXLauncher.java";
  private static final String LAUNCHER_FILE =
"""
import javafx.application.Application;
import javafx.stage.Stage;

// If this project will not build and run make sure it is using
// a JDK that includes JavaFX, such as Bellsoft's Liberica JDK.
public class AliceJavaFXLauncher extends Application {
    private static String[] startingArgs;

    @Override
    public void start(Stage primaryStage) throws Exception {
        requirePrimaryStage(primaryStage);
        Thread thread = new Thread(() -> Program.main(startingArgs));
        thread.start();
    }

    private static void requirePrimaryStage(Stage primaryStage) {
        if (primaryStage == null) {
            throw new IllegalStateException(
                "JavaFX Application.start requires a primary Stage before Program.main can run.");
        }
    }

    public static void main(final String[] args) {
        startingArgs = args;
        launch(args);
    }
}""";
}
