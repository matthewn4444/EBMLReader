package com.matthewn4444.ebml;

import com.matthewn4444.ebml.node.FloatNode;
import com.matthewn4444.ebml.node.IntNode;
import com.matthewn4444.ebml.node.LongNode;
import com.matthewn4444.ebml.node.MasterNode;
import com.matthewn4444.ebml.node.StringNode;

public abstract class Tracks {
    public static final int ID = 0x1654AE6B;
    public static final int ENTRY =  0xAE;
    public static final int VIDEO_ENTRY = 0xE0;
    public static final int AUDIO_ENTRY = 0xE1;
    public static final int CONTENT_ENCODINGS_ENTRY = 0x6D80;

    // Entry Values
    public static final int NUMBER = 0xD7;
    public static final int UID = 0x73C5;
    public static final int TYPE = 0x83;
    public static final int IS_ENABLED = 0xB9;
    public static final int IS_DEFAULT = 0x88;
    public static final int FLAG_FORCED = 0x55AA;
    public static final int FLAG_LACED = 0x9C;
    public static final int MIN_CACHE = 0x6DE7;
    public static final int MAX_CACHE = 0x6DF8;
    public static final int DEFAULT_DURATION = 0x23E383;
    public static final int MAX_BLOCK_ADDITION_ID = 0x55EE;
    public static final int NAME = 0x536E;
    public static final int LANGUAGE = 0x22B59C;
    public static final int CODEC_ID = 0x86;
    public static final int CODEC_PRIVATE = 0x63A2;
    public static final int CODEC_NAME = 0x258688;
    public static final int ATTACHMENT_LINK = 0x7446;
    public static final int CODEC_DECODE_ALL = 0xAA;
    public static final int OVERLAY = 0x6FAB;
    public static final int CODEC_DELAY = 0x56AA;
    public static final int TIMECODE_SCALE = 0x23314F;

    // Video Entry Values
    public static final int FLAG_INTERLACED = 0x9A;
    public static final int STEREO_MODE = 0x53B8;
    public static final int ALPHA_MODE = 0x53C0;
    public static final int PIXEL_WIDTH = 0xB0;
    public static final int PIXEL_HEIGHT = 0xBA;
    public static final int PIXEL_CROP_BOTTOM = 0x54AA;
    public static final int PIXEL_CROP_TOP = 0x54BB;
    public static final int PIXEL_CROP_LEFT = 0x54CC;
    public static final int PIXEL_CROP_RIGHT = 0x54DD;
    public static final int DISPLAY_WIDTH = 0x54B0;
    public static final int DISPLAY_HEIGHT = 0x54BA;
    public static final int DISPLAY_UNIT = 0x54B2;
    public static final int ASPECT_RATIO = 0x54B3;
    public static final int COLOR_SPACE = 0x2EB524;

    // Audio Entry Values
    public static final int SAMPLING_FREQUENCY = 0xB5;
    public static final int OUTPUT_SAMPLING_FREQUENCY = 0x78B5;
    public static final int CHANNELS = 0x9F;
    public static final int BIT_DEPTH = 0x6264;

    // Content Encodings
    public static final int CONTENT_ENCODING = 0x6240;
    public static final int CONTENT_ENCODING_ORDER = 0x5031;
    public static final int CONTENT_ENCODING_SCOPE = 0x5032;
    public static final int CONTENT_ENCODING_TYPE = 0X5033;
    public static final int CONTENT_COMPRESSION = 0X5034;
    public static final int CONTENT_COMP_ALGO = 0x4254;
    public static final int CONTENT_COMP_SETTINGS = 0x4255;
    public static final int CONTENT_ENCRYPTION = 0X5035;
    public static final int CONTENT_ENC_ALGO = 0X47E1;
    public static final int CONTENT_ENC_KEY_ID = 0X47E2;
    public static final int CONTENT_SIGNATURE = 0X47E3;
    public static final int CONTENT_SIG_KEY_ID = 0X47E4;
    public static final int CONTENT_SIG_ALGO = 0X47E5;
    public static final int CONTENT_SIG_HASH_ALGO = 0X47E6;


