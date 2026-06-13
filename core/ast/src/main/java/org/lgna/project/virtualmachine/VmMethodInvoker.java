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
import org.lgna.project.ast.AbstractMethod;
import org.lgna.project.ast.AbstractParameter;
import org.lgna.project.ast.JavaMethod;
import org.lgna.project.ast.UserMethod;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

final class VmMethodInvoker {
  private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPERS = Map.of(
      Boolean.TYPE, Boolean.class,
      Byte.TYPE, Byte.class,
      Character.TYPE, Character.class,
      Short.TYPE, Short.class,
      Integer.TYPE, Integer.class,
      Long.TYPE, Long.class,
      Float.TYPE, Float.class,
      Double.TYPE, Double.class,
      Void.TYPE, Void.class);

  private final VirtualMachine vm;

  VmMethodInvoker(VirtualMachine vm) {
    this.vm = vm;
  }

  Object invokeUserMethod(Object instance, UserMethod method, Object... arguments) {
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
    vm.pushMethodFrame(userInstance, method, map);
    try {
      vm.execute(method.body.getValue());
      if (method.isProcedure() || vm.isStopped) {
        return null;
      } else {
        throw new LgnaVmNoReturnException(vm);
      }
    } catch (ReturnException re) {
      return re.getValue();
    } finally {
      vm.popFrame();
    }
  }

  Object invokeMethodDeclaredInJava(Object instance, JavaMethod method, Object... arguments) {
    instance = UserInstance.getJavaInstanceIfNecessary(instance);
    UserInstance.updateArrayWithInstancesInJavaIfNecessary(arguments);
    Method reflectedMethod = method.getMethodReflectionProxy().getReification();

    Class<?>[] parameterTypes = reflectedMethod.getParameterTypes();
    int lastParameterIndex = parameterTypes.length - 1;
    if (lastParameterIndex == arguments.length) {
      if (reflectedMethod.isVarArgs()) {
        Object[] fixedArguments = new Object[parameterTypes.length];
        System.arraycopy(arguments, 0, fixedArguments, 0, arguments.length);
        assert parameterTypes[lastParameterIndex].isArray() : parameterTypes[lastParameterIndex];
        fixedArguments[lastParameterIndex] = Array.newInstance(parameterTypes[lastParameterIndex].getComponentType(), 0);
        arguments = fixedArguments;
      }
    }

    if (ReflectionUtilities.isProtected(reflectedMethod)) {
      Class<?> adapterCls = vm.mapAbstractClsToAdapterCls.get(reflectedMethod.getDeclaringClass());
      if (adapterCls != null) {
        reflectedMethod = ReflectionUtilities.getMethod(adapterCls, reflectedMethod.getName(), reflectedMethod.getParameterTypes());
      }
    }
    assert ReflectionUtilities.isPublic(reflectedMethod) : reflectedMethod;

    try {
      return reflectedMethod.invoke(instance, arguments);
    } catch (IllegalArgumentException illegalArgumentException) {
      checkArguments(reflectedMethod.getParameterTypes(), arguments, illegalArgumentException, ReflectionUtilities.getDetail(instance, reflectedMethod, arguments));
      throw illegalArgumentException;
    } catch (IllegalAccessException illegalAccessException) {
      throw new RuntimeException(ReflectionUtilities.getDetail(instance, reflectedMethod, arguments), illegalAccessException);
    } catch (InvocationTargetException ite) {
      Throwable throwable = ite.getTargetException();
      if (throwable instanceof RuntimeException re) {
        throw re;
      } else {
        throw new RuntimeException(ReflectionUtilities.getDetail(instance, reflectedMethod, arguments), throwable);
      }
    }
  }

  Object invoke(Object instance, AbstractMethod method, Object... arguments) {
    assert method != null;

    if (!method.isStatic()) {
      vm.checkNotNull(instance, "Instance method target is null");
    }

    return method.invoke(vm, instance, arguments);
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
          Class<?> wrapperType = PRIMITIVE_WRAPPERS.get(parameterType);
          if ((wrapperType != null) && !wrapperType.isAssignableFrom(argument.getClass())) {
            throw new RuntimeException("parameterType[" + i + "] " + parameterType.getName() + " is not assignable from argument[" + i + "]: " + argument + ". " + text, iae);
          }
        } else {
          if (!parameterType.isAssignableFrom(argument.getClass())) {
            throw new RuntimeException("parameterType[" + i + "] " + parameterType.getName() + " is not assignable from argument[" + i + "]: " + argument + ". " + text, iae);
          }
        }
      }
      i++;
    }
  }
}
