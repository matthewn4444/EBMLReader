package com.matthewn4444.ebml.elements;


import android.util.Log;

import com.matthewn4444.ebml.EBMLParsingException;
import com.matthewn4444.ebml.node.LongNode;
import com.matthewn4444.ebml.node.NodeBase;

import java.io.IOException;
import java.io.RandomAccessFile;

public class LongElement extends ElementBase {
    private long mData;

    LongElement(LongNode node) {
        super(NodeBase.Type.LONG, node.id());
        mData = node.getDefault();
    }

    /**
     * Get the long data from this element entry
     * @return long data
     */
    public long getData() {
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
            mData = raf.readInt() & 0x00000000ffffffffL;        // Convert the signed int to unsigned long
            break;
        case 5:
            mData = ((raf.readByte() & 0xFF) << 32) | (raf.readInt() & 0x00000000ffffffffL);
            break;
        case 8:
            mData = raf.readLong();
            break;
        default:
            throw new EBMLParsingException("get long [id= " + hexId() + " @ 0x" +
                    Long.toHexString(raf.getFilePointer()) + "] with len = " + len + " is not supported");
        }
        return true;
    }

    @Override
    public StringBuilder output(int level) {
        StringBuilder sb = super.output(level);
        Log.v(TAG, sb.toString() + "LONG [" + hexId() + "]: " + mData);
        return null;
    }
}
