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
package org.alice.tweedle.run;

import org.alice.tweedle.*;
import org.alice.tweedle.ast.TweedleExpression;

public abstract class VirtualMachine {

  public Object[] ENTRY_POINT_evaluate(TweedleObject instance, TweedleExpression[] expressions) {
    Frame frame = new Frame(instance);
    Object[] values = new Object[expressions.length];
    for (int i = 0; i < expressions.length; i++) {
      values[i] = evaluate(frame, expressions[i]);
    }
    return values;
  }

  public void ENTRY_POINT_invoke(TweedleObject instance, TweedleMethod method, TweedleValue... arguments) {
    Frame frame = new Frame(instance);
    invoke(frame, instance, method, arguments);
  }

  public TweedleObject ENTRY_POINT_createInstance(TweedleClass entryPointType, TweedleValue... arguments) {
    Frame frame = new Frame(null);
    return entryPointType.instantiate(frame, arguments);
  }

  public TweedleValue createAndSetFieldInstance(Frame frame, TweedleObject instance, TweedleField field) {
    return instance.initializeField(frame, field);
  }

  public Object ACCEPTABLE_HACK_FOR_SCENE_EDITOR_initializeField(TweedleObject instance, TweedleField field) {
    Frame frame = new Frame(instance);
    return createAndSetFieldInstance(frame, instance, field);
  }

  public void ACCEPTABLE_HACK_FOR_SCENE_EDITOR_executeStatement(TweedleObject instance, TweedleStatement statement) {
    Frame frame = new Frame(instance);
    execute(frame, statement);
  }

  protected TweedleValue get(TweedleField field, TweedleObject instance) {
    return instance.get(field);
  }

  protected void set(TweedleField field, TweedleObject instance, TweedleValue value) {
    instance.set(field, value);
  }

  protected Object invoke(Frame frame, TweedleObject target, TweedleMethod method, TweedleValue... arguments) {
    return method.invoke(frame, target, arguments);
  }

  protected TweedleValue evaluate(Frame frame, TweedleExpression expression) {
    return expression.evaluate(frame);
  }

  protected void execute(Frame frame, TweedleStatement statement) {
    if (statement.isEnabled()) {
      statement.execute(frame);
    }
  }
}
