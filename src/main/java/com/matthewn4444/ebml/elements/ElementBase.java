package com.matthewn4444.ebml.elements;

import java.io.IOException;
import java.io.RandomAccessFile;

import com.matthewn4444.ebml.EBMLParsingException;
import com.matthewn4444.ebml.node.NodeBase;

public abstract class ElementBase {
    protected static final String TAG = "EBMLParsing";

    protected final NodeBase.Type mType;
    protected final int mId;
    protected final long mPosition;

    protected long mLength;
    protected long mInnerLength;

    /**
     * Reads the id of EBML format of 1-4 bytes
     * Read readBytes below to understand how we get this id
     * @param raf file stream
     * @return the number id
     * @throws IOException
     */
    public static int readId(RandomAccessFile raf) throws IOException {
        return readBytes(raf, false);
    }

    /**
     * Reads the length of the next section
     * * Read readBytes below to understand how we get this length
     * @param raf file stream
     * @return the length of following data
     * @throws IOException
     */
    static int readLength(RandomAccessFile raf) throws IOException {
        return readBytes(raf, true);
    }

    /**
     * Reads data following from the current position of the file stream following EBML documentation.
     * http://www.matroska.org/technical/specs/index.html#EBML_ex
     *
     * Basic explanation of this function:
     *      The first byte represents how many bytes to read after this byte:
     *          if you '&' with 0x80, read 1 byte
     *          if you '&' with 0x40, read 2 bytes
     *          if you '&' with 0x20, read 3 bytes
     *          if you '&' with 0x10, read 4 bytes
     *      Following this read the next however many bytes from above and or them as digits (so
     *      shift them).
     *      Reading as length requires you to take the next bit and subtract the first byte
     * @param raf file stream
     * @param readAsLength whether to read this as length, if false it reads as id
     * @return a value, id or length
     * @throws IOException
     */
    protected synchronized static int readBytes(RandomAccessFile raf, boolean readAsLength) throws IOException {
        long pos = raf.getFilePointer();
        int b1 = raf.readByte() & 0xFF;

        if ((b1 & 0x80) != 0) {
            return b1 - (readAsLength ? 0x80 : 0);
        }
        else if ((b1 & 0x40) != 0) {
            return ((b1 - (readAsLength ? 0x40 : 0)) << 8) | (raf.readByte() & 0xFF);
        }
        else if ((b1 & 0x20) != 0) {
            return ((b1 - (readAsLength ? 0x20 : 0)) << 16) | ((raf.readByte() & 0xFF) << 8) | (raf.readByte() & 0xFF);
        }
        else if ((b1 & 0x10) != 0) {
            return (b1 - (readAsLength ? 0x10 : 0)) << 24 | ((raf.readByte() & 0xFF) << 16) | ((raf.readByte() & 0xFF) << 8) | (raf.readByte() & 0xFF);
        }
        else if (b1 == 0x01) {
            // TODO if this errors because long -> int shortens the number, fix to return long
            raf.seek(pos);
            long length = raf.readLong();
            return (int) (readAsLength ? length & 0x00ffffffffffffffL : length);
        } else {
            throw new EBMLParsingException("Unable to get length from stream. [Byte: 0x"
                    + Integer.toHexString(b1) + " @ 0x" + Long.toHexString(pos)
                    + "]");
        }
    }

    ElementBase(NodeBase.Type type, int id, long position) {
        mType = type;
        mId = id;
        mPosition = position;
    }

    /**
     * Reads the file stream and handles the data depending on the type of inherited class
     * @param raf file stream
     * @return if successful
     * @throws IOException
     */
    boolean read(RandomAccessFile raf) throws IOException {
        mInnerLength = readLength(raf);
        mLength = raf.getFilePointer() - mPosition + mInnerLength;
        return true;
    }

    public StringBuilder output(int level) {
        StringBuilder sb = new StringBuilder();
        if (level > 0) {
            for (int i = 0; i < level; i++) {
                sb.append("  ");
            }
            sb.append("|- ");
        }
        return sb;
    }

    /**
     * Get the type of this element
     * @return type
     */
    public NodeBase.Type getType() {
        return mType;
    }

    /**
     * Get the id of this element
     * @return id
     */
    public int id() {
        return mId;
    }

    /**
     * Get the file position of the element
     * @return position in file
     */
    public long getFilePosition() {
        return mPosition;
    }

    /**
     * Get the file size of the element
     * @param raf file stream
     * @return size in file
     */
    public long getFileLength() {
        return mLength;
    }

    /**
     * Convenience function to get the id as a hex string
     * @return hex string of id
     */
    public String hexId() {
        return "0x" + Integer.toHexString(mId);
    }
}
