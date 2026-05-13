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
package org.alice.stageide.sceneeditor;

import edu.cmu.cs.dennisc.java.lang.ArrayUtilities;
import edu.cmu.cs.dennisc.java.util.Lists;
import edu.cmu.cs.dennisc.pattern.IsInstanceCrawler;
import org.alice.ide.IDE;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.AxisAlignedBox;
import org.alice.math.immutable.OrthogonalMatrix3x3;
import org.alice.math.immutable.Point3;
import org.alice.stageide.StageIDE;
import org.alice.stageide.modelresource.ClassResourceKey;
import org.lgna.project.ast.*;
import org.lgna.project.virtualmachine.UserInstance;
import org.lgna.story.*;
import org.lgna.story.implementation.alice.AliceResourceClassUtilities;
import org.lgna.story.resources.ModelResource;

import java.util.*;

/**
 * Delegate that handles field-related code generation for StorytellingSceneEditor.
 * Extracted from StorytellingSceneEditor to reduce its size (issue #528).
 */
class SceneFieldCodeGenerator {

  private final StorytellingSceneEditor editor;

  SceneFieldCodeGenerator(StorytellingSceneEditor editor) {
    this.editor = editor;
  }

  private void fillInAutomaticSetUpMethod(StatementListProperty bodyStatementsProperty, boolean isThis, AbstractField field, boolean getFullFieldState) {
    SetUpMethodGenerator.fillInAutomaticSetUpMethod(bodyStatementsProperty, isThis, field, editor.getInstanceInJavaVMForField(field), editor.getActiveSceneInstance(), getFullFieldState);
  }

  Statement getCurrentStateCodeForField(UserField field) {
    Statement rv = null;
    BlockStatement bs = new BlockStatement();
    fillInAutomaticSetUpMethod(bs.statements, false, field, true);

    Statement setVehicleStatement = null;
    for (Statement statement : bs.statements.getValue()) {
      if (isSetVehicleInvocation(statement)) {
        setVehicleStatement = statement;
        break;
      }
    }
    if (setVehicleStatement != null) {
      bs.statements.getValue().remove(setVehicleStatement);
    }
    DoTogether dt = new DoTogether(bs);
    if (setVehicleStatement != null) {
      DoInOrder dio = new DoInOrder(new BlockStatement(setVehicleStatement, dt));
      rv = dio;
    } else {
      rv = dt;
    }

    return rv;
  }

  void generateCodeForSetUp(StatementListProperty bodyStatementsProperty) {
    AbstractField sceneField = editor.getActiveSceneField();
    this.fillInAutomaticSetUpMethod(bodyStatementsProperty, true, sceneField, false);
    for (UserField userField : editor.getActiveSceneType().getDeclaredFields()) {
      if (userField != null) {
        if (userField.getManagementLevel() == ManagementLevel.MANAGED) {
          this.fillInAutomaticSetUpMethod(bodyStatementsProperty, false, userField, false);
        }
      }
    }
  }

  private Statement replaceReferencesInExpression(UserField fieldToReplace, UserField replacement, Statement statement) {
    IsInstanceCrawler<FieldAccess> crawler = IsInstanceCrawler.createInstance(FieldAccess.class);
    statement.crawl(crawler, CrawlPolicy.COMPLETE, null);

    for (FieldAccess fieldAccess : crawler.getList()) {
      AbstractField field = fieldAccess.field.getValue();
      if (field == fieldToReplace) {
        fieldAccess.field.setValue(replacement);
      }
    }
    return statement;
  }

