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

import edu.cmu.cs.dennisc.java.awt.datatransfer.ClipboardUtilities;
import edu.cmu.cs.dennisc.java.util.Maps;
import edu.cmu.cs.dennisc.java.util.logging.Logger;
import org.lgna.croquet.codecs.EnumCodec;
import org.lgna.croquet.data.ListData;
import org.lgna.croquet.data.MutableListData;
import org.lgna.croquet.data.RefreshableListData;
import org.lgna.croquet.imp.cascade.BlankNode;
import org.lgna.croquet.preferences.PreferenceBooleanState;
import org.lgna.croquet.preferences.PreferenceStringState;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Owns all internal state classes, state maps, factory methods, contains(),
 * and localize() logic extracted from {@link AbstractComposite}.
 */
class CompositeResourceManager {

  // ── Private internal state classes ──────────────────────────────────

  private static final class InternalStringValue extends AbstractComposite.AbstractInternalStringValue {
    private InternalStringValue(AbstractComposite.Key key) {
      super(UUID.fromString("142b66a2-0b95-42d0-8ea4-a22a79c8ff8c"), key);
    }
  }

  private static final class InternalStringState extends StringState {
    private final AbstractComposite.Key key;

    private InternalStringState(String initialValue, AbstractComposite.Key key) {
      super(Application.INHERIT_GROUP, UUID.fromString("ed65869f-8d26-48b1-8240-cf74ba403a2f"), initialValue);
      this.key = key;
    }

    public AbstractComposite.Key getKey() {
      return this.key;
    }

    @Override
    protected Class<? extends Element> getClassUsedForLocalization() {
      return this.key.getComposite().getClass();
    }

    @Override
    protected String getSubKeyForLocalization() {
      return this.key.getLocalizationKey();
    }

    @Override
    protected void appendRepr(StringBuilder sb) {
      super.appendRepr(sb);
      sb.append(";key=");
      sb.append(this.key);
    }
  }

  private static final class InternalPreferenceStringState extends PreferenceStringState {
    private final AbstractComposite.Key key;
    private final BooleanState isStoringPreferenceDesiredState;

    private InternalPreferenceStringState(String initialValue, AbstractComposite.Key key, BooleanState isStoringPreferenceDesiredState, UUID encryptionId) {
      super(Application.INHERIT_GROUP, UUID.fromString("ad98acf0-db41-43a6-8619-269a7d8cbc2d"), initialValue, key.getPreferenceKey(), getEncryptionKey(encryptionId != null ? encryptionId.toString() : null));
      this.key = key;
      this.isStoringPreferenceDesiredState = isStoringPreferenceDesiredState;
    }

    @Override
    protected boolean isStoringPreferenceDesired() {
      return (this.isStoringPreferenceDesiredState == null) || this.isStoringPreferenceDesiredState.getValue();
    }

    public AbstractComposite.Key getKey() {
      return this.key;
    }

    @Override
    protected Class<? extends Element> getClassUsedForLocalization() {
      return this.key.getComposite().getClass();
    }

    @Override
    protected String getSubKeyForLocalization() {
      return this.key.getLocalizationKey();
    }

    @Override
    protected void appendRepr(StringBuilder sb) {
      super.appendRepr(sb);
      sb.append(";key=");
      sb.append(this.key);
    }
  }

  private static final class InternalBooleanState extends BooleanState {
    private final AbstractComposite.Key key;

    private InternalBooleanState(boolean initialValue, AbstractComposite.Key key) {
      super(Application.INHERIT_GROUP, UUID.fromString("5053e40f-9561-41c8-835d-069bd106723c"), initialValue);
      this.key = key;
    }

    public AbstractComposite.Key getKey() {
      return this.key;
    }

    @Override
    protected Class<? extends Element> getClassUsedForLocalization() {
      return this.key.getComposite().getClass();
    }

    @Override
    protected String getSubKeyForLocalization() {
      return this.key.getLocalizationKey();
    }

    @Override
    protected void appendRepr(StringBuilder sb) {
      super.appendRepr(sb);
      sb.append(";key=");
      sb.append(this.key);
    }
  }

