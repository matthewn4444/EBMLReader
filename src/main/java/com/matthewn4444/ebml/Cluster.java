package com.matthewn4444.ebml;

import java.util.ArrayList;

import com.matthewn4444.ebml.node.BlockNode;
import com.matthewn4444.ebml.node.IntNode;
import com.matthewn4444.ebml.node.MasterNode;
import com.matthewn4444.ebml.node.StringNode;

public class Cluster {
    public static final int ID = 0x1F43B675;
    public static final int BLOCK_GROUP = 0xA0;

    // Cluster Values
    public static final int TIMECODE = 0xE7;
    public static final int POSITION = 0xA7;
    public static final int PREVIOUS_SIZE = 0xAB;
    public static final int SIMPLE_BLOCK = 0xA3;

    // Block Group Values
    public static final int BLOCK_DURATION = 0x9B;
    public static final int REFERENCE_PRIORITY = 0xFA;
    public static final int REFERENCE_BLOCK = 0xFB;
    public static final int CODEC_STATE = 0xA4;
    public static final int BLOCK_ID = 0xA1;


    static final MasterNode ENTRY = new MasterNode(ID);
    static final MasterNode BLOCK_GROUP_NODE = new MasterNode(BLOCK_GROUP);

    final static class Entry {
        int mTimecode;
        int mNextTimecode;
        long mStartAddress;
        long mEndAddress;
        int mRelativePosition;
        boolean mHasParsed;

        ArrayList<Entry> mSubEntries;

        public Entry(int timecode, long startAddress) {
            this(timecode, startAddress, 0);
        }

        public Entry(int timecode, long startAddress, int relativePosition) {
            mTimecode = timecode;
            mStartAddress = startAddress;
            mRelativePosition = relativePosition;
            mEndAddress = 0;
            mNextTimecode = 0;
        }

        public void addSubtitle(Entry entry) {
            if (mSubEntries == null) {
                mSubEntries = new ArrayList<>();
            }
            mSubEntries.add(entry);
        }
    }

    static void init() {
        ENTRY.addNode(new IntNode(TIMECODE));
        ENTRY.addNode(new IntNode(POSITION));
        ENTRY.addNode(new IntNode(PREVIOUS_SIZE));
        ENTRY.addNode(new BlockNode(SIMPLE_BLOCK));
        ENTRY.addNode(BLOCK_GROUP_NODE);

        BLOCK_GROUP_NODE.addNode(new IntNode(BLOCK_DURATION));
        BLOCK_GROUP_NODE.addNode(new IntNode(REFERENCE_PRIORITY));
        BLOCK_GROUP_NODE.addNode(new IntNode(REFERENCE_BLOCK));
        BLOCK_GROUP_NODE.addNode(new StringNode(CODEC_STATE));
        BLOCK_GROUP_NODE.addNode(new BlockNode(BLOCK_ID));
    }

    private Cluster() {}
}
