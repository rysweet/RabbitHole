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

import edu.cmu.cs.dennisc.scenegraph.Component;
import edu.cmu.cs.dennisc.scenegraph.Vertex;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

/**
 * Façade for scene graph serialization.
 * Delegates encoding to {@link ASGEncoder} and decoding to {@link ASGDecoder}.
 *
 * @author Dennis Cosgrove
 */
public class ASG {
  public static final double VERSION = 1.0;
  static final String ROOT_FILENAME = "root.xml";

  public static void encodeVertexArrayInBinary(Vertex[] vertices, OutputStream os) {
    ASGEncoder.encodeVertexArrayInBinary(vertices, os);
  }

  public static Vertex[] decodeVertexArrayInBinary(InputStream is) {
    return ASGDecoder.decodeVertexArrayInBinary(is);
  }

  public static void encodeIntArrayInBinary(int[] array, OutputStream os) {
    ASGEncoder.encodeIntArrayInBinary(array, os);
  }

  public static int[] decodeIntArrayInBinary(InputStream is) {
    return ASGDecoder.decodeIntArrayInBinary(is);
  }

  public static void encodeDoubleArrayInBinary(double[] array, OutputStream os) {
    ASGEncoder.encodeDoubleArrayInBinary(array, os);
  }

  public static double[] decodeDoubleArrayInBinary(InputStream is) {
    return ASGDecoder.decodeDoubleArrayInBinary(is);
  }

  public static void encode(Component component, OutputStream os) {
    ASGEncoder.encode(component, os);
  }

  public static void encode(Component component, File file) {
    ASGEncoder.encode(component, file);
  }

  public static void encode(Component component, String path) {
    ASGEncoder.encode(component, path);
  }

  public static Component decode(InputStream is, HashMap<String, InputStream> filenameToStreamMap) {
    return ASGDecoder.decode(is, filenameToStreamMap);
  }

  public static Component decodeZip(InputStream is) {
    return ASGDecoder.decodeZip(is);
  }

  public static Component decode(File file) {
    return ASGDecoder.decode(file);
  }

  public static Component decode(String path) {
    return ASGDecoder.decode(path);
  }
}
