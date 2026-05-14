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

package org.lgna.story.implementation.alice;

import edu.cmu.cs.dennisc.codec.BinaryDecoder;
import edu.cmu.cs.dennisc.codec.BinaryEncoder;
import edu.cmu.cs.dennisc.codec.InputStreamBinaryDecoder;
import edu.cmu.cs.dennisc.codec.OutputStreamBinaryEncoder;
import edu.cmu.cs.dennisc.codec.ReferenceableBinaryEncodableAndDecodable;
import edu.cmu.cs.dennisc.image.ImageUtilities;
import edu.cmu.cs.dennisc.java.io.FileUtilities;
import edu.cmu.cs.dennisc.java.util.Maps;
import edu.cmu.cs.dennisc.java.util.logging.Logger;
import edu.cmu.cs.dennisc.scenegraph.Appearance;
import edu.cmu.cs.dennisc.scenegraph.Geometry;
import edu.cmu.cs.dennisc.scenegraph.Joint;
import edu.cmu.cs.dennisc.scenegraph.SkeletonVisual;
import edu.cmu.cs.dennisc.scenegraph.TexturedAppearance;
import edu.cmu.cs.dennisc.scenegraph.WeightedMesh;
import edu.cmu.cs.dennisc.scenegraph.qa.Problem;
import edu.cmu.cs.dennisc.scenegraph.qa.QualityAssuranceUtilities;
import edu.cmu.cs.dennisc.texture.BufferedImageTexture;
import edu.cmu.cs.dennisc.texture.Texture;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.AxisAlignedBox;
import org.alice.math.immutable.Matrix3x3;
import org.alice.math.immutable.UnitQuaternion;
import org.lgna.story.resources.JointId;
import org.lgna.story.resources.ModelResource;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Binary codec operations, cached resource loading, and skeleton operations.
 * Extracted from AliceResourceUtilities to separate concerns.
 *
 * @see AliceResourceUtilities
 */
class ModelResourceLoader {

  private static final Map<URL, SkeletonVisual> urlToVisualMap = Maps.newHashMap();
  private static final Map<URL, TexturedAppearance[]> urlToTextureMap = Maps.newHashMap();

  private ModelResourceLoader() {
    throw new AssertionError();
  }

