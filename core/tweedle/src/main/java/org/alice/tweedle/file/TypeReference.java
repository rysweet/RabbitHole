package org.alice.tweedle.file;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public class TypeReference extends ResourceReference {
  public static final String CONTENT_TYPE = "Class";

  /**
   * Names of the other user-authored types this type references (supertype,
   * field types, method parameter/return types, and types named in its bodies),
   * computed with a bounded, non-tunneling crawl. Recorded so an importer can
   * detect, without loading the full AST, which sibling classes a single-class
   * import depends on. Only serialized when non-empty to keep legacy archives and
   * dependency-free types byte-identical.
   */
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public List<String> dependencies;

  public TypeReference() {
    super();
  }

  public TypeReference(String id, String fileName, String format) {
    super(id, fileName, format);
  }

  @Override
  public String getContentType() {
    return CONTENT_TYPE;
  }
}
