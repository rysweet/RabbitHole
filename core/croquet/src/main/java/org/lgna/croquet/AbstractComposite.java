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

import edu.cmu.cs.dennisc.java.util.Objects;
import org.lgna.croquet.codecs.EnumCodec;
import org.lgna.croquet.data.ListData;
import org.lgna.croquet.data.RefreshableListData;
import org.lgna.croquet.edits.Edit;
import org.lgna.croquet.history.UserActivity;
import org.lgna.croquet.imp.cascade.BlankNode;
import org.lgna.croquet.preferences.PreferenceBooleanState;
import org.lgna.croquet.preferences.PreferenceStringState;
import org.lgna.croquet.views.CompositeView;
import org.lgna.croquet.views.ScrollPane;
import org.lgna.croquet.views.SwingComponentView;

import java.util.List;
import java.util.UUID;

/**
 * @author Dennis Cosgrove
 */
public abstract class AbstractComposite<V extends CompositeView<?, ?>> extends AbstractElement implements Composite<V> {

  // ── Key (used by subclasses and all internal state classes) ────────

  protected static final class Key {
    private final AbstractComposite<?> composite;
    private final String localizationKey;

    private Key(AbstractComposite<?> composite, String localizationKey) {
      this.composite = composite;
      this.localizationKey = localizationKey;
    }

    public AbstractComposite<?> getComposite() {
      return this.composite;
    }

    public String getLocalizationKey() {
      return this.localizationKey;
    }

