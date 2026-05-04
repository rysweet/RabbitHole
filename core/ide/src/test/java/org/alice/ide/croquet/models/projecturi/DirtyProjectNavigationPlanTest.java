package org.alice.ide.croquet.models.projecturi;

import org.junit.Test;

import java.util.List;

import static org.alice.ide.croquet.models.projecturi.DirtyProjectNavigationPlan.Action.BACK_UP_CURRENT_PROJECT;
import static org.alice.ide.croquet.models.projecturi.DirtyProjectNavigationPlan.Action.CANCEL;
import static org.alice.ide.croquet.models.projecturi.DirtyProjectNavigationPlan.Action.RUN_POST_CLEARANCE;
import static org.alice.ide.croquet.models.projecturi.DirtyProjectNavigationPlan.Action.SAVE_CURRENT_PROJECT;
import static org.alice.ide.croquet.models.projecturi.DirtyProjectNavigationPlan.PromptChoice.DISCARD;
import static org.alice.ide.croquet.models.projecturi.DirtyProjectNavigationPlan.PromptChoice.SAVE;
import static org.junit.Assert.assertEquals;

public class DirtyProjectNavigationPlanTest {
  @Test
  public void cleanNavigationRunsPostClearanceWithoutPromptAction() {
    DirtyProjectNavigationPlan plan = DirtyProjectNavigationPlan.choose(false, null, true);

    assertEquals(List.of(RUN_POST_CLEARANCE), plan.getActions());
  }

  @Test
  public void cancelDirtyNavigationStopsBeforeSaveDiscardOrNavigation() {
    DirtyProjectNavigationPlan plan = DirtyProjectNavigationPlan.choose(
        true, DirtyProjectNavigationPlan.PromptChoice.CANCEL, true);

    assertEquals(List.of(CANCEL), plan.getActions());
  }

  @Test
  public void saveDirtyNavigationRunsSaveBeforePostClearance() {
    DirtyProjectNavigationPlan plan = DirtyProjectNavigationPlan.choose(true, SAVE, true);

    assertEquals(List.of(SAVE_CURRENT_PROJECT, RUN_POST_CLEARANCE), plan.getActions());
  }

  @Test
  public void discardDirtyNavigationBacksUpThenRunsPostClearance() {
    DirtyProjectNavigationPlan plan = DirtyProjectNavigationPlan.choose(true, DISCARD, true);

    assertEquals(List.of(BACK_UP_CURRENT_PROJECT, RUN_POST_CLEARANCE), plan.getActions());
  }
}