  private static final class InternalPreferenceBooleanState extends PreferenceBooleanState {
    private final AbstractComposite.Key key;

    private InternalPreferenceBooleanState(boolean initialValue, AbstractComposite.Key key) {
      super(Application.INHERIT_GROUP, UUID.fromString("034f99f7-74ec-4a89-8396-3c187d9684a2"), initialValue, key.getPreferenceKey());
      this.key = key;
    }

    public AbstractComposite.Key getKey() {
      return this.key;
    }

    @Override
    protected Class<? extends Element> getClassUsedForLocalization() {
      return this.key.getComposite().getClass();
    }

    @Override
    protected String getSubKeyForLocalization() {
      return this.key.getLocalizationKey();
    }

    @Override
    protected void appendRepr(StringBuilder sb) {
      super.appendRepr(sb);
      sb.append(";key=");
      sb.append(this.key);
    }
  }

  private static final class InternalSingleSelectListState<T> extends SingleSelectListState<T, ListData<T>> {
    private final AbstractComposite.Key key;

    private InternalSingleSelectListState(int selectionIndex, ListData<T> data, AbstractComposite.Key key) {
      super(Application.INHERIT_GROUP, UUID.fromString("4f0640c9-eceb-4801-a8bb-bf8e282cef0f"), selectionIndex, data);
      this.key = key;
    }

    public AbstractComposite.Key getKey() {
      return this.key;
    }

    @Override
    protected Class<? extends Element> getClassUsedForLocalization() {
      return this.key.getComposite().getClass();
    }

    @Override
    protected String getSubKeyForLocalization() {
      return this.key.getLocalizationKey();
    }

    @Override
    protected void appendRepr(StringBuilder sb) {
      super.appendRepr(sb);
      sb.append(";key=");
      sb.append(this.key);
    }
  }

  private static final class InternalImmutableDataSingleSelectListState<T> extends ImmutableDataSingleSelectListState<T> {
    private final AbstractComposite.Key key;

    private InternalImmutableDataSingleSelectListState(int selectionIndex, ItemCodec<T> codec, T[] values, AbstractComposite.Key key) {
      super(Application.INHERIT_GROUP, UUID.fromString("091d5251-d278-4eb1-8214-a27c154f5378"), selectionIndex, codec, values);
      this.key = key;
    }

    public AbstractComposite.Key getKey() {
      return this.key;
    }

    @Override
    protected Class<? extends Element> getClassUsedForLocalization() {
      return this.key.getComposite().getClass();
    }

    @Override
    protected String getSubKeyForLocalization() {
      return this.key.getLocalizationKey();
    }

    @Override
    protected void appendRepr(StringBuilder sb) {
      super.appendRepr(sb);
      sb.append(";key=");
      sb.append(this.key);
    }
  }

  private static final class InternalRefreshableDataSingleSelectListState<T> extends RefreshableDataSingleSelectListState<T> {
    private final AbstractComposite.Key key;

    private InternalRefreshableDataSingleSelectListState(int selectionIndex, RefreshableListData<T> data, AbstractComposite.Key key) {
      super(Application.INHERIT_GROUP, UUID.fromString("4d7ef91c-a8ae-4b17-9d8a-91ffac4ba12e"), selectionIndex, data);
      this.key = key;
    }

    public AbstractComposite.Key getKey() {
      return this.key;
    }

    @Override
    protected Class<? extends Element> getClassUsedForLocalization() {
      return this.key.getComposite().getClass();
    }

    @Override
    protected String getSubKeyForLocalization() {
      return this.key.getLocalizationKey();
    }

    @Override
    protected void appendRepr(StringBuilder sb) {
      super.appendRepr(sb);
      sb.append(";key=");
      sb.append(this.key);
    }
  }

  private static final class InternalMutableDataSingleSelectListState<T> extends MutableDataSingleSelectListState<T> {
    private final AbstractComposite.Key key;

