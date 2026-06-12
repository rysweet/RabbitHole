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
package org.lgna.project.virtualmachine;

import edu.cmu.cs.dennisc.java.lang.reflect.ReflectionUtilities;
import edu.cmu.cs.dennisc.java.util.Maps;
import edu.cmu.cs.dennisc.java.util.logging.Logger;
import org.lgna.project.ast.*;
import org.lgna.project.virtualmachine.events.VirtualMachineListener;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Dennis Cosgrove
 */
public abstract class VirtualMachine {
  public abstract LgnaStackTraceElement[] getStackTrace(Thread thread);

  protected abstract UserInstance getThis();

  protected abstract void pushBogusFrame(UserInstance instance);

  protected abstract void pushConstructorFrame(NamedUserType type, Map<AbstractParameter, Object> map);

  protected abstract void setConstructorFrameUserInstance(UserInstance instance);

  protected abstract void pushMethodFrame(UserInstance instance, UserMethod method, Map<AbstractParameter, Object> map);

  protected abstract void pushLambdaFrame(UserInstance instance, UserLambda lambda, AbstractMethod singleAbstractMethod, Map<AbstractParameter, Object> map);

  protected abstract void popFrame();

  protected abstract Object lookup(UserParameter parameter);

  protected abstract void pushLocal(UserLocal local, Object value);

  protected abstract Object getLocal(UserLocal local);

  protected abstract void setLocal(UserLocal local, Object value);

  protected abstract void popLocal(UserLocal local);

  //  protected abstract Frame createCopyOfCurrentFrame();
  protected abstract Frame getFrameForThread(Thread thread);

  protected abstract void pushCurrentThread(Frame frame);

  protected abstract void popCurrentThread();

  public Object[] ENTRY_POINT_evaluate(UserInstance instance, Expression[] expressions) {
    this.pushBogusFrame(instance);
    try {
      Object[] rv = new Object[expressions.length];
      for (int i = 0; i < expressions.length; i++) {
        try {
          rv[i] = this.evaluate(expressions[i]);
        } catch (LgnaVmMethodInvocationException e) {
          if (isForRunning()) {
            throw e;
          }
          handleSceneEditorMethodInvocationException(e);
          rv[i] = null;
        }
      }
      return rv;
    } finally {
      this.popFrame();
    }
  }

  public Object ENTRY_POINT_invoke(UserInstance target, AbstractMethod method, Object... arguments) {
    try {
      return invoke(target, method, arguments);
    } catch (LgnaVmMethodInvocationException e) {
      if (isForRunning()) {
        throw e;
      }
      handleSceneEditorMethodInvocationException(e);
      return null;
    }
  }

  boolean isForRunning() {
    return sceneEditorPolicy.isForRunning();
  }

  void handleSceneEditorMethodInvocationException(LgnaVmMethodInvocationException e) {
    sceneEditorPolicy.handleSceneEditorMethodInvocationException(e);
  }

  private NamedUserConstructor getConstructor(NamedUserType entryPointType, Object[] arguments) {
    for (NamedUserConstructor constructor : entryPointType.constructors) {
      List<? extends AbstractParameter> parameters = constructor.getRequiredParameters();
      if (parameters.size() == arguments.length) {
        //todo: check types
        return constructor;
      }
    }
    return null;
  }

  public UserInstance ENTRY_POINT_createInstance(NamedUserType entryPointType, Object... arguments) {
    return getConstructor(entryPointType, arguments).evaluate(this, null, arguments);
  }

  public void createAndSetFieldInstance(UserInstance userInstance, UserField field) {
    try {
      Object value = evaluate(field.initializer.getValue());
      userInstance.setFieldValue(field, value);
    } catch (RuntimeException e) {
      if (isForRunning()) {
        throw e;
      }
      Logger.warning("Error when setting up scene " + e.getMessage());
    }
  }

  public void ACCEPTABLE_HACK_FOR_SCENE_EDITOR_initializeField(UserInstance instance, UserField field) {
    this.pushBogusFrame(instance);
    try {
      createAndSetFieldInstance(instance, field);
    } finally {
      this.popFrame();
    }
  }

  public void ACCEPTABLE_HACK_FOR_SCENE_EDITOR_executeStatement(UserInstance instance, Statement statement) {
    assert (statement instanceof ReturnStatement) == false;
    this.pushBogusFrame(instance);
    try {
      try {
        this.execute(statement);
      } catch (ReturnException re) {
        throw new AssertionError();
      } catch (LgnaVmMethodInvocationException e) {
        if (isForRunning()) {
          throw e;
        }
        handleSceneEditorMethodInvocationException(e);
      }
    } finally {
      this.popFrame();
    }
  }

  final Map<Class<?>, Class<?>> mapAbstractClsToAdapterCls = Maps.newHashMap();

  public void registerAbstractClassAdapter(Class<?> abstractCls, Class<?> adapterCls) {
    if (!ReflectionUtilities.isAbstract(abstractCls)) {
      Logger.severe(abstractCls);
    }
    this.mapAbstractClsToAdapterCls.put(abstractCls, adapterCls);
  }

