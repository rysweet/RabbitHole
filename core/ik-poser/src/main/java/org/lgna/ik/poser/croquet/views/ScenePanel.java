package org.lgna.ik.poser.croquet.views;

import org.lgna.croquet.views.BorderPanel;
import org.lgna.ik.poser.croquet.SceneComposite;
import org.lgna.story.SProgram;
import org.lgna.story.implementation.ProgramImp;

public class ScenePanel extends BorderPanel {
  public ScenePanel(SceneComposite composite) {
    super(composite);
  }

  public void initializeInAwtContainer(SProgram program) {
    ProgramImp programImp = program.getImplementation();
    programImp.initializeInAwtContainer(this.getAwtComponent());
  }
}
