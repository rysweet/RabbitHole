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
package org.lgna.story.resourceutilities;

import edu.cmu.cs.dennisc.eula.swing.JEulaPane;
import edu.cmu.cs.dennisc.java.awt.WindowUtilities;
import edu.cmu.cs.dennisc.javax.swing.JDialogBuilder;
import edu.cmu.cs.dennisc.javax.swing.WindowStack;
import edu.cmu.cs.dennisc.ui.prompt.EulaPromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.MessagePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.MessageSeverity;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptResult;
import edu.cmu.cs.dennisc.ui.prompt.UiPromptBoundary;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;

public final class SwingUiPromptBoundary implements UiPromptBoundary {
  public static final SwingUiPromptBoundary INSTANCE = new SwingUiPromptBoundary();

  public SwingUiPromptBoundary() {
  }

  @Override
  public ResourcePromptResult requestResourceLocation(ResourcePromptRequest request) {
    File galleryDir = FindResourcesPanel.getInstance().getGalleryDir();
    if (request.alwaysPrompt() || (galleryDir == null)) {
      FindResourcesPanel.getInstance().show(null);
      galleryDir = FindResourcesPanel.getInstance().getGalleryDir();
    }
    return galleryDir != null ? ResourcePromptResult.selected(galleryDir) : ResourcePromptResult.noSelection();
  }

  @Override
  public void showMessage(MessagePromptRequest request) {
    if ((request.title() == null) && (request.severity() == MessageSeverity.INFO)) {
      JOptionPane.showMessageDialog(null, request.message());
    } else {
      JOptionPane.showMessageDialog(null, request.message(), request.title(), toJOptionPaneMessageType(request.severity()));
    }
  }

  @Override
  public boolean requestEulaAcceptance(EulaPromptRequest request) {
    JEulaPane eulaPane = new JEulaPane(request.licenseText());
    Component owner = WindowStack.peek();
    while (true) {
      JDialog dialog = new JDialogBuilder().owner(owner).isModal(true).title(request.title()).build();
      dialog.getContentPane().add(eulaPane, BorderLayout.CENTER);
      dialog.pack();
      if ((owner != null) && owner.isVisible()) {
        WindowUtilities.setLocationOnScreenToCenteredWithin(dialog, owner);
      }
      dialog.setVisible(true);
      if (eulaPane.isAccepted()) {
        return true;
      }
      String message = "You must accept the license agreement in order to use " + request.productName() + ".\n\nWould you like to return to the license agreement?";
      if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(owner, message, "Return to license agreement?", JOptionPane.YES_NO_OPTION)) {
        return false;
      }
    }
  }

  private static int toJOptionPaneMessageType(MessageSeverity severity) {
    return switch (severity) {
    case ERROR -> JOptionPane.ERROR_MESSAGE;
    case WARNING -> JOptionPane.WARNING_MESSAGE;
    case INFO -> JOptionPane.INFORMATION_MESSAGE;
    };
  }
}