    private InternalMutableDataSingleSelectListState(int selectionIndex, MutableListData<T> data, AbstractComposite.Key key) {
      super(Application.INHERIT_GROUP, UUID.fromString("6cc16988-0fc8-476b-9026-b19fd15748ea"), selectionIndex, data);
      this.key = key;
    }

    public AbstractComposite.Key getKey() {
      return this.key;
    }

    @Override
    protected Class<? extends Element> getClassUsedForLocalization() {
      return this.key.getComposite().getClass();
    }

    @Override
    protected String getSubKeyForLocalization() {
      return this.key.getLocalizationKey();
    }

    @Override
    protected void appendRepr(StringBuilder sb) {
      super.appendRepr(sb);
      sb.append(";key=");
      sb.append(this.key);
    }
  }

  private static final class InternalTabState<T extends SimpleTabComposite<?>> extends SimpleTabState<T> {
    private final AbstractComposite.Key key;

    public InternalTabState(int selectionIndex, Class<T> cls, T[] values, AbstractComposite.Key key) {
      super(Application.INHERIT_GROUP, UUID.fromString("bea99c2f-45ad-40a8-a99c-9c125a72f0be"), selectionIndex, cls, values);
      this.key = key;
    }

    public AbstractComposite.Key getKey() {
      return this.key;
    }

    @Override
    protected Class<? extends Element> getClassUsedForLocalization() {
      return this.key.getComposite().getClass();
    }

    @Override
    protected String getSubKeyForLocalization() {
      return this.key.getLocalizationKey();
    }

    @Override
    protected void appendRepr(StringBuilder sb) {
      super.appendRepr(sb);
      sb.append(";key=");
      sb.append(this.key);
    }
  }

  private static final class InternalBoundedIntegerState extends BoundedIntegerState {
    private final AbstractComposite.Key key;

    private InternalBoundedIntegerState(BoundedIntegerState.Details details, AbstractComposite.Key key) {
      super(details);
      this.key = key;
    }

    @Override
    protected Class<? extends Element> getClassUsedForLocalization() {
      return this.key.getComposite().getClass();
    }

    @Override
    protected String getSubKeyForLocalization() {
      return this.key.getLocalizationKey();
    }

    @Override
    protected void appendRepr(StringBuilder sb) {
      super.appendRepr(sb);
      sb.append(";key=");
      sb.append(this.key);
    }
  }

  private static final class InternalBoundedDoubleState extends BoundedDoubleState {
    private final AbstractComposite.Key key;

    private InternalBoundedDoubleState(BoundedDoubleState.Details details, AbstractComposite.Key key) {
      super(details);
      this.key = key;
    }

    public AbstractComposite.Key getKey() {
      return this.key;
    }

    @Override
    protected Class<? extends Element> getClassUsedForLocalization() {
      return this.key.getComposite().getClass();
    }

    @Override
    protected String getSubKeyForLocalization() {
      return this.key.getLocalizationKey();
    }

    @Override
    protected void appendRepr(StringBuilder sb) {
      super.appendRepr(sb);
      sb.append(";key=");
      sb.append(this.key);
    }
  }

  private static final class InternalCascadeWithInternalBlank<T> extends CascadeWithInternalBlank<T> {
    private final AbstractComposite.CascadeCustomizer<T> customizer;
    private final AbstractComposite.Key key;

    private InternalCascadeWithInternalBlank(AbstractComposite.CascadeCustomizer<T> customizer, Class<T> componentType, AbstractComposite.Key key) {
      super(Application.INHERIT_GROUP, UUID.fromString("165e65a4-fd9b-4a09-921d-ecc3cc808de0"), componentType);
      this.customizer = customizer;
      this.key = key;
    }

    public AbstractComposite.Key getKey() {
      return this.key;
    }

    @Override
    protected Class<? extends Element> getClassUsedForLocalization() {
      return this.key.getComposite().getClass();
    }

    @Override
    protected String getSubKeyForLocalization() {
      return this.key.getLocalizationKey();
    }

    @Override
    protected org.lgna.croquet.edits.Edit createEdit(org.lgna.croquet.history.UserActivity userActivity, T[] values) {
      return this.customizer.createEdit(values);
    }

