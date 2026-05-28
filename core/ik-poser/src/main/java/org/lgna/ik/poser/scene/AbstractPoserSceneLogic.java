package org.lgna.ik.poser.scene;

final class AbstractPoserSceneLogic {
  private AbstractPoserSceneLogic() {
    throw new AssertionError();
  }

  static double computeBackupAmount(double width, double height) {
    return Math.max(width, height) * 2.5;
  }
}
