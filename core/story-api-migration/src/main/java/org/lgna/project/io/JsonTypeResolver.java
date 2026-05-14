/*******************************************************************************
 * Copyright (c) 2018 Carnegie Mellon University. All rights reserved.
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
package org.lgna.project.io;

import edu.cmu.cs.dennisc.java.io.InputStreamUtilities;
import org.alice.serialization.tweedle.TweedleEncoderDecoder;
import org.alice.serialization.tweedle.UnsupportedTweedleDecodeException;
import org.alice.tweedle.file.Manifest;
import org.alice.tweedle.file.ResourceReference;
import org.alice.tweedle.file.TypeReference;
import org.lgna.project.VersionNotSupportedException;
import org.lgna.project.ast.AbstractDeclaration;
import org.lgna.project.ast.NamedUserType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reads and verifies Tweedle type entries from a JSON project archive.
 * Used by {@link JsonProjectIo.JsonProjectReader} to decode type references.
 */
final class JsonTypeResolver {
  private static final String TWEEDLE_FORMAT = "tweedle";
  private static final int MAX_UNSUPPORTED_TWEEDLE_REASON_LENGTH = 512;

  private final ZipEntryContainer container;
  private final TweedleEncoderDecoder coder;

  JsonTypeResolver(ZipEntryContainer container, TweedleEncoderDecoder coder) {
    this.container = container;
    this.coder = coder;
  }

  TypeReadResult readTypes(
      Manifest manifest,
      boolean allowLiteralArithmeticFieldInitializers) throws IOException {
    TypeReadResult result = new TypeReadResult();
    if (manifest == null) {
      return result;
    }
    TypeReadPlan plan = typeReadPlan(manifest);
    result.hasTypeReferences = !plan.typeReferences.isEmpty();
    for (TypeReference typeReference : plan.typeReferences) {
      try {
        NamedUserType type = readTweedleType(
            typeReference,
            plan.typeTerminals,
            allowLiteralArithmeticFieldInitializers);
        if (type != null) {
          result.add(type);
        }
      } catch (UnsupportedTweedleDecodeException e) {
        result.addUnsupportedTweedleType(typeReference, e);
      }
    }
    return result;
  }

  private NamedUserType readTweedleType(
      TypeReference typeReference,
      Set<AbstractDeclaration> typeTerminals,
      boolean allowLiteralArithmeticFieldInitializers) throws IOException {
    if (!TWEEDLE_FORMAT.equals(typeReference.format)) {
      throw new IOException(
          "Unsupported type reference format '" + typeReference.format + "' for " + typeReferenceContext(typeReference));
    }
    if (typeReference.file == null) {
      throw new IOException("Type " + typeReference.name + " does not specify archive entry");
    }
    if (!ResourceExportNames.isSourceEntryName(typeReference.file)) {
      throw new IOException("Type " + typeReference.name + " references archive entry outside src directory: " + typeReference.file);
    }
    InputStream is = container.getInputStream(typeReference.file);
    if (is == null) {
      throw new IOException("Archive does not contain type entry " + typeReference.file);
    }
    try (InputStream typeStream = is) {
      byte[] typeBytes = InputStreamUtilities.getBytes(typeStream);
      org.lgna.project.ast.AbstractNode decoded = coder.decode(
          new String(typeBytes, StandardCharsets.UTF_8),
          typeTerminals,
          allowLiteralArithmeticFieldInitializers);
      if (decoded == null) {
        throw new IOException("Tweedle type entry " + typeReference.file + " decoded to null");
      }
      if (decoded instanceof NamedUserType namedUserType) {
        return namedUserType;
      }
      throw new IOException("Tweedle type entry " + typeReference.file + " did not decode to a user type");
    } catch (UnsupportedTweedleDecodeException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new IOException("Unable to decode Tweedle type entry " + typeReference.file, e);
    } catch (VersionNotSupportedException e) {
      throw new IOException("Unable to decode Tweedle type entry " + typeReference.file, e);
    }
  }