    @Override
    protected List<CascadeBlankChild> updateBlankChildren(List<CascadeBlankChild> rv, BlankNode<T> blankNode) {
      this.customizer.appendBlankChildren(rv, blankNode);
      return rv;
    }

    @Override
    protected void appendRepr(StringBuilder sb) {
      super.appendRepr(sb);
      sb.append(";key=");
      sb.append(this.key);
    }
  }

  // ── State maps ──────────────────────────────────────────────────────

  private final Map<AbstractComposite.Key, AbstractComposite.AbstractInternalStringValue> mapKeyToStringValue = Maps.newHashMap();
  private final Map<AbstractComposite.Key, InternalBooleanState> mapKeyToBooleanState = Maps.newHashMap();
  private final Map<AbstractComposite.Key, InternalPreferenceBooleanState> mapKeyToPreferenceBooleanState = Maps.newHashMap();
  private final Map<AbstractComposite.Key, InternalStringState> mapKeyToStringState = Maps.newHashMap();
  private final Map<AbstractComposite.Key, InternalPreferenceStringState> mapKeyToPreferenceStringState = Maps.newHashMap();
  private final Map<AbstractComposite.Key, InternalSingleSelectListState> mapKeyToSingleSelectListState = Maps.newHashMap();
  private final Map<AbstractComposite.Key, InternalImmutableDataSingleSelectListState> mapKeyToImmutableSingleSelectListState = Maps.newHashMap();
  private final Map<AbstractComposite.Key, InternalRefreshableDataSingleSelectListState> mapKeyToRefreshableSingleSelectListState = Maps.newHashMap();
  private final Map<AbstractComposite.Key, InternalMutableDataSingleSelectListState> mapKeyToMutableSingleSelectListState = Maps.newHashMap();
  private final Map<AbstractComposite.Key, InternalTabState> mapKeyToTabState = Maps.newHashMap();
  private final Map<AbstractComposite.Key, InternalBoundedIntegerState> mapKeyToBoundedIntegerState = Maps.newHashMap();
  private final Map<AbstractComposite.Key, InternalBoundedDoubleState> mapKeyToBoundedDoubleState = Maps.newHashMap();
  private final Map<AbstractComposite.Key, AbstractComposite.InternalActionOperation> mapKeyToActionOperation = Maps.newHashMap();
  private final Map<AbstractComposite.Key, InternalCascadeWithInternalBlank> mapKeyToCascade = Maps.newHashMap();
  private final Map<AbstractComposite.Key, AbstractComposite.InternalCustomItemState> mapKeyToItemState = Maps.newHashMap();

  // O(1) identity-based lookup index for contains()
  private final Set<Object> containsIndex = Collections.newSetFromMap(new IdentityHashMap<>());

  // ── Map accessors ───────────────────────────────────────────────────

  Map<AbstractComposite.Key, InternalTabState> getMapKeyToTabState() {
    return this.mapKeyToTabState;
  }

  // ── Registration (for types whose constructors are in AC) ───────────

  void registerStringValue(AbstractComposite.AbstractInternalStringValue stringValue) {
    this.mapKeyToStringValue.put(stringValue.getKey(), stringValue);
  }

  void registerActionOperation(AbstractComposite.Key key, AbstractComposite.InternalActionOperation operation) {
    this.mapKeyToActionOperation.put(key, operation);
    this.containsIndex.add(operation);
  }

  void registerCustomItemState(AbstractComposite.Key key, AbstractComposite.InternalCustomItemState<?> itemState) {
    this.mapKeyToItemState.put(key, itemState);
    this.containsIndex.add(itemState);
  }

  // ── Factory methods ─────────────────────────────────────────────────

  PlainStringValue createStringValue(AbstractComposite.Key key) {
    InternalStringValue rv = new InternalStringValue(key);
    this.mapKeyToStringValue.put(key, rv);
    return rv;
  }

  StringState createStringState(AbstractComposite.Key key, String initialValue) {
    InternalStringState rv = new InternalStringState(initialValue, key);
    this.mapKeyToStringState.put(key, rv);
    this.containsIndex.add(rv);
    return rv;
  }

