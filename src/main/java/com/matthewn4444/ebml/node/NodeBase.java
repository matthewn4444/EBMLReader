package com.matthewn4444.ebml.node;

public abstract class NodeBase {
    public static enum Type { UNSET, INT, STRING, BYTES, BLOCK, MASTER };

    protected Type mType;
    protected int mId;

    public NodeBase(Type t, int elementId) {
        mType = t;
        mId = elementId;
    }

    public Type getType() {
        return mType;
    }

    public int id() {
        return mId;
    }
}
