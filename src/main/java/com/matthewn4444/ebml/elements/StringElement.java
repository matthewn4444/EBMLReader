package com.matthewn4444.ebml.elements;

import android.util.Log;

import com.matthewn4444.ebml.node.NodeBase;
import com.matthewn4444.ebml.node.StringNode;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

public class StringElement extends ElementBase {
    private String mData;

    StringElement(StringNode node) throws UnsupportedEncodingException {
        super(NodeBase.Type.STRING, node.id());
        if (node.getDefault() != null) {
            mData = node.getDefault();
        }
    }

    /**
     * Get the string data in this element
     * @return string
     */
    public String getString() {
        return mData;
    }

    @Override
    public boolean read(RandomAccessFile raf) throws IOException {
        int contentLength = readLength(raf);
        byte[] buffer = new byte[contentLength];
        raf.read(buffer);
        mData = new String(buffer, "utf8");
        return true;
    }

    @Override
    public StringBuilder output(int level) {
        StringBuilder sb = super.output(level);
        String data = mData.length() > 20 ? mData.substring(0, 20) + "..." : mData;
        Log.v(TAG, sb.toString() + "STR [" + hexId() + "]: '" + data + "'");
        return null;
    }
}
