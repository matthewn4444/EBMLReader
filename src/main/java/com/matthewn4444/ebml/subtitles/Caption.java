package com.matthewn4444.ebml.subtitles;

import com.matthewn4444.ebml.elements.BlockElement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public abstract class Caption {

    public static class TimePoint {
        private final int mHour;
        private final int mMin;
        private final int mSec;
        private final int mMillsec;
        private final int mTime;

        public TimePoint(int time) {
            int left = time;
            mHour = (int) (left / (1000.f * 60 * 60));
            left -= mHour * (1000 * 60 * 60);
            mMin = (int) (left / (1000.f * 60));
            left -= mMin * (1000 * 60);
            mSec = (int) (left / 1000.0f);
            left -= mSec * 1000;
            mMillsec = left;
            mTime = time;
        }

        public int getHours() {
            return mHour;
        }

        public int getMinutes() {
            return mMin;
        }

        public int getSeconds() {
            return mSec;
        }

        public int getMilliseconds() {
            return mMillsec;
        }

        public int getTime() {
            return mTime;
        }

        public String format() {
            StringBuilder sb = new StringBuilder();
            if (0 <= mHour && mHour < 10) sb.append(0);
            sb.append(mHour).append(':');
            if (0 <= mMin && mMin < 10) sb.append(0);
            sb.append(mMin).append(':');
            if (0 <= mSec && mSec < 10) sb.append(0);
            sb.append(mSec).append('.');
            if (0 <= mMillsec && mMillsec < 100) sb.append(0);
            if (0 <= mMillsec && mMillsec < 10) sb.append(0);
            sb.append(mMillsec);
            return sb.toString();
        }
    }

    private final TimePoint mStart;
    private final TimePoint mEnd;
    private final Subtitles.Type mType;
    private final boolean mIsCompressed;
    private final Inflater mDecompressor;
    private final BlockElement mBlock;

    public Caption(Subtitles.Type type, BlockElement block, int timecode, int duration, boolean isCompressed) {
        mType = type;
        mBlock = block;
        mIsCompressed = isCompressed;
        int start = block.getTimecode() + timecode;
        mStart = new TimePoint(start);
        mEnd = new TimePoint(start + duration);
        mDecompressor = new Inflater();
    }

    public abstract String getFormattedText();
    public abstract String getFormattedVTT();

    public byte[] getByteData() {
        try {
            byte[] data = mBlock.readData();
            if (mIsCompressed) {
                // Run zlib decompression on these bytes
                mDecompressor.setInput(data);
                ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
                try {
                    byte[] buf = new byte[1024];
                    while (!mDecompressor.finished()) {
                        int count = mDecompressor.inflate(buf);
                        bos.write(buf, 0, count);
                    }
                    return bos.toByteArray();
                } catch (DataFormatException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        bos.close();
                    } catch (IOException ignored) {
                    }
                }
            } else {
                return data;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getStringData() {
        byte[] data = getByteData();
        if (data == null) {
            return null;
        }
        try {
            return new String(data, "utf8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public TimePoint getStartTime() {
        return mStart;
    }

    public TimePoint getEndTime() {
        return mEnd;
    }

    public Subtitles.Type getType() {
        return mType;
    }

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
        sb.append(sec).append('.');
        if (0 <= msec && msec < 100) sb.append(0);
        if (0 <= msec && msec < 10) sb.append(0);
        sb.append(msec);
    }
}
