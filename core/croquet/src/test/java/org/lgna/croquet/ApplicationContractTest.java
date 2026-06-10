package org.lgna.croquet;

import org.junit.Test;

import javax.swing.JComponent;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class ApplicationContractTest {
  @Test
  public void ensureTestApplicationRegistersActiveSingleton() {
    Application<?> application = CroquetTestUtils.ensureTestApplication();

    assertNotNull(application);
    assertSame(application, Application.getActiveInstance());
  }

  @Test
  public void getLocaleReturnsSwingDefaultLocale() {
    Locale previousLocale = JComponent.getDefaultLocale();
    try {
      JComponent.setDefaultLocale(Locale.CANADA_FRENCH);

      assertEquals(Locale.CANADA_FRENCH, Application.getLocale());
    } finally {
      JComponent.setDefaultLocale(previousLocale);
    }
  }
}