  // --- Verification helpers ---

  static NamedUserType fallbackTypeForUnnamedManifest(Manifest manifest, TypeReadResult decodedTypes) {
    if (JsonProjectManifest.hasNoManifestName(manifest) && !decodedTypes.types.isEmpty()) {
      return decodedTypes.types.iterator().next();
    }
    return null;
  }

  static void verifyTypeArchiveHasExpectedType(Manifest manifest, TypeReadResult decodedTypes) throws IOException {
    verifyArchiveHasExpectedType(
        "Type archive",
        "",
        "a type reference",
        manifest,
        decodedTypes);
  }

  static void verifyProjectArchiveHasExpectedProgramType(Manifest manifest, TypeReadResult decodedTypes) throws IOException {
    if (JsonProjectManifest.hasNoManifestName(manifest)) {
      return;
    }
    verifyArchiveHasExpectedType(
        "Project archive",
        "program type ",
        "a type reference for the program type",
        manifest,
        decodedTypes);
  }

  static void verifyArchiveHasNoUnsupportedManifestTypes(
      String archiveKind,
      TypeReadResult decodedTypes) throws IOException {
    if (decodedTypes.hasUnsupportedTweedleTypes()) {
      throw new IOException(
          archiveKind + " contains unsupported manifest-declared Tweedle type names "
              + decodedTypes.unsupportedTweedleTypeNames()
              + unsupportedDecodeReasonsClause(decodedTypes));
    }
  }

  // --- Private helpers ---

  private static TypeReadPlan typeReadPlan(Manifest manifest) {
    List<TypeReference> typeReferences = new ArrayList<>();
    Map<String, NamedUserType> terminalsByName = new LinkedHashMap<>();
    for (ResourceReference resourceReference : manifest.resources) {
      if (resourceReference instanceof TypeReference typeReference) {
        typeReferences.add(typeReference);
        if ((typeReference.name != null) && !typeReference.name.isEmpty()) {
          terminalsByName.computeIfAbsent(typeReference.name, name -> {
            NamedUserType terminal = new NamedUserType();
            terminal.name.setValue(name);
            return terminal;
          });
        }
      }
    }
    return new TypeReadPlan(typeReferences, new LinkedHashSet<>(terminalsByName.values()));
  }

  private static void verifyArchiveHasExpectedType(
      String archiveKind,
      String expectedNameRole,
      String missingReferenceDescription,
      Manifest manifest,
      TypeReadResult decodedTypes) throws IOException {
    String expectedName = JsonProjectManifest.manifestName(manifest);
    if (decodedTypes.hasTypeReferences) {
      throw new IOException(
          archiveKind + " manifest names " + expectedNameRole + "'" + expectedName
              + "' but decoded type names are " + decodedTypeNames(decodedTypes.types)
              + unsupportedTypeNamesClause(decodedTypes)
              + unsupportedDecodeReasonsClause(decodedTypes));
    }
    throw new IOException(
        archiveKind + " manifest for '" + expectedName + "' does not contain " + missingReferenceDescription);
  }

  private static String decodedTypeNames(Set<NamedUserType> decodedTypes) {
    return decodedTypes.stream()
        .map(NamedUserType::getName)
        .sorted()
        .collect(Collectors.joining(", ", "[", "]"));
  }

  private static String unsupportedTypeNamesClause(TypeReadResult decodedTypes) {
    if (!decodedTypes.hasUnsupportedTweedleTypes()) {
      return "";
    }
    return "; unsupported manifest-declared Tweedle type names are " + decodedTypes.unsupportedTweedleTypeNames();
  }

  private static String unsupportedDecodeReasonsClause(TypeReadResult decodedTypes) {
    if (!decodedTypes.hasUnsupportedTweedleTypes()) {
      return "";
    }
    return "; unsupported Tweedle decode reasons are " + decodedTypes.unsupportedTweedleDecodeReasons();
  }