    public static final class Type {
        public static final int VIDEO = 1;
        public static final int AUDIO = 2;
        public static final int COMPLEX = 3;
        public static final int LOGO = 0x10;
        public static final int SUBTITLE = 0x11;
        public static final int BUTTONS = 0x12;
        public static final int CONTROL = 0x20;

        private Type() {}
    }

    static final MasterNode HEADER = new MasterNode(ID);
    static final MasterNode ENTRY_NODE = new MasterNode(ENTRY);
    static final MasterNode VIDEO_ENTRY_NODE = new MasterNode(VIDEO_ENTRY);
    static final MasterNode AUDIO_ENTRY_NODE = new MasterNode(AUDIO_ENTRY);
    static final MasterNode CONTENT_ENCODINGS_NODE = new MasterNode(CONTENT_ENCODINGS_ENTRY);
    static final MasterNode CONTENT_ENCODING_NODE = new MasterNode(CONTENT_ENCODING);
    static final MasterNode CONTENT_COMPRESSION_NODE = new MasterNode(CONTENT_COMPRESSION);
    static final MasterNode CONTENT_ENCYPTION_NODE = new MasterNode(CONTENT_ENCRYPTION);

    static void init() {
        HEADER.addNode(ENTRY_NODE);

        ENTRY_NODE.addNode(new IntNode(NUMBER));
        ENTRY_NODE.addNode(new LongNode(UID));
        ENTRY_NODE.addNode(new IntNode(TYPE));
        ENTRY_NODE.addNode(new IntNode(IS_ENABLED));
        ENTRY_NODE.addNode(new IntNode(IS_DEFAULT));
        ENTRY_NODE.addNode(new IntNode(FLAG_FORCED));
        ENTRY_NODE.addNode(new IntNode(FLAG_LACED));
        ENTRY_NODE.addNode(new IntNode(MIN_CACHE));
        ENTRY_NODE.addNode(new IntNode(MAX_CACHE));
        ENTRY_NODE.addNode(new IntNode(DEFAULT_DURATION));
        ENTRY_NODE.addNode(new IntNode(MAX_BLOCK_ADDITION_ID));
        ENTRY_NODE.addNode(new StringNode(NAME));
        ENTRY_NODE.addNode(new StringNode(LANGUAGE));
        ENTRY_NODE.addNode(new StringNode(CODEC_ID));
        ENTRY_NODE.addNode(new StringNode(CODEC_PRIVATE));
        ENTRY_NODE.addNode(new StringNode(CODEC_NAME));
        ENTRY_NODE.addNode(new IntNode(ATTACHMENT_LINK));
        ENTRY_NODE.addNode(new IntNode(CODEC_DECODE_ALL));
        ENTRY_NODE.addNode(new IntNode(OVERLAY));
        ENTRY_NODE.addNode(new IntNode(CODEC_DELAY));
        ENTRY_NODE.addNode(new IntNode(TIMECODE_SCALE));
        ENTRY_NODE.addNode(VIDEO_ENTRY_NODE);
        ENTRY_NODE.addNode(AUDIO_ENTRY_NODE);
        ENTRY_NODE.addNode(CONTENT_ENCODINGS_NODE);

        VIDEO_ENTRY_NODE.addNode(new IntNode(FLAG_INTERLACED));
        VIDEO_ENTRY_NODE.addNode(new IntNode(STEREO_MODE));
        VIDEO_ENTRY_NODE.addNode(new IntNode(ALPHA_MODE));
        VIDEO_ENTRY_NODE.addNode(new IntNode(PIXEL_WIDTH));
        VIDEO_ENTRY_NODE.addNode(new IntNode(PIXEL_HEIGHT));
        VIDEO_ENTRY_NODE.addNode(new IntNode(PIXEL_CROP_BOTTOM));
        VIDEO_ENTRY_NODE.addNode(new IntNode(PIXEL_CROP_TOP));
        VIDEO_ENTRY_NODE.addNode(new IntNode(PIXEL_CROP_LEFT));
        VIDEO_ENTRY_NODE.addNode(new IntNode(PIXEL_CROP_RIGHT));
        VIDEO_ENTRY_NODE.addNode(new IntNode(DISPLAY_WIDTH));
        VIDEO_ENTRY_NODE.addNode(new IntNode(DISPLAY_HEIGHT));
        VIDEO_ENTRY_NODE.addNode(new IntNode(DISPLAY_UNIT));
        VIDEO_ENTRY_NODE.addNode(new IntNode(ASPECT_RATIO));
        VIDEO_ENTRY_NODE.addNode(new IntNode(COLOR_SPACE));

        AUDIO_ENTRY_NODE.addNode(new FloatNode(SAMPLING_FREQUENCY));
        AUDIO_ENTRY_NODE.addNode(new FloatNode(OUTPUT_SAMPLING_FREQUENCY));
        AUDIO_ENTRY_NODE.addNode(new IntNode(CHANNELS));
        AUDIO_ENTRY_NODE.addNode(new IntNode(BIT_DEPTH));

        CONTENT_ENCODINGS_NODE.addNode(CONTENT_ENCODING_NODE);

        CONTENT_ENCODING_NODE.addNode(new IntNode(CONTENT_ENCODING_ORDER));
        CONTENT_ENCODING_NODE.addNode(new IntNode(CONTENT_ENCODING_SCOPE));
        CONTENT_ENCODING_NODE.addNode(new IntNode(CONTENT_ENCODING_TYPE));
        CONTENT_ENCODING_NODE.addNode(CONTENT_COMPRESSION_NODE);
        CONTENT_ENCODING_NODE.addNode(CONTENT_ENCYPTION_NODE);

        CONTENT_COMPRESSION_NODE.addNode(new IntNode(CONTENT_COMP_ALGO));
        CONTENT_COMPRESSION_NODE.addNode(new StringNode(CONTENT_COMP_SETTINGS));

        CONTENT_ENCYPTION_NODE.addNode(new IntNode(CONTENT_ENC_ALGO));
        CONTENT_ENCYPTION_NODE.addNode(new StringNode(CONTENT_ENC_KEY_ID));
        CONTENT_ENCYPTION_NODE.addNode(new StringNode(CONTENT_SIGNATURE));
        CONTENT_ENCYPTION_NODE.addNode(new StringNode(CONTENT_SIG_KEY_ID));
        CONTENT_ENCYPTION_NODE.addNode(new IntNode(CONTENT_SIG_ALGO));
        CONTENT_ENCYPTION_NODE.addNode(new IntNode(CONTENT_SIG_HASH_ALGO));
    }

