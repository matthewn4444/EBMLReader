package com.matthewn4444.ebml.node;

public class LongNode extends NodeBase {
    protected long mDefaultValue;

    public LongNode(int elementId) {
        this(elementId, 0);
    }

    public LongNode(int elementId, long defaultValue) {
        super(NodeBase.Type.LONG, elementId);
        mDefaultValue = defaultValue;
    }

    public long getDefault() {
        return mDefaultValue;
    }
}
