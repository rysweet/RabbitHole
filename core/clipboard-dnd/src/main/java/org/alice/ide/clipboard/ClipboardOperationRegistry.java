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

package org.alice.ide.clipboard;

import edu.cmu.cs.dennisc.java.util.Maps;
import org.alice.ide.ast.draganddrop.BlockStatementIndexPair;
import org.lgna.project.ast.Statement;

import java.util.Map;
import java.util.Objects;

/**
 * Owns clipboard and drag-and-drop operation memoization for one context.
 */
public final class ClipboardOperationRegistry {
  private final Map<Statement, CopyToClipboardOperation> copyToClipboardOperations = Maps.newHashMap();
  private final Map<Statement, CutToClipboardOperation> cutToClipboardOperations = Maps.newHashMap();
  private final Map<BlockStatementIndexPair, PasteFromClipboardOperation> pasteFromClipboardOperations =
      Maps.newHashMap();
  private final Map<BlockStatementIndexPair, CopyFromClipboardOperation> copyFromClipboardOperations =
      Maps.newHashMap();

  public synchronized CopyToClipboardOperation getCopyToClipboardOperation(Statement statement) {
    Objects.requireNonNull(statement, "statement");
    CopyToClipboardOperation rv = this.copyToClipboardOperations.get(statement);
    if (rv == null) {
      rv = new CopyToClipboardOperation(statement);
      this.copyToClipboardOperations.put(statement, rv);
    }
    return rv;
  }

  public synchronized CutToClipboardOperation getCutToClipboardOperation(Statement statement) {
    Objects.requireNonNull(statement, "statement");
    CutToClipboardOperation rv = this.cutToClipboardOperations.get(statement);
    if (rv == null) {
      rv = new CutToClipboardOperation(statement);
      this.cutToClipboardOperations.put(statement, rv);
    }
    return rv;
  }

  public synchronized PasteFromClipboardOperation getPasteFromClipboardOperation(
      BlockStatementIndexPair blockStatementIndexPair) {
    Objects.requireNonNull(blockStatementIndexPair, "blockStatementIndexPair");
    PasteFromClipboardOperation rv = this.pasteFromClipboardOperations.get(blockStatementIndexPair);
    if (rv == null) {
      rv = new PasteFromClipboardOperation(blockStatementIndexPair);
      this.pasteFromClipboardOperations.put(blockStatementIndexPair, rv);
    }
    return rv;
  }

  public synchronized CopyFromClipboardOperation getCopyFromClipboardOperation(
      BlockStatementIndexPair blockStatementIndexPair) {
    Objects.requireNonNull(blockStatementIndexPair, "blockStatementIndexPair");
    CopyFromClipboardOperation rv = this.copyFromClipboardOperations.get(blockStatementIndexPair);
    if (rv == null) {
      rv = new CopyFromClipboardOperation(blockStatementIndexPair);
      this.copyFromClipboardOperations.put(blockStatementIndexPair, rv);
    }
    return rv;
  }
}
