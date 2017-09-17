package com.matthewn4444.ebml.subtitles;

import com.matthewn4444.ebml.elements.BlockElement;

import static com.matthewn4444.ebml.subtitles.Subtitles.Type.PGS;

public class PGSSubtitles extends Subtitles {

    PGSSubtitles(int trackNumber, long position, long size, boolean isEnabled, boolean isDefault,
                 String name, String language, boolean isCompressed) {
        super(PGS, trackNumber, position, size, isEnabled, isDefault, name, language,
                isCompressed);
    }

    @Override
    public void appendBlock(BlockElement block, int timecode, int duration) {
        appendCaption(new PGSCaption(block, timecode, duration, mIsCompressed));
    }

    @Override
    protected String getContents() {
        throw new UnsupportedOperationException();
    }
}
