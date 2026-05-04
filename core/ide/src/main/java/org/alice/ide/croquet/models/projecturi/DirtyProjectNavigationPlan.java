package org.alice.ide.croquet.models.projecturi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class DirtyProjectNavigationPlan {
  enum PromptChoice {
    SAVE,
    DISCARD,
    CANCEL
  }

  enum Action {
    SAVE_CURRENT_PROJECT,
    BACK_UP_CURRENT_PROJECT,
    RUN_POST_CLEARANCE,
    CANCEL
  }

  private final List<Action> actions;

  private DirtyProjectNavigationPlan(List<Action> actions) {
    this.actions = Collections.unmodifiableList(actions);
  }

  static DirtyProjectNavigationPlan choose(boolean requiresClearance, PromptChoice promptChoice,
                                          boolean postClearanceRequested) {
    List<Action> actions = new ArrayList<>();

    if (requiresClearance) {
      if (promptChoice == null) {
        throw new IllegalArgumentException("promptChoice is required when clearance is needed");
      }
      switch (promptChoice) {
      case CANCEL:
        actions.add(Action.CANCEL);
        return new DirtyProjectNavigationPlan(actions);
      case SAVE:
        actions.add(Action.SAVE_CURRENT_PROJECT);
        break;
      case DISCARD:
        actions.add(Action.BACK_UP_CURRENT_PROJECT);
        break;
      default:
        throw new AssertionError(promptChoice);
      }
    }

    if (postClearanceRequested) {
      actions.add(Action.RUN_POST_CLEARANCE);
    }

    return new DirtyProjectNavigationPlan(actions);
  }

  List<Action> getActions() {
    return actions;
  }
}
