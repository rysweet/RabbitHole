/*******************************************************************************
 * Copyright (c) 2018 Carnegie Mellon University. All rights reserved.
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
package org.lgna.project.io;

import edu.cmu.cs.dennisc.java.io.InputStreamUtilities;
import edu.cmu.cs.dennisc.java.util.zip.ByteArrayDataSource;
import edu.cmu.cs.dennisc.java.util.zip.DataSource;
import org.alice.tweedle.file.Manifest;
import org.alice.tweedle.file.ManifestEncoderDecoder;
import org.alice.tweedle.file.ProjectManifest;
import org.lgna.project.Project;
import org.lgna.project.ProjectVersion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Static utilities for reading and creating JSON project manifest and version entries. */
final class JsonProjectManifest {
  private JsonProjectManifest() {
  }

  static <M extends Manifest> M readManifest(
      ZipEntryContainer container,
      Class<M> manifestClass) throws IOException {
    InputStream is = container.getInputStream(ProjectIo.MANIFEST_ENTRY_NAME);
    if (is == null) {
      return null;
    }
    try (InputStream manifestStream = is) {
      byte[] manifestBytes = InputStreamUtilities.getBytes(manifestStream);
      try {
        return ManifestEncoderDecoder.fromJsonOrThrow(
            new String(manifestBytes, StandardCharsets.UTF_8),
            manifestClass);
      } catch (IOException e) {
        throw new IOException("Unable to read " + ProjectIo.MANIFEST_ENTRY_NAME, e);
      }
    }
  }

  static String manifestName(Manifest manifest) {
    return (manifest == null) ? null : manifest.getName();
  }

  static boolean hasNoManifestName(Manifest manifest) {
    String name = manifestName(manifest);
    return (name == null) || name.isEmpty();
  }

  static Project.SceneCameraType sceneCameraType(ProjectManifest manifest) {
    if ((manifest == null) || (manifest.projectStructure == null) || (manifest.projectStructure.sceneCameraType == null)) {
      return Project.SceneCameraType.WindowCamera;
    }
    return manifest.projectStructure.sceneCameraType;
  }

  static DataSource versionDataSource() {
    return new ByteArrayDataSource(ProjectIo.VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
  }

  static DataSource manifestDataSource(Manifest manifest) {
    return new ByteArrayDataSource(ProjectIo.MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
  }
}