  private static String typeReferenceContext(TypeReference typeReference) {
    StringBuilder sb = new StringBuilder("type reference");
    if ((typeReference.name != null) && !typeReference.name.isEmpty()) {
      sb.append(" '").append(typeReference.name).append("'");
    }
    if ((typeReference.file != null) && !typeReference.file.isEmpty()) {
      sb.append(" at archive entry '").append(typeReference.file).append("'");
    }
    return sb.toString();
  }

  // --- Inner result types ---

  static final class TypeReadResult {
    final Set<NamedUserType> types = new LinkedHashSet<>();
    private final Map<String, NamedUserType> typesByName = new HashMap<>();
    private final Map<String, UnsupportedTweedleDecodeException> unsupportedTweedleDecodeCausesByTypeName = new HashMap<>();
    boolean hasTypeReferences;

    void add(NamedUserType type) {
      types.add(type);
      if (type.getName() != null) {
        typesByName.putIfAbsent(type.getName(), type);
      }
    }

    NamedUserType findByName(String name) {
      return (name == null) ? null : typesByName.get(name);
    }

    void addUnsupportedTweedleType(TypeReference typeReference, UnsupportedTweedleDecodeException e) {
      String typeName = unsupportedTweedleTypeName(typeReference);
      unsupportedTweedleDecodeCausesByTypeName.putIfAbsent(typeName, e);
    }

    boolean hasUnsupportedTweedleDecodeFor(String name) {
      return (name != null) && unsupportedTweedleDecodeCausesByTypeName.containsKey(name);
    }

    UnsupportedTweedleDecodeException unsupportedTweedleDecodeCauseFor(String name) {
      return (name == null) ? null : unsupportedTweedleDecodeCausesByTypeName.get(name);
    }

    boolean hasUnsupportedTweedleTypes() {
      return !unsupportedTweedleDecodeCausesByTypeName.isEmpty();
    }

    String unsupportedTweedleTypeNames() {
      return unsupportedTweedleDecodeCausesByTypeName.keySet().stream()
          .sorted()
          .collect(Collectors.joining(", ", "[", "]"));
    }

    String unsupportedTweedleDecodeReasons() {
      return unsupportedTweedleDecodeCausesByTypeName.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .map(entry -> entry.getKey() + ": " + unsupportedTweedleDecodeReason(entry.getValue()))
          .collect(Collectors.joining(", ", "[", "]"));
    }

    private static String unsupportedTweedleTypeName(TypeReference typeReference) {
      if ((typeReference.name != null) && !typeReference.name.isEmpty()) {
        return typeReference.name;
      }
      if ((typeReference.file != null) && !typeReference.file.isEmpty()) {
        return typeReference.file;
      }
      return "<unnamed>";
    }

    private static String unsupportedTweedleDecodeReason(UnsupportedTweedleDecodeException e) {
      String message = e.getMessage();
      if ((message != null) && !message.isBlank()) {
        return boundedSingleLineUnsupportedTweedleReason(message);
      }
      return e.getClass().getSimpleName();
    }

    private static String boundedSingleLineUnsupportedTweedleReason(String message) {
      String singleLine = message.strip().replaceAll("\\s+", " ");
      if (singleLine.length() <= MAX_UNSUPPORTED_TWEEDLE_REASON_LENGTH) {
        return singleLine;
      }
      return singleLine.substring(0, MAX_UNSUPPORTED_TWEEDLE_REASON_LENGTH - 3) + "...";
    }
  }

  private static final class TypeReadPlan {
    final List<TypeReference> typeReferences;
    final Set<AbstractDeclaration> typeTerminals;

    TypeReadPlan(List<TypeReference> typeReferences, Set<AbstractDeclaration> typeTerminals) {
      this.typeReferences = typeReferences;
      this.typeTerminals = typeTerminals;
    }
  }
}
