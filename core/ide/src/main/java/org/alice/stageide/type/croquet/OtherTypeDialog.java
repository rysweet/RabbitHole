/*******************************************************************************
 * Copyright (c) 2006, 2015, Carnegie Mellon University. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Products derived from the software may not be called "Alice", nor may
 *    "Alice" appear in their name, without prior written permission of
 *    Carnegie Mellon University.
 *
 * 4. All advertising materials mentioning features or use of this software must
 *    display the following acknowledgement: "This product includes software
 *    developed by Carnegie Mellon University"
 *
 * 5. The gallery of art assets and animations provided with this software is
 *    contributed by Electronic Arts Inc. and may be used for personal,
 *    non-commercial, and academic use only. Redistributions of any program
 *    source code that utilizes The Sims 2 Assets must also retain the copyright
 *    notice, list of conditions and the disclaimer contained in
 *    The Alice 3.0 Art Gallery License.
 *
 * DISCLAIMER:
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 * ANY AND ALL EXPRESS, STATUTORY OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY,  FITNESS FOR A
 * PARTICULAR PURPOSE, TITLE, AND NON-INFRINGEMENT ARE DISCLAIMED. IN NO EVENT
 * SHALL THE AUTHORS, COPYRIGHT OWNERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING FROM OR OTHERWISE RELATING TO
 * THE USE OF OR OTHER DEALINGS WITH THE SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package org.alice.stageide.type.croquet;

import edu.cmu.cs.dennisc.java.util.InitializingIfAbsentMap;
import edu.cmu.cs.dennisc.java.util.Maps;
import org.alice.ide.ProjectStack;
import org.alice.stageide.type.croquet.data.SceneFieldListData;
import org.alice.stageide.type.croquet.views.OtherTypeDialogPane;
import org.lgna.croquet.*;
import org.lgna.croquet.Element;
import org.lgna.croquet.data.ListData;
import org.lgna.croquet.event.ValueEvent;
import org.lgna.croquet.event.ValueListener;
import org.lgna.croquet.history.UserActivity;
import org.lgna.croquet.simple.SimpleApplication;
import org.lgna.croquet.triggers.NullTrigger;
import org.lgna.croquet.views.Panel;
import org.lgna.project.Project;
import org.lgna.project.ast.*;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SModel;
import org.lgna.story.SThing;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Dennis Cosgrove
 */
public class OtherTypeDialog extends ValueCreatorInputDialogCoreComposite<Panel, AbstractType<?, ?, ?>> {
  private static class SingletonHolder {
    private static final OtherTypeDialog instance = new OtherTypeDialog();
  }

  public static OtherTypeDialog getInstance() {
    return SingletonHolder.instance;
  }

  private class ValueCreatorForRootFilterType extends ValueCreator<AbstractType<?, ?, ?>> {
    public ValueCreatorForRootFilterType(JavaType rootFilterType) {
      super(UUID.fromString("84922129-0658-47af-8e32-f2476f030e41"));
      this.rootFilterType = rootFilterType;
    }

    @Override
    protected Class<? extends Element> getClassUsedForLocalization() {
      return OtherTypeDialog.class;
    }

    @Override
    protected AbstractType<?, ?, ?> createValue(UserActivity userActivity) {
      OtherTypeDialog.this.initializeRootFilterType(this.rootFilterType);
      return OtherTypeDialog.this.createValue(userActivity);
    }

    private final JavaType rootFilterType;
  }

  private final InitializingIfAbsentMap<JavaType, ValueCreator<AbstractType<?, ?, ?>>> mapTypeToValueCreator = Maps.newInitializingIfAbsentHashMap();

  private final Map<AbstractType<?, ?, ?>, TypeNode> typeNodeMap = Maps.newHashMap();

  private final TypeTreeState typeTreeState = new TypeTreeState();
  private final StringValue descriptionText = new HtmlStringValue(UUID.fromString("5417d9ee-bbe5-457b-aa63-1e5d0958ae1f")) {
  };
  private final SceneFieldListData sceneFieldListData = new SceneFieldListData();
  private final MultipleSelectionListState<UserField> sceneFieldsState = new SceneFieldsState(sceneFieldListData);

  private final AssignableTab assignableTab = new AssignableTab(this);
  private final ContainsTab containsTab = new ContainsTab(this);
  private final ImmutableDataTabState<?> tabState = this.createImmutableTabState("tabState", 0, this.assignableTab, this.containsTab);

  private final ErrorStatus noSelectionError = this.createErrorStatus("noSelectionError");
  private final Status notAssignableError = new Status() {
    @Override
    public boolean isGoodToGo() {
      return false;
    }

    @Override
    public String getText() {
      return OtherTypeDialogLogic.getNotAssignableErrorText(rootFilterType);
    }
  };

  private JavaType rootFilterType;

  private boolean isInTheMidstOfLowestCommonAncestorSetting;
  private final ValueListener<TypeNode> typeListener = e -> {
    TypeNode nextValue = e.getNextValue();
    handleTypeChange(nextValue != null ? nextValue.getType() : null);
  };

  private final ValueListener<List<UserField>> sceneFieldListener = new ValueListener<>() {
    @Override
    public void valueChanged(ValueEvent<List<UserField>> e) {
      List<UserField> fields = e.getNextValue();
      TypeNode sharedNode = OtherTypeDialogLogic.getSharedTypeNode(fields, typeNodeMap);
      isInTheMidstOfLowestCommonAncestorSetting = true;
      try {
        typeTreeState.setValueTransactionlessly(sharedNode);
      } finally {
        isInTheMidstOfLowestCommonAncestorSetting = false;
      }
    }
  };

