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

import edu.cmu.cs.dennisc.pattern.IsInstanceCrawler;
import org.lgna.project.ast.CrawlPolicy;
import org.lgna.project.ast.NamedUserType;

import java.util.Set;
import java.util.TreeSet;

/**
 * Resolves the bounded (non-tunneling) user-type dependencies of a type being
 * imported from the Gallery, mirroring the dependency list recorded in the
 * hybrid export manifest ({@code TypeReference.dependencies}).
 *
 * <p>The crawl deliberately collects only {@link NamedUserType} references
 * ({@link CrawlPolicy#INCLUDE_REFERENCES_BUT_DO_NOT_TUNNEL}), so built-in
 * story-API types ({@code JavaType}) are never reported — they are always
 * available and thus never "missing." This is pure logic with no UI so it can
 * be unit-tested headlessly; the import prompt itself lives at the operation
 * boundary.
 *
 * @author Copilot
 */
public final class ImportDependencyResolver {
  private ImportDependencyResolver() {
    throw new AssertionError();
  }

  /**
   * Returns the names of the user types directly referenced by {@code type}
   * (supertype, field types, parameter/return types, ...), excluding the type
   * itself. Bounded: references are visited but not tunneled into.
   */
  public static Set<String> collectDependencyNames(NamedUserType type) {
    Set<String> names = new TreeSet<>();
    if (type == null) {
      return names;
    }
    IsInstanceCrawler<NamedUserType> crawler = IsInstanceCrawler.createInstance(NamedUserType.class);
    type.crawl(crawler, CrawlPolicy.INCLUDE_REFERENCES_BUT_DO_NOT_TUNNEL);
    String ownName = type.getName();
    for (NamedUserType referenced : crawler.getList()) {
      String name = referenced.getName();
      if ((name != null) && !name.equals(ownName)) {
        names.add(name);
      }
    }
    return names;
  }

  /**
   * Returns the dependency names of {@code importedType} that are absent from
   * {@code availableTypeNames} (the names of the user types already present in
   * the destination project). Story-API types are excluded from the crawl, so
   * they never appear here.
   */
  public static Set<String> findMissingDependencyNames(NamedUserType importedType, Set<String> availableTypeNames) {
    Set<String> missing = new TreeSet<>();
    for (String dependency : collectDependencyNames(importedType)) {
      if (!availableTypeNames.contains(dependency)) {
        missing.add(dependency);
      }
    }
    return missing;
  }
}
