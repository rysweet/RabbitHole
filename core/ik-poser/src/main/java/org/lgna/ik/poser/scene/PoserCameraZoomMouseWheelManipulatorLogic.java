package org.lgna.ik.poser.scene;

import org.alice.math.immutable.Vector3;

final class PoserCameraZoomMouseWheelManipulatorLogic {
  private PoserCameraZoomMouseWheelManipulatorLogic() {
    throw new AssertionError();
  }

  static double computeIdealBackwardY(double x, boolean useUpCurve, double lateralDistanceForUp, double distanceUpScale, double coefficient, double flatteningFactor, double targetLowDownAmount) {
    double y = 0;
    if (x > 0) {
      y = coefficient * 2 * x * flatteningFactor;
    } else if (useUpCurve && (x > (-lateralDistanceForUp))) {
      double shiftedX = x + lateralDistanceForUp;
      y = ((distanceUpScale * shiftedX * Math.PI) / lateralDistanceForUp) * (-Math.sin((shiftedX * Math.PI) / lateralDistanceForUp));
    }
    return Math.max(y, targetLowDownAmount);
  }

  static double computeHeightForX(double x, boolean useUpCurve, double lateralDistanceForUp, double distanceUpScale, double originalY, double targetLowHeight, double coefficient, double inflectionY) {
    if (x < 0) {
      if (useUpCurve) {
        if (x < -lateralDistanceForUp) {
          return targetLowHeight;
        }
        double shiftedX = x + lateralDistanceForUp;
        double cosineValue = distanceUpScale * Math.cos((shiftedX * Math.PI) / lateralDistanceForUp);
        return cosineValue + distanceUpScale + originalY;
      }
      return targetLowHeight;
    }
    return coefficient * x + inflectionY;
  }

  static double computeOrthographicZoomAmount(int direction, double zoomPerWheelClick) {
    return zoomPerWheelClick * direction;
  }

  static double computeClampedOrthographicZoom(double currentZoom, double zoomAmount, double minZoom, double maxZoom) {
    return Math.max(minZoom, Math.min(maxZoom, currentZoom + zoomAmount));
  }

  static Vector3 interpolateNormalizedVector(Vector3 a, Vector3 b, double percent) {
    double x = a.x() + ((b.x() - a.x()) * percent);
    double y = a.y() + ((b.y() - a.y()) * percent);
    double z = a.z() + ((b.z() - a.z()) * percent);
    return new Vector3(x, y, z).normalized();
  }
}