    protected final int mTrackNumber;
    protected final boolean mEnabled;
    protected final boolean mDefault;
    protected final String mName;
    protected final String mLanguage;

    protected Tracks(int trackNumber, boolean isEnabled, boolean isDefault, String name,
                     String language) {
        mTrackNumber = trackNumber;
        mEnabled = isEnabled;
        mDefault = isDefault;
        mName = name;
        mLanguage = language;
    }

    /**
     * Get the track number that was assigned
     * @return the track number
     */
    public int getTrackNumber() {
        return mTrackNumber;
    }

    /**
     * The subtitle data will specify if this track should be enabled or not
     * @return if track is enabled or not
     */
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * The subtitle data will specify if this track is the default track
     * @return if default track
     */
    public boolean isDefault() {
        return mDefault;
    }

    /**
     * Get the name of this subtitle entry
     * @return the name of this subtitle
     */
    public String getName() {
        return mName;
    }

    /**
     * Get the language of this subtitle entry
     * @return the language of this subtitle
     */
    public String getLanguage() {
        return mLanguage;
    }

    /**
     * Gets the track type. Can be any of the following
     * 	Tracks.Type.VIDEO
     * 	Tracks.Type.AUDIO
     * 	Tracks.Type.COMPLEX
     * 	Tracks.Type.LOGO
     * 	Tracks.Type.SUBTITLE
     * 	Tracks.Type.BUTTONS
     * 	Tracks.Type.CONTROL
     * @return track type
     */
    public abstract int getTrackType();
}