  PreferenceStringState createPreferenceStringState(AbstractComposite.Key key, String initialValue, BooleanState isStoringPreferenceDesiredState, UUID encryptionId) {
    InternalPreferenceStringState rv = new InternalPreferenceStringState(initialValue, key, isStoringPreferenceDesiredState, encryptionId);
    this.mapKeyToPreferenceStringState.put(key, rv);
    this.containsIndex.add(rv);
    return rv;
  }

  BooleanState createBooleanState(AbstractComposite.Key key, boolean initialValue) {
    InternalBooleanState rv = new InternalBooleanState(initialValue, key);
    this.mapKeyToBooleanState.put(key, rv);
    this.containsIndex.add(rv);
    return rv;
  }

  PreferenceBooleanState createPreferenceBooleanState(AbstractComposite.Key key, boolean initialValue) {
    InternalPreferenceBooleanState rv = new InternalPreferenceBooleanState(initialValue, key);
    this.mapKeyToPreferenceBooleanState.put(key, rv);
    this.containsIndex.add(rv);
    return rv;
  }

  BoundedIntegerState createBoundedIntegerState(AbstractComposite.Key key, BoundedIntegerState.Details details) {
    InternalBoundedIntegerState rv = new InternalBoundedIntegerState(details, key);
    this.mapKeyToBoundedIntegerState.put(key, rv);
    this.containsIndex.add(rv);
    return rv;
  }

  BoundedDoubleState createBoundedDoubleState(AbstractComposite.Key key, BoundedDoubleState.Details details) {
    InternalBoundedDoubleState rv = new InternalBoundedDoubleState(details, key);
    this.mapKeyToBoundedDoubleState.put(key, rv);
    this.containsIndex.add(rv);
    return rv;
  }

  <T> Cascade<T> createCascadeWithInternalBlank(AbstractComposite.Key key, Class<T> cls, AbstractComposite.CascadeCustomizer<T> customizer) {
    InternalCascadeWithInternalBlank<T> rv = new InternalCascadeWithInternalBlank<T>(customizer, cls, key);
    this.mapKeyToCascade.put(key, rv);
    this.containsIndex.add(rv);
    return rv;
  }

  <T> SingleSelectListState<T, ListData<T>> createGenericListState(AbstractComposite.Key key, ListData<T> data, int selectionIndex) {
    InternalSingleSelectListState<T> rv = new InternalSingleSelectListState<T>(selectionIndex, data, key);
    this.mapKeyToSingleSelectListState.put(key, rv);
    return rv;
  }

  <T> ImmutableDataSingleSelectListState<T> createImmutableListState(AbstractComposite.Key key, int selectionIndex, ItemCodec<T> codec, T[] values) {
    InternalImmutableDataSingleSelectListState<T> rv = new InternalImmutableDataSingleSelectListState<T>(selectionIndex, codec, values, key);
    this.mapKeyToImmutableSingleSelectListState.put(key, rv);
    this.containsIndex.add(rv);
    return rv;
  }

  <T extends Enum<T>> ImmutableDataSingleSelectListState<T> createImmutableListStateForEnum(AbstractComposite.Key key, Class<T> valueCls, EnumCodec.LocalizationCustomizer<T> localizationCustomizer, T initialValue) {
    T[] constants = valueCls.getEnumConstants();
    int selectionIndex = Arrays.asList(constants).indexOf(initialValue);
    EnumCodec<T> enumCodec = localizationCustomizer != null ? EnumCodec.createInstance(valueCls, localizationCustomizer) : EnumCodec.getInstance(valueCls);
    InternalImmutableDataSingleSelectListState<T> rv = new InternalImmutableDataSingleSelectListState<T>(selectionIndex, enumCodec, constants, key);
    this.mapKeyToImmutableSingleSelectListState.put(key, rv);
    this.containsIndex.add(rv);
    return rv;
  }

