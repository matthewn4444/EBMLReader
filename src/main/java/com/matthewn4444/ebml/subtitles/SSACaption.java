package com.matthewn4444.ebml.subtitles;

import com.matthewn4444.ebml.elements.BlockElement;

public class SSACaption extends Caption {
    private String mCachedData;
    private String mCachedVTT;

    public SSACaption(BlockElement block, int timecode, int duration, boolean isCompressed) {
        super(Subtitles.Type.SSA, block, timecode, duration, isCompressed);
    }

    @Override
    public String getFormattedText() {
        if (mCachedData == null) {
            StringBuilder sb = new StringBuilder();
            String text = getStringData();
            int start = text.indexOf(",");
            if (start != -1) {
                int start2 = text.indexOf(",", start + 1);
                if (start2 != -1) {
                    sb.append("Dialogue: ")
                        .append(text.substring(start + 1, start2))
                        .append(',');
                    formatTimePoint(getStartTime(), sb);
                    sb.append(',');
                    formatTimePoint(getEndTime(), sb);
                    sb.append(',')
                        .append(text.substring(start2 + 1));
                } else {
                    throw new SSAParsingException("Subtitle entry sbc not formatted correctly for SSA");
                }
            } else {
                throw new SSAParsingException("Subtitle entry sbc not formatted correctly for SSA");
            }
            mCachedData = sb.toString();
        }
        return mCachedData;
    }

    @Override
    public String getFormattedVTT() {
        // TODO do this properly
        if (mCachedVTT == null) {
            String[] sections = getStringData().split(",", 9);
            String style = sections[2];
            if (style.equalsIgnoreCase("Default")) {
                String dialogue = sections[8];
                mCachedVTT = dialogue.replaceAll("\\{.*?\\}","");
            }
        }
        return mCachedVTT;
    }

    @Override
    protected void formatTimePoint(TimePoint time, StringBuilder sb) {
        int hours = time.getHours();
        int min = time.getMinutes();
        int sec = time.getSeconds();
        int msec = time.getMilliseconds();
        msec /= 10;
        sb.append(hours).append(':');
        if (0 <= min && min < 10) sb.append(0);
        sb.append(min).append(':');
        if (0 <= sec && sec < 10) sb.append(0);
        sb.append(sec).append('.');
        if (0 <= msec && msec < 10) sb.append(0);
        sb.append(msec);
    }
}
