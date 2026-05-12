package edu.cmu.cs.dennisc.render.joglrenderer;

import java.text.CharacterIterator;

/**
 * Extracted from NonCachingTextRenderer.CharSequenceIterator inner class.
 *
 * Suppressing checkstyle to ease comparison with TextRenderer.
 */
@SuppressWarnings("CheckStyle")
class CharSequenceIterator implements CharacterIterator {
  CharSequence mSequence;
  int mLength;
  int mCurrentIndex;

  CharSequenceIterator() {
  }

  CharSequenceIterator(final CharSequence sequence) {
    initFromCharSequence(sequence);
  }

  public void initFromCharSequence(final CharSequence sequence) {
    mSequence = sequence;
    mLength = mSequence.length();
    mCurrentIndex = 0;
  }

  @Override
  public char last() {
    mCurrentIndex = Math.max(0, mLength - 1);

    return current();
  }

  @Override
  public char current() {
    if ((mLength == 0) || (mCurrentIndex >= mLength)) {
      return CharacterIterator.DONE;
    }

    return mSequence.charAt(mCurrentIndex);
  }

  @Override
  public char next() {
    mCurrentIndex++;

    return current();
  }

  @Override
  public char previous() {
    mCurrentIndex = Math.max(mCurrentIndex - 1, 0);

    return current();
  }

  @Override
  public char setIndex(final int position) {
    mCurrentIndex = position;

    return current();
  }

  @Override
  public int getBeginIndex() {
    return 0;
  }

  @Override
  public int getEndIndex() {
    return mLength;
  }

  @Override
  public int getIndex() {
    return mCurrentIndex;
  }

  @Override
  public Object clone() {
    final CharSequenceIterator iter = new CharSequenceIterator(mSequence);
    iter.mCurrentIndex = mCurrentIndex;

    return iter;
  }

  @Override
  public char first() {
    if (mLength == 0) {
      return CharacterIterator.DONE;
    }

    mCurrentIndex = 0;

    return current();
  }
}
