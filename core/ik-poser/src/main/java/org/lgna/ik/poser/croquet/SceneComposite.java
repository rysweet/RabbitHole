package org.lgna.ik.poser.croquet;

import org.lgna.croquet.SimpleComposite;
import org.lgna.ik.poser.croquet.views.ScenePanel;

import java.util.UUID;

public class SceneComposite extends SimpleComposite<ScenePanel> {
  public SceneComposite() {
    super(UUID.fromString("d34e5678-1cf9-41bd-9031-10e16cde2dd6"));
  }

  @Override
  protected ScenePanel createView() {
    return new ScenePanel(this);
  }
}