  Statement[] getDoStatementsForCopyField(UserField fieldToCopy, UserField newField, AffineMatrix4x4 initialTransform) {
    Statement stateCodeStatement = this.getCurrentStateCodeForField(fieldToCopy);
    stateCodeStatement = replaceReferencesInExpression(fieldToCopy, newField, stateCodeStatement);

    List<BlockStatement> blockStatements = new LinkedList<BlockStatement>();
    if (stateCodeStatement instanceof BlockStatement statement) {
      blockStatements.add(statement);
    } else if (stateCodeStatement instanceof AbstractStatementWithBody body) {
      blockStatements.add(body.body.getValue());
    }
    while (!blockStatements.isEmpty()) {
      BlockStatement bs = blockStatements.removeFirst();
      Statement setVehicleStatement = null;
      Statement setPositionStatement = null;
      Statement setOrientationStatement = null;
      for (Statement s : bs.statements.getValue()) {
        if (s instanceof BlockStatement block) {
          blockStatements.add(block);
        } else if (s instanceof AbstractStatementWithBody body) {
          blockStatements.add(body.body.getValue());
        } else if (s instanceof ExpressionStatement expressionStatement) {
          Expression expression = expressionStatement.expression.getValue();
          if (expression instanceof MethodInvocation mi) {
            Method method = mi.method.getValue();
            if (method.getName().equalsIgnoreCase("setVehicle") && (mi.expression.getValue() instanceof FieldAccess)) {
              setVehicleStatement = s;
            } else if (method.getName().equalsIgnoreCase("setOrientationRelativeToVehicle") && (mi.expression.getValue() instanceof FieldAccess)) {
              setOrientationStatement = s;
            } else if (method.getName().equalsIgnoreCase("setPositionRelativeToVehicle") && (mi.expression.getValue() instanceof FieldAccess)) {
              setPositionStatement = s;
            }
          }
        }
      }
      if (setVehicleStatement != null) {
        bs.statements.getValue().remove(setVehicleStatement);
      }
      if (setPositionStatement != null) {
        bs.statements.getValue().remove(setPositionStatement);
      }
      if (setOrientationStatement != null) {
        bs.statements.getValue().remove(setOrientationStatement);
      }
    }

    Object toCopyInstance = editor.getInstanceInJavaVMForField(fieldToCopy);
    AbstractField toCopyVehicleField = null;
    if (toCopyInstance instanceof Rider rider) {
      SThing vehicleInstance = rider.getVehicle();
      toCopyVehicleField = editor.getFieldForInstanceInJavaVM(vehicleInstance);
    }
    Statement[] initializeStatements = SetUpMethodGenerator.getSetupStatementsForField(false, newField, editor.getActiveSceneInstance(), toCopyVehicleField, initialTransform);
    Statement[] statementsToReturn = new Statement[initializeStatements.length + 1];
    System.arraycopy(initializeStatements, 0, statementsToReturn, 0, initializeStatements.length);
    statementsToReturn[initializeStatements.length] = stateCodeStatement;
    return statementsToReturn;
  }

  Statement[] getDoStatementsForAddField(UserField field, AffineMatrix4x4 initialTransform) {
    if ((initialTransform == null) && field.getValueType().isAssignableTo(SModel.class)) {
      AbstractType<?, ?, ?> type = field.getValueType();
      JavaType javaType = type.getFirstEncounteredJavaType();
      Class<?> cls = javaType.getClassReflectionProxy().getReification();
      Class<? extends ModelResource> resourceCls = null;
      if (SModel.class.isAssignableFrom(cls)) {
        resourceCls = AliceResourceClassUtilities.getResourceClassForModelClass((Class<? extends SModel>) cls);
      }
      Point3 location;
      if (resourceCls != null) {
        ClassResourceKey childKey = new ClassResourceKey((Class<? extends ModelResource>) cls);
        AxisAlignedBox box = childKey.getBoundingBox();
        boolean shouldPlaceOnGround = childKey.getPlaceOnGround();
        double y = (box != null) && shouldPlaceOnGround ? -box.getXMinimum() : 0;
        location = new Point3(0, y, 0);
      } else {
        location = Point3.ORIGIN;
      }

      initialTransform = new AffineMatrix4x4(OrthogonalMatrix3x3.IDENTITY, location);
    }
    return SetUpMethodGenerator.getSetupStatementsForField(false, field, editor.getActiveSceneInstance(), null, initialTransform);
  }

  Statement[] getUndoStatementsForAddField(UserField field) {
    List<Statement> undoStatements = Lists.newLinkedList();

    undoStatements.add(SetUpMethodGenerator.createSetVehicleNullStatement(field));

    return ArrayUtilities.createArray(undoStatements, Statement.class);
  }

