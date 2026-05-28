package org.lgna.ik.poser.animation;

import java.util.List;

final class TimeLineMath {
  private TimeLineMath() {
    throw new AssertionError();
  }

  static double clampCurrentTime(double desiredTime, double endTime) {
    return Math.max(0.0, Math.min(desiredTime, endTime));
  }

  static KeyFrameWindow findKeyFrameWindow(List<KeyFrameData> keyFrames, double desiredTime) {
    KeyFrameData before = null;
    KeyFrameData after = null;

    for (KeyFrameData data : keyFrames) {
      if (desiredTime >= data.getEventTime()) {
        before = data;
      } else {
        after = data;
        break;
      }
    }

    return new KeyFrameWindow(before, after);
  }

  static double calculateInterpolationPortion(double previousTime, double nextTime, double targetTime) {
    if (nextTime == previousTime) {
      return 0.0;
    }
    return (targetTime - previousTime) / (nextTime - previousTime);
  }

  static final class KeyFrameWindow {
    private final KeyFrameData before;
    private final KeyFrameData after;

    private KeyFrameWindow(KeyFrameData before, KeyFrameData after) {
      this.before = before;
      this.after = after;
    }

    KeyFrameData getBefore() {
      return this.before;
    }

    KeyFrameData getAfter() {
      return this.after;
    }
  }
}
