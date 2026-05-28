package org.alice.ide.clipboard;

import org.alice.ide.ast.draganddrop.BlockStatementIndexPair;
import org.alice.ide.croquet.models.clipboard.CopyOperation;
import org.alice.ide.croquet.models.clipboard.CutOperation;
import org.alice.ide.croquet.models.clipboard.PasteOperation;
import org.lgna.croquet.DragModel;
import org.lgna.croquet.DropReceptor;
import org.lgna.croquet.StandardMenuItemPrepModel;
import org.lgna.croquet.Triggerable;
import org.lgna.croquet.views.DragComponent;
import org.lgna.project.ast.Statement;

public final class ClipboardDnDProvider implements ClipboardProvider {
  @Override
  public DragModel getDragModel() {
    return Clipboard.SINGLETON.getDragModel();
  }

  @Override
  public DragComponent<?> getDragComponent() {
    return Clipboard.SINGLETON.getDragComponent();
  }

  @Override
  public DropReceptor getDropReceptor() {
    return Clipboard.SINGLETON.getDropReceptor();
  }

  @Override
  public Triggerable getCopyFromClipboardOperation(BlockStatementIndexPair blockStatementIndexPair) {
    return CopyFromClipboardOperation.getInstance(blockStatementIndexPair);
  }

  @Override
  public Triggerable getPasteFromClipboardOperation(BlockStatementIndexPair blockStatementIndexPair) {
    return PasteFromClipboardOperation.getInstance(blockStatementIndexPair);
  }

  @Override
  public StandardMenuItemPrepModel getCopyToClipboardMenuItem(Statement statement) {
    return CopyToClipboardOperation.getInstance(statement).getMenuItemPrepModel();
  }

  @Override
  public StandardMenuItemPrepModel getEditMenuCopyItem() {
    return CopyOperation.getInstance().getMenuItemPrepModel();
  }

  @Override
  public StandardMenuItemPrepModel getEditMenuCutItem() {
    return CutOperation.getInstance().getMenuItemPrepModel();
  }

  @Override
  public StandardMenuItemPrepModel getEditMenuPasteItem() {
    return PasteOperation.getInstance().getMenuItemPrepModel();
  }
}
