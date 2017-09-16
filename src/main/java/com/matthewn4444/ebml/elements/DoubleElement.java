package com.matthewn4444.ebml.elements;


import android.util.Log;

import com.matthewn4444.ebml.EBMLParsingException;
import com.matthewn4444.ebml.node.FloatNode;
import com.matthewn4444.ebml.node.NodeBase;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DoubleElement extends ElementBase {
    private double mData;

    DoubleElement(FloatNode nextNode, long position) {
        super(NodeBase.Type.DOUBLE, nextNode.id(), position);
        mData = nextNode.getDefault();
    }

    /**
     * Get the double data from this element entry
     * @return double data
     */
    public double getData() {
        return mData;
    }

    @Override
    boolean read(RandomAccessFile raf) throws IOException {
        super.read(raf);

        if (mInnerLength == 8) {
            byte[] bytes = new byte[8];
            raf.read(bytes);
            mData = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getDouble();
        } else {
            throw new EBMLParsingException("get double [id= " + hexId() + " @ 0x" +
                    Long.toHexString(raf.getFilePointer()) + "] with len = " + mInnerLength
                    + " is not supported");
        }
        return true;
    }

    @Override
    public StringBuilder output(int level) {
        StringBuilder sb = super.output(level);
        Log.v(TAG, sb.toString() + "DOUBLE [" + hexId() + "]: " + mData);
        return null;
    }
}
