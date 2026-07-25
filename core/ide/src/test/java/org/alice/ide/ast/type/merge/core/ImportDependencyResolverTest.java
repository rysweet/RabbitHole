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

package org.alice.ide.ast.type.merge.core;

import org.junit.Test;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;

import java.util.HashSet;
import java.util.Set;

import static org.alice.ide.ast.type.merge.core.MergeUtilitiesTestSupport.addField;
import static org.alice.ide.ast.type.merge.core.MergeUtilitiesTestSupport.namedType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the bounded dependency crawl and missing-dependency resolution used
 * by the Gallery single-class import prompt.
 */
public class ImportDependencyResolverTest {

  @Test
  public void collectDependencyNamesIncludesReferencedUserFieldTypeButNotSelf() {
    NamedUserType prop = namedType("Prop");
    NamedUserType scene = namedType("Scene");
    addField(scene, "prop", prop);

    Set<String> dependencies = ImportDependencyResolver.collectDependencyNames(scene);

    assertTrue("Referenced user-type field must be recorded as a dependency", dependencies.contains("Prop"));
    assertFalse("A type must not list itself as a dependency", dependencies.contains("Scene"));
  }

  @Test
  public void collectDependencyNamesIgnoresBuiltInJavaTypeReferences() {
    NamedUserType scene = namedType("SceneWithBuiltInFieldsOnly");
    addField(scene, "count", JavaType.getInstance(Object.class));

    Set<String> dependencies = ImportDependencyResolver.collectDependencyNames(scene);

    assertTrue("Built-in (JavaType) references must not be reported as dependencies", dependencies.isEmpty());
  }

  @Test
  public void findMissingDependencyNamesReturnsOnlyAbsentDependencies() {
    NamedUserType prop = namedType("Prop");
    NamedUserType helper = namedType("Helper");
    NamedUserType scene = namedType("Scene");
    addField(scene, "prop", prop);
    addField(scene, "helper", helper);

    Set<String> available = new HashSet<>();
    available.add("Prop");

    Set<String> missing = ImportDependencyResolver.findMissingDependencyNames(scene, available);

    assertEquals("Only the dependency absent from the project should be reported missing",
        Set.of("Helper"), missing);
  }

  @Test
  public void findMissingDependencyNamesIsEmptyWhenAllDependenciesArePresent() {
    NamedUserType prop = namedType("Prop");
    NamedUserType scene = namedType("Scene");
    addField(scene, "prop", prop);

    Set<String> available = new HashSet<>();
    available.add("Prop");

    Set<String> missing = ImportDependencyResolver.findMissingDependencyNames(scene, available);

    assertTrue("No dependency should be reported missing when all are present", missing.isEmpty());
  }
}
