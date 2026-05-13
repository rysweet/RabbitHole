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
package edu.cmu.cs.dennisc.scenegraph.io;

import edu.cmu.cs.dennisc.color.Color4f;
import edu.cmu.cs.dennisc.image.ImageUtilities;
import edu.cmu.cs.dennisc.java.io.FileUtilities;
import edu.cmu.cs.dennisc.java.lang.reflect.ReflectionUtilities;
import edu.cmu.cs.dennisc.property.InstanceProperty;
import edu.cmu.cs.dennisc.scenegraph.Component;
import edu.cmu.cs.dennisc.scenegraph.Composite;
import edu.cmu.cs.dennisc.scenegraph.Vertex;
import edu.cmu.cs.dennisc.texture.TextureCoordinate2f;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.Matrix3x3;
import org.alice.math.immutable.Point3;
import org.alice.math.immutable.Vector2f;
import org.alice.math.immutable.Vector3f;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.MemoryImageSource;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.zip.*;

/**
 * Handles all decoding (deserialization) logic for ASG scene graph I/O.
 * Extracted from ASG.java to reduce file size and improve maintainability.
 *
 * @author Dennis Cosgrove
 */
class ASGDecoder {

  static abstract class AbstractPropertyReference {
    private edu.cmu.cs.dennisc.scenegraph.Element m_element;
    private InstanceProperty m_property;

    public AbstractPropertyReference(edu.cmu.cs.dennisc.scenegraph.Element element, InstanceProperty property) {
      m_element = element;
      m_property = property;
    }

    public abstract void resolve(HashMap<Integer, edu.cmu.cs.dennisc.scenegraph.Element> map);

    protected edu.cmu.cs.dennisc.scenegraph.Element getElement() {
      return m_element;
    }

    protected String getPropertyName() {
      return m_property.getName();
    }

    protected void setPropertyValue(Object value) {
      m_property.setValue(value);
    }
  }

  static class PropertyReferenceToElement extends AbstractPropertyReference {
    private Integer m_key;

    public PropertyReferenceToElement(edu.cmu.cs.dennisc.scenegraph.Element element, InstanceProperty<?> property, Integer key) {
      super(element, property);
      m_key = key;
    }

    @Override
    public void resolve(HashMap<Integer, edu.cmu.cs.dennisc.scenegraph.Element> map) {
      edu.cmu.cs.dennisc.scenegraph.Element value = map.get(m_key);
      if (value != null) {
        setPropertyValue(value);
      } else {
        throw new RuntimeException("could not resolve reference- element: " + getElement() + "; propertyName: " + getPropertyName() + "; key: " + m_key);
      }
    }
  }

  private static final Set<String> DEAD_PROPERTIES = Set.of(
      "IsFirstClass", "OpacityMap", "EmissiveColorMap", "SpecularHighlightColorMap",
      "BumpMap", "DetailMap", "Format", "VertexLowerBound", "VertexUpperBound"
  );

  private static boolean isDeadProperty(String property) {
    return DEAD_PROPERTIES.contains(property);
  }

  private static String convertPropertyIfNecessary(String property) {
    if (property.equals("DiffuseColorMap")) {
      property = "DiffuseColorTexture";
    }
    if (property.equals("Indices")) {
      property = "TriangleData";
    }
    return property;
  }

  private static final String OLD_PACKAGE = "edu.cmu.cs.stage3.";

  private static String convertClassnameIfNecessary(String className) {
    if (className.equals("edu.cmu.cs.stage3.alice.scenegraph.Color")) {
      className = "edu.cmu.cs.dennisc.color.Color4f";
    }
    if (className.startsWith(OLD_PACKAGE)) {
      className = "edu.cmu.cs.dennisc." + className.substring(OLD_PACKAGE.length());
    }
    if (className.startsWith("[L" + OLD_PACKAGE)) {
      className = "[Lorg." + className.substring(2 + OLD_PACKAGE.length());
    }
    if (className.endsWith("Vertex3d;")) {
      className = className.substring(0, className.length() - 3) + ';';
    }
    if (className.endsWith("Vertex3d")) {
      className = className.substring(0, className.length() - 2);
    }
    if (className.endsWith("TextureMap")) {
      className = className.substring(0, className.length() - 3);
    }
    return className;
  }

