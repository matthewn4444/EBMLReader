package com.matthewn4444.ebml.node;

public class FloatNode extends NodeBase {
    protected float mDefaultValue;

    public FloatNode(int elementId) {
        this(elementId, 0);
    }

    public FloatNode(int elementId, float defaultValue) {
        super(NodeBase.Type.FLOAT, elementId);
        mDefaultValue = defaultValue;
    }

    public float getDefault() {
        return mDefaultValue;
    }
}
