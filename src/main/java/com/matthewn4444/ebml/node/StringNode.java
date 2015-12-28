package com.matthewn4444.ebml.node;

public class StringNode extends NodeBase {
    protected String mDefaultValue;

    public StringNode(int elementId) {
        this(elementId, null);
    }

    public StringNode(int elementId, String defaultValue) {
        super(NodeBase.Type.STRING, elementId);
        mDefaultValue = defaultValue;
    }

    public String getDefault() {
        return mDefaultValue;
    }
}
