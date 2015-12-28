package com.matthewn4444.ebml.subtitles;

import com.matthewn4444.ebml.elements.BlockElement;

public class SRTSubtitles extends Subtitles {

    SRTSubtitles(int trackNumber, boolean isEnabled,
            boolean isDefault, String name, String language) {
        super(Subtitles.Type.SRT, trackNumber, isEnabled, isDefault, name, language);
    }

    @Override
    public void appendBlock(BlockElement block, int timecode, int duration) {
        appendCaption(new SRTCaption(block, timecode, duration));
    }

    @Override
    protected String getContents() {
        StringBuilder sb = new StringBuilder();
        int subNumber = 1;
        for (int i = 0; i < mReadCaptions.size(); i++) {
            sb.append(subNumber++).append('\n');
            sb.append(mReadCaptions.get(i).getFormattedText());
        }
        for (int i = 0; i < mUnreadCaptions.size(); i++) {
            sb.append(subNumber++).append('\n');
            sb.append(mUnreadCaptions.get(i).getFormattedText());
        }
        return sb.toString();
    }
}
