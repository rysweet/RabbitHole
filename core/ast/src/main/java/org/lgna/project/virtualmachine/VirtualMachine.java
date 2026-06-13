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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
          rv[i] = handleMethodInvocationException(e);
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
      return handleMethodInvocationException(e);
    }
  }

  Object handleMethodInvocationException(LgnaVmMethodInvocationException e) {
    return errorPolicy.handleMethodInvocationException(e);
  }

  void handleSceneSetupException(RuntimeException e) {
    errorPolicy.handleSceneSetupException(e);
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
      handleSceneSetupException(e);
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
        handleMethodInvocationException(e);
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

  private UserArrayInstance createUserArrayInstance(UserArrayType type, int[] lengths, Object[] values) {
    return new UserArrayInstance(type, lengths, values);
  }

  private Object createJavaArrayInstance(JavaType type, int[] lengths, Object[] values) {
    Class<?> cls = type.getClassReflectionProxy().getReification();
    assert cls != null;
    Class<?> componentCls = cls.getComponentType();
    assert componentCls != null;
    Object rv = Array.newInstance(componentCls, lengths);
    for (int i = 0; i < values.length; i++) {
      if (values[i] instanceof UserInstance userValue) {
        values[i] = userValue.getJavaInstance();
      }
      Array.set(rv, i, values[i]);
    }
    return rv;
  }

  protected Object createArrayInstance(AbstractType<?, ?, ?> type, int[] lengths, Object... values) {
    assert type != null;
    if (type instanceof UserArrayType arrayType) {
      return this.createUserArrayInstance(arrayType, lengths, values);
    } else if (type instanceof JavaType javaType) {
      return this.createJavaArrayInstance(javaType, lengths, values);
    } else {
      throw new RuntimeException();
    }
  }

  public Object[] evaluateArguments(AbstractCode code, NodeListProperty<SimpleArgument> arguments, NodeListProperty<SimpleArgument> variableArguments, NodeListProperty<JavaKeyedArgument> keyedArguments) {
    return expressionEvaluator.evaluateArguments(code, arguments, variableArguments, keyedArguments);
  }

  protected Integer getArrayLength(Object array) {
    if (array != null) {
      if (array instanceof UserArrayInstance userArrayInstance) {
        return userArrayInstance.getLength();
      } else {
        return Array.getLength(array);
      }
    } else {
      throw new NullPointerException();
    }
  }

  protected Object getUserField(UserField field, Object instance) {
    assert instance != null : field.getName();
    assert instance instanceof UserInstance;
    UserInstance userInstance = (UserInstance) instance;
    return userInstance.getFieldValue(field);
  }

  protected void setUserField(UserField field, Object instance, Object value) {
    assert instance instanceof UserInstance;
    UserInstance userInstance = (UserInstance) instance;
    userInstance.setFieldValue(field, value);
  }

  protected Object getFieldDeclaredInJavaWithField(JavaField field, Object instance) {
    instance = UserInstance.getJavaInstanceIfNecessary(instance);
    Field fld = field.getFieldReflectionProxy().getReification();
    assert fld != null : field.getFieldReflectionProxy();
    return ReflectionUtilities.get(fld, instance);
  }

  protected void setFieldDeclaredInJavaWithField(JavaField field, Object instance, Object value) {
    instance = UserInstance.getJavaInstanceIfNecessary(instance);
    Field fld = field.getFieldReflectionProxy().getReification();
    assert fld != null : field;
    ReflectionUtilities.set(fld, instance, value);
  }

  public Object get(AbstractField field, Object instance) {
    assert field != null;
    assert (instance != null) || field.isStatic() : field;
    if (field instanceof UserField userField) {
      return this.getUserField(userField, instance);
    } else if (field instanceof JavaField javaField) {
      return this.getFieldDeclaredInJavaWithField(javaField, instance);
    } else {
      throw new RuntimeException();
    }
  }

  public void set(AbstractField field, Object instance, Object value) {
    assert field != null;
    if (field instanceof UserField userField) {
      this.setUserField(userField, instance, value);
    } else if (field instanceof JavaField javaField) {
      this.setFieldDeclaredInJavaWithField(javaField, instance, value);
    } else {
      throw new RuntimeException();
    }
  }

  private void checkIndex(int index, int length) {
    if ((index < 0) || (length <= index)) {
      throw new LgnaVmArrayIndexOutOfBoundsException(this, index, length);
    }
  }

  void checkNotNull(Object value, String message) {
    if (value == null) {
      throw new LgnaVmNullPointerException(message, this);
    }
  }

  public Object getItemAtIndex(AbstractType<?, ?, ?> arrayType, Object array, Integer index) {
    assert arrayType != null;
    assert arrayType.isArray();
    if (array instanceof UserArrayInstance userArrayInstance) {
      this.checkIndex(index, userArrayInstance.getLength());
      return userArrayInstance.get(index);
    } else {
      this.checkIndex(index, Array.getLength(array));
      return Array.get(array, index);
    }
  }

  public void setItemAtIndex(AbstractType<?, ?, ?> arrayType, Object array, Integer index, Object value) {
    assert arrayType != null;
    assert arrayType.isArray() : arrayType;
    if (array instanceof UserArrayInstance userArrayInstance) {
      this.checkIndex(index, userArrayInstance.getLength());
      userArrayInstance.set(index, value);
    } else {
      value = UserInstance.getJavaInstanceIfNecessary(value);
      this.checkIndex(index, Array.getLength(array));
      Array.set(array, index, value);
    }
  }

  public Object invokeUserMethod(Object instance, UserMethod method, Object... arguments) {
    if (method.isStatic()) {
      assert instance == null;
    } else {
      assert instance != null : method;
      assert instance instanceof UserInstance : instance;
    }
    UserInstance userInstance = (UserInstance) instance;
    Map<AbstractParameter, Object> map;
    if (arguments.length == 0) {
      map = Collections.emptyMap();
    } else {
      map = Maps.newHashMap();
      for (int i = 0; i < arguments.length; i++) {
        map.put(method.requiredParameters.get(i), arguments[i]);
      }
    }
    this.pushMethodFrame(userInstance, method, map);
    try {
      this.execute(method.body.getValue());
      if (method.isProcedure() || isStopped) {
        return null;
      } else {
        throw new LgnaVmNoReturnException(this);
      }
    } catch (ReturnException re) {
      return re.getValue();
    } finally {
      this.popFrame();
    }
  }

  private static void checkArguments(Class<?>[] parameterTypes, Object[] arguments, IllegalArgumentException iae, String text) {
    if (parameterTypes.length != arguments.length) {
      throw new RuntimeException("wrong number of arguments.  expected: " + parameterTypes.length + "; received: " + arguments.length + ". " + text, iae);
    }
    int i = 0;
    for (Class<?> parameterType : parameterTypes) {
      Object argument = arguments[i];
      if (argument != null) {
        if (parameterType.isPrimitive()) {
          //todo
        } else {
          if (!parameterType.isAssignableFrom(argument.getClass())) {
            throw new RuntimeException("parameterType[" + i + "] " + parameterType.getName() + " is not assignable from argument[" + i + "]: " + argument + ". " + text, iae);
          }
        }
      }
      i++;
    }
  }

  public Object invokeMethodDeclaredInJava(Object instance, JavaMethod method, Object... arguments) {
    instance = UserInstance.getJavaInstanceIfNecessary(instance);
    UserInstance.updateArrayWithInstancesInJavaIfNecessary(arguments);
    Method mthd = method.getMethodReflectionProxy().getReification();

    Class<?>[] parameterTypes = mthd.getParameterTypes();
    int lastParameterIndex = parameterTypes.length - 1;
    if (lastParameterIndex == arguments.length) {
      if (mthd.isVarArgs()) {
        Object[] fixedArguments = new Object[parameterTypes.length];
        System.arraycopy(arguments, 0, fixedArguments, 0, arguments.length);
        assert parameterTypes[lastParameterIndex].isArray() : parameterTypes[lastParameterIndex];
        fixedArguments[lastParameterIndex] = Array.newInstance(parameterTypes[lastParameterIndex].getComponentType(), 0);
        arguments = fixedArguments;
      }
    }

    if (ReflectionUtilities.isProtected(mthd)) {
      Class<?> adapterCls = mapAbstractClsToAdapterCls.get(mthd.getDeclaringClass());
      if (adapterCls != null) {
        mthd = ReflectionUtilities.getMethod(adapterCls, mthd.getName(), mthd.getParameterTypes());
      }
    }
    assert ReflectionUtilities.isPublic(mthd) : mthd;

    try {
      return mthd.invoke(instance, arguments);
    } catch (IllegalArgumentException illegalArgumentException) {
      checkArguments(mthd.getParameterTypes(), arguments, illegalArgumentException, ReflectionUtilities.getDetail(instance, mthd, arguments));
      throw illegalArgumentException;
    } catch (IllegalAccessException illegalAccessException) {
      throw new RuntimeException(ReflectionUtilities.getDetail(instance, mthd, arguments), illegalAccessException);
    } catch (InvocationTargetException ite) {
      Throwable throwable = ite.getTargetException();
      if (throwable instanceof RuntimeException re) {
        throw re;
      } else {
        throw new RuntimeException(ReflectionUtilities.getDetail(instance, mthd, arguments), throwable);
      }
    }
  }

  protected Object invoke(Object instance, AbstractMethod method, Object... arguments) {
    assert method != null;

    if (!method.isStatic()) {
      checkNotNull(instance, "Instance method target is null");
    }

    return method.invoke(this, instance, arguments);
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
    errorPolicy = VmExecutionErrorPolicy.SCENE_EDITOR_BEST_EFFORT;
  }

  final CopyOnWriteArrayList<VirtualMachineListener> virtualMachineListeners = new CopyOnWriteArrayList<>();
  boolean isStopped = false;
  final VmExpressionEvaluator expressionEvaluator = new VmExpressionEvaluator(this);
  final VmStatementExecutor statementExecutor = new VmStatementExecutor(this);

  private VmExecutionErrorPolicy errorPolicy = VmExecutionErrorPolicy.FAIL_FAST;
}