    public String getPreferenceKey() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.composite.getClass().getName());
      sb.append("_");
      sb.append(this.localizationKey);
      return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Key key) {
        return Objects.equals(this.composite, key.composite) && Objects.equals(this.localizationKey, key.localizationKey);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      int rv = 17;
      if (this.composite != null) {
        rv = (37 * rv) + this.composite.hashCode();
      }
      if (this.localizationKey != null) {
        rv = (37 * rv) + this.localizationKey.hashCode();
      }
      return rv;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getClass().getSimpleName());
      sb.append("[");
      sb.append(this.composite);
      sb.append(";");
      sb.append(this.localizationKey);
      sb.append("]");
      return sb.toString();
    }
  }

  // ── AbstractInternalStringValue (extended by subclasses) ──────────

  protected abstract static class AbstractInternalStringValue extends PlainStringValue {
    private final Key key;

    public AbstractInternalStringValue(UUID id, Key key) {
      super(id);
      this.key = key;
    }

    public Key getKey() {
      return this.key;
    }

    @Override
    protected Class<? extends Element> getClassUsedForLocalization() {
      return this.key.composite.getClass();
    }

    @Override
    protected String getSubKeyForLocalization() {
      return this.key.localizationKey;
    }

    @Override
    protected void appendRepr(StringBuilder sb) {
      super.appendRepr(sb);
      sb.append(";key=");
      sb.append(this.key);
    }
  }

  // ── Externally-referenced public types ────────────────────────────

  public static class BoundedIntegerDetails extends BoundedIntegerState.Details {
    public BoundedIntegerDetails() {
      super(Application.INHERIT_GROUP, UUID.fromString("3cb7dfc5-de8c-442c-9e9a-deab2eff38e8"));
    }
  }

  public static class BoundedDoubleDetails extends BoundedDoubleState.Details {
    public BoundedDoubleDetails() {
      super(Application.INHERIT_GROUP, UUID.fromString("603d4a60-cc60-41df-b5a5-992966128b41"));
    }
  }

  // ── Protected interfaces (implemented by subclasses) ──────────────

  protected static interface Action {
    // TODO remove userActivity if possible. It is used by only two implementors
    Edit perform(UserActivity userActivity, InternalActionOperation source) throws CancelException;
  }

  protected static final class InternalActionOperation extends ActionOperation {
    private final Action action;
    private final Key key;

    private InternalActionOperation(Action action, Key key) {
      super(Application.INHERIT_GROUP, UUID.fromString("2c311356-2bf2-4a57-b06b-f6cdb39b0d78"));
      this.action = action;
      this.key = key;
    }

    public Key getKey() {
      return this.key;
    }

    @Override
    protected Class<? extends Element> getClassUsedForLocalization() {
      return this.key.composite.getClass();
    }

    @Override
    protected String getSubKeyForLocalization() {
      return this.key.localizationKey;
    }

    @Override
    protected void perform(UserActivity activity) {
      try {
        Edit edit = this.action.perform(activity, this);
        if (edit != null) {
          activity.commitAndInvokeDo(edit);
        } else {
          activity.finish();
        }
      } catch (CancelException ce) {
        activity.cancel(ce);
      }
    }

    @Override
    protected void appendRepr(StringBuilder sb) {
      super.appendRepr(sb);
      sb.append(";key=");
      sb.append(this.key);
    }
  }

  protected static interface CascadeCustomizer<T> {
    void appendBlankChildren(List<CascadeBlankChild> rv, BlankNode<T> blankNode);

    Edit createEdit(T[] values);
  }

  protected static interface ItemStateCustomizer<T> {
    public CascadeFillIn<T, ?> getFillInFor(T value);

    public void appendBlankChildren(List<CascadeBlankChild> blankChildren, BlankNode<T> blankNode);

    public void prologue();

    public void epilogue();
  }

  protected static final class InternalCustomItemState<T> extends DefaultCustomItemState<T> {
    private final ItemStateCustomizer<T> customizer;
    private final Key key;

    private InternalCustomItemState(ItemStateCustomizer<T> customizer, ItemCodec<T> itemCodec, T initialValue, Key key) {
      super(Application.INHERIT_GROUP, UUID.fromString("eac974ec-8b09-4f1a-9a4c-7bae8f0780f1"), itemCodec, initialValue);
      this.customizer = customizer;
      this.key = key;
    }

    public Key getKey() {
      return this.key;
    }

    public ItemStateCustomizer<T> getCustomizer() {
      return this.customizer;
    }

    @Override
    protected void prologue() {
      super.prologue();
      this.customizer.prologue();
    }

    @Override
    protected void epilogue() {
      this.customizer.epilogue();
      super.epilogue();
    }

    @Override
    protected void updateBlankChildren(List<CascadeBlankChild> blankChildren, BlankNode<T> blankNode) {
      this.customizer.appendBlankChildren(blankChildren, blankNode);
    }

    @Override
    protected void appendRepr(StringBuilder sb) {
      super.appendRepr(sb);
      sb.append(";key=");
      sb.append(this.key);
    }
  }

  // ── Managers ──────────────────────────────────────────────────────

  private final CompositeViewLifecycle<V> viewLifecycle;
  final CompositeTabManager tabManager = new CompositeTabManager();
  final CompositeResourceManager resourceManager = new CompositeResourceManager();

  public AbstractComposite(UUID id) {
    super(id);
    this.viewLifecycle = new CompositeViewLifecycle<>(this.createScrollPaneIfDesired());
  }

  // ── View lifecycle (delegates to CompositeViewLifecycle) ──────────

  @Override
  public synchronized UUID getCardId() {
    return this.viewLifecycle.getCardId();
  }

  protected abstract ScrollPane createScrollPaneIfDesired();

  protected abstract V createView();

  protected V peekView() {
    return this.viewLifecycle.peekView();
  }

  @Override
  public final synchronized V getView() {
    return this.viewLifecycle.initView(this::createView);
  }

  @Override
  public final ScrollPane getScrollPaneIfItExists() {
    return this.viewLifecycle.getScrollPaneIfItExists();
  }

  @Override
  public final SwingComponentView<?> getRootComponent() {
    return this.viewLifecycle.getRootComponent(this::createView);
  }

  @Override
  public void releaseView() {
    this.viewLifecycle.releaseView();
  }

  // ── Tab/composite management (delegates to CompositeTabManager) ───

  protected <C extends Composite<?>> C registerSubComposite(C subComposite) {
    return this.tabManager.registerSubComposite(subComposite);
  }

  protected void unregisterSubComposite(Composite<?> subComposite) {
    this.tabManager.unregisterSubComposite(subComposite);
  }

  protected void registerTabState(TabState<?, ?> tabState) {
    this.tabManager.registerTabState(tabState);
  }

  protected void unregisterTabState(TabState<?, ?> tabState) {
    this.tabManager.unregisterTabState(tabState);
  }

  @Override
  public void handlePreActivation() {
    this.initializeIfNecessary();
    this.getView().handleCompositePreActivation();
    this.tabManager.activateAll(this.resourceManager.getMapKeyToTabState().values());
  }

  @Override
  public void handlePostDeactivation() {
    this.getView().handleCompositePostDeactivation();
    this.tabManager.deactivateAll(this.resourceManager.getMapKeyToTabState().values());
  }

  // ── Resource management (delegates to CompositeResourceManager) ───

  protected String modifyLocalizedText(Element element, String localizedText) {
    return localizedText;
  }

  @Override
  protected void localize() {
    this.resourceManager.localize(this);
  }

  @Override
  public boolean contains(Model model) {
    return this.resourceManager.contains(model);
  }

  protected void registerStringValue(AbstractInternalStringValue stringValue) {
    this.resourceManager.registerStringValue(stringValue);
  }

  protected Key createKey(String localizationKey) {
    return new Key(this, localizationKey);
  }

  protected PlainStringValue createStringValue(String keyText) {
    return this.resourceManager.createStringValue(this.createKey(keyText));
  }

  protected StringState createStringState(String keyText, String initialValue) {
    return this.resourceManager.createStringState(this.createKey(keyText), initialValue);
  }

  protected StringState createStringState(String keyText) {
    return createStringState(keyText, "");
  }

  protected PreferenceStringState createPreferenceStringState(String keyText, String initialValue, BooleanState isStoringPreferenceDesiredState, UUID encryptionId) {
    return this.resourceManager.createPreferenceStringState(this.createKey(keyText), initialValue, isStoringPreferenceDesiredState, encryptionId);
  }

  protected PreferenceStringState createPreferenceStringState(String keyText, String initialValue, BooleanState isStoringPreferenceDesiredState) {
    return createPreferenceStringState(keyText, initialValue, isStoringPreferenceDesiredState, null);
  }

  protected BooleanState createBooleanState(String keyText, boolean initialValue) {
    return this.resourceManager.createBooleanState(this.createKey(keyText), initialValue);
  }

  protected PreferenceBooleanState createPreferenceBooleanState(String keyText, boolean initialValue) {
    return this.resourceManager.createPreferenceBooleanState(this.createKey(keyText), initialValue);
  }

  protected BoundedIntegerState createBoundedIntegerState(String keyText, BoundedIntegerState.Details details) {
    return this.resourceManager.createBoundedIntegerState(this.createKey(keyText), details);
  }

  protected BoundedDoubleState createBoundedDoubleState(String keyText, BoundedDoubleState.Details details) {
    return this.resourceManager.createBoundedDoubleState(this.createKey(keyText), details);
  }

  protected ActionOperation createActionOperation(String keyText, Action action) {
    Key key = this.createKey(keyText);
    InternalActionOperation rv = new InternalActionOperation(action, key);
    this.resourceManager.registerActionOperation(key, rv);
    return rv;
  }

  protected <T> Cascade<T> createCascadeWithInternalBlank(String keyText, Class<T> cls, CascadeCustomizer<T> customizer) {
    return this.resourceManager.createCascadeWithInternalBlank(this.createKey(keyText), cls, customizer);
  }

  protected <T> CustomItemState<T> createCustomItemState(String keyText, ItemCodec<T> itemCodec, T initialValue, ItemStateCustomizer<T> customizer) {
    Key key = this.createKey(keyText);
    InternalCustomItemState<T> rv = new InternalCustomItemState<T>(customizer, itemCodec, initialValue, key);
    this.resourceManager.registerCustomItemState(key, rv);
    return rv;
  }

  protected <T> SingleSelectListState<T, ListData<T>> createGenericListState(String keyText, ListData<T> data, int selectionIndex) {
    return this.resourceManager.createGenericListState(this.createKey(keyText), data, selectionIndex);
  }

  protected <T> ImmutableDataSingleSelectListState<T> createImmutableListState(String keyText, Class<T> valueCls, ItemCodec<T> codec, int selectionIndex, T... values) {
    return this.resourceManager.createImmutableListState(this.createKey(keyText), selectionIndex, codec, values);
  }

  protected <T extends Enum<T>> ImmutableDataSingleSelectListState<T> createImmutableListStateForEnum(String keyText, Class<T> valueCls, EnumCodec.LocalizationCustomizer<T> localizationCustomizer, T initialValue) {
    return this.resourceManager.createImmutableListStateForEnum(this.createKey(keyText), valueCls, localizationCustomizer, initialValue);
  }

  protected <T extends Enum<T>> ImmutableDataSingleSelectListState<T> createImmutableListStateForEnum(String keyText, Class<T> valueCls, T initialValue) {
    return this.createImmutableListStateForEnum(keyText, valueCls, null, initialValue);
  }

  protected <T> RefreshableDataSingleSelectListState<T> createRefreshableListState(String keyText, RefreshableListData<T> data, int selectionIndex) {
    return this.resourceManager.createRefreshableListState(this.createKey(keyText), data, selectionIndex);
  }

  protected <T> MutableDataSingleSelectListState<T> createMutableListState(String keyText, Class<T> valueCls, ItemCodec<T> codec, int selectionIndex, T... values) {
    return this.resourceManager.createMutableListState(this.createKey(keyText), codec, selectionIndex, values);
  }

  protected <C extends SimpleTabComposite<?>> ImmutableDataTabState<C> createImmutableTabState(String keyText, int selectionIndex, Class<C> cls, C... tabComposites) {
    return this.resourceManager.createImmutableTabState(this.createKey(keyText), selectionIndex, cls, tabComposites);
  }

  protected ImmutableDataTabState<SimpleTabComposite<?>> createImmutableTabState(String keyText, int selectionIndex, SimpleTabComposite<?>... tabComposites) {
    return (ImmutableDataTabState) this.createImmutableTabState(keyText, selectionIndex, SimpleTabComposite.class, tabComposites);
  }

  protected SplitComposite createHorizontalSplitComposite(Composite<?> leadingComposite, Composite<?> trailingComposite, double resizeWeight) {
    return this.tabManager.createHorizontalSplitComposite(leadingComposite, trailingComposite, resizeWeight);
  }

  protected SplitComposite createVerticalSplitComposite(Composite<?> leadingComposite, Composite<?> trailingComposite, double resizeWeight) {
    return this.tabManager.createVerticalSplitComposite(leadingComposite, trailingComposite, resizeWeight);
  }

  protected CardOwnerComposite createAndRegisterCardOwnerComposite(Composite<?>... cards) {
    return this.tabManager.createAndRegisterCardOwnerComposite(cards);
  }

  protected CardOwnerComposite createCardOwnerCompositeButDoNotRegister(Composite<?>... cards) {
    return this.tabManager.createCardOwnerCompositeButDoNotRegister(cards);
  }
}
