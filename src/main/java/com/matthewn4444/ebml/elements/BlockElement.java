package com.matthewn4444.ebml.elements;

import android.util.Log;

import com.matthewn4444.ebml.EBMLParsingException;
import com.matthewn4444.ebml.node.BlockNode;
import com.matthewn4444.ebml.node.NodeBase;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Set;

/**
 * A block element is simple representation of a BLOCK object represented in the EBML documentation.
 * http://www.matroska.org/technical/specs/index.html#block_structure
 *
 * This will contain all the information gained from parsing track data from cluster entries
 */
public class BlockElement extends ElementBase {
    private byte[] mData;
    private int mTrackNumber;
    private int mTimecode;
    private int mFlag;

    /**
     * Quickly scan the block data whether to read it or not depending if we whitelisted the track
     * number of this block of data
     * @param raf file stream
     * @param validNumbers whitelist of valid track numbers
     * @return whether to skip or not
     * @throws IOException
     */
    static boolean skipBlockIfNotTrackNumber(RandomAccessFile raf, Set<Integer> validNumbers) throws IOException {
        long startPos = raf.getFilePointer();
        int len = readLength(raf);
        long anchorPos = raf.getFilePointer();
        int trackNumber = readLength(raf);
        if (!validNumbers.contains(trackNumber)) {
            // Track number is not allowed, skip
            raf.seek(anchorPos + len);
            return true;
        }
        // This is allowed, go back
        raf.seek(startPos);
        return false;
    }

    BlockElement(BlockNode node) {
        super(NodeBase.Type.BLOCK, node.id());
        mTrackNumber = 0;
        mTimecode = 0;
        mFlag = 0;
    }

    /**
     * Get the bytes of data, could be compressed
     * @return data
     */
    public byte[] getData() {
        return mData;
    }

    /**
     * Get the track number
     * @return track number
     */
    public int getTrackNumber() {
        return mTrackNumber;
    }

    /**
     * Get the timecode of this track entry (video, audio frames or subtitle block)
     * @return timecode
     */
    public int getTimecode() {
        return mTimecode;
    }

    @Override
    public boolean read(RandomAccessFile raf) throws IOException {
        // Read documentation to understand what this does
        // http://www.matroska.org/technical/specs/index.html#block_structure
        long len = readLength(raf);
        long start = raf.getFilePointer();
        mTrackNumber = readLength(raf);
        mTimecode = raf.readShort();
        mFlag = raf.readByte();
        if (mFlag >= 5) {
            throw new EBMLParsingException("Parsing block entries with flags above 5 is not implemented yet");
        }

        // Copy the text out
        int contentLength = (int)(len - (raf.getFilePointer() - start));
        mData = new byte[contentLength];
        raf.read(mData);
        return true;
    }

    @Override
    public StringBuilder output(int level) {
        StringBuilder sb = super.output(level);
        Log.v(TAG, sb.toString() + "BLK [" + hexId() + "]: Track=" + mTrackNumber + ", TC=" + mTimecode
                + ", Content='BINARY/TEXT'");
        return null;
    }
}
