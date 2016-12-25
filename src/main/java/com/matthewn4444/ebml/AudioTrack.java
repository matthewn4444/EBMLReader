package com.matthewn4444.ebml;

import com.matthewn4444.ebml.elements.IntElement;
import com.matthewn4444.ebml.elements.MasterElement;

import java.io.UnsupportedEncodingException;

public class AudioTrack extends Tracks {

    protected final Type mType;
    protected final int mChannels;

    public enum Type {
        AAC("A_AAC"),
        DTS("A_DTS"),
        AC3("A_AC3"),
        FLAC("A_FLAC"),
        UNKNOWN("Unknown");

        static Type fromString(String text) {
            if (text.equals(AAC.mName)) {
                return AAC;
            } else if (text.equals(DTS.mName)) {
                return DTS;
            } else if (text.equals(AC3.mName)) {
                return AC3;
            } else if (text.equals(FLAC.mName)) {
                return FLAC;
            }
            return UNKNOWN;
        }

        Type(String name) {
            mName = name;
        }

        private final String mName;
    }

    static AudioTrack fromMasterTrackElement(MasterElement el) throws UnsupportedEncodingException {
        int trackNum = el.getValueInt(Tracks.NUMBER);
        IntElement enableEl = (IntElement) el.getElement(Tracks.IS_ENABLED);
        IntElement defaultEl = (IntElement) el.getElement(Tracks.IS_DEFAULT);
        boolean isEnabled = enableEl == null || enableEl.getData() == 1;
        boolean isDefault = defaultEl == null || defaultEl.getData() == 1;
        String name = el.getValueString(Tracks.NAME);
        String language = el.getValueString(Tracks.LANGUAGE);
        String codec = el.getValueString(Tracks.CODEC_ID);

        MasterElement audioEl = (MasterElement) el.getElement(Tracks.AUDIO_ENTRY);
        int numChannels = audioEl.getValueInt(Tracks.CHANNELS);
        return new AudioTrack(Type.fromString(codec), trackNum, el.getFilePosition(),
                el.getFileLength(), isEnabled, isDefault, name, language, numChannels);
    }

    AudioTrack(Type type, int trackNumber, long position, long length, boolean isEnabled,
               boolean isDefault, String name, String language, int channels) {
        super(trackNumber, position, length, isEnabled, isDefault, name, language);
        mType = type;
        mChannels = channels;
    }

    /**
     * Gets the type of Audio
     *
     * @return the audio type
     */
    public Type getType() {
        return mType;
    }

    /**
     * Get the number of channels the audio track has
     *
     * @return number of channels
     */
    public int getNumChannels() {
        return mChannels;
    }

    /**
     * Gets the channel presentable name such as mono for 1 channel,
     * 2 channel as stereo and 5.1 for 6 channels. If there is no
     * standard name for the channel, this will return "X Channels"
     *
     * @return channel presentable name
     */
    public String getChannelPresentable() {
        switch (mChannels) {
            case 0:
                return "Unknown";
            case 1:
                return "Mono";
            case 2:
                return "Stereo";
            case 6:
                return "5.1";
            case 7:
                return "5.2";
            case 8:
                return "7.1";
            case 9:
                return "7.2";
            default:
                return mChannels + " Channels";
        }
    }

    @Override
    public int getTrackType() {
        return Tracks.Type.AUDIO;
    }
}
