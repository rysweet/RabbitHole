package org.alice.ide.clipboard;

import org.alice.ide.ast.draganddrop.BlockStatementIndexPair;
import org.junit.Test;
import org.lgna.croquet.Application;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Comment;
import org.lgna.project.ast.Statement;

import java.util.UUID;

import static org.junit.Assert.*;

public class ClipboardOperationRegistryTest {

  @Test
  public void staticFacadesPreserveCurrentStatementMemoizationBehavior() {
    Statement firstStatement = statement("first");
    Statement secondStatement = statement("second");

    assertSame(
        CopyToClipboardOperation.getInstance(firstStatement),
        CopyToClipboardOperation.getInstance(firstStatement));
    assertNotSame(
        CopyToClipboardOperation.getInstance(firstStatement),
        CopyToClipboardOperation.getInstance(secondStatement));

    assertSame(
        CutToClipboardOperation.getInstance(firstStatement),
        CutToClipboardOperation.getInstance(firstStatement));
    assertNotSame(
        CutToClipboardOperation.getInstance(firstStatement),
        CutToClipboardOperation.getInstance(secondStatement));
  }

  @Test
  public void staticFacadesPreserveCurrentDropSiteMemoizationBehavior() {
    BlockStatement blockStatement = new BlockStatement();
    BlockStatementIndexPair firstSite = new BlockStatementIndexPair(blockStatement, 0);
    BlockStatementIndexPair equivalentSite = new BlockStatementIndexPair(blockStatement, 0);
    BlockStatementIndexPair differentSite = new BlockStatementIndexPair(blockStatement, 1);

    assertEquals(firstSite, equivalentSite);
    assertSame(
        PasteFromClipboardOperation.getInstance(firstSite),
        PasteFromClipboardOperation.getInstance(equivalentSite));
    assertNotSame(
        PasteFromClipboardOperation.getInstance(firstSite),
        PasteFromClipboardOperation.getInstance(differentSite));

    assertSame(
        CopyFromClipboardOperation.getInstance(firstSite),
        CopyFromClipboardOperation.getInstance(equivalentSite));
    assertNotSame(
        CopyFromClipboardOperation.getInstance(firstSite),
        CopyFromClipboardOperation.getInstance(differentSite));

    assertNotSame(
        PasteFromClipboardOperation.getInstance(firstSite),
        CopyFromClipboardOperation.getInstance(firstSite));
  }

  @Test
  public void operationIdsAndGroupsRemainCompatibleWithBaseline() {
    Statement statement = statement("ids");
    BlockStatementIndexPair site = insertionSite();

    CopyToClipboardOperation copy = CopyToClipboardOperation.getInstance(statement);
    CutToClipboardOperation cut = CutToClipboardOperation.getInstance(statement);
    PasteFromClipboardOperation paste = PasteFromClipboardOperation.getInstance(site);
    CopyFromClipboardOperation copyFromClipboard = CopyFromClipboardOperation.getInstance(site);

    assertEquals(UUID.fromString("86025bf5-1f1f-4f2d-8182-190574a3c3d0"), copy.getMigrationId());
    assertEquals(Application.DOCUMENT_UI_GROUP, copy.getGroup());
    assertEquals(UUID.fromString("9ae5c84b-60f4-486f-aaf1-bd7b5dc6ba86"), cut.getMigrationId());
    assertEquals(Application.PROJECT_GROUP, cut.getGroup());
    assertEquals(UUID.fromString("4dea691b-af8f-4991-80e2-3db880f1883f"), paste.getMigrationId());
    assertEquals(UUID.fromString("fc162a45-2175-4ccf-a5f2-d3de969692c3"), copyFromClipboard.getMigrationId());
  }

  @Test
  public void registryMemoizesAllOperationTypesInsideOneScope() {
    ClipboardOperationRegistry registry = new ClipboardOperationRegistry();
    Statement statement = statement("scoped");
    BlockStatementIndexPair site = insertionSite();
    BlockStatementIndexPair equivalentSite = new BlockStatementIndexPair(site.getBlockStatement(), site.getIndex());

    assertSame(
        registry.getCopyToClipboardOperation(statement),
        registry.getCopyToClipboardOperation(statement));
    assertSame(
        registry.getCutToClipboardOperation(statement),
        registry.getCutToClipboardOperation(statement));
    assertSame(
        registry.getPasteFromClipboardOperation(site),
        registry.getPasteFromClipboardOperation(equivalentSite));
    assertSame(
        registry.getCopyFromClipboardOperation(site),
        registry.getCopyFromClipboardOperation(equivalentSite));
    assertNotSame(
        registry.getPasteFromClipboardOperation(site),
        registry.getCopyFromClipboardOperation(site));
  }

