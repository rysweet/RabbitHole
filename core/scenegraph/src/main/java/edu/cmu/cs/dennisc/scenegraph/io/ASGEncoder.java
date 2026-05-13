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
import edu.cmu.cs.dennisc.property.InstanceProperty;
import edu.cmu.cs.dennisc.scenegraph.Component;
import edu.cmu.cs.dennisc.scenegraph.Composite;
import edu.cmu.cs.dennisc.scenegraph.Vertex;
import edu.cmu.cs.dennisc.texture.TextureCoordinate2f;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.Matrix3x3;
import org.alice.math.immutable.Tuple3;
import org.alice.math.immutable.Tuple3f;
import org.alice.math.immutable.Vector4;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.Image;
import java.io.*;
import java.util.HashMap;
import java.util.zip.*;

/**
 * Handles all encoding (serialization) logic for ASG scene graph I/O.
 * Extracted from ASG.java to reduce file size and improve maintainability.
 *
 * @author Dennis Cosgrove
 */
class ASGEncoder {
  private static final int SMALL_ENOUGH_PRIMITIVE_ARRAY_LENGTH_TO_ENCODE_AS_TEXT = 32;

  private static class MatrixUtilities {
    public static void getRow(double[] rv, AffineMatrix4x4 m, int row) {
      Vector4 rowValues = switch (row) {
        case 0 -> m.rowX();
        case 1 -> m.rowY();
        case 2 -> m.rowZ();
        case 3 -> m.rowW();
        default -> throw new IllegalArgumentException();
      };
      rv[0] = rowValues.x();
      rv[1] = rowValues.y();
      rv[2] = rowValues.z();
      rv[3] = rowValues.w();
    }

    public static void getRow(double[] rv, Matrix3x3 m, int row) {
      switch (row) {
      case 0:
        rv[0] = m.getRight().x();
        rv[1] = m.getUp().x();
        rv[2] = m.getBackward().x();
        break;
      case 1:
        rv[0] = m.getRight().y();
        rv[1] = m.getUp().y();
        rv[2] = m.getBackward().y();
        break;
      case 2:
        rv[0] = m.getRight().z();
        rv[1] = m.getUp().z();
        rv[2] = m.getBackward().z();
        break;
      default:
        throw new IllegalArgumentException();
      }
    }
  }

  private static String getKey(edu.cmu.cs.dennisc.scenegraph.Element element) {
    return Integer.toString(element.hashCode());
  }

