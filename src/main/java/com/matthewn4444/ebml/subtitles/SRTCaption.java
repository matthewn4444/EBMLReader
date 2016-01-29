package com.matthewn4444.ebml.subtitles;

import com.matthewn4444.ebml.elements.BlockElement;

public class SRTCaption extends Caption {
    private String mCachedData;

    public SRTCaption(BlockElement block, int timecode, int duration, boolean isCompressed) {
        super(Subtitles.Type.SRT, block, timecode, duration, isCompressed);
    }

    @Override
    public String getFormattedText() {
        if (mCachedData == null) {
            StringBuilder sb = new StringBuilder();
            formatTimePoint(getStartTime(), sb);
            sb.append(" --> ");
            formatTimePoint(getEndTime(), sb);
            sb.append('\n')
                .append(getData())
                .append("\n\n");
            mCachedData = sb.toString();
        }
        return mCachedData;
    }

    @Override
    public String getFormattedVTT() {
        return getData();
    }

    @Override
    protected void formatTimePoint(TimePoint time, StringBuilder sb) {
        int hours = time.getHours();
        int min = time.getMinutes();
        int sec = time.getSeconds();
        int msec = time.getMilliseconds();
        if (0 <= hours && hours < 10) sb.append(0);
        sb.append(hours).append(':');
        if (0 <= min && min < 10) sb.append(0);
        sb.append(min).append(':');
        if (0 <= sec && sec < 10) sb.append(0);
        sb.append(sec).append(',');
        if (0 <= msec && msec < 100) sb.append(0);
        if (0 <= msec && msec < 10) sb.append(0);
        sb.append(msec);
    }

}
