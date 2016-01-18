package com.matthewn4444.ebml.elements;


import java.io.IOException;
import java.io.RandomAccessFile;

import android.util.Log;

import com.matthewn4444.ebml.EBMLParsingException;
import com.matthewn4444.ebml.node.FloatNode;
import com.matthewn4444.ebml.node.NodeBase;

public class FloatElement extends ElementBase {
    private float mData;

    FloatElement(FloatNode nextNode) {
        super(NodeBase.Type.FLOAT, nextNode.id());
        mData = nextNode.getDefault();
    }

    /**
     * Get the long data from this element entry
     * @return long data
     */
    public float getData() {
        return mData;
    }

    @Override
    public boolean read(RandomAccessFile raf) throws IOException {
        int len = readLength(raf);
        switch (len) {
        case 4:
            mData = raf.readFloat();
            break;
        default:
            throw new EBMLParsingException("get float [id= " + hexId() + " @ 0x" +
                    Long.toHexString(raf.getFilePointer()) + "] with len = " + len + " is not supported");
        }
        return true;
    }

    @Override
    public StringBuilder output(int level) {
        StringBuilder sb = super.output(level);
        Log.v(TAG, sb.toString() + "FLOAT [" + hexId() + "]: " + mData);
        return null;
    }
}
