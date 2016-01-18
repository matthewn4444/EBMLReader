package com.matthewn4444.ebml;

import com.matthewn4444.ebml.node.ByteNode;
import com.matthewn4444.ebml.node.FloatNode;
import com.matthewn4444.ebml.node.IntNode;
import com.matthewn4444.ebml.node.LongNode;
import com.matthewn4444.ebml.node.MasterNode;
import com.matthewn4444.ebml.node.StringNode;

public class Info {
    public static final int ID = 0x1549A966;

    public static final int SEGMENT_UID = 0x73A4;
    public static final int SEGMENT_FILENAME = 0x7384;
    public static final int PREV_UID = 0x3CB923;
    public static final int PREV_FILENAME = 0x3C83AB;
    public static final int NEXT_UID = 0x3EB923;
    public static final int NEXT_FILENAME = 0x3C83BB;
    public static final int SEGMENT_FAMILY = 0x4444;
    public static final int CHAPTER_TRANSLATE_ENTRY = 0x6924;
    public static final int TIMECODE_SCALE = 0x2AD7B1;
    public static final int DURATION = 0x4489;
    public static final int DATE = 0x4461;
    public static final int TITLE = 0x7BA9;
    public static final int MUXING_APP = 0x4D80;
    public static final int WRITING_APP = 0x5741;

    public static final int CHAPTER_TRANSLATE_EDITION_UID = 0x69FC;
    public static final int CHAPTER_TRANSLATE_CODEC = 0x69BF;
    public static final int CHAPTER_TRANSLATE_ID = 0x69A5;

    static final MasterNode HEADER = new MasterNode(ID);
    static final MasterNode CHAPTER_TRANSLATE_NODE = new MasterNode(CHAPTER_TRANSLATE_ENTRY);

    static void init() {
        HEADER.addNode(CHAPTER_TRANSLATE_NODE);
        HEADER.addNode(new ByteNode(SEGMENT_UID));
        HEADER.addNode(new StringNode(SEGMENT_FILENAME));
        HEADER.addNode(new ByteNode(PREV_UID));
        HEADER.addNode(new StringNode(PREV_FILENAME));
        HEADER.addNode(new ByteNode(NEXT_UID));
        HEADER.addNode(new StringNode(NEXT_FILENAME));
        HEADER.addNode(new ByteNode(SEGMENT_FAMILY));
        HEADER.addNode(new IntNode(TIMECODE_SCALE));
        HEADER.addNode(new FloatNode(DURATION));
        HEADER.addNode(new LongNode(DATE));
        HEADER.addNode(new StringNode(TITLE));
        HEADER.addNode(new StringNode(MUXING_APP));
        HEADER.addNode(new StringNode(WRITING_APP));

        CHAPTER_TRANSLATE_NODE.addNode(new ByteNode(CHAPTER_TRANSLATE_EDITION_UID));
        CHAPTER_TRANSLATE_NODE.addNode(new IntNode(CHAPTER_TRANSLATE_CODEC));
        CHAPTER_TRANSLATE_NODE.addNode(new ByteNode(CHAPTER_TRANSLATE_ID));
    }

    private Info() {
    }
}
