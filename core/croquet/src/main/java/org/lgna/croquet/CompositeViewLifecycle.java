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

import org.lgna.croquet.views.CompositeView;
import org.lgna.croquet.views.ScrollPane;
import org.lgna.croquet.views.SwingComponentView;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Manages view lifecycle state (cardId, view, scrollPane) extracted from
 * {@link AbstractComposite}. Thread-safe: getCardId() and initView() are
 * synchronized on this instance.
 */
class CompositeViewLifecycle<V extends CompositeView<?, ?>> {
  private UUID cardId;
  private V view;
  private final ScrollPane scrollPane;

  CompositeViewLifecycle(ScrollPane scrollPane) {
    this.scrollPane = scrollPane;
  }

  synchronized UUID getCardId() {
    if (this.cardId == null) {
      this.cardId = UUID.randomUUID();
    }
    return this.cardId;
  }

  V peekView() {
    return this.view;
  }

  synchronized V initView(Supplier<V> viewFactory) {
    if (this.view == null) {
      this.view = viewFactory.get();
      assert this.view != null;
      if (this.scrollPane != null) {
        this.scrollPane.setViewportView(this.view);
      }
    }
    return this.view;
  }

  ScrollPane getScrollPaneIfItExists() {
    return this.scrollPane;
  }

  SwingComponentView<?> getRootComponent(Supplier<V> viewFactory) {
    V v = this.initView(viewFactory);
    if (this.scrollPane != null) {
      return this.scrollPane;
    } else {
      return v;
    }
  }

  void releaseView() {
    this.view = null;
  }
}
