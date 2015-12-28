package com.matthewn4444.ebml.elements;

import android.util.Log;

import com.matthewn4444.ebml.node.ByteNode;
import com.matthewn4444.ebml.node.NodeBase;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Similar to StringElement but this will not allocate nor copy the data from the file stream.
 * You can access the data later if you need it
 */
public class ByteElement extends ElementBase {
    private long mPosition;
    private long mLength;

    ByteElement(ByteNode node) {
        super(NodeBase.Type.BYTES, node.id());
    }

    /**
     * Allocates and returns the data of this element
     * @param raf file stream
     * @return data
     * @throws IOException
     */
    public byte[] getData(RandomAccessFile raf) throws IOException {
        long oldPointer = raf.getFilePointer();
        raf.seek(mPosition);
        byte[] buffer = new byte[(int) mLength];
        raf.read(buffer);
        raf.seek(oldPointer);
        return buffer;
    }

    /**
     * Get the position of the data using RandomAccessFile.seek()
     * @return position
     */
    public long getPosition() {
        return mPosition;
    }

    /**
     * Get the data length of the data to be read
     * @return data length of content
     */
    public long getLength() {
        return mLength;
    }

    @Override
    public boolean read(RandomAccessFile raf) throws IOException {
        mLength = readLength(raf);
        mPosition = raf.getFilePointer();
        raf.seek(mLength + mPosition);
        return true;
    }

    @Override
    public StringBuilder output(int level) {
        StringBuilder sb = super.output(level);
        String data = "Binary(@" + Integer.toHexString((int) mPosition) + " len=" + mLength + ")";
        Log.v(TAG, sb.toString() + "BYTE [" + hexId() + "]: '" + data + "'");
        return null;
    }
}
