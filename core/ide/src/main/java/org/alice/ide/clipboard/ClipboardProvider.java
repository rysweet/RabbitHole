package org.alice.ide.clipboard;

import org.alice.ide.ast.draganddrop.BlockStatementIndexPair;
import org.lgna.croquet.DragModel;
import org.lgna.croquet.DropReceptor;
import org.lgna.croquet.StandardMenuItemPrepModel;
import org.lgna.croquet.Triggerable;
import org.lgna.croquet.views.DragComponent;
import org.lgna.project.ast.Statement;

import java.util.ServiceLoader;

/**
 * Bridge between core/ide and the extracted clipboard module.
 */
public interface ClipboardProvider {
  DragModel getDragModel();

  DragComponent<?> getDragComponent();

  DropReceptor getDropReceptor();

  Triggerable getCopyFromClipboardOperation(BlockStatementIndexPair blockStatementIndexPair);

  Triggerable getPasteFromClipboardOperation(BlockStatementIndexPair blockStatementIndexPair);

  StandardMenuItemPrepModel getCopyToClipboardMenuItem(Statement statement);

  StandardMenuItemPrepModel getEditMenuCopyItem();

  StandardMenuItemPrepModel getEditMenuCutItem();

  StandardMenuItemPrepModel getEditMenuPasteItem();

  static ClipboardProvider getInstance() {
    return Holder.INSTANCE;
  }

  final class Holder {
    private static final ClipboardProvider INSTANCE = ServiceLoader.load(ClipboardProvider.class)
        .findFirst()
        .orElse(NoOpClipboardProvider.INSTANCE);

    private Holder() {
    }
  }

  enum NoOpClipboardProvider implements ClipboardProvider {
    INSTANCE;

    @Override
    public DragModel getDragModel() {
      return null;
    }

    @Override
    public DragComponent<?> getDragComponent() {
      return null;
    }

    @Override
    public DropReceptor getDropReceptor() {
      return null;
    }

    @Override
    public Triggerable getCopyFromClipboardOperation(BlockStatementIndexPair blockStatementIndexPair) {
      return null;
    }

    @Override
    public Triggerable getPasteFromClipboardOperation(BlockStatementIndexPair blockStatementIndexPair) {
      return null;
    }

    @Override
    public StandardMenuItemPrepModel getCopyToClipboardMenuItem(Statement statement) {
      return null;
    }

    @Override
    public StandardMenuItemPrepModel getEditMenuCopyItem() {
      return null;
    }

    @Override
    public StandardMenuItemPrepModel getEditMenuCutItem() {
      return null;
    }

    @Override
    public StandardMenuItemPrepModel getEditMenuPasteItem() {
      return null;
    }
  }
}