  static Vertex[] decodeVertexArrayInBinary(InputStream is) {
    Vertex[] vertices = null;
    BufferedInputStream bis = new BufferedInputStream(is);
    DataInputStream dis = new DataInputStream(bis);
    try {
      int version = dis.readInt();
      if (version == 1) {
        int vertexCount = dis.readInt();
        vertices = new Vertex[vertexCount];
        for (int index = 0; index < vertices.length; index++) {
          double x = dis.readDouble();
          double y = dis.readDouble();
          double z = dis.readDouble();
          float i = (float) dis.readDouble();
          float j = (float) dis.readDouble();
          float k = (float) dis.readDouble();
          float u = (float) dis.readDouble();
          float v = (float) dis.readDouble();
          vertices[index] = Vertex.createXYZIJKUV(x, y, z, i, j, k, u, v);
        }
      } else if (version == 2) {
        int vertexCount = dis.readInt();
        vertices = new Vertex[vertexCount];
        for (int index = 0; index < vertices.length; index++) {
          int format = dis.readInt();
          Point3 position = Point3.NaN;
          if ((format & Vertex.FORMAT_POSITION) != 0) {
            position = new Point3(dis.readDouble(), dis.readDouble(), dis.readDouble());
          }
          Vector3f normal = Vector3f.NaN;
          if ((format & Vertex.FORMAT_NORMAL) != 0) {
            normal = new Vector3f(dis.readFloat(), dis.readFloat(), dis.readFloat());
          }
          final Color4f diffuseColor;
          if ((format & Vertex.FORMAT_DIFFUSE_COLOR) != 0) {
            float red = (float) dis.readDouble();
            float green = (float) dis.readDouble();
            float blue = (float) dis.readDouble();
            float alpha = (float) dis.readDouble();
            diffuseColor = new Color4f(red, green, blue, alpha);
          } else {
            diffuseColor = null;
          }
          final TextureCoordinate2f textureCoordinate0;
          if ((format & Vertex.FORMAT_TEXTURE_COORDINATE_0) != 0) {
            float u = (float) dis.readDouble();
            float v = (float) dis.readDouble();
            textureCoordinate0 = new TextureCoordinate2f(u, v);
          } else {
            textureCoordinate0 = null;
          }
          vertices[index] = new Vertex(position, normal, diffuseColor, null, textureCoordinate0);
        }
      } else if (version == 3) {
        int vertexCount = dis.readInt();
        vertices = new Vertex[vertexCount];
        for (int index = 0; index < vertices.length; index++) {
          int format = dis.readInt();
          Point3 position = Point3.NaN;
          if ((format & Vertex.FORMAT_POSITION) != 0) {
            position = new Point3(dis.readDouble(), dis.readDouble(), dis.readDouble());
          }
          Vector3f normal = Vector3f.NaN;
          if ((format & Vertex.FORMAT_NORMAL) != 0) {
            normal = new Vector3f(dis.readFloat(), dis.readFloat(), dis.readFloat());
          }
          final Color4f diffuseColor;
          if ((format & Vertex.FORMAT_DIFFUSE_COLOR) != 0) {
            float red = dis.readFloat();
            float green = dis.readFloat();
            float blue = dis.readFloat();
            float alpha = dis.readFloat();
            diffuseColor = new Color4f(red, green, blue, alpha);
          } else {
            diffuseColor = null;
          }
          final Color4f specularHighlightColor;
          if ((format & Vertex.FORMAT_SPECULAR_HIGHLIGHT_COLOR) != 0) {
            float red = dis.readFloat();
            float green = dis.readFloat();
            float blue = dis.readFloat();
            float alpha = dis.readFloat();
            specularHighlightColor = new Color4f(red, green, blue, alpha);
          } else {
            specularHighlightColor = null;
          }
          final TextureCoordinate2f textureCoordinate0;
          if ((format & Vertex.FORMAT_TEXTURE_COORDINATE_0) != 0) {
            float u = dis.readFloat();
            float v = dis.readFloat();
            textureCoordinate0 = new TextureCoordinate2f(u, v);
          } else {
            textureCoordinate0 = null;
          }
          vertices[index] = new Vertex(position, normal, diffuseColor, specularHighlightColor, textureCoordinate0);
        }
      } else {
        throw new RuntimeException("invalid file version: " + version);
      }
      return vertices;
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  static int[] decodeIntArrayInBinary(InputStream is) {
    int[] array = null;
    BufferedInputStream bis = new BufferedInputStream(is);
    DataInputStream dis = new DataInputStream(bis);
    try {
      int version = dis.readInt();
      if (version == 1) {
        int faceCount = dis.readInt();
        /* unused int verticesPerFace = */
        dis.readInt();
        array = new int[faceCount * 3];
        for (int i = 0; i < array.length; i++) {
          array[i] = dis.readInt();
        }
      } else if (version == 2) {
        int count = dis.readInt();
        array = new int[count];
        for (int i = 0; i < array.length; i++) {
          array[i] = dis.readInt();
        }
        for (int i = 0; i < array.length; i += 3) {
          int temp = array[i];
          array[i] = array[i + 2];
          array[i + 2] = temp;
        }
      } else {
        throw new RuntimeException("invalid file version: " + version);
      }
      return array;
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  static double[] decodeDoubleArrayInBinary(InputStream is) {
    double[] array = null;
    BufferedInputStream bis = new BufferedInputStream(is);
    DataInputStream dis = new DataInputStream(bis);
    try {
      int version = dis.readInt();
      if (version == 1) {
        // there was no version 1
      } else if (version == 2) {
        int count = dis.readInt();
        array = new double[count];
        for (int i = 0; i < array.length; i++) {
          array[i] = dis.readDouble();
        }
      } else {
        throw new RuntimeException("invalid file version: " + version);
      }
      return array;
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private static void decodeIntArray(String s, int[] array, int offset, int length, boolean isHexadecimal) {
    int index = offset;
    int begin = 0;
    for (int lcv = 0; lcv < length; lcv++) {
      int end = s.indexOf(' ', begin);
      if (end == -1) {
        end = s.length();
      }
      String substr = s.substring(begin, end);
      int value;
      if (isHexadecimal) {
        value = (int) Long.parseLong(substr, 16);
      } else {
        value = Integer.parseInt(substr);
      }
      array[index++] = value;
      begin = end + 1;
    }
  }

  private static void decodeIntArray(String s, int[] array, boolean isHexadecimal) {
    decodeIntArray(s, array, 0, array.length, isHexadecimal);
  }

  private static void decodeDoubleArray(String s, double[] array, int offset, int length) {
    int index = offset;
    int begin = 0;
    for (int lcv = 0; lcv < length; lcv++) {
      int end = s.indexOf(' ', begin);
      if (end == -1) {
        end = s.length();
      }
      String substr = s.substring(begin, end);
      array[index++] = Double.parseDouble(substr);
      begin = end + 1;
    }
  }

  private static Point3 decodePoint3(String s) {
    int begin = 0;
    int end = s.indexOf(' ', begin);
    double x = Double.parseDouble(s.substring(begin, end));
    begin = end + 1;
    end = s.indexOf(' ', begin);
    double y = Double.parseDouble(s.substring(begin, end));
    begin = end + 1;
    end = s.length();
    double z = Double.parseDouble(s.substring(begin, end));
    return new Point3(x, y, z);
  }

  private static Vector3f decodeVector3f(String s) {
    int begin = 0;
    int end = s.indexOf(' ', begin);
    float x = Float.parseFloat(s.substring(begin, end));
    begin = end + 1;
    end = s.indexOf(' ', begin);
    float y = Float.parseFloat(s.substring(begin, end));
    begin = end + 1;
    end = s.length();
    float z = Float.parseFloat(s.substring(begin, end));
    return new Vector3f(x, y, z);
  }

  private static Vector2f decodeVector2f(String s) {
    int begin = 0;
    int end = s.indexOf(' ', begin);
    begin = end + 1;
    end = s.length();
    return new Vector2f(Float.parseFloat(s.substring(begin, end)), Float.parseFloat(s.substring(begin, end)));
  }

  private static TextureCoordinate2f decodeTexCoord2f(String s) {
    int begin = 0;
    int end = s.indexOf(' ', begin);
    float u = Float.parseFloat(s.substring(begin, end));
    begin = end + 1;
    end = s.length();
    float v = Float.parseFloat(s.substring(begin, end));
    return new TextureCoordinate2f(u, v);
  }

  private static Color4f decodeColor4f(String s) {
    int begin = 0;
    int end = s.indexOf(' ', begin);
    float red = Float.parseFloat(s.substring(begin, end));
    begin = end + 1;
    end = s.indexOf(' ', begin);
    float green = Float.parseFloat(s.substring(begin, end));
    begin = end + 1;
    end = s.indexOf(' ', begin);
    float blue = Float.parseFloat(s.substring(begin, end));
    begin = end + 1;
    end = s.length();
    float alpha = Float.parseFloat(s.substring(begin, end));
    return new Color4f(red, green, blue, alpha);
  }

  private static Element getFirstChild(Node node, String tag) {
    Node childNode = node.getFirstChild();
    while (childNode != null) {
      if (childNode instanceof Element element) {
        if (childNode.getNodeName().equals(tag)) {
          return element;
        }
      }
      childNode = childNode.getNextSibling();
    }
    return null;
  }

  private static Element[] getChildren(Node node, String tag) {
    List<Element> list = new ArrayList<>();
    Node childNode = node.getFirstChild();
    while (childNode != null) {
      if (childNode instanceof Element element) {
        if (childNode.getNodeName().equals(tag)) {
          list.add(element);
        }
      }
      childNode = childNode.getNextSibling();
    }
    return list.toArray(new Element[0]);
  }

  private static String getNodeText(Node node) {
    StringBuilder propertyTextBuffer = new StringBuilder();
    NodeList children = node.getChildNodes();
    for (int j = 0; j < children.getLength(); j++) {
      Text textNode = (Text) children.item(j);
      propertyTextBuffer.append(textNode.getData().trim());
    }
    return propertyTextBuffer.toString();
  }

  private static Object valueOf(Class<?> cls, String text) {
    if (String.class.isAssignableFrom(cls)) {
      return text;
    } else if (cls.equals(Double.class) && text.equals("Infinity")) {
      return Double.POSITIVE_INFINITY;
    } else if (cls.equals(Double.class) && text.equals("NaN")) {
      return Double.NaN;
    } else {
      Class<?>[] parameterTypes = {String.class};
      try {
        Method valueOfMethod = cls.getMethod("valueOf", parameterTypes);
        int modifiers = valueOfMethod.getModifiers();
        if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
          Object[] parameters = {text};
          return valueOfMethod.invoke(null, parameters);
        } else {
          throw new RuntimeException("valueOf method not public static.");
        }
      } catch (NoSuchMethodException nsme) {
        throw new RuntimeException("NoSuchMethodException: class[" + cls.getName() + "]; method[" + text + "]");
      } catch (IllegalAccessException iae) {
        throw new RuntimeException("IllegalAccessException: " + cls + " " + text);
      } catch (InvocationTargetException ite) {
        throw new RuntimeException("java.lang.reflect.InvocationTargetException: " + cls + " " + text);
      }
    }
  }

  private static edu.cmu.cs.dennisc.scenegraph.Element decodeElement(Element xmlElement, HashMap<String, InputStream> filenameToStreamMap, HashMap<Integer, edu.cmu.cs.dennisc.scenegraph.Element> keyToElementMap, List<AbstractPropertyReference> referencesToBeResolved) {
    String className = xmlElement.getAttribute("class");
    Integer elementKey = Integer.parseInt(xmlElement.getAttribute("key"));
    String elementName = xmlElement.getAttribute("name");
    className = convertClassnameIfNecessary(className);
    edu.cmu.cs.dennisc.scenegraph.Element sgElement = (edu.cmu.cs.dennisc.scenegraph.Element) ReflectionUtilities.newInstance(className);
    sgElement.setName(elementName);
    keyToElementMap.put(elementKey, sgElement);
    Element[] xmlProperties = getChildren(xmlElement, "property");
    for (Element xmlProperty : xmlProperties) {
      String propertyName = xmlProperty.getAttribute("name");
      propertyName = convertPropertyIfNecessary(propertyName);
      if (isDeadProperty(propertyName)) {
        continue;
      }
      InstanceProperty property = sgElement.getPropertyNamed(propertyName);
      if (xmlProperty.hasAttribute("class")) {
        String propertyValueClassname = xmlProperty.getAttribute("class");
        propertyValueClassname = convertClassnameIfNecessary(propertyValueClassname);
        Class<?> propertyValueClass = ReflectionUtilities.getClassForName(propertyValueClassname);
        Object value;
        if (xmlProperty.hasAttribute("filename")) {
          String filename = xmlProperty.getAttribute("filename");
          InputStream is = filenameToStreamMap.get(filename);
          if (is != null) {
            if (Image.class.isAssignableFrom(propertyValueClass)) {
              String ext = FileUtilities.getExtension(filename);
              String codecName = ImageUtilities.getCodecNameForExtension(ext);
              try {
                value = ImageUtilities.read(codecName, is);
              } catch (IOException ioe) {
                throw new RuntimeException(filename, ioe);
              }
            } else if (Vertex[].class.isAssignableFrom(propertyValueClass)) {
              value = decodeVertexArrayInBinary(is);
            } else if (int[].class.isAssignableFrom(propertyValueClass)) {
              value = decodeIntArrayInBinary(is);
            } else if (double[].class.isAssignableFrom(propertyValueClass)) {
              value = decodeDoubleArrayInBinary(is);
            } else {
              throw new RuntimeException();
            }
          } else {
            throw new RuntimeException();
          }
        } else {
          if (AffineMatrix4x4.class.isAssignableFrom(propertyValueClass)) {
            Element[] xmlRows = getChildren(xmlProperty, "row");
            double[] values = new double[16];
            for (int rowIndex = 0; rowIndex < 4; rowIndex++) {
              decodeDoubleArray(getNodeText(xmlRows[rowIndex]), values, 4 * rowIndex,  4);
            }
            value = AffineMatrix4x4.createFromRowMajorArray(values);
          } else if (Matrix3x3.class.isAssignableFrom(propertyValueClass)) {
            Element[] xmlRows = getChildren(xmlProperty, "row");
            double[] values = new double[9];
            for (int rowIndex = 0; rowIndex < 3; rowIndex++) {
              decodeDoubleArray(getNodeText(xmlRows[rowIndex]), values, 3 * rowIndex,  3);
            }
            value = Matrix3x3.create(values);
          } else if (Image.class.isAssignableFrom(propertyValueClass)) {
            int width = Integer.parseInt(xmlProperty.getAttribute("width"));
            int height = Integer.parseInt(xmlProperty.getAttribute("height"));
            Element[] xmlRows = getChildren(xmlProperty, "row");
            int[] pixels = new int[width * height];
            int pixelIndex = 0;
            for (int rowIndex = 0; rowIndex < height; rowIndex++) {
              String s = getNodeText(xmlRows[rowIndex]);
              decodeIntArray(s, pixels, pixelIndex, width, true);
              pixelIndex += width;
            }
            value = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(width, height, pixels, 0, width));
          } else if (Color4f.class.isAssignableFrom(propertyValueClass)) {
            float red = Float.parseFloat(getNodeText(getFirstChild(xmlProperty, "red")));
            float green = Float.parseFloat(getNodeText(getFirstChild(xmlProperty, "green")));
            float blue = Float.parseFloat(getNodeText(getFirstChild(xmlProperty, "blue")));
            float alpha = Float.parseFloat(getNodeText(getFirstChild(xmlProperty, "alpha")));
            value = new Color4f(red, green, blue, alpha);
          } else if (int[].class.isAssignableFrom(propertyValueClass)) {
            int length = Integer.parseInt(xmlProperty.getAttribute("length"));
            int[] intArray = new int[length];
            decodeIntArray(getNodeText(xmlProperty), intArray, false);
            value = intArray;
          } else if (double[].class.isAssignableFrom(propertyValueClass)) {
            int length = Integer.parseInt(xmlProperty.getAttribute("length"));
            double[] doubleArray = new double[length];
            decodeDoubleArray(getNodeText(xmlProperty), doubleArray, 0, length);
            value = doubleArray;
          } else if (Point3[].class.isAssignableFrom(propertyValueClass)) {
            Element[] xmlPoints = getChildren(xmlProperty, "point");
            Point3[] pointArray = new Point3[xmlPoints.length];
            for (int tupleIndex = 0; tupleIndex < xmlPoints.length; tupleIndex++) {
              pointArray[tupleIndex] = decodePoint3(getNodeText(xmlPoints[tupleIndex]));
            }
            value = pointArray;
          } else if (Vector3f[].class.isAssignableFrom(propertyValueClass)) {
            Element[] xmlNormals = getChildren(xmlProperty, "normal");
            Vector3f[] normalArray = new Vector3f[xmlNormals.length];
            for (int tupleIndex = 0; tupleIndex < xmlNormals.length; tupleIndex++) {
              Vector3f v = decodeVector3f(getNodeText(xmlNormals[tupleIndex]));
              normalArray[tupleIndex] = v;
            }
            value = normalArray;
          } else if (Vector2f[].class.isAssignableFrom(propertyValueClass)) {
            Element[] xmlTextureCoords = getChildren(xmlProperty, "textureCoordinate");
            Vector2f[] texCoordArray = new Vector2f[xmlTextureCoords.length];
            for (int tupleIndex = 0; tupleIndex < xmlTextureCoords.length; tupleIndex++) {
              texCoordArray[tupleIndex] = decodeVector2f(getNodeText(xmlTextureCoords[tupleIndex]));
            }
            value = texCoordArray;
          } else if (Vertex[].class.isAssignableFrom(propertyValueClass)) {
            Element[] xmlVertices = getChildren(xmlProperty, "vertex");
            Vertex[] vertexArray = new Vertex[xmlVertices.length];
            for (int vertexIndex = 0; vertexIndex < xmlVertices.length; vertexIndex++) {
              Element xmlVertex = xmlVertices[vertexIndex];
              Element xmlPosition = getFirstChild(xmlVertex, "position");
              Point3 position = Point3.NaN;
              if (xmlPosition != null) {
                position = decodePoint3(getNodeText(xmlPosition));
              }
              Element xmlNormal = getFirstChild(xmlVertex, "normal");
              Vector3f normal = Vector3f.NaN;
              if (xmlNormal != null) {
                normal = decodeVector3f(getNodeText(xmlNormal));
              }
              Element xmlDiffuseColor = getFirstChild(xmlVertex, "diffuseColor");
              final Color4f diffuseColor;
              if (xmlDiffuseColor != null) {
                diffuseColor = decodeColor4f(getNodeText(xmlDiffuseColor));
              } else {
                diffuseColor = null;
              }
              Element xmlTextureCoordinate0 = getFirstChild(xmlVertex, "textureCoordinate0");
              final TextureCoordinate2f textureCoordinate0;
              if (xmlTextureCoordinate0 != null) {
                textureCoordinate0 = decodeTexCoord2f(getNodeText(xmlTextureCoordinate0));
              } else {
                textureCoordinate0 = null;
              }
              Vertex vertex = new Vertex(position, normal, diffuseColor, null, textureCoordinate0);
              vertexArray[vertexIndex] = vertex;
            }
            value = vertexArray;
          } else {
            value = valueOf(propertyValueClass, getNodeText(xmlProperty));
          }
        }
        property.setValue(value);
      } else if (xmlProperty.hasAttribute("key")) {
        Integer key = Integer.parseInt(xmlProperty.getAttribute("key"));
        referencesToBeResolved.add(new PropertyReferenceToElement(sgElement, property, key));
      } else {
        property.setValue(null);
      }
    }
    return sgElement;
  }

  private static Component decodeComponent(Element xmlComponent, HashMap<String, InputStream> filenameToStreamMap, HashMap<Integer, edu.cmu.cs.dennisc.scenegraph.Element> keyToElementMap, List<AbstractPropertyReference> referencesToBeResolved) {
    Component sgComponent = (Component) decodeElement(xmlComponent, filenameToStreamMap, keyToElementMap, referencesToBeResolved);
    Element[] xmlChildren = getChildren(xmlComponent, "child");
    for (Element element : xmlChildren) {
      decodeComponent(element, filenameToStreamMap, keyToElementMap, referencesToBeResolved).setParent((Composite) sgComponent);
    }
    return sgComponent;
  }

  private static Component decodeInternal(InputStream is, HashMap<String, InputStream> filenameToStreamMap) {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.parse(is);
      Element xmlRoot = document.getDocumentElement();
      HashMap<Integer, edu.cmu.cs.dennisc.scenegraph.Element> keyToElementMap = new HashMap<>();
      List<AbstractPropertyReference> referencesToBeResolved = new ArrayList<>();
      Component sgRoot = decodeComponent(xmlRoot, filenameToStreamMap, keyToElementMap, referencesToBeResolved);
      Element[] xmlElements = getChildren(xmlRoot, "element");
      for (Element xmlElement : xmlElements) {
        decodeElement(xmlElement, filenameToStreamMap, keyToElementMap, referencesToBeResolved);
      }
      for (AbstractPropertyReference propertyReference : referencesToBeResolved) {
        propertyReference.resolve(keyToElementMap);
      }
      return sgRoot;
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    } catch (SAXException saxe) {
      throw new RuntimeException(saxe);
    } catch (ParserConfigurationException pce) {
      throw new RuntimeException(pce);
    }
  }

  static Component decode(InputStream is, HashMap<String, InputStream> filenameToStreamMap) {
    BufferedInputStream bis;
    if (is instanceof BufferedInputStream stream) {
      bis = stream;
    } else {
      bis = new BufferedInputStream(is);
    }
    return decodeInternal(bis, filenameToStreamMap);
  }

  static Component decodeZip(InputStream is) {
    ZipInputStream zis;
    if (is instanceof ZipInputStream stream) {
      zis = stream;
    } else {
      zis = new ZipInputStream(is);
    }
    HashMap<String, InputStream> filenameToStreamMap = new HashMap<>();
    ZipEntry zipEntry;
    try {
      while ((zipEntry = zis.getNextEntry()) != null) {
        String name = zipEntry.getName();
        if (zipEntry.isDirectory()) {
          // pass
        } else {
          final int BUFFER_SIZE = 2048;
          byte[] buffer = new byte[BUFFER_SIZE];
          ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
          int count;
          while ((count = zis.read(buffer, 0, BUFFER_SIZE)) != -1) {
            baos.write(buffer, 0, count);
          }
          zis.closeEntry();
          ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
          filenameToStreamMap.put(name, bais);
        }
      }
      InputStream rootIS = filenameToStreamMap.get(ASG.ROOT_FILENAME);
      if (rootIS == null) {
        throw new RuntimeException(ASG.ROOT_FILENAME);
      }
      filenameToStreamMap.remove(ASG.ROOT_FILENAME);
      return decode(rootIS, filenameToStreamMap);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  static Component decode(File file) {
    try {
      try {
        ZipFile zipFile = new ZipFile(file);
        zipFile.close();
        return decodeZip(new ZipInputStream(new FileInputStream(file)));
      } catch (ZipException ze) {
        HashMap<String, InputStream> filenameToStreamMap = new HashMap<>();
        return decode(new FileInputStream(file), filenameToStreamMap);
      }
    } catch (FileNotFoundException fnfe) {
      throw new RuntimeException(fnfe);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  static Component decode(String path) {
    return decode(new File(path));
  }
}
