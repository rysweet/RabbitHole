package org.lgna.ik.poser.scene;

import org.alice.math.immutable.Ray;

final class PicturePlaneInteractionHelper {
  private PicturePlaneInteractionHelper() {
    throw new AssertionError();
  }

  static boolean hasPlaneDrag(double planeZ0) {
    return !Double.isNaN(planeZ0);
  }

  static boolean hasRayDrag(Ray ray) {
    return ray != null;
  }

  static boolean shouldSwitchToRay(boolean shiftDown, boolean isPlaneMode) {
    return shiftDown && isPlaneMode;
  }

  static boolean shouldSwitchToPlane(boolean shiftDown, boolean isRayMode) {
    return !shiftDown && isRayMode;
  }

  static double calculateRayT(double rayT0, double rayPixelY0, double currentPixelY, double pixelsToRayFactor) {
    return rayT0 + ((currentPixelY - rayPixelY0) * pixelsToRayFactor);
  }
}
