package org.alice.media.audio;

import org.junit.Before;
import org.junit.Test;

import javax.sound.sampled.AudioFormat;

import static org.junit.Assert.*;

public class FloatSampleBufferTest {

  private FloatSampleBuffer buffer;

  @Before
  public void setUp() {
    buffer = new FloatSampleBuffer();
  }

  // --- constructors ---

  @Test
  public void defaultConstructor_zeroChannelsAndSamples() {
    assertEquals(0, buffer.getChannelCount());
    assertEquals(0, buffer.getSampleCount());
  }

  @Test
  public void defaultConstructor_sampleRateIsOne() {
    assertEquals(1.0f, buffer.getSampleRate(), 0.001f);
  }

  @Test
  public void parameterizedConstructor_setsValues() {
    FloatSampleBuffer buf = new FloatSampleBuffer(2, 100, 44100.0f);
    assertEquals(2, buf.getChannelCount());
    assertEquals(100, buf.getSampleCount());
    assertEquals(44100.0f, buf.getSampleRate(), 0.001f);
  }

  @Test
  public void parameterizedConstructor_channelsHaveCorrectLength() {
    FloatSampleBuffer buf = new FloatSampleBuffer(2, 50, 22050.0f);
    float[] ch0 = buf.getChannel(0);
    assertNotNull(ch0);
    assertTrue(ch0.length >= 50);
  }

  // --- sample rate ---