  static void encodeVertexArrayInBinary(Vertex[] vertices, OutputStream os) {
    BufferedOutputStream bos = new BufferedOutputStream(os);
    DataOutputStream dos = new DataOutputStream(bos);
    try {
      dos.writeInt(3);
      dos.writeInt(vertices.length);
      for (Vertex vertice : vertices) {
        int format = vertice.getFormat();
        dos.writeInt(format);
        if ((format & Vertex.FORMAT_POSITION) != 0) {
          dos.writeDouble(vertice.position.x());
          dos.writeDouble(vertice.position.y());
          dos.writeDouble(vertice.position.z());
        }
        if ((format & Vertex.FORMAT_NORMAL) != 0) {
          dos.writeDouble(vertice.normal.x());
          dos.writeDouble(vertice.normal.y());
          dos.writeDouble(vertice.normal.z());
        }
        if ((format & Vertex.FORMAT_DIFFUSE_COLOR) != 0) {
          dos.writeFloat(vertice.diffuseColor.red);
          dos.writeFloat(vertice.diffuseColor.green);
          dos.writeFloat(vertice.diffuseColor.blue);
          dos.writeFloat(vertice.diffuseColor.alpha);
        }
        if ((format & Vertex.FORMAT_SPECULAR_HIGHLIGHT_COLOR) != 0) {
          dos.writeFloat(vertice.specularHighlightColor.red);
          dos.writeFloat(vertice.specularHighlightColor.green);
          dos.writeFloat(vertice.specularHighlightColor.blue);
          dos.writeFloat(vertice.specularHighlightColor.alpha);
        }
        if ((format & Vertex.FORMAT_TEXTURE_COORDINATE_0) != 0) {
          dos.writeFloat(vertice.textureCoordinate0.u);
          dos.writeFloat(vertice.textureCoordinate0.v);
        }
      }
      dos.flush();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  static void encodeIntArrayInBinary(int[] array, OutputStream os) {
    BufferedOutputStream bos = new BufferedOutputStream(os);
    DataOutputStream dos = new DataOutputStream(bos);
    try {
      dos.writeInt(2);
      dos.writeInt(array.length);
      for (int element : array) {
        dos.writeInt(element);
      }
      dos.flush();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  static void encodeDoubleArrayInBinary(double[] array, OutputStream os) {
    BufferedOutputStream bos = new BufferedOutputStream(os);
    DataOutputStream dos = new DataOutputStream(bos);
    try {
      dos.writeInt(2);
      dos.writeInt(array.length);
      for (double element : array) {
        dos.writeDouble(element);
      }
      dos.flush();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private static String encodeIntArray(int[] array, int offset, int length, boolean isHexadecimal) {
    StringBuilder buffer = new StringBuilder();
    int index = offset;
    for (int lcv = 0; lcv < length; lcv++) {
      String s;
      int value = array[index++];
      if (isHexadecimal) {
        s = Integer.toHexString(value).toUpperCase();
      } else {
        s = Integer.toString(value);
      }
      buffer.append(s);
      if (lcv < (length - 1)) {
        buffer.append(' ');
      }
    }
    return buffer.toString();
  }

  private static String encodeIntArray(int[] array, boolean isHexadecimal) {
    return encodeIntArray(array, 0, array.length, isHexadecimal);
  }

  private static String encodeDoubleArray(double[] array, int offset, int length) {
    StringBuilder buffer = new StringBuilder();
    int index = offset;
    for (int lcv = 0; lcv < length; lcv++) {
      buffer.append(array[index++]);
      if (lcv < (length - 1)) {
        buffer.append(' ');
      }
    }
    return buffer.toString();
  }

  private static String encodeDoubleArray(double[] array) {
    return encodeDoubleArray(array, 0, array.length);
  }

  private static String encodeTuple3d(Tuple3 tuple3d) {
    return Double.toString(tuple3d.x()) + ' ' + tuple3d.y() + ' ' + tuple3d.z();
  }

  private static String encodeTuple3f(Tuple3f tuple3d) {
    return Double.toString(tuple3d.x()) + ' ' + Double.toString(tuple3d.y()) + ' ' + Double.toString(tuple3d.z());
  }

  private static String encodeTexCoord2f(TextureCoordinate2f tc2f) {
    return String.valueOf(tc2f.u) + ' ' + tc2f.v;
  }

  private static String encodeColor4f(Color4f color4f) {
    return String.valueOf(color4f.red) + ' ' + color4f.green + ' ' + color4f.blue + ' ' + color4f.alpha;
  }

  private static Element encodeElement(edu.cmu.cs.dennisc.scenegraph.Element element, Document document, String s, HashMap<String, ByteArrayOutputStream> filenameToStreamMap, HashMap<String, edu.cmu.cs.dennisc.scenegraph.Element> keyToElementToBeEncodedMap, boolean isTextAlwaysDesired) {
    Element xmlElement = document.createElement(s);
    Class<? extends edu.cmu.cs.dennisc.scenegraph.Element> elementClass = element.getClass();
    xmlElement.setAttribute("class", elementClass.getName());
    xmlElement.setAttribute("key", getKey(element));
    for (InstanceProperty<?> property : element.getProperties()) {
      String propertyName = property.getName();
      if (propertyName.equals("Parent")) {
        // pass
      } else if (propertyName.equals("Bonus")) {
        // pass
      } else {
        Element xmlProperty = document.createElement("property");
        xmlProperty.setAttribute("name", propertyName);
        Object value = property.getValue();
        if (value != null) {
          Class<?> propertyValueClass = value.getClass();
          if (edu.cmu.cs.dennisc.scenegraph.Element.class.isAssignableFrom(propertyValueClass)) {
            String key = Integer.toString(value.hashCode());
            xmlProperty.setAttribute("key", key);
            if (!Component.class.isAssignableFrom(propertyValueClass)) {
              keyToElementToBeEncodedMap.put(key, (edu.cmu.cs.dennisc.scenegraph.Element) value);
            }
          } else {
            if (AffineMatrix4x4.class.isAssignableFrom(propertyValueClass)) {
              xmlProperty.setAttribute("class", "edu.cmu.cs.dennisc.math.Matrix4d");
              AffineMatrix4x4 m = (AffineMatrix4x4) value;
              double[] row = new double[4];
              for (int rowIndex = 0; rowIndex < 4; rowIndex++) {
                Element xmlRow = document.createElement("row");
                MatrixUtilities.getRow(row, m, rowIndex);
                xmlRow.appendChild(document.createTextNode(encodeDoubleArray(row)));
                xmlProperty.appendChild(xmlRow);
              }
            } else if (Matrix3x3.class.isAssignableFrom(propertyValueClass)) {
              xmlProperty.setAttribute("class", "edu.cmu.cs.dennisc.math.Matrix3d");
              Matrix3x3 m = (Matrix3x3) value;
              double[] row = new double[3];
              for (int rowIndex = 0; rowIndex < 3; rowIndex++) {
                Element xmlRow = document.createElement("row");
                MatrixUtilities.getRow(row, m, rowIndex);
                xmlRow.appendChild(document.createTextNode(encodeDoubleArray(row)));
                xmlProperty.appendChild(xmlRow);
              }
            } else if (Image.class.isAssignableFrom(propertyValueClass)) {
              Image image = (Image) value;
              xmlProperty.setAttribute("class", "java.awt.Image");
              if (isTextAlwaysDesired) {
                int width = ImageUtilities.getWidth(image);
                int height = ImageUtilities.getHeight(image);
                int[] pixels = ImageUtilities.getPixels(image, width, height);
                xmlProperty.setAttribute("width", Integer.toString(width));
                xmlProperty.setAttribute("height", Integer.toString(width));
                int pixelIndex = 0;
                for (int rowIndex = 0; rowIndex < height; rowIndex++) {
                  Element xmlRow = document.createElement("row");
                  xmlRow.appendChild(document.createTextNode(encodeIntArray(pixels, pixelIndex, width, true)));
                  pixelIndex += width;
                  xmlProperty.appendChild(xmlRow);
                }
              } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                  ImageUtilities.write(ImageUtilities.PNG_CODEC_NAME, baos, image);
                  String filename = image.hashCode() + ".png";
                  xmlProperty.setAttribute("filename", filename);
                  filenameToStreamMap.put(filename, baos);
                } catch (IOException ioe) {
                  throw new RuntimeException(ioe);
                }
              }
            } else if (Color4f.class.isAssignableFrom(propertyValueClass)) {
              xmlProperty.setAttribute("class", "edu.cmu.cs.dennisc.color.Color4f");
              Color4f color = (Color4f) value;
              Element xmlRed = document.createElement("red");
              xmlRed.appendChild(document.createTextNode(Float.toString(color.red)));
              xmlProperty.appendChild(xmlRed);
              Element xmlGreen = document.createElement("green");
              xmlGreen.appendChild(document.createTextNode(Float.toString(color.green)));
              xmlProperty.appendChild(xmlGreen);
              Element xmlBlue = document.createElement("blue");
              xmlBlue.appendChild(document.createTextNode(Float.toString(color.blue)));
              xmlProperty.appendChild(xmlBlue);
              Element xmlAlpha = document.createElement("alpha");
              xmlAlpha.appendChild(document.createTextNode(Float.toString(color.alpha)));
              xmlProperty.appendChild(xmlAlpha);
            } else if (int[].class.isAssignableFrom(propertyValueClass)) {
              int[] array = (int[]) value;
              xmlProperty.setAttribute("class", "[I");
              if (isTextAlwaysDesired || (array.length < SMALL_ENOUGH_PRIMITIVE_ARRAY_LENGTH_TO_ENCODE_AS_TEXT)) {
                xmlProperty.setAttribute("length", Integer.toString(array.length));
                xmlProperty.appendChild(document.createTextNode(encodeIntArray(array, false)));
              } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                encodeIntArrayInBinary(array, baos);
                String filename = "int array " + array.hashCode() + ".bin";
                xmlProperty.setAttribute("filename", filename);
                filenameToStreamMap.put(filename, baos);
              }
            } else if (double[].class.isAssignableFrom(propertyValueClass)) {
              double[] array = (double[]) value;
              xmlProperty.setAttribute("class", "[D");
              if (isTextAlwaysDesired || (array.length < SMALL_ENOUGH_PRIMITIVE_ARRAY_LENGTH_TO_ENCODE_AS_TEXT)) {
                xmlProperty.setAttribute("length", Integer.toString(array.length));
                xmlProperty.appendChild(document.createTextNode(encodeDoubleArray(array)));
              } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                encodeDoubleArrayInBinary(array, baos);
                String filename = "double array " + array.hashCode() + ".bin";
                xmlProperty.setAttribute("filename", filename);
                filenameToStreamMap.put(filename, baos);
              }
            } else if (Vertex[].class.isAssignableFrom(propertyValueClass)) {
              Vertex[] array = (Vertex[]) value;
              xmlProperty.setAttribute("class", "[Ledu.cmu.cs.dennisc.scenegraph.Vertex;");
              if (isTextAlwaysDesired || (array.length == 0)) {
                for (Vertex vertex : array) {
                  Element xmlVertex = document.createElement("vertex");
                  if (vertex.position != null) {
                    Element xmlPosition = document.createElement("position");
                    xmlPosition.appendChild(document.createTextNode(encodeTuple3d(vertex.position)));
                    xmlVertex.appendChild(xmlPosition);
                  }
                  if (vertex.normal != null) {
                    Element xmlNormal = document.createElement("normal");
                    xmlNormal.appendChild(document.createTextNode(encodeTuple3f(vertex.normal)));
                    xmlVertex.appendChild(xmlNormal);
                  }
                  if (vertex.diffuseColor != null) {
                    Element xmlDiffuseColor = document.createElement("diffuseColor");
                    xmlDiffuseColor.appendChild(document.createTextNode(encodeColor4f(vertex.diffuseColor)));
                    xmlVertex.appendChild(xmlDiffuseColor);
                  }
                  if (vertex.textureCoordinate0 != null) {
                    Element xmlTextureCoordinate0 = document.createElement("textureCoordinate0");
                    xmlTextureCoordinate0.appendChild(document.createTextNode(encodeTexCoord2f(vertex.textureCoordinate0)));
                    xmlVertex.appendChild(xmlTextureCoordinate0);
                  }
                  xmlProperty.appendChild(xmlVertex);
                }
              } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                encodeVertexArrayInBinary(array, baos);
                String filename = "Vertex array " + array.hashCode() + ".bin";
                xmlProperty.setAttribute("filename", filename);
                filenameToStreamMap.put(filename, baos);
              }
            } else {
              xmlProperty.setAttribute("class", propertyValueClass.getName());
              xmlProperty.appendChild(document.createTextNode(value.toString()));
            }
          }
        }
        xmlElement.appendChild(xmlProperty);
      }
    }
    return xmlElement;
  }

  private static Element encodeComponent(Component component, Document document, String s, HashMap<String, ByteArrayOutputStream> filenameToStreamMap, HashMap<String, edu.cmu.cs.dennisc.scenegraph.Element> keyToElementToBeEncodedMap, boolean isTextAlwaysDesired) {
    Element xmlComponent = encodeElement(component, document, s, filenameToStreamMap, keyToElementToBeEncodedMap, isTextAlwaysDesired);
    if (component instanceof Composite sgComposite) {
      for (Component sgComponent : sgComposite.getComponents()) {
        xmlComponent.appendChild(encodeComponent(sgComponent, document, "child", filenameToStreamMap, keyToElementToBeEncodedMap, isTextAlwaysDesired));
      }
    }
    return xmlComponent;
  }

  private static void encodeInternal(Component component, OutputStream os, HashMap<String, ByteArrayOutputStream> filenameToStreamMap, boolean isTextAlwaysDesired) {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.newDocument();
      HashMap<String, edu.cmu.cs.dennisc.scenegraph.Element> keyToElementToBeEncodedMap = new HashMap<>();
      Element rootNode = encodeComponent(component, document, "root", filenameToStreamMap, keyToElementToBeEncodedMap, isTextAlwaysDesired);
      rootNode.setAttribute("version", Double.toString(ASG.VERSION));
      while (keyToElementToBeEncodedMap.size() > 0) {
        HashMap<String, edu.cmu.cs.dennisc.scenegraph.Element> tempCopy = new HashMap<>(keyToElementToBeEncodedMap);
        for (String key : tempCopy.keySet()) {
          edu.cmu.cs.dennisc.scenegraph.Element element = tempCopy.get(key);
          rootNode.appendChild(encodeElement(element, document, "element", filenameToStreamMap, keyToElementToBeEncodedMap, isTextAlwaysDesired));
          keyToElementToBeEncodedMap.remove(key);
        }
      }
      document.appendChild(rootNode);
      document.getDocumentElement().normalize();
      try {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer trans = tf.newTransformer();
        trans.transform(new DOMSource(document), new StreamResult(os));
      } catch (TransformerException te) {
        throw new RuntimeException(te);
      }
    } catch (ParserConfigurationException pce) {
      throw new RuntimeException(pce);
    }
  }

