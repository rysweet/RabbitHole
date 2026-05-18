package org.alice.media.audio;

import org.junit.Test;
import javax.sound.sampled.AudioFormat;
import static org.junit.Assert.*;

public class FloatSampleBufferExtendedTest {

  @Test
  public void constructWithByteArray_16bitMono() {
    AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
    byte[] data = new byte[200];
    for (int i = 0; i < 200; i++) {
      data[i] = (byte) (i % 256);
    }
    FloatSampleBuffer buf = new FloatSampleBuffer(data, 0, 200, format);
    assertEquals(1, buf.getChannelCount());
    assertEquals(100, buf.getSampleCount());
    assertEquals(44100.0f, buf.getSampleRate(), 0.001f);
  }

  @Test
  public void constructWithByteArray_8bitStereo() {
    AudioFormat format = new AudioFormat(22050.0f, 8, 2, true, false);
    byte[] data = new byte[100];
    FloatSampleBuffer buf = new FloatSampleBuffer(data, 0, 100, format);
    assertEquals(2, buf.getChannelCount());
    assertEquals(50, buf.getSampleCount());
  }

  @Test
  public void convertToByteArray_16bitMonoRoundTrip() {
    AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
    FloatSampleBuffer buf = new FloatSampleBuffer(1, 10, 44100.0f);
    float[] ch = buf.getChannel(0);
    ch[0] = 0.5f;
    ch[1] = -0.5f;
    ch[2] = 0.0f;
    byte[] output = new byte[buf.getByteArrayBufferSize(format)];
    buf.convertToByteArray(output, 0, format);
    FloatSampleBuffer restored = new FloatSampleBuffer(output, 0, output.length, format);
    assertEquals(0.5f, restored.getChannel(0)[0], 0.01f);
    assertEquals(-0.5f, restored.getChannel(0)[1], 0.01f);
    assertEquals(0.0f, restored.getChannel(0)[2], 0.01f);
  }

  @Test
  public void changeSampleCount_expand_silencesPadding() {
    FloatSampleBuffer buf = new FloatSampleBuffer(1, 5, 44100.0f);
    float[] ch = buf.getChannel(0);
    ch[0] = 0.7f;
    buf.changeSampleCount(10, true);
    float[] newCh = buf.getChannel(0);
    assertEquals(0.7f, newCh[0], 0.001f);
    assertEquals(0.0f, newCh[9], 0.001f);
  }

  @Test
  public void insertChannel_middlePosition() {
    FloatSampleBuffer buf = new FloatSampleBuffer(2, 10, 44100.0f);
    buf.insertChannel(1, true);
    assertEquals(3, buf.getChannelCount());
  }

  @Test
  public void removeChannel_lazy_hidesChannel() {
    FloatSampleBuffer buf = new FloatSampleBuffer(3, 10, 44100.0f);
    buf.removeChannel(1, true);
    assertEquals(2, buf.getChannelCount());
  }

  @Test
  public void removeChannel_notLazy_removesCompletely() {
    FloatSampleBuffer buf = new FloatSampleBuffer(3, 10, 44100.0f);
    buf.removeChannel(0, false);
    assertEquals(2, buf.getChannelCount());
  }

  @Test
  public void makeSilence_multipleChannels_allSilent() {
    FloatSampleBuffer buf = new FloatSampleBuffer(2, 10, 44100.0f);
    buf.getChannel(0)[0] = 0.5f;
    buf.getChannel(1)[5] = -0.3f;
    buf.makeSilence();
    for (int ch = 0; ch < buf.getChannelCount(); ch++) {
      for (int i = 0; i < buf.getSampleCount(); i++) {
        assertEquals(0.0f, buf.getChannel(ch)[i], 0.0001f);
      }
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void convertToByteArray_wrongSampleRate_throws() {
    FloatSampleBuffer buf = new FloatSampleBuffer(1, 10, 44100.0f);
    AudioFormat format = new AudioFormat(22050.0f, 16, 1, true, true);
    byte[] output = new byte[100];
    buf.convertToByteArray(output, 0, format);
  }

  @Test(expected = IllegalArgumentException.class)
  public void convertToByteArray_wrongChannelCount_throws() {
    FloatSampleBuffer buf = new FloatSampleBuffer(1, 10, 44100.0f);
    AudioFormat format = new AudioFormat(44100.0f, 16, 2, true, true);
    byte[] output = new byte[100];
    buf.convertToByteArray(output, 0, format);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructWithByteArray_tooSmall_throws() {
    AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
    byte[] data = new byte[5];
    new FloatSampleBuffer(data, 0, 100, format);
  }

  @Test
  public void initFromFloatSampleBuffer_copiesAllChannels() {
    FloatSampleBuffer src = new FloatSampleBuffer(2, 10, 44100.0f);
    src.getChannel(0)[0] = 0.1f;
    src.getChannel(1)[0] = 0.2f;
    FloatSampleBuffer dst = new FloatSampleBuffer();
    dst.initFromFloatSampleBuffer(src);
    assertEquals(2, dst.getChannelCount());
    assertEquals(0.1f, dst.getChannel(0)[0], 0.001f);
    assertEquals(0.2f, dst.getChannel(1)[0], 0.001f);
  }
}
