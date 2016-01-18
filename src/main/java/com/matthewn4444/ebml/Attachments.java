package com.matthewn4444.ebml;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

import com.matthewn4444.ebml.elements.ByteElement;
import com.matthewn4444.ebml.elements.MasterElement;
import com.matthewn4444.ebml.node.ByteNode;
import com.matthewn4444.ebml.node.LongNode;
import com.matthewn4444.ebml.node.MasterNode;
import com.matthewn4444.ebml.node.StringNode;

public class Attachments {
    public static final int ID = 0x1941A469;

    public static final int ATTACHED_FILE = 0x61A7;
    public static final int FILE_DESCRIPTION = 0x467E;
    public static final int FILE_NAME = 0x466E;
    public static final int FILE_MIME_TYPE = 0x4660;
    public static final int FILE_DATA = 0x465C;
    public static final int FILE_UID = 0x46AE;

    static final MasterNode HEADER = new MasterNode(ID);
    static final MasterNode ATTACHED_FILE_NODE = new MasterNode(ATTACHED_FILE);

    static void init() {
        HEADER.addNode(ATTACHED_FILE_NODE);

        ATTACHED_FILE_NODE.addNode(new StringNode(FILE_DESCRIPTION));
        ATTACHED_FILE_NODE.addNode(new StringNode(FILE_NAME));
        ATTACHED_FILE_NODE.addNode(new StringNode(FILE_MIME_TYPE));
        ATTACHED_FILE_NODE.addNode(new ByteNode(FILE_DATA));
        ATTACHED_FILE_NODE.addNode(new LongNode(FILE_UID));
    }

    private Attachments() {}

    public static class FileAttachment {
        private final String mDescription;
        private final String mName;
        private final String mMimeType;
        private final RandomAccessFile mRaf;
        private final long mDataLength;
        private final long mDataPosition;

        FileAttachment(MasterElement element, RandomAccessFile raf) throws UnsupportedEncodingException {
            mDescription = element.getValueString(FILE_DESCRIPTION);
            mName = element.getValueString(FILE_NAME);
            mMimeType = element.getValueString(FILE_MIME_TYPE);
            ByteElement byteEl = element.getByteElement(FILE_DATA);
            mDataPosition = byteEl.getPosition();
            mDataLength = byteEl.getLength();
            mRaf = raf;
        }

        /**
         * Get the description of this attachment
         * @return description
         */
        public String getDescription() {
            return mDescription;
        }

        /**
         * Get the name of this attachment
         * @return name
         */
        public String getName() {
            return mName;
        }

        /**
         * Get the mime type of this attachment
         * @return mime type
         */
        public String getMimeType() {
            return mMimeType;
        }

        /**
         * Get the data of this attachment
         * @return attachment bytes
         * @throws IOException
         */
        public byte[] getData() throws IOException {
            long oldPointer = mRaf.getFilePointer();
            mRaf.seek(mDataPosition);
            byte[] buffer = new byte[(int) mDataLength];
            mRaf.read(buffer);
            mRaf.seek(oldPointer);
            return buffer;
        }
    }
}