  <T> RefreshableDataSingleSelectListState<T> createRefreshableListState(AbstractComposite.Key key, RefreshableListData<T> data, int selectionIndex) {
    InternalRefreshableDataSingleSelectListState<T> rv = new InternalRefreshableDataSingleSelectListState<T>(selectionIndex, data, key);
    this.mapKeyToRefreshableSingleSelectListState.put(key, rv);
    this.containsIndex.add(rv);
    return rv;
  }

  <T> MutableDataSingleSelectListState<T> createMutableListState(AbstractComposite.Key key, ItemCodec<T> codec, int selectionIndex, T[] values) {
    InternalMutableDataSingleSelectListState<T> rv = new InternalMutableDataSingleSelectListState<T>(selectionIndex, new MutableListData<T>(codec, values), key);
    this.mapKeyToMutableSingleSelectListState.put(key, rv);
    this.containsIndex.add(rv);
    return rv;
  }

  <C extends SimpleTabComposite<?>> ImmutableDataTabState<C> createImmutableTabState(AbstractComposite.Key key, int selectionIndex, Class<C> cls, C[] tabComposites) {
    InternalTabState<C> rv = new InternalTabState<C>(selectionIndex, cls, tabComposites, key);
    this.mapKeyToTabState.put(key, rv);
    this.containsIndex.add(rv);
    return rv;
  }

  // ── contains() ──────────────────────────────────────────────────────

  boolean contains(Model model) {
    return this.containsIndex.contains(model);
  }

  // ── localize() ──────────────────────────────────────────────────────

  private static final String SIDEKICK_LABEL_EPILOGUE = ".sidekickLabel";

  @SuppressWarnings("unchecked")
  private void localizeSidekicks(AbstractComposite<?> composite, Map<AbstractComposite.Key, ? extends CompletionModel>... maps) {
    for (Map<AbstractComposite.Key, ? extends CompletionModel> map : maps) {
      for (Map.Entry<AbstractComposite.Key, ? extends CompletionModel> entry : map.entrySet()) {
        AbstractComposite.Key key = entry.getKey();
        CompletionModel model = entry.getValue();
        String text = composite.findLocalizedText(key.getLocalizationKey() + SIDEKICK_LABEL_EPILOGUE);
        if (text != null) {
          StringValue sidekickLabel = model.getSidekickLabel();
          text = composite.modifyLocalizedText(sidekickLabel, text);
          sidekickLabel.setText(text);
        } else {
          if (model.hasSidekickLabel()) {
            Class<?> cls = composite.getClassUsedForLocalization();
            String localizationKey = cls.getSimpleName() + "." + key.getLocalizationKey() + SIDEKICK_LABEL_EPILOGUE;
            Logger.errln();
            Logger.errln("WARNING: could not find localization for sidekick label");
            Logger.errln("looking for:");
            Logger.errln();
            Logger.errln("   ", localizationKey);
            Logger.errln();
            Logger.errln("in croquet.properties file in package:", cls.getPackage().getName());
            Logger.errln();
            Logger.errln(localizationKey, "has been copied to the clipboard for your convenience.");
            Logger.errln("if this does not solve your problem please feel free to ask dennis for help.");
            ClipboardUtilities.setClipboardContents(localizationKey);
          }
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  void localize(AbstractComposite<?> composite) {
    for (Map.Entry<AbstractComposite.Key, AbstractComposite.AbstractInternalStringValue> entry : this.mapKeyToStringValue.entrySet()) {
      AbstractComposite.AbstractInternalStringValue stringValue = entry.getValue();
      stringValue.setText(composite.modifyLocalizedText(stringValue, composite.findLocalizedText(entry.getKey().getLocalizationKey())));
    }
    this.localizeSidekicks(composite, this.mapKeyToActionOperation, this.mapKeyToBooleanState, this.mapKeyToPreferenceBooleanState, this.mapKeyToBoundedDoubleState, this.mapKeyToBoundedIntegerState, this.mapKeyToCascade, this.mapKeyToItemState, this.mapKeyToImmutableSingleSelectListState, this.mapKeyToRefreshableSingleSelectListState, this.mapKeyToMutableSingleSelectListState, this.mapKeyToTabState, this.mapKeyToPreferenceStringState, this.mapKeyToStringState);
  }
}