  /* package-private */Object createInstance(UserType<?> type, final UserInstance userInstance, Constructor<?> cnstrctr, Object... arguments) {
    Class<?> cls = cnstrctr.getDeclaringClass();
    Class<?> adapterCls = this.mapAbstractClsToAdapterCls.get(cls);
    if (adapterCls != null) {
      MethodContext context = new MethodContext() {
        @Override
        public void invokeEntryPoint(AbstractMethod method, final Object... arguments) {
          VirtualMachine.this.ENTRY_POINT_invoke(userInstance, method, arguments);
        }
      };
      Class<?>[] parameterTypes = {MethodContext.class, UserType.class, Object[].class};
      Object[] args = {context, type, arguments};
      return ReflectionUtilities.newInstance(adapterCls, parameterTypes, args);
    } else {
      return ReflectionUtilities.newInstance(cnstrctr, arguments);
    }
  }

  protected Object createArrayInstance(AbstractType<?, ?, ?> type, int[] lengths, Object... values) {
    return arrayAccessHelper.createArrayInstance(type, lengths, values);
  }

  public Object[] evaluateArguments(AbstractCode code, NodeListProperty<SimpleArgument> arguments, NodeListProperty<SimpleArgument> variableArguments, NodeListProperty<JavaKeyedArgument> keyedArguments) {
    return expressionEvaluator.evaluateArguments(code, arguments, variableArguments, keyedArguments);
  }

  protected Integer getArrayLength(Object array) {
    return arrayAccessHelper.getArrayLength(array);
  }

  protected Object getUserField(UserField field, Object instance) {
    return fieldAccessHelper.getUserField(field, instance);
  }

  protected void setUserField(UserField field, Object instance, Object value) {
    fieldAccessHelper.setUserField(field, instance, value);
  }

  protected Object getFieldDeclaredInJavaWithField(JavaField field, Object instance) {
    return fieldAccessHelper.getFieldDeclaredInJavaWithField(field, instance);
  }

  protected void setFieldDeclaredInJavaWithField(JavaField field, Object instance, Object value) {
    fieldAccessHelper.setFieldDeclaredInJavaWithField(field, instance, value);
  }

  public Object get(AbstractField field, Object instance) {
    return fieldAccessHelper.get(field, instance);
  }

  public void set(AbstractField field, Object instance, Object value) {
    fieldAccessHelper.set(field, instance, value);
  }

  void checkNotNull(Object value, String message) {
    if (value == null) {
      throw new LgnaVmNullPointerException(message, this);
    }
  }

  public Object getItemAtIndex(AbstractType<?, ?, ?> arrayType, Object array, Integer index) {
    return arrayAccessHelper.getItemAtIndex(arrayType, array, index);
  }

  public void setItemAtIndex(AbstractType<?, ?, ?> arrayType, Object array, Integer index, Object value) {
    arrayAccessHelper.setItemAtIndex(arrayType, array, index, value);
  }

  public Object invokeUserMethod(Object instance, UserMethod method, Object... arguments) {
    return methodInvoker.invokeUserMethod(instance, method, arguments);
  }

  public Object invokeMethodDeclaredInJava(Object instance, JavaMethod method, Object... arguments) {
    return methodInvoker.invokeMethodDeclaredInJava(instance, method, arguments);
  }

  protected Object invoke(Object instance, AbstractMethod method, Object... arguments) {
    return methodInvoker.invoke(instance, method, arguments);
  }

  protected Object evaluate(Expression expression) {
    return expressionEvaluator.evaluate(expression);
  }

  protected final <E> E evaluate(Expression expression, Class<E> cls) {
    return expressionEvaluator.evaluate(expression, cls);
  }

  protected void execute(Statement statement) throws ReturnException {
    statementExecutor.execute(statement);
  }

  public void stopExecution() {
    isStopped = true;
  }

  public void addVirtualMachineListener(VirtualMachineListener virtualMachineListener) {
    this.virtualMachineListeners.add(virtualMachineListener);
  }

  public void removeVirtualMachineListener(VirtualMachineListener virtualMachineListener) {
    this.virtualMachineListeners.remove(virtualMachineListener);
  }

  public List<VirtualMachineListener> getVirtualMachineListeners() {
    return Collections.unmodifiableList(this.virtualMachineListeners);
  }

  public void setForSceneEditor() {
    sceneEditorPolicy.setForSceneEditor();
  }

  final CopyOnWriteArrayList<VirtualMachineListener> virtualMachineListeners = new CopyOnWriteArrayList<>();
  boolean isStopped = false;
  final VmArrayAccessHelper arrayAccessHelper = new VmArrayAccessHelper(this);
  final VmFieldAccessHelper fieldAccessHelper = new VmFieldAccessHelper();
  final VmMethodInvoker methodInvoker = new VmMethodInvoker(this);
  final VmSceneEditorPolicy sceneEditorPolicy = new VmSceneEditorPolicy();
  final VmExpressionEvaluator expressionEvaluator = new VmExpressionEvaluator(this);
  final VmStatementExecutor statementExecutor = new VmStatementExecutor(this);
}
