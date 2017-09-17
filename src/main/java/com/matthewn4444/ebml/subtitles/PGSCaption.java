package com.matthewn4444.ebml.subtitles;

import com.matthewn4444.ebml.elements.BlockElement;

public class PGSCaption extends Caption {
    public PGSCaption(BlockElement block, int timecode, int duration,
                      boolean isCompressed) {
        super(Subtitles.Type.PGS, block, timecode, duration, isCompressed);
    }

    @Override
    public String getFormattedText() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFormattedVTT() {
        throw new UnsupportedOperationException();
    }
}