  Map<AbstractField, Statement> getRiders(UserField vehicle) {
    IDE.getActiveInstance().ensureProjectCodeUpToDate();
    UserInstance sceneAliceInstance = editor.getActiveSceneInstance();
    UserMethod generatedSetupMethod = sceneAliceInstance.getType().getDeclaredMethod(StageIDE.PERFORM_GENERATED_SET_UP_METHOD_NAME);
    Map<AbstractField, Statement> riders = new HashMap<>();
    for (Statement statement : generatedSetupMethod.body.getValue().statements.getValue()) {
      MethodInvocation setVehicleCall = asSetVehicleCall(statement);
      if (setVehicleCall != null && doesSetVehicleImplyVehicle(setVehicleCall, vehicle)) {
        FieldAccess riderAccess = (FieldAccess) setVehicleCall.expression.getValue();
        riders.put(riderAccess.field.getValue(), statement);
      }
    }
    return riders;
  }

  private boolean doesSetVehicleImplyVehicle(MethodInvocation setVehicleCall, UserField vehicle) {
    ArrayList<SimpleArgument> args = setVehicleCall.requiredArguments.getValue();
    if (args.size() == 1 && setVehicleCall.expression.getValue() instanceof FieldAccess) {
      Expression vehicleExpr = args.getFirst().expression.getValue();
      return isDirectRider(vehicle, vehicleExpr) || isJointRider(vehicle, vehicleExpr);
    }
    return false;
  }

  private boolean isDirectRider(UserField vehicle, Expression vehicleExpr) {
    return vehicleExpr instanceof FieldAccess fa && fa.field.getValue() == vehicle;
  }

  private boolean isJointRider(UserField vehicle, Expression vehicleExpr) {
    if (vehicleExpr instanceof MethodInvocation vehicleMethod) {
      if (vehicleMethod.expression.getValue() instanceof FieldAccess) {
        FieldAccess target = (FieldAccess) vehicleMethod.expression.getValue();
        return target.field.getValue() == vehicle;
      }
    }
    return false;
  }

  Statement[] getDoStatementsForRemoveField(UserField field, Map<AbstractField, Statement> riders) {
    List<Statement> doStatements = Lists.newLinkedList();
    for (AbstractField rider : riders.keySet()) {
      doStatements.add(SetUpMethodGenerator.createSetVehicleSceneStatement(rider));
    }
    doStatements.add(SetUpMethodGenerator.createSetVehicleNullStatement(field));
    return ArrayUtilities.createArray(doStatements, Statement.class);
  }

  Statement[] getUndoStatementsForRemoveField(UserField field, Map<AbstractField, Statement> riders) {
    Object instance = editor.getInstanceInJavaVMForField(field);
    AbstractField vehicleField = null;
    if (instance instanceof Rider rider) {
      SThing vehicleInstance = rider.getVehicle();
      vehicleField = editor.getFieldForInstanceInJavaVM(vehicleInstance);
    }
    Statement[] setupStatements = SetUpMethodGenerator.getSetupStatementsForInstance(false, instance, editor.getActiveSceneInstance(), false);
    Statement vehicleStatement = SetUpMethodGenerator.createSetVehicleFieldStatement(field, vehicleField);
    Statement[] statements = new Statement[setupStatements.length + 1 + riders.size()];
    statements[0] = vehicleStatement;
    System.arraycopy(setupStatements, 0, statements, 1, setupStatements.length);

    int i = setupStatements.length + 1;
    for (Statement setVehicleStatement : riders.values()) {
      statements[i++] = setVehicleStatement;
    }
    return statements;
  }

  static MethodInvocation asSetVehicleCall(Statement statement) {
    if (statement instanceof ExpressionStatement expressionStatement) {
      Expression expression = expressionStatement.expression.getValue();
      if (expression instanceof MethodInvocation mi) {
        Method method = mi.method.getValue();
        if (method.getName().equalsIgnoreCase("setVehicle")) {
          return mi;
        }
      }
    }
    return null;
  }

  static boolean isSetVehicleInvocation(Statement statement) {
    return asSetVehicleCall(statement) != null;
  }
}