  @Test
  public void separateRegistriesReturnSeparateOperationsForSameKeys() {
    ClipboardOperationRegistry firstRegistry = new ClipboardOperationRegistry();
    ClipboardOperationRegistry secondRegistry = new ClipboardOperationRegistry();
    Statement statement = statement("shared-key");
    BlockStatementIndexPair site = insertionSite();

    assertNotSame(
        firstRegistry.getCopyToClipboardOperation(statement),
        secondRegistry.getCopyToClipboardOperation(statement));
    assertNotSame(
        firstRegistry.getCutToClipboardOperation(statement),
        secondRegistry.getCutToClipboardOperation(statement));
    assertNotSame(
        firstRegistry.getPasteFromClipboardOperation(site),
        secondRegistry.getPasteFromClipboardOperation(site));
    assertNotSame(
        firstRegistry.getCopyFromClipboardOperation(site),
        secondRegistry.getCopyFromClipboardOperation(site));
  }

  @Test
  public void staticFacadesUseScopedRegistryOverrideAndDoNotLeakAfterClose() {
    ClipboardOperationRegistry scopedRegistry = new ClipboardOperationRegistry();
    Statement statement = statement("override");
    BlockStatementIndexPair site = insertionSite();

    CopyToClipboardOperation scopedCopy;
    CutToClipboardOperation scopedCut;
    PasteFromClipboardOperation scopedPaste;
    CopyFromClipboardOperation scopedCopyFromClipboard;
    try (ClipboardOperationRegistries.RegistryScope scope =
        ClipboardOperationRegistries.useRegistry(scopedRegistry)) {
      scopedCopy = CopyToClipboardOperation.getInstance(statement);
      scopedCut = CutToClipboardOperation.getInstance(statement);
      scopedPaste = PasteFromClipboardOperation.getInstance(site);
      scopedCopyFromClipboard = CopyFromClipboardOperation.getInstance(site);

      assertSame(scopedRegistry.getCopyToClipboardOperation(statement), scopedCopy);
      assertSame(scopedRegistry.getCutToClipboardOperation(statement), scopedCut);
      assertSame(scopedRegistry.getPasteFromClipboardOperation(site), scopedPaste);
      assertSame(scopedRegistry.getCopyFromClipboardOperation(site), scopedCopyFromClipboard);
    }

    assertNotSame(scopedCopy, CopyToClipboardOperation.getInstance(statement));
    assertNotSame(scopedCut, CutToClipboardOperation.getInstance(statement));
    assertNotSame(scopedPaste, PasteFromClipboardOperation.getInstance(site));
    assertNotSame(scopedCopyFromClipboard, CopyFromClipboardOperation.getInstance(site));
  }

  @Test
  public void nestedRegistryOverridesRestoreThePreviousRegistry() {
    ClipboardOperationRegistry outerRegistry = new ClipboardOperationRegistry();
    ClipboardOperationRegistry innerRegistry = new ClipboardOperationRegistry();
    Statement statement = statement("nested");

    try (ClipboardOperationRegistries.RegistryScope outerScope =
        ClipboardOperationRegistries.useRegistry(outerRegistry)) {
      CopyToClipboardOperation outerOperation = CopyToClipboardOperation.getInstance(statement);

      try (ClipboardOperationRegistries.RegistryScope innerScope =
          ClipboardOperationRegistries.useRegistry(innerRegistry)) {
        assertNotSame(outerOperation, CopyToClipboardOperation.getInstance(statement));
        assertSame(
            innerRegistry.getCopyToClipboardOperation(statement),
            CopyToClipboardOperation.getInstance(statement));
      }

      assertSame(outerOperation, CopyToClipboardOperation.getInstance(statement));
    }
  }

  @Test
  public void registryLookupRejectsNullKeysAtTheBoundary() {
    ClipboardOperationRegistry registry = new ClipboardOperationRegistry();

    expectNullPointerException(() -> registry.getCopyToClipboardOperation(null));
    expectNullPointerException(() -> registry.getCutToClipboardOperation(null));
    expectNullPointerException(() -> registry.getPasteFromClipboardOperation(null));
    expectNullPointerException(() -> registry.getCopyFromClipboardOperation(null));
  }

  @Test
  public void useRegistryRejectsNullRegistry() {
    expectNullPointerException(() -> ClipboardOperationRegistries.useRegistry(null));
  }

  private static Statement statement(String text) {
    return new Comment(text);
  }

  private static BlockStatementIndexPair insertionSite() {
    return new BlockStatementIndexPair(new BlockStatement(), 0);
  }

  private static void expectNullPointerException(Runnable runnable) {
    try {
      runnable.run();
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
      // Expected.
    }
  }
}
