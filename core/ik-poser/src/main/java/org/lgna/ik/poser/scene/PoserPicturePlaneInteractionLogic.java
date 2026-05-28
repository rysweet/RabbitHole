package org.lgna.ik.poser.scene;

import org.alice.math.immutable.Point3;
import org.alice.math.immutable.Ray;

final class PoserPicturePlaneInteractionLogic {
  enum Selection {
    FIRST,
    SECOND
  }

  private PoserPicturePlaneInteractionLogic() {
    throw new AssertionError();
  }

  static double getSphereRayIntersectionLength(Ray ray, Point3 center, double radius) {
    double dx = ray.direction().x() - ray.origin().x();
    double dy = ray.direction().y() - ray.origin().y();
    double dz = ray.direction().z() - ray.origin().z();
    double a = (dx * dx) + (dy * dy) + (dz * dz);
    double b = (2 * dx * (ray.origin().x() - center.x())) + (2 * dy * (ray.origin().y() - center.y())) + (2 * dz * (ray.origin().z() - center.z()));
    double c = ((center.x() * center.x()) + (center.y() * center.y()) + (center.z() * center.z()) + (ray.origin().x() * ray.origin().x()) + (ray.origin().y() * ray.origin().y()) + (ray.origin().z() * ray.origin().z()) + (-2 * ((center.x() * ray.origin().x()) + (center.y() * ray.origin().y()) + (center.z() * ray.origin().z())))) - (radius * radius);
    double t = (-b - Math.sqrt((b * b) - (4 * a * c))) / (2 * a);
    if (Double.isNaN(t) || (t < 0)) {
      return -1;
    }
    double intersectionX = ray.origin().x() + (t * dx);
    double intersectionY = ray.origin().y() + (t * dy);
    double intersectionZ = ray.origin().z() + (t * dz);
    return Math.sqrt((intersectionX * intersectionX) + (intersectionY * intersectionY) + (intersectionZ * intersectionZ));
  }

  static Selection pickPreferred(double oneCameraDistance, double twoCameraDistance, double distOne, double distTwo, double decisiveCameraThreshold) {
    double cameraDelta = oneCameraDistance - twoCameraDistance;
    if (Math.abs(cameraDelta) > decisiveCameraThreshold) {
      return oneCameraDistance < twoCameraDistance ? Selection.FIRST : Selection.SECOND;
    }
    if (oneCameraDistance < twoCameraDistance) {
      if (distOne < distTwo) {
        return Selection.FIRST;
      } else if ((distTwo * 2) < distOne) {
        return Selection.SECOND;
      } else {
        return Selection.FIRST;
      }
    } else if (twoCameraDistance < oneCameraDistance) {
      if (distTwo < distOne) {
        return Selection.SECOND;
      } else if ((distOne * 2) < distTwo) {
        return Selection.FIRST;
      } else {
        return Selection.SECOND;
      }
    }
    return distOne < distTwo ? Selection.FIRST : Selection.SECOND;
  }
}