  static SkeletonVisual decodeVisual(URL url) {
    try (InputStream is = url.openStream()) {
      BinaryDecoder decoder = new InputStreamBinaryDecoder(is);
      return decoder.decodeReferenceableBinaryEncodableAndDecodable(new HashMap<Integer, ReferenceableBinaryEncodableAndDecodable>());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  static TexturedAppearance[] decodeTexture(URL url) {
    try (InputStream is = url.openStream()) {
      BinaryDecoder decoder = new InputStreamBinaryDecoder(is);
      TexturedAppearance[] rv = decoder.decodeReferenceableBinaryEncodableAndDecodableArray(TexturedAppearance.class, new HashMap<Integer, ReferenceableBinaryEncodableAndDecodable>());
      for (TexturedAppearance ta : rv) {
        correctDimensions(ta);
        ((BufferedImageTexture) ta.diffuseColorTexture.getValue()).directSetMipMappingDesired(false);
      }
      return rv;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  // Stretch images to power of two to fix rendering on certain Mac graphics configurations.
  private static void correctDimensions(TexturedAppearance ta) {
    Texture texture = ta.diffuseColorTexture.getValue();
    if (texture instanceof BufferedImageTexture buffTexture) {
      buffTexture.setBufferedImage(ImageUtilities.stretchToPowersOfTwo(buffTexture.getBufferedImage()));
    }
  }

  static void encodeVisual(final SkeletonVisual toSave, OutputStream os) throws IOException {
    BinaryEncoder encoder = new OutputStreamBinaryEncoder(os);
    encoder.encode(toSave, new HashMap<ReferenceableBinaryEncodableAndDecodable, Integer>());
    encoder.flush();
  }

  static void encodeVisual(final SkeletonVisual toSave, File file) throws IOException {
    FileUtilities.createParentDirectoriesIfNecessary(file);
    if (!file.exists()) {
      file.createNewFile();
    }
    try (FileOutputStream fos = new FileOutputStream(file)) {
      encodeVisual(toSave, fos);
    }
  }

  static void encodeTexture(final TexturedAppearance[] toSave, OutputStream os) throws IOException {
    BinaryEncoder encoder = new OutputStreamBinaryEncoder(os);
    encoder.encode(toSave, new HashMap<ReferenceableBinaryEncodableAndDecodable, Integer>());
    encoder.flush();
  }

  static void encodeTexture(final TexturedAppearance[] toSave, File file) throws IOException {
    FileUtilities.createParentDirectoriesIfNecessary(file);
    if (!file.exists()) {
      file.createNewFile();
    }
    try (FileOutputStream fos = new FileOutputStream(file)) {
      encodeTexture(toSave, fos);
    }
  }

  static SkeletonVisual getVisual(ModelResource resource) {
    URL resourceURL = ResourceTextureManager.getVisualURL(resource);
    SkeletonVisual cached = urlToVisualMap.get(resourceURL);
    if (cached != null || urlToVisualMap.containsKey(resourceURL)) {
      return cached;
    }
    SkeletonVisual visual = decodeVisual(resourceURL);
    List<Problem> problems = QualityAssuranceUtilities.inspect(visual);
    if (!problems.isEmpty()) {
      Logger.errln(resourceURL);
      for (Problem problem : problems) {
        Logger.errln(problem);
      }
    }
    urlToVisualMap.put(resourceURL, visual);
    return visual;
  }

  static SkeletonVisual getVisualCopy(ModelResource resource) {
    SkeletonVisual original = getVisual(resource);
    return createCopy(original);
  }

  static TexturedAppearance[] getTexturedAppearances(ModelResource resource) {
    URL resourceURL = ResourceTextureManager.getTextureURL(resource);
    TexturedAppearance[] cached = urlToTextureMap.get(resourceURL);
    if (cached != null || urlToTextureMap.containsKey(resourceURL)) {
      return cached;
    }
    TexturedAppearance[] texture = decodeTexture(resourceURL);
    urlToTextureMap.put(resourceURL, texture);
    return texture;
  }

  static SkeletonVisual createCopy(SkeletonVisual sgOriginal) {
    Geometry[] sgGeometries = sgOriginal.geometries.getValue();
    TexturedAppearance[] sgTextureAppearances = sgOriginal.textures.getValue();
    WeightedMesh[] sgWeightedMeshes = sgOriginal.weightedMeshes.getValue();
    WeightedMesh[] sgDefaultPoseWeightedMeshes = sgOriginal.defaultPoseWeightedMeshes.getValue();
    boolean hasDefaultPoseWeightedMeshes = sgOriginal.hasDefaultPoseWeightedMeshes.getValue();
    Joint sgSkeletonRoot = sgOriginal.skeleton.getValue();
    AxisAlignedBox bbox = sgOriginal.baseBoundingBox.getValue();
    Matrix3x3 scaleCopy = sgOriginal.scale.getValue();
    Appearance sgFrontAppearanceCopy;
    if (sgOriginal.frontFacingAppearance.getValue() != null) {
      sgFrontAppearanceCopy = (Appearance) sgOriginal.frontFacingAppearance.getValue().newCopy();
    } else {
      sgFrontAppearanceCopy = null;
    }
    Appearance sgBackAppearanceCopy;
    if (sgOriginal.backFacingAppearance.getValue() != null) {
      sgBackAppearanceCopy = (Appearance) sgOriginal.backFacingAppearance.getValue().newCopy();
    } else {
      sgBackAppearanceCopy = null;
    }

    SkeletonVisual rv = new SkeletonVisual();
    final Joint sgSkeletonRootCopy;
    if (sgSkeletonRoot != null) {
      sgSkeletonRootCopy = (Joint) sgSkeletonRoot.newCopy();
    } else {
      sgSkeletonRootCopy = null;
    }

    rv.skeleton.setValue(sgSkeletonRootCopy);
    rv.geometries.setValue(sgGeometries);
    rv.weightedMeshes.setValue(sgWeightedMeshes);
    rv.defaultPoseWeightedMeshes.setValue(sgDefaultPoseWeightedMeshes);
    rv.hasDefaultPoseWeightedMeshes.setValue(hasDefaultPoseWeightedMeshes);
    rv.textures.setValue(sgTextureAppearances);
    rv.frontFacingAppearance.setValue(sgFrontAppearanceCopy);
    rv.backFacingAppearance.setValue(sgBackAppearanceCopy);
    rv.baseBoundingBox.setValue(bbox);
    rv.isShowing.setValue(sgOriginal.isShowing.getValue());
    rv.scale.setValue(scaleCopy);
    return rv;
  }

  static SkeletonVisual createReplaceVisualElements(SkeletonVisual sgOriginal, ModelResource resource) {
    SkeletonVisual sgToReplaceWith = getVisual(resource);
    Geometry[] sgGeometries = sgToReplaceWith.geometries.getValue();
    WeightedMesh[] sgWeightedMeshes = sgToReplaceWith.weightedMeshes.getValue();
    AxisAlignedBox bbox = sgToReplaceWith.baseBoundingBox.getValue();
    Joint sgNewSkeletonRoot = sgToReplaceWith.skeleton.getValue();
    final Joint sgNewSkeleton;
    if (sgNewSkeletonRoot != null) {
      sgNewSkeleton = (Joint) sgNewSkeletonRoot.newCopy();
    } else {
      sgNewSkeleton = null;
    }
    if (sgNewSkeleton != null) {
      sgNewSkeleton.setParent(sgOriginal.getParent());
    }

    sgOriginal.skeleton.setValue(sgNewSkeleton);
    sgOriginal.geometries.setValue(sgGeometries);
    sgOriginal.weightedMeshes.setValue(sgWeightedMeshes);
    sgOriginal.baseBoundingBox.setValue(bbox);
    return sgOriginal;
  }

  static AffineMatrix4x4 getOriginalJointTransformation(ModelResource resource, JointId jointId) {
    SkeletonVisual sgOriginal = getVisual(resource);
    Joint sgSkeletonRoot = sgOriginal.skeleton.getValue();
    Joint sgJoint = sgSkeletonRoot.getJoint(jointId.toString());
    return sgJoint.getLocalTransformation();
  }

  static UnitQuaternion getOriginalJointOrientation(ModelResource resource, JointId jointId) {
    SkeletonVisual sgOriginal = getVisual(resource);
    Joint sgSkeletonRoot = sgOriginal.skeleton.getValue();
    Joint sgJoint = sgSkeletonRoot.getJoint(jointId.toString());
    return sgJoint.getLocalTransformation().orientation().asUnitQuaternion();
  }
}
