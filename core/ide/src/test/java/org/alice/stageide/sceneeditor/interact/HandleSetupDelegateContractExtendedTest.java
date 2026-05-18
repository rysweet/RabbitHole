package org.alice.stageide.sceneeditor.interact;

import org.alice.interact.DragAdapter;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class HandleSetupDelegateContractExtendedTest {
  @Test
  public void constructorIsOnlyDeclaredConstructorAndIsPrivate() {
    Constructor<?>[] constructors = HandleSetupDelegate.class.getDeclaredConstructors();

    assertEquals(1, constructors.length);
    assertEquals(0, constructors[0].getParameterCount());
    assertTrue(Modifier.isPrivate(constructors[0].getModifiers()));
  }

  @Test
  public void classDeclaresOnlySetupHandlesMethodAndNoFields() {
    Method[] methods = HandleSetupDelegate.class.getDeclaredMethods();

    assertEquals(1, methods.length);
    assertEquals("setupHandles", methods[0].getName());
    assertEquals(DragAdapter.class, methods[0].getParameterTypes()[0]);
    assertEquals(0, HandleSetupDelegate.class.getDeclaredFields().length);
  }

  @Test
  public void sourceContainsTwentyFourHandleNames() throws Exception {
    long count = Files.lines(findSourceFile())
        .filter(line -> line.contains(".setName("))
        .count();

    assertEquals(24, count);
  }

  @Test
  public void sourceRemainsAdapterGenericAndDoesNotInstantiateDelegate() throws Exception {
    String source = Files.readString(findSourceFile());

    assertTrue(source.contains("setupHandles(DragAdapter adapter)"));
    assertFalse(source.contains("new HandleSetupDelegate"));
    assertFalse(source.contains("new GlobalDragAdapter"));
    assertTrue(source.contains("private HandleSetupDelegate()"));
  }

  private static Path findSourceFile() {
    Path fromRoot = Paths.get("core/ide/src/main/java/org/alice/stageide/sceneeditor/interact/HandleSetupDelegate.java");
    return Files.exists(fromRoot)
        ? fromRoot
        : Paths.get("src/main/java/org/alice/stageide/sceneeditor/interact/HandleSetupDelegate.java");
  }
}