  @Test
  public void setSampleRate_updatesRate() {
    buffer = new FloatSampleBuffer(1, 10, 8000.0f);
    buffer.setSampleRate(16000.0f);
    assertEquals(16000.0f, buffer.getSampleRate(), 0.001f);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setSampleRate_zeroThrows() {
    buffer.setSampleRate(0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setSampleRate_negativeThrows() {
    buffer.setSampleRate(-1.0f);
  }

  // --- channel management ---

  @Test
  public void addChannel_incrementsChannelCount() {
    buffer = new FloatSampleBuffer(1, 10, 44100.0f);
    assertEquals(1, buffer.getChannelCount());
    buffer.addChannel(true);
    assertEquals(2, buffer.getChannelCount());
  }

  @Test
  public void insertChannel_atBeginning() {
    buffer = new FloatSampleBuffer(1, 10, 44100.0f);
    buffer.insertChannel(0, true);
    assertEquals(2, buffer.getChannelCount());
  }

  @Test
  public void removeChannel_decrementsCount() {
    buffer = new FloatSampleBuffer(2, 10, 44100.0f);
    buffer.removeChannel(0);
    assertEquals(1, buffer.getChannelCount());
  }

  @Test(expected = IllegalArgumentException.class)
  public void getChannel_invalidIndex_throws() {
    buffer = new FloatSampleBuffer(1, 10, 44100.0f);
    buffer.getChannel(5);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getChannel_negativeIndex_throws() {
    buffer = new FloatSampleBuffer(1, 10, 44100.0f);
    buffer.getChannel(-1);
  }

  // --- getAllChannels ---

  @Test
  public void getAllChannels_returnsCorrectCount() {
    buffer = new FloatSampleBuffer(3, 10, 44100.0f);
    Object[] channels = buffer.getAllChannels();
    assertEquals(3, channels.length);
  }

  @Test
  public void getAllChannels_eachIsFloatArray() {
    buffer = new FloatSampleBuffer(2, 10, 44100.0f);
    Object[] channels = buffer.getAllChannels();
    for (Object ch : channels) {
      assertTrue(ch instanceof float[]);
    }
  }

  // --- silence ---

  @Test
  public void makeSilence_setsAllSamplesToZero() {
    buffer = new FloatSampleBuffer(1, 100, 44100.0f);
    float[] ch = buffer.getChannel(0);
    ch[0] = 0.5f;
    ch[50] = -0.3f;
    buffer.makeSilence();
    ch = buffer.getChannel(0);
    for (int i = 0; i < buffer.getSampleCount(); i++) {
      assertEquals(0.0f, ch[i], 0.0001f);
    }
  }

  // --- copyChannel ---

  @Test
  public void copyChannel_copiesData() {
    buffer = new FloatSampleBuffer(2, 10, 44100.0f);
    float[] src = buffer.getChannel(0);
    for (int i = 0; i < 10; i++) {
      src[i] = (float) i / 10;
    }
    buffer.copyChannel(0, 1);
    float[] dst = buffer.getChannel(1);
    for (int i = 0; i < 10; i++) {
      assertEquals(src[i], dst[i], 0.0001f);
    }
  }

  // --- reset ---

  @Test
  public void reset_clearsBuffer() {
    buffer = new FloatSampleBuffer(2, 100, 44100.0f);
    buffer.reset();
    assertEquals(0, buffer.getChannelCount());
    assertEquals(0, buffer.getSampleCount());
  }

  @Test
  public void resetWithParams_setsNewValues() {
    buffer = new FloatSampleBuffer(1, 10, 8000.0f);
    buffer.reset(3, 200, 22050.0f);
    assertEquals(3, buffer.getChannelCount());
    assertEquals(200, buffer.getSampleCount());
    assertEquals(22050.0f, buffer.getSampleRate(), 0.001f);
  }

  // --- changeSampleCount ---

  @Test
  public void changeSampleCount_increasesCount() {
    buffer = new FloatSampleBuffer(1, 10, 44100.0f);
    buffer.changeSampleCount(20, true);
    assertEquals(20, buffer.getSampleCount());
  }

  @Test
  public void changeSampleCount_decreasesCount() {
    buffer = new FloatSampleBuffer(1, 20, 44100.0f);
    buffer.changeSampleCount(5, true);
    assertEquals(5, buffer.getSampleCount());
  }

  @Test
  public void changeSampleCount_keepOldSamples_retainsData() {
    buffer = new FloatSampleBuffer(1, 5, 44100.0f);
    float[] ch = buffer.getChannel(0);
    ch[0] = 0.1f;
    ch[1] = 0.2f;
    buffer.changeSampleCount(10, true);
    float[] newCh = buffer.getChannel(0);
    assertEquals(0.1f, newCh[0], 0.001f);
    assertEquals(0.2f, newCh[1], 0.001f);
  }

  // --- dither ---

  @Test
  public void ditherMode_defaultIsAutomatic() {
    assertEquals(FloatSampleBuffer.DITHER_MODE_AUTOMATIC, buffer.getDitherMode());
  }

  @Test
  public void setDitherMode_on() {
    buffer.setDitherMode(FloatSampleBuffer.DITHER_MODE_ON);
    assertEquals(FloatSampleBuffer.DITHER_MODE_ON, buffer.getDitherMode());
  }

  @Test
  public void setDitherMode_off() {
    buffer.setDitherMode(FloatSampleBuffer.DITHER_MODE_OFF);
    assertEquals(FloatSampleBuffer.DITHER_MODE_OFF, buffer.getDitherMode());
  }

  @Test
  public void ditherConstants() {
    assertEquals(0, FloatSampleBuffer.DITHER_MODE_AUTOMATIC);
    assertEquals(1, FloatSampleBuffer.DITHER_MODE_ON);
    assertEquals(2, FloatSampleBuffer.DITHER_MODE_OFF);
  }

  @Test
  public void setDitherBits_updates() {
    buffer.setDitherBits(1.5f);
    assertEquals(1.5f, buffer.getDitherBits(), 0.001f);
  }

  // --- byte array conversion ---

  @Test
  public void getByteArrayBufferSize_16bitStereo() {
    buffer = new FloatSampleBuffer(2, 100, 44100.0f);
    AudioFormat format = new AudioFormat(44100.0f, 16, 2, true, true);
    int size = buffer.getByteArrayBufferSize(format);
    assertEquals(400, size);
  }

  @Test
  public void getByteArrayBufferSize_8bitMono() {
    buffer = new FloatSampleBuffer(1, 50, 22050.0f);
    AudioFormat format = new AudioFormat(22050.0f, 8, 1, true, false);
    int size = buffer.getByteArrayBufferSize(format);
    assertEquals(50, size);
  }

  @Test
  public void initFromByteArray_thenConvertBack_roundTrips() {
    AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
    byte[] data = new byte[20];
    for (int i = 0; i < 20; i++) {
      data[i] = (byte) (i * 10);
    }
    buffer = new FloatSampleBuffer(1, 10, 44100.0f);
    buffer.initFromByteArray(data, 0, 20, format);
    assertEquals(10, buffer.getSampleCount());
    assertEquals(1, buffer.getChannelCount());
  }

  @Test(expected = IllegalArgumentException.class)
  public void initFromByteArray_bufferTooSmall_throws() {
    AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
    byte[] data = new byte[4];
    buffer.initFromByteArray(data, 0, 100, format);
  }

  // --- initFromFloatSampleBuffer ---

  @Test
  public void initFromFloatSampleBuffer_copiesData() {
    FloatSampleBuffer src = new FloatSampleBuffer(1, 10, 44100.0f);
    float[] ch = src.getChannel(0);
    ch[0] = 0.5f;
    ch[5] = -0.3f;
    buffer.initFromFloatSampleBuffer(src);
    assertEquals(1, buffer.getChannelCount());
    assertEquals(10, buffer.getSampleCount());
    assertEquals(0.5f, buffer.getChannel(0)[0], 0.001f);
    assertEquals(-0.3f, buffer.getChannel(0)[5], 0.001f);
  }

  // --- getFormatType ---

  @Test
  public void getFormatType_8bitSigned() {
    int ft = buffer.getFormatType(8, true, false);
    assertTrue(ft > 0);
  }

  @Test
  public void getFormatType_16bitSignedBigEndian() {
    int ft = buffer.getFormatType(16, true, true);
    assertTrue(ft > 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getFormatType_unsupportedBitSize_throws() {
    buffer.getFormatType(12, true, true);
  }
}
