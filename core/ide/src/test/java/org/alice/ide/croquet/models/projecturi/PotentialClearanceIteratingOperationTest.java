package org.alice.ide.croquet.models.projecturi;

import org.junit.Test;
import org.lgna.croquet.Application;
import org.lgna.croquet.Triggerable;
import org.lgna.croquet.history.UserActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PotentialClearanceIteratingOperationTest {
  @Test
  public void successfulSaveContinuesToPostClearanceAndFinishesParentActivity() {
    List<String> fired = new ArrayList<>();
    PotentialClearanceIteratingOperation operation = operationWithSteps(
        finishStep("save", fired), finishStep("postClearance", fired));
    UserActivity activity = new UserActivity();

    operation.performInActivity(activity);

    assertEquals(List.of("save", "postClearance"), fired);
    assertTrue(activity.isSuccessfullyCompleted());
    assertFalse(activity.isCanceled());
  }

  @Test
  public void failedSaveCancelsBeforePostClearance() {
    List<String> fired = new ArrayList<>();
    PotentialClearanceIteratingOperation operation = operationWithSteps(
        cancelStep("save", fired), finishStep("postClearance", fired));
    UserActivity activity = new UserActivity();

    operation.performInActivity(activity);

    assertEquals(List.of("save"), fired);
    assertFalse(activity.isSuccessfullyCompleted());
    assertTrue(activity.isCanceled());
  }

  private static PotentialClearanceIteratingOperation operationWithSteps(Triggerable... steps) {
    return new PotentialClearanceIteratingOperation(
        Application.APPLICATION_UI_GROUP, UUID.fromString("68bd6268-6f7e-4671-92d2-d927b91019e1"), null) {
      @Override
      protected List<Triggerable> createIteratingData() {
        return List.of(steps);
      }
    };
  }

  private static Triggerable finishStep(String name, List<String> fired) {
    return activity -> {
      fired.add(name);
      activity.finish();
    };
  }

  private static Triggerable cancelStep(String name, List<String> fired) {
    return activity -> {
      fired.add(name);
      activity.cancel();
    };
  }
}
