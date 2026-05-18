package org.alice.ide.croquet.models.project.find.core;

import edu.cmu.cs.dennisc.pattern.Criterion;
import org.junit.Before;
import org.junit.Test;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.UserField;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class FindContentManagerExtendedTest {

  private FindContentManager manager;

  @Before
  public void setUp() {
    manager = new FindContentManager();
  }

  @Test
  public void constructorCreatesEmptyManager() throws Exception {
    assertTrue(getObjectList().isEmpty());
    assertTrue(manager.getSearchResults(new String[]{"anything"}).isEmpty());
  }

  @Test
  public void initializeWithEmptyTypeYieldsEmptySearchResults() throws Exception {
    NamedUserType sceneType = createSceneType("Scene");

    manager.initialize(sceneType, Collections.<Criterion>emptyList());

    assertTrue(getObjectList().isEmpty());
    assertTrue(manager.getSearchResults(new String[]{"scene"}).isEmpty());
  }

  @Test
  public void getSearchResultsWithEmptyTermsOnUninitializedManagerIsEmpty() {
    List<SearchResult> results = manager.getSearchResults(new String[]{});

    assertNotNull(results);
    assertTrue(results.isEmpty());
  }

  @Test
  public void refreshClearsPreviousState() throws Exception {
    manager.initialize(createSceneTypeWithField("Scene", "alphaField"), Collections.<Criterion>emptyList());
    List<SearchResult> initialObjects = getObjectList();
    assertEquals(1, initialObjects.size());
    initialObjects.get(0).addReference(new NullLiteral());
    assertEquals(1, manager.getSearchResults(new String[]{}).size());

    manager.refresh(createSceneTypeWithField("Scene", "betaField"), Collections.<Criterion>emptyList());

    List<SearchResult> refreshedObjects = getObjectList();
    assertEquals(1, refreshedObjects.size());
    assertEquals("betaField", refreshedObjects.get(0).getName());
    assertTrue(manager.getSearchResults(new String[]{}).isEmpty());
  }

  private NamedUserType createSceneType(String name) {
    NamedUserType sceneType = new NamedUserType();
    sceneType.name.setValue(name);
    return sceneType;
  }

  private NamedUserType createSceneTypeWithField(String typeName, String fieldName) {
    NamedUserType sceneType = createSceneType(typeName);
    UserField field = new UserField();
    field.name.setValue(fieldName);
    sceneType.fields.add(field);
    return sceneType;
  }

  @SuppressWarnings("unchecked")
  private List<SearchResult> getObjectList() throws Exception {
    Field field = FindContentManager.class.getDeclaredField("objectList");
    field.setAccessible(true);
    return (List<SearchResult>) field.get(manager);
  }
}
