package com.matthewn4444.ebml.elements;


import android.util.Log;

import com.matthewn4444.ebml.EBMLParsingException;
import com.matthewn4444.ebml.node.IntNode;
import com.matthewn4444.ebml.node.NodeBase;

import java.io.IOException;
import java.io.RandomAccessFile;

public class IntElement extends ElementBase {
    private int mData;

    IntElement(IntNode node) {
        super(NodeBase.Type.INT, node.id());
        mData = node.getDefault();
    }

    /**
     * Get the integer data from this element entry
     * @return integer data
     */
    public int getData() {
        return mData;
    }

    @Override
    public boolean read(RandomAccessFile raf) throws IOException {
        int len = readLength(raf);
        switch (len) {
        case 1:
            mData = raf.readByte() & 0xFF;
            break;
        case 2:
            mData = raf.readShort() & 0xFFFF;
            break;
        case 3:
            mData = ((raf.readByte() & 0xFF) << 16) | (raf.readShort() & 0xFFFF);
            break;
        case 4:
            mData = raf.readInt();
            break;
        case 8:
            // 8 bytes for an integer is too long, we ignore it. Most likely an ID
            Log.w("IntElement", "Integer is contains 8 bytes, way too long for an integer shell. [id="
                    + hexId() + " @ 0x" + Long.toHexString(raf.getFilePointer()) + "]");
            raf.seek(raf.getFilePointer() + 8);
            mData = 0;
            break;
        default:
            throw new EBMLParsingException("get int [id= " + hexId() + " @ 0x" +
                    Long.toHexString(raf.getFilePointer()) + "] with len = " + len + " is not supported");
        }
        return true;
    }

    @Override
    public StringBuilder output(int level) {
        StringBuilder sb = super.output(level);
        Log.v(TAG, sb.toString() + "INT [" + hexId() + "]: " + mData);
        return null;
    }
}
