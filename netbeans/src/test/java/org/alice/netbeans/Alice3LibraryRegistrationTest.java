package org.alice.netbeans;

import org.junit.Assume;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Alice3LibraryRegistrationTest {
  private static final String LIBRARY_PATH = "target/classes/org/alice/netbeans/Alice3Library.xml";
  private static final String MANIFEST_PATH = "target/classes/META-INF/MANIFEST.MF";
  private static final String MODULE_EXTENSION_ROOT = "nbinst:/modules/ext/org.alice.netbeans/";
  private static final String MODULE_EXTENSION_MANIFEST_ROOT = "ext/org.alice.netbeans/";
  private static final Set<String> SIMS_ONLY_CLASSPATH_RESOURCES = Set.of(
      MODULE_EXTENSION_ROOT + "org-alice-nonfree/models-nonfree.jar",
      MODULE_EXTENSION_ROOT + "org-alice-nonfree/story-api-nonfree.jar");
  private static final Set<String> NONFREE_ARTIFACT_MARKERS = Set.of(
      "models-nonfree",
      "story-api-nonfree",
      "ide-nonfree",
      "resources-nonfree",
      "org-alice-nonfree");

  @Test
  public void layerRegistersAlice3LibraryDefinition() throws Exception {
    String layer = Files.readString(
        Path.of("target/classes/org/alice/netbeans/layer.xml"),
        StandardCharsets.UTF_8);

    assertTrue(layer.contains("<folder name=\"org-netbeans-api-project-libraries\">"));
    assertTrue(layer.contains("<folder name=\"Libraries\">"));
    assertTrue(layer.contains("<file name=\"Alice3Library.xml\" url=\"Alice3Library.xml\"/>"));
  }

  @Test
  public void alice3LibraryDeclaresExportedProjectVolumes() throws Exception {
    String library = Files.readString(
        Path.of("target/classes/org/alice/netbeans/Alice3Library.xml"),
        StandardCharsets.UTF_8);

    assertTrue(library.contains("<name>Alice3Library</name>"));
    assertTrue(library.contains("<type>j2se</type>"));
    assertTrue(library.contains("<type>classpath</type>"));
    assertTrue(library.contains("nbinst:/modules/ext/org.alice.netbeans/org-alice/util.jar"));
    assertTrue(library.contains("nbinst:/modules/ext/org.alice.netbeans/org-alice/story-api.jar"));
    assertTrue(library.contains("nbinst:/modules/ext/org.alice.netbeans/org-openjfx/javafx-graphics.jar"));
    assertTrue(library.contains("<type>src</type>"));
    assertTrue(library.contains("nbinst:/src/aliceSource.jar"));
    assertTrue(library.contains("<type>javadoc</type>"));
    assertTrue(library.contains("nbinst:/doc/aliceDocs.zip"));
  }

  @Test
  public void alice3LibraryClasspathSurrogateUsesPackagedJarLocations() throws Exception {
    List<String> classpathResources = resourcesForVolume("classpath");

    assertEquals(new HashSet<>(classpathResources).size(), classpathResources.size());
    assertTrue(classpathResources.containsAll(Set.of(
        MODULE_EXTENSION_ROOT + "org-alice/util.jar",
        MODULE_EXTENSION_ROOT + "org-alice/story-api.jar",
        MODULE_EXTENSION_ROOT + "org-openjfx/javafx-base.jar",
        MODULE_EXTENSION_ROOT + "org-openjfx/javafx-graphics.jar",
        MODULE_EXTENSION_ROOT + "org-openjfx/javafx-media.jar")));

    for (String resource : classpathResources) {
      assertTrue(resource, resource.startsWith(MODULE_EXTENSION_ROOT));
      assertTrue(resource, resource.endsWith(".jar"));
      assertTrue(
          resource,
          resource.substring(MODULE_EXTENSION_ROOT.length()).matches("[a-z0-9-]+/[A-Za-z0-9_.-]+\\.jar"));
    }
  }

  @Test
  public void alice3LibraryClasspathEntriesResolveToNetBeansModuleArtifacts() throws Exception {
    Set<String> moduleClassPathEntries = moduleManifestClassPathEntries();

    for (String resource : resourcesForVolume("classpath")) {
      String moduleEntry = toModuleClassPathEntry(resource);
      if (!includeSims() && SIMS_ONLY_CLASSPATH_RESOURCES.contains(resource)) {
        assertFalse(
            "no-Sims module should not package Sims-only resource " + resource,
            moduleClassPathEntries.contains(moduleEntry));
        continue;
      }
      assertTrue(resource, moduleClassPathEntries.contains(moduleEntry));
    }
  }

  @Test
  public void includeSimsProfilesRequireExplicitOptIn() throws Exception {
    assertIncludeSimsProfileRequiresExplicitTrue(Path.of("../pom.xml"));
    assertIncludeSimsProfileRequiresExplicitTrue(Path.of("../alice-ide/pom.xml"));
    assertIncludeSimsProfileRequiresExplicitTrue(Path.of("pom.xml"));
  }

  @Test
  public void includeSimsLibraryDefinitionIncludesNonfreeClasspathEntries() throws Exception {
    Assume.assumeTrue("includeSims guard only applies with -DincludeSims=true", includeSims());

    assertTrue(
        "includeSims library definition should include Sims-only classpath resources",
        resourcesForVolume("classpath").containsAll(SIMS_ONLY_CLASSPATH_RESOURCES));
  }

  @Test
  public void defaultLibraryAndManifestOmitNonfreeArtifacts() throws Exception {
    Assume.assumeFalse("default open-asset guard only applies without -DincludeSims=true", includeSims());

    assertNoNonfreeMarkers("Alice3Library classpath", resourcesForVolume("classpath"));
    assertNoNonfreeMarkers("NetBeans module manifest classpath", moduleManifestClassPathEntries());
  }

  @Test
  public void defaultRuntimeClasspathOmitsNonfreeJars() {
    Assume.assumeFalse("default open-asset guard only applies without -DincludeSims=true", includeSims());

    List<String> jarNames = Arrays.stream(System.getProperty(
            "surefire.test.class.path",
            System.getProperty("java.class.path", "")).split(File.pathSeparator))
        .filter(entry -> entry.endsWith(".jar"))
        .map(entry -> Path.of(entry).getFileName().toString())
        .toList();

    assertNoNonfreeMarkers("Surefire runtime jar classpath", jarNames);
  }

  @Test
  public void defaultResourceDistributionUsesOpenAliceAssetsAndOmitsSimsAssets() throws Exception {
    Assume.assumeFalse("default open-asset guard only applies without -DincludeSims=true", includeSims());

    Path applicationResources = Path.of("../core/resources/src/application/resources");
    assertTrue("default application resources should exist", Files.exists(applicationResources));
    assertTrue(
        "default resource distribution should include open Alice gallery assets",
        Files.exists(applicationResources.resolve("gallery/assets/alice")));
    assertFalse(
        "default resource distribution must not contain Sims assets",
        Files.exists(applicationResources.resolve("gallery/assets/sims")));
    assertFalse(
        "default resource distribution must not contain Sims EULA",
        Files.exists(applicationResources.resolve("EULA_TheSimsTM2ArtAsset.txt")));
  }

  @Test
  public void pomPackagesAliceLibrarySourceAndJavadocVolumes() throws Exception {
    String pom = Files.readString(Path.of("pom.xml"), StandardCharsets.UTF_8);

    assertTrue(pom.contains("<id>javadoc</id>"));
    assertTrue(pom.contains("<descriptor>src/main/resources/assemblies/rename-javadoc.xml</descriptor>"));
    assertTrue(pom.contains("<id>story-src</id>"));
    assertTrue(pom.contains("<descriptor>src/main/resources/assemblies/story-src.xml</descriptor>"));
    assertTrue(pom.contains("<finalName>nbm/clusters/extra/src/aliceSource</finalName>"));
    assertTrue(pom.contains("<id>final-name</id>"));
    assertTrue(pom.contains("<descriptor>src/main/resources/assemblies/rename-nbm.xml</descriptor>"));
  }

  @Test
  public void installAssemblyRenamesPackagedPluginForDistribution() throws Exception {
    String descriptor = Files.readString(
        Path.of("src/main/resources/assemblies/rename-nbm.xml"),
        StandardCharsets.UTF_8);

    assertTrue(descriptor.contains("<source>${project.build.directory}/netbeans-9.1.0-SNAPSHOT.nbm</source>"));
    assertTrue(descriptor.contains("<destName>Alice3_netbeans_plugin_${alice.build.version}${alice.build.prerelease}${alice.build.metadata}.nbm</destName>"));
  }

  private static List<String> resourcesForVolume(String volumeType) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    var builder = factory.newDocumentBuilder();
    builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
    Document document = builder.parse(Path.of(LIBRARY_PATH).toFile());
    NodeList volumes = document.getElementsByTagName("volume");
    return IntStream.range(0, volumes.getLength())
        .mapToObj(index -> (Element) volumes.item(index))
        .filter(volume -> volumeType.equals(volume.getElementsByTagName("type").item(0).getTextContent()))
        .flatMap(volume -> elements(volume.getElementsByTagName("resource")).stream())
        .map(Element::getTextContent)
        .toList();
  }

  private static List<Element> elements(NodeList nodes) {
    return IntStream.range(0, nodes.getLength())
        .mapToObj(index -> (Element) nodes.item(index))
        .toList();
  }

  private static Set<String> moduleManifestClassPathEntries() throws Exception {
    try (var manifestStream = Files.newInputStream(Path.of(MANIFEST_PATH))) {
      String classPath = new Manifest(manifestStream)
          .getMainAttributes()
          .getValue("X-Class-Path");

      assertTrue("Generated NetBeans manifest must declare X-Class-Path", classPath != null);
      return new HashSet<>(Arrays.asList(classPath.split("\\s+")));
    }
  }

  private static String toModuleClassPathEntry(String resource) {
    assertTrue(resource, resource.startsWith(MODULE_EXTENSION_ROOT));
    return MODULE_EXTENSION_MANIFEST_ROOT + resource.substring(MODULE_EXTENSION_ROOT.length());
  }

  private static void assertNoNonfreeMarkers(String source, Iterable<String> values) {
    for (String value : values) {
      for (String marker : NONFREE_ARTIFACT_MARKERS) {
        assertFalse(
            source + " should not contain " + marker + ": " + value,
            value.contains(marker));
      }
    }
  }

  private static void assertIncludeSimsProfileRequiresExplicitTrue(Path pomPath) throws Exception {
    String pom = Files.readString(pomPath, StandardCharsets.UTF_8);
    int profileStart = pom.indexOf("<id>includeSims</id>");
    assertTrue("Missing includeSims profile in " + pomPath, profileStart >= 0);
    int activationStart = pom.indexOf("<activation>", profileStart);
    int activationEnd = pom.indexOf("</activation>", activationStart);
    assertTrue("Missing includeSims activation in " + pomPath, activationStart >= 0);
    assertTrue("Missing includeSims activation end in " + pomPath, activationEnd > activationStart);

    String activation = pom.substring(activationStart, activationEnd);
    assertTrue(
        "includeSims profile must activate only when -DincludeSims=true in " + pomPath,
        activation.contains("<name>includeSims</name>") && activation.contains("<value>true</value>"));
    assertFalse(
        "includeSims profile must not be active by default in " + pomPath,
        activation.contains("<value>!false</value>"));
  }

  private static boolean includeSims() {
    return "true".equals(System.getProperty("includeSims"));
  }
}
