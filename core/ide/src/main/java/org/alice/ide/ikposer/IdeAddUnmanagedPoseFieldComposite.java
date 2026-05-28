package org.alice.ide.ikposer;

import edu.cmu.cs.dennisc.java.util.InitializingIfAbsentMap;
import edu.cmu.cs.dennisc.java.util.Maps;
import org.alice.ide.ast.declaration.AddFieldComposite;
import org.alice.ide.ast.declaration.views.AddFieldView;
import org.alice.ide.croquet.edits.ast.DeclareFieldEdit;
import org.alice.ide.croquet.edits.ast.DeclareNonGalleryFieldEdit;
import org.alice.ide.croquet.models.ui.preferences.IsNullAllowedForFieldInitializers;
import org.lgna.croquet.history.UserActivity;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.ManagementLevel;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserType;
import org.lgna.story.Pose;

import java.util.UUID;

final class IdeAddUnmanagedPoseFieldComposite extends AddFieldComposite {
  private static final InitializingIfAbsentMap<NamedUserType, IdeAddUnmanagedPoseFieldComposite> MAP = Maps.newInitializingIfAbsentHashMap();

  static IdeAddUnmanagedPoseFieldComposite getInstance(NamedUserType declaringType) {
    return MAP.get(declaringType, IdeAddUnmanagedPoseFieldComposite::new);
  }

  private IdeAddUnmanagedPoseFieldComposite(NamedUserType declaringType) {
    super(UUID.fromString("882dc293-d176-48c6-9b42-abc15c734779"), new FieldDetailsBuilder()
        .isFinal(ApplicabilityStatus.APPLICABLE_BUT_NOT_DISPLAYED, true)
        .valueComponentType(ApplicabilityStatus.DISPLAYED, JavaType.getInstance(Pose.class))
        .valueIsArrayType(ApplicabilityStatus.APPLICABLE_BUT_NOT_DISPLAYED, false)
        .initializer(ApplicabilityStatus.EDITABLE, null)
        .build());
    this.declaringType = declaringType;
  }

  @Override
  protected boolean isNullAllowedForInitializer() {
    return IsNullAllowedForFieldInitializers.getInstance().getValue();
  }

  @Override
  public NamedUserType getDeclaringType() {
    return this.declaringType;
  }

  @Override
  protected Expression getInitializerInitialValue() {
    return this.initializerInitialValue;
  }

  void setInitializerInitialValue(Expression initializerInitialValue) {
    this.initializerInitialValue = initializerInitialValue;
  }

  @Override
  protected boolean isFieldFinal() {
    return this.getIsFinalState().getValue();
  }

  @Override
  protected ManagementLevel getManagementLevel() {
    return ManagementLevel.NONE;
  }

  @Override
  protected DeclareFieldEdit createEdit(UserActivity step, UserType<?> declaringType, UserField field) {
    return new DeclareNonGalleryFieldEdit(step, declaringType, field);
  }

  @Override
  protected AddFieldView createView() {
    return new AddFieldView(this);
  }

  private final NamedUserType declaringType;
  private Expression initializerInitialValue;
}
