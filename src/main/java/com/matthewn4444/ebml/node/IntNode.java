package com.matthewn4444.ebml.node;

public class IntNode extends NodeBase {
    protected int mDefaultValue;

    public IntNode(int elementId) {
        this(elementId, 0);
    }

    public IntNode(int elementId, int defaultValue) {
        super(NodeBase.Type.INT, elementId);
        mDefaultValue = defaultValue;
    }

    public int getDefault() {
        return mDefaultValue;
    }
}
