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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.cmu.cs.dennisc.java.util.Lists;
import org.lgna.project.VersionNotSupportedException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * JSON counterpart of {@link TypeXmlUtitlities} for the Gallery tile summary.
 *
 * <p>The hybrid export writes this summary as {@code typeSummary.json} alongside
 * the legacy {@code typeSummary.xml} sidecar so a Gallery tile can render
 * (name, hierarchy, resource, procedure/function/field lists) without loading
 * and decoding the full AST. The schema mirrors the XML tags one-for-one so the
 * two forms carry identical information.
 *
 * @author Copilot
 */
public final class TypeSummaryJsonUtilities {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final String VERSION = "version";
  private static final String TYPE_NAME = "typeName";
  private static final String HIERARCHY = "hierarchyClassNames";
  private static final String RESOURCE = "resource";
  private static final String CLASS_NAME = "className";
  private static final String FIELD_NAME = "fieldName";
  private static final String PROCEDURES = "procedureNames";
  private static final String FUNCTIONS = "functions";
  private static final String FIELDS = "fields";
  private static final String RETURN_CLASS_NAME = "returnClassName";
  private static final String VALUE_CLASS_NAME = "valueClassName";
  private static final String NAME = "name";

  private TypeSummaryJsonUtilities() {
    throw new AssertionError();
  }

  public static JsonNode encode(TypeSummary typeSummary) {
    ObjectNode root = OBJECT_MAPPER.createObjectNode();
    root.put(VERSION, typeSummary.getVersion());
    root.put(TYPE_NAME, typeSummary.getTypeName());

    ArrayNode hierarchy = root.putArray(HIERARCHY);
    for (String hierarchyClassName : typeSummary.getHierarchyClassNames()) {
      hierarchy.add(hierarchyClassName);
    }

    ResourceInfo resourceInfo = typeSummary.getResourceInfo();
    if (resourceInfo != null) {
      ObjectNode resource = root.putObject(RESOURCE);
      resource.put(CLASS_NAME, resourceInfo.getClassName());
      if (resourceInfo.getFieldName() != null) {
        resource.put(FIELD_NAME, resourceInfo.getFieldName());
      }
    }

    ArrayNode procedures = root.putArray(PROCEDURES);
    for (String procedureName : typeSummary.getProcedureNames()) {
      procedures.add(procedureName);
    }

    ArrayNode functions = root.putArray(FUNCTIONS);
    for (FunctionInfo functionInfo : typeSummary.getFunctionInfos()) {
      ObjectNode function = functions.addObject();
      function.put(RETURN_CLASS_NAME, functionInfo.getReturnClassName());
      function.put(NAME, functionInfo.getName());
    }

    ArrayNode fields = root.putArray(FIELDS);
    for (FieldInfo fieldInfo : typeSummary.getFieldInfos()) {
      ObjectNode field = fields.addObject();
      field.put(VALUE_CLASS_NAME, fieldInfo.getValueClassName());
      field.put(NAME, fieldInfo.getName());
    }

    return root;
  }

  public static byte[] toBytes(TypeSummary typeSummary) {
    try {
      return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(encode(typeSummary));
    } catch (IOException ioe) {
      throw new UncheckedIOException(ioe);
    }
  }

  public static TypeSummary decode(byte[] data) throws VersionNotSupportedException {
    JsonNode root;
    try {
      root = OBJECT_MAPPER.readTree(data);
    } catch (IOException ioe) {
      throw new UncheckedIOException(ioe);
    }
    double version = root.path(VERSION).asDouble();
    if (version < TypeSummary.MINIMUM_ACCEPTABLE_VERSION) {
      throw new VersionNotSupportedException(TypeSummary.MINIMUM_ACCEPTABLE_VERSION, version);
    }
    String typeName = root.path(TYPE_NAME).asText(null);

    List<String> hierarchyClassNames = Lists.newLinkedList();
    for (JsonNode node : root.path(HIERARCHY)) {
      hierarchyClassNames.add(node.asText());
    }

    ResourceInfo resourceInfo;
    JsonNode resource = root.get(RESOURCE);
    if ((resource != null) && !resource.isNull()) {
      String fieldName = resource.hasNonNull(FIELD_NAME) ? resource.get(FIELD_NAME).asText() : null;
      resourceInfo = new ResourceInfo(resource.path(CLASS_NAME).asText(null), fieldName);
    } else {
      resourceInfo = null;
    }

    List<String> procedureNames = Lists.newLinkedList();
    for (JsonNode node : root.path(PROCEDURES)) {
      procedureNames.add(node.asText());
    }

    List<FunctionInfo> functionInfos = Lists.newLinkedList();
    for (JsonNode node : root.path(FUNCTIONS)) {
      functionInfos.add(new FunctionInfo(node.path(RETURN_CLASS_NAME).asText(null), node.path(NAME).asText(null)));
    }

    List<FieldInfo> fieldInfos = Lists.newLinkedList();
    for (JsonNode node : root.path(FIELDS)) {
      fieldInfos.add(new FieldInfo(node.path(VALUE_CLASS_NAME).asText(null), node.path(NAME).asText(null)));
    }

    return new TypeSummary(version, typeName, hierarchyClassNames, resourceInfo, procedureNames, functionInfos, fieldInfos);
  }
}
