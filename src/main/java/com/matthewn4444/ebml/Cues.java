package com.matthewn4444.ebml;

import com.matthewn4444.ebml.node.IntNode;
import com.matthewn4444.ebml.node.LongNode;
import com.matthewn4444.ebml.node.MasterNode;

public class Cues {
    public static final int ID = 0x1C53BB6B;

    public static final int POINT = 0xBB;
    public static final int TIME = 0xB3;
    public static final int TRACK_POSITIONS = 0xB7;
    public static final int TRACK = 0xF7;
    public static final int CLUSTER_POSITION = 0xF1;
    public static final int RELATIVE_POSITION = 0xF0;
    public static final int DURATION = 0xB2;
    public static final int BLOCK_NUMBER = 0x5378;
    public static final int CODEC_STATE = 0xEA;

    static final MasterNode HEADER = new MasterNode(ID);
    static final MasterNode POINT_NODE = new MasterNode(POINT);
    static final MasterNode TRACK_POSITION = new MasterNode(TRACK_POSITIONS);

    static void init() {
        HEADER.addNode(POINT_NODE);

        POINT_NODE.addNode(new IntNode(TIME));
        POINT_NODE.addNode(TRACK_POSITION);

        TRACK_POSITION.addNode(new IntNode(TRACK));
        TRACK_POSITION.addNode(new LongNode(CLUSTER_POSITION));
        TRACK_POSITION.addNode(new IntNode(RELATIVE_POSITION));
        TRACK_POSITION.addNode(new IntNode(DURATION));
        TRACK_POSITION.addNode(new IntNode(BLOCK_NUMBER));
        TRACK_POSITION.addNode(new IntNode(CODEC_STATE));
    }

    private Cues() {
    }
}
