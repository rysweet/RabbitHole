package org.alice.ide.croquet.models.ui;

import org.alice.ide.croquet.models.ui.formatter.FormatterState;
import org.alice.ide.croquet.models.ui.locale.LocaleState;
import org.alice.ide.croquet.models.ui.preferences.IsIncludingManagedUserMethods;
import org.alice.ide.croquet.models.ui.preferences.IsJavaCodeOnTheSideState;
import org.alice.ide.formatter.AliceFormatter;
import org.alice.ide.formatter.JavaFormatter;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class UiModelStateManagementBehaviorTest {

  @Test
  public void formatterStateDefaultsToAliceAndStillExposesJavaAsTheSecondOption() {
    FormatterState state = FormatterState.getInstance();

    assertSame(AliceFormatter.getInstance(), state.getItemAt(0));
    assertSame(JavaFormatter.getInstance(), state.getItemAt(1));
    assertFalse(FormatterState.isJava());
  }

  @Test
  public void localeStateDefaultsToUnitedStatesEnglishAndIncludesPreviewLocales() {
    LocaleState state = LocaleState.getInstance();
    Locale englishUnitedStates = Locale.of("en", "US");
    Locale portuguese = Locale.of("pt", "BR");
    Locale japanese = Locale.of("ja");

    assertEquals(englishUnitedStates, state.getValue());
    assertEquals(englishUnitedStates, state.getItemAt(0));
    assertTrue(containsLocale(state, portuguese));
    assertTrue(containsLocale(state, japanese));
  }

  @Test
  public void preferenceStatesRemainSingletonBackedAndDefaultToFalse() {
    IsJavaCodeOnTheSideState javaCodeState = IsJavaCodeOnTheSideState.getInstance();
    IsIncludingManagedUserMethods managedMethodsState = IsIncludingManagedUserMethods.getInstance();

    assertSame(javaCodeState, IsJavaCodeOnTheSideState.getInstance());
    assertSame(managedMethodsState, IsIncludingManagedUserMethods.getInstance());
    assertFalse(javaCodeState.getValue());
    assertFalse(managedMethodsState.getValue());
  }

  private static boolean containsLocale(LocaleState state, Locale locale) {
    for (int i = 0; i < state.getItemCount(); i++) {
      if (locale.equals(state.getItemAt(i))) {
        return true;
      }
    }
    return false;
  }
}
