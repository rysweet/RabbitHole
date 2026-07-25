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
import org.junit.Test;
import org.lgna.project.VersionNotSupportedException;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TypeSummaryReaderTest {

  private static TypeSummary summary(String typeName) {
    return new TypeSummary(
        3.1,
        typeName,
        new ArrayList<>(Arrays.asList("ParentType")),
        new ResourceInfo("org.example.Resource", "DEFAULT"),
        new ArrayList<>(Arrays.asList("walk")),
        new ArrayList<>(),
        new ArrayList<>());
  }

  private static byte[] xmlBytes(TypeSummary typeSummary) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    XMLUtilities.write(TypeXmlUtitlities.encode(typeSummary), out);
    return out.toByteArray();
  }

  @Test
  public void prefersJsonWhenBothFormsArePresent() throws Exception {
    Map<String, byte[]> data = new HashMap<>();
    data.put(TypeSummaryJsonDataSource.FILENAME, TypeSummaryJsonUtilities.toBytes(summary("FromJson")));
    data.put(TypeSummaryDataSource.FILENAME, xmlBytes(summary("FromXml")));

    TypeSummary result = TypeSummaryReader.read(data);

    assertEquals("JSON summary must win when both forms are present", "FromJson", result.getTypeName());
  }

  @Test
  public void fallsBackToXmlWhenJsonIsAbsent() throws Exception {
    Map<String, byte[]> data = new HashMap<>();
    data.put(TypeSummaryDataSource.FILENAME, xmlBytes(summary("FromXml")));

    TypeSummary result = TypeSummaryReader.read(data);

    assertEquals("XML summary must be used when no JSON summary exists", "FromXml", result.getTypeName());
  }

  @Test
  public void returnsNullWhenNeitherFormIsPresent() throws VersionNotSupportedException {
    assertNull(TypeSummaryReader.read(new HashMap<>()));
  }

  @Test
  public void returnsNullForNullMap() throws VersionNotSupportedException {
    assertNull(TypeSummaryReader.read(null));
  }
}
