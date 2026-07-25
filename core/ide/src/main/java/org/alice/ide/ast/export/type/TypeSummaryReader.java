/*
 * Copyright (c) 2006-2025, Carnegie Mellon University. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Products derived from the software may not be called "Alice", nor may
 *    "Alice" appear in their name, without prior written permission of Carnegie
 *    Mellon University.
 *
 * 4. All advertising materials mentioning features or use of this software must
 *    display the following acknowledgement: "This product includes software
 *    developed by Carnegie Mellon University"
 *
 * 5. The gallery of art assets and animations provided with this software is
 *    contributed by Electronic Arts Inc. and may be used for personal,
 *    non-commercial, and academic use only. Redistributions of any program
 *    source code that utilizes the Alice Gallery of art assets and animations
 *    must reproduce the above copyright notice.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHORS AND CONTRIBUTORS "AS IS" AND ANY
 * AND ALL EXPRESS, STATUTORY OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE, TITLE, AND NON-INFRINGEMENT ARE DISCLAIMED. IN NO EVENT SHALL THE
 * AUTHORS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.alice.ide.ast.export.type;

import edu.cmu.cs.dennisc.xml.XMLUtilities;
import org.lgna.project.VersionNotSupportedException;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.util.Map;

/**
 * Reads a Gallery tile's {@link TypeSummary} from an extracted archive,
 * preferring the JSON summary ({@code typeSummary.json}) and falling back to the
 * legacy XML sidecar ({@code typeSummary.xml}). Returns {@code null} when neither
 * form is present. Kept UI-free so the JSON-first preference is unit-testable.
 *
 * @author Copilot
 */
public final class TypeSummaryReader {
  private TypeSummaryReader() {
    throw new AssertionError();
  }

  public static TypeSummary read(Map<String, byte[]> filenameToData) throws VersionNotSupportedException {
    if (filenameToData == null) {
      return null;
    }
    byte[] jsonData = filenameToData.get(TypeSummaryJsonDataSource.FILENAME);
    if (jsonData != null) {
      return TypeSummaryJsonUtilities.decode(jsonData);
    }
    byte[] xmlData = filenameToData.get(TypeSummaryDataSource.FILENAME);
    if (xmlData != null) {
      Document xmlDocument = XMLUtilities.read(new ByteArrayInputStream(xmlData));
      return TypeXmlUtitlities.decode(xmlDocument);
    }
    return null;
  }
}