  static void encode(Component component, OutputStream os) {
    HashMap<String, ByteArrayOutputStream> filenameToStreamMap = new HashMap<>();
    ByteArrayOutputStream rootBAOS = new ByteArrayOutputStream();
    encodeInternal(component, rootBAOS, filenameToStreamMap, false);
    filenameToStreamMap.put(ASG.ROOT_FILENAME, rootBAOS);
    ZipOutputStream zos;
    if (os instanceof ZipOutputStream stream) {
      zos = stream;
    } else {
      zos = new ZipOutputStream(os);
    }
    CRC32 crc32 = new CRC32();
    try {
      for (String filename : filenameToStreamMap.keySet()) {
        ByteArrayOutputStream baos = filenameToStreamMap.get(filename);
        baos.flush();
        byte[] ba = baos.toByteArray();
        ZipEntry zipEntry = new ZipEntry(filename);
        int method;
        if (filename.endsWith(".png")) {
          crc32.reset();
          crc32.update(ba);
          zipEntry.setCrc(crc32.getValue());
          zipEntry.setSize(ba.length);
          method = ZipOutputStream.STORED;
        } else {
          method = ZipOutputStream.DEFLATED;
        }
        zos.setMethod(method);
        zos.putNextEntry(zipEntry);
        zos.write(ba, 0, ba.length);
        zos.closeEntry();
      }
      zos.flush();
      zos.finish();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  static void encode(Component component, File file) {
    try {
      OutputStream os = new FileOutputStream(file);
      encode(component, os);
      os.close();
    } catch (FileNotFoundException fnfe) {
      throw new RuntimeException(fnfe);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  static void encode(Component component, String path) {
    encode(component, new File(path));
  }
}