  private OtherTypeDialog() {
    super(UUID.fromString("58d24fb6-a6f5-4ad9-87b0-dfb5e9e4de41"));
  }

  public ValueCreator<AbstractType<?, ?, ?>> getValueCreator(JavaType rootType) {
    return this.mapTypeToValueCreator.get(rootType, ValueCreatorForRootFilterType::new);
  }

  public ValueCreator<AbstractType<?, ?, ?>> getValueCreator(Class<? extends SThing> rootCls) {
    return this.getValueCreator(JavaType.getInstance(rootCls));
  }

  private void initializeRootFilterType(JavaType rootFilterType) {
    this.rootFilterType = rootFilterType;
  }

  @Override
  protected Integer getWiderGoldenRatioSizeFromHeight() {
    return 600;
  }

  public TabState<?, ?> getTabState() {
    return this.tabState;
  }

  public MultipleSelectionListState<UserField> getSceneFieldsState() {
    return this.sceneFieldsState;
  }

  public SingleSelectTreeState<TypeNode> getTypeTreeState() {
    return this.typeTreeState;
  }

  public StringValue getDescriptionText() {
    return this.descriptionText;
  }

  @Override
  protected AbstractType<?, ?, ?> createValue() {
    TypeNode typeNode = this.typeTreeState.getValue();
    if (typeNode != null) {
      return typeNode.getType();
    } else {
      return null;
    }
  }

  @Override
  protected Status getStatusPreRejectorCheck() {
    TypeNode typeNode = this.typeTreeState.getValue();
    if (typeNode == null) {
      return this.noSelectionError;
    }
    return OtherTypeDialogLogic.isSelectionAssignable(this.rootFilterType, typeNode) ? IS_GOOD_TO_GO_STATUS : this.notAssignableError;
  }

  private TypeNode build(AbstractType<?, ?, ?> type) {
    assert type != null;
    TypeNode typeNode = typeNodeMap.get(type);
    if (typeNode == null) {
      typeNode = new TypeNode(type);
      typeNodeMap.put(type, typeNode);
      AbstractType<?, ?, ?> superType = type.getSuperType();
      TypeNode superTypeNode = build(superType);
      superTypeNode.add(typeNode);
    }
    return typeNode;
  }

  @Override
  public void handlePreActivation() {
    Project project = ProjectStack.peekProject();
    Iterable<NamedUserType> types = project.getNamedUserTypes();
    typeNodeMap.clear();

    JavaType rootType = JavaType.getInstance(SThing.class);
    TypeNode rootNode = new TypeNode(rootType);
    typeNodeMap.put(rootType, rootNode);
    for (NamedUserType type : types) {
      if (this.rootFilterType.isAssignableFrom(type)) {
        build(type);
      }
    }
    this.sceneFieldListData.refresh();

    // handle JavaType scene fields
    synchronized (this.sceneFieldListData) {
      final int N = this.sceneFieldListData.getItemCount();
      for (int i = 0; i < N; i++) {
        UserField field = this.sceneFieldListData.getItemAt(i);
        AbstractType<?, ?, ?> valueType = field.getValueType();
        if (valueType instanceof JavaType) {
          build(valueType);
        }
      }
    }

    this.typeTreeState.setRoot(rootNode);
    this.typeTreeState.addAndInvokeNewSchoolValueListener(this.typeListener);
    this.sceneFieldsState.addNewSchoolValueListener(this.sceneFieldListener);

    this.containsTab.getMemberListData().connect(rootNode);
    super.handlePreActivation();
  }

  @Override
  public void handlePostDeactivation() {
    this.containsTab.getMemberListData().disconnect();
    this.sceneFieldsState.removeNewSchoolValueListener(this.sceneFieldListener);
    this.typeTreeState.removeNewSchoolValueListener(this.typeListener);
    super.handlePostDeactivation();
  }

  @Override
  protected Panel createView() {
    return new OtherTypeDialogPane(this);
  }

  private static boolean isInclusionDesired(AbstractMember member) {
    return OtherTypeDialogLogic.isInclusionDesired(member);
  }

  private static void appendMembers(StringBuilder sb, AbstractType<?, ?, ?> type, boolean isSelected) {
    OtherTypeDialogLogic.appendMembers(sb, type, isSelected);
  }

  private void handleTypeChange(AbstractType<?, ?, ?> type) {
    descriptionText.setText(OtherTypeDialogLogic.createDescriptionHtml(type));

    ListData<UserField> data = sceneFieldsState.getData();

    this.getView().repaint();
    if (!this.isInTheMidstOfLowestCommonAncestorSetting) {
      this.sceneFieldsState.setValue(OtherTypeDialogLogic.filterAssignableFields(type, data));
    }
  }

  public TypeNode getTypeNodeFor(AbstractType<?, ?, ?> nextValue) {
    return nextValue != null ? typeNodeMap.get(nextValue) : null;
  }

  public static void main(String[] args) throws Exception {
    new SimpleApplication();
    Project project = IoUtilities.readProject(args[0]);
    ProjectStack.pushProject(project);
    OtherTypeDialog.getInstance().getValueCreator(SModel.class).fire(NullTrigger.createUserActivity());
    ProcessTerminator.requestExit(0);
  }
}
