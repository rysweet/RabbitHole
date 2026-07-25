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

import org.junit.Test;
import org.lgna.project.VersionNotSupportedException;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TypeSummaryJsonUtilitiesTest {

  private static TypeSummary createSummary() {
    return new TypeSummary(
        3.1,
        "Hero",
        new ArrayList<>(Arrays.asList("ParentType", "java.lang.Object")),
        new ResourceInfo("org.example.HeroResource", "DEFAULT"),
        new ArrayList<>(Arrays.asList("walk", "jump")),
        new ArrayList<>(Arrays.asList(new FunctionInfo("java.lang.Integer", "getScore"), new FunctionInfo("java.lang.Boolean", "isHappy"))),
        new ArrayList<>(Arrays.asList(new FieldInfo("java.lang.String", "name"))));
  }

  @Test
  public void roundTripPreservesEveryField() throws VersionNotSupportedException {
    TypeSummary decoded = TypeSummaryJsonUtilities.decode(TypeSummaryJsonUtilities.toBytes(createSummary()));

    assertEquals(3.1, decoded.getVersion(), 0.0001);
    assertEquals("Hero", decoded.getTypeName());
    assertEquals(Arrays.asList("ParentType", "java.lang.Object"), decoded.getHierarchyClassNames());
    assertEquals(Arrays.asList("walk", "jump"), decoded.getProcedureNames());

    assertNotNull(decoded.getResourceInfo());
    assertEquals("org.example.HeroResource", decoded.getResourceInfo().getClassName());
    assertEquals("DEFAULT", decoded.getResourceInfo().getFieldName());

    assertEquals(2, decoded.getFunctionInfos().size());
    assertEquals("java.lang.Integer", decoded.getFunctionInfos().get(0).getReturnClassName());
    assertEquals("getScore", decoded.getFunctionInfos().get(0).getName());

    assertEquals(1, decoded.getFieldInfos().size());
    assertEquals("java.lang.String", decoded.getFieldInfos().get(0).getValueClassName());
    assertEquals("name", decoded.getFieldInfos().get(0).getName());
  }

  @Test
  public void roundTripOmitsAndRestoresNullResourceInfo() throws VersionNotSupportedException {
    TypeSummary summary = new TypeSummary(
        3.1, "Hero", new ArrayList<>(), null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

    TypeSummary decoded = TypeSummaryJsonUtilities.decode(TypeSummaryJsonUtilities.toBytes(summary));

    assertNull("A summary without a resource must decode a null ResourceInfo", decoded.getResourceInfo());
  }

  @Test
  public void resourceInfoWithoutFieldNameRoundTrips() throws VersionNotSupportedException {
    TypeSummary summary = new TypeSummary(
        3.1, "Hero", new ArrayList<>(), new ResourceInfo("org.example.HeroResource", null),
        new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

    TypeSummary decoded = TypeSummaryJsonUtilities.decode(TypeSummaryJsonUtilities.toBytes(summary));

    assertNotNull(decoded.getResourceInfo());
    assertEquals("org.example.HeroResource", decoded.getResourceInfo().getClassName());
    assertNull(decoded.getResourceInfo().getFieldName());
  }

  @Test(expected = VersionNotSupportedException.class)
  public void decodeRejectsUnsupportedOlderVersion() throws VersionNotSupportedException {
    TypeSummary tooOld = new TypeSummary(
        3.0, "Hero", new ArrayList<>(), null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    TypeSummaryJsonUtilities.decode(TypeSummaryJsonUtilities.toBytes(tooOld));
  }
}
