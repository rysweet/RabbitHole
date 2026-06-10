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
import org.alice.ide.ProjectApplication;
import org.lgna.croquet.Application;
import org.lgna.project.Project;

import java.util.Map;
import java.util.Objects;

public final class ClipboardOperationRegistries {
  private static final ThreadLocal<ClipboardOperationRegistry> THREAD_REGISTRY = new ThreadLocal<>();
  private static final Map<Project, ClipboardOperationRegistry> PROJECT_REGISTRIES = Maps.newWeakHashMap();
  private static final Map<Application<?>, ClipboardOperationRegistry> APPLICATION_REGISTRIES =
      Maps.newWeakHashMap();
  private static final ClipboardOperationRegistry FALLBACK_REGISTRY = new ClipboardOperationRegistry();

  private ClipboardOperationRegistries() {
    throw new Error();
  }

  public static ClipboardOperationRegistry getActiveRegistry() {
    ClipboardOperationRegistry threadRegistry = THREAD_REGISTRY.get();
    if (threadRegistry != null) {
      return threadRegistry;
    }

    ProjectApplication projectApplication = ProjectApplication.getActiveInstance();
    if (projectApplication != null) {
      Project project = projectApplication.getProject();
      if (project != null) {
        return getRegistry(PROJECT_REGISTRIES, project);
      }
    }

    Application<?> application = Application.getActiveInstance();
    if (application != null) {
      return getRegistry(APPLICATION_REGISTRIES, application);
    }

    return FALLBACK_REGISTRY;
  }

  public static RegistryScope useRegistry(ClipboardOperationRegistry registry) {
    Objects.requireNonNull(registry, "registry");
    ClipboardOperationRegistry previousRegistry = THREAD_REGISTRY.get();
    THREAD_REGISTRY.set(registry);
    return new RegistryScopeImpl(previousRegistry);
  }

  private static <K> ClipboardOperationRegistry getRegistry(
      Map<K, ClipboardOperationRegistry> registries, K key) {
    synchronized (registries) {
      ClipboardOperationRegistry rv = registries.get(key);
      if (rv == null) {
        rv = new ClipboardOperationRegistry();
        registries.put(key, rv);
      }
      return rv;
    }
  }

  public interface RegistryScope extends AutoCloseable {
    @Override
    void close();
  }

  private static final class RegistryScopeImpl implements RegistryScope {
    private final ClipboardOperationRegistry previousRegistry;
    private boolean isClosed;

    private RegistryScopeImpl(ClipboardOperationRegistry previousRegistry) {
      this.previousRegistry = previousRegistry;
    }

    @Override
    public void close() {
      if (this.isClosed) {
        throw new IllegalStateException("Clipboard registry scope is already closed.");
      }
      if (this.previousRegistry != null) {
        THREAD_REGISTRY.set(this.previousRegistry);
      } else {
        THREAD_REGISTRY.remove();
      }
      this.isClosed = true;
    }
  }
}
