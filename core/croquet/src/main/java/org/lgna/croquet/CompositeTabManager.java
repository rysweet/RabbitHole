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

package org.lgna.croquet;

import edu.cmu.cs.dennisc.java.util.Lists;
import edu.cmu.cs.dennisc.java.util.Sets;
import org.lgna.croquet.views.SplitPane;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manages sub-composite registration, tab state activation loops, and
 * split/card composite factories extracted from {@link AbstractComposite}.
 */
class CompositeTabManager {

  interface Activatable {
    void handlePreActivation();
    void handlePostDeactivation();
  }

  private static final class InternalSplitComposite extends SplitComposite {
    private final boolean isHorizontal;
    private final double resizeWeight;

    private InternalSplitComposite(Composite<?> leadingComposite, Composite<?> trailingComposite, boolean isHorizontal, double resizeWeight) {
      super(UUID.fromString("0a7dee81-a213-4168-b71c-99b9e364dcf3"), leadingComposite, trailingComposite);
      this.isHorizontal = isHorizontal;
      this.resizeWeight = resizeWeight;
    }

    @Override
    protected SplitPane createView() {
      SplitPane rv;
      if (this.isHorizontal) {
        rv = this.createHorizontalSplitPane();
      } else {
        rv = this.createVerticalSplitPane();
      }
      rv.setResizeWeight(this.resizeWeight);
      return rv;
    }
  }

  private static final class InternalCardOwnerComposite extends CardOwnerComposite {
    private InternalCardOwnerComposite(Composite<?>... cards) {
      super(UUID.fromString("3a6b3b22-9c35-473b-96cf-f69640176948"), cards);
    }
  }

  private final List<Composite<?>> subComposites = Lists.newCopyOnWriteArrayList();
  private final Set<Activatable> registeredTabStates = Sets.newHashSet();

  <C extends Composite<?>> C registerSubComposite(C subComposite) {
    this.subComposites.add(subComposite);
    return subComposite;
  }

  void unregisterSubComposite(Composite<?> subComposite) {
    this.subComposites.remove(subComposite);
  }

  List<Composite<?>> getSubComposites() {
    return this.subComposites;
  }

  void registerTabState(Activatable tabState) {
    this.registeredTabStates.add(tabState);
  }

  void unregisterTabState(Activatable tabState) {
    this.registeredTabStates.remove(tabState);
  }

  Set<Activatable> getRegisteredTabStates() {
    return this.registeredTabStates;
  }

  void activateAll(Iterable<? extends Activatable> mapTabStateValues) {
    for (Composite<?> subComposite : this.subComposites) {
      subComposite.handlePreActivation();
    }
    for (Activatable tabState : mapTabStateValues) {
      tabState.handlePreActivation();
    }
    for (Activatable tabState : this.registeredTabStates) {
      tabState.handlePreActivation();
    }
  }

  void deactivateAll(Iterable<? extends Activatable> mapTabStateValues) {
    for (Activatable tabState : this.registeredTabStates) {
      tabState.handlePostDeactivation();
    }
    for (Activatable tabState : mapTabStateValues) {
      tabState.handlePostDeactivation();
    }
    for (Composite<?> subComposite : this.subComposites) {
      subComposite.handlePostDeactivation();
    }
  }

  SplitComposite createHorizontalSplitComposite(Composite<?> leadingComposite, Composite<?> trailingComposite, double resizeWeight) {
    return this.registerSubComposite(new InternalSplitComposite(leadingComposite, trailingComposite, true, resizeWeight));
  }

  SplitComposite createVerticalSplitComposite(Composite<?> leadingComposite, Composite<?> trailingComposite, double resizeWeight) {
    return this.registerSubComposite(new InternalSplitComposite(leadingComposite, trailingComposite, false, resizeWeight));
  }

  CardOwnerComposite createAndRegisterCardOwnerComposite(Composite<?>... cards) {
    return this.registerSubComposite(this.createCardOwnerCompositeButDoNotRegister(cards));
  }

  CardOwnerComposite createCardOwnerCompositeButDoNotRegister(Composite<?>... cards) {
    return new InternalCardOwnerComposite(cards);
  }
}
