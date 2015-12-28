package com.matthewn4444.ebml;

import com.matthewn4444.ebml.node.IntNode;
import com.matthewn4444.ebml.node.MasterNode;

public final class Segment {
    public static final int ID = 0x18538067;
    public static final int SEEK_HEAD = 0x114D9B74;

    public static final int SEEK = 0x4DBB;
    public static final int SEEK_ID = 0x53AB;
    public static final int SEEK_POSITION = 0x53AC;

    static final MasterNode HEADER = new MasterNode(SEEK_HEAD);
    static final MasterNode ENTRY = new MasterNode(SEEK);

    static void init() {
        HEADER.addNode(ENTRY);

        ENTRY.addNode(new IntNode(SEEK));
        ENTRY.addNode(new IntNode(SEEK_ID));
        ENTRY.addNode(new IntNode(SEEK_POSITION));
    }

    private Segment() {}
}
