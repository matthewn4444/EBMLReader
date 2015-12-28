package com.matthewn4444.ebml.node;

import java.util.HashMap;

public class MasterNode extends NodeBase {
    private final HashMap<Integer, NodeBase> mLookup;

    public MasterNode(int elementId) {
        super(NodeBase.Type.MASTER, elementId);
        mLookup = new HashMap<Integer, NodeBase>();
    }

    public void addNode(NodeBase node) {
        mLookup.put(node.id(), node);
    }

    public HashMap<Integer, NodeBase> getLookup() {
        return mLookup;
    }
}
