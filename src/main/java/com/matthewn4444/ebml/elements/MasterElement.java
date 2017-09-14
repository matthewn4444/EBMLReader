package com.matthewn4444.ebml.elements;

import android.util.Log;

import com.matthewn4444.ebml.EBMLParsingException;
import com.matthewn4444.ebml.node.BlockNode;
import com.matthewn4444.ebml.node.ByteNode;
import com.matthewn4444.ebml.node.FloatNode;
import com.matthewn4444.ebml.node.IntNode;
import com.matthewn4444.ebml.node.LongNode;
import com.matthewn4444.ebml.node.MasterNode;
import com.matthewn4444.ebml.node.NodeBase;
import com.matthewn4444.ebml.node.StringNode;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Set;

public class MasterElement extends ElementBase {

    public static int VOID = 0xEC;

    // Searching data
    protected int mSearchOnceId;
    protected ElementBase mSearchOnceFoundElement;

    private final ArrayList<ElementBase> mElements;
    private final MasterNode mSchema;

    protected static ElementBase getElementFromPath(MasterElement master, int index, int... ids) {
        if (index >= ids.length) {
            return master;
        }
        ElementBase el = master.getElement(ids[index]);
        if (el != null) {
            return getElementFromPath((MasterElement) el, index + 1, ids);
        }
        return null;
    }

    /**
     * Reads only the id and length of the next element.
     * Make sure you seek to the correct location which the next element must have
     * what you passed. Does not read any data. You can use this to read the next
     * element inside.
     * This is the static version of parseOnlyIdAndLength
     *
     * @return the length of this master element
     */
    public static int parseUpToLength(RandomAccessFile raf, int id) throws IOException {
        int elementId = readId(raf);
        if (elementId == id) {
            return readLength(raf);
        }
        return 0;
    }

    public MasterElement(MasterNode node, long position) {
        super(NodeBase.Type.MASTER, node.id(), position);
        mSchema = node;
        mElements = new ArrayList<ElementBase>();
    }

    public ElementBase parseElementIdOnce(RandomAccessFile raf, int searchId)
            throws IOException {
        mSearchOnceId = searchId;
        int elementId = readId(raf);
        if (elementId == mId) {
            int len = readLength(raf);
            if (readSection(raf, len, null, null)) {
                return mSearchOnceFoundElement;
            }
        }
        return null;
    }

    /**
     * Reads only the id and length of the next element. Be cautious to use this
     * because it will not scan any contents of this master element You mainly
     * use this to parse each child element one by one
     *
     * @return the length of this master element
     */
    public int parseOnlyIdAndLength(RandomAccessFile raf) throws IOException {
        mSearchOnceId = 0;
        return parseUpToLength(raf, mId);
    }

    public boolean parse(RandomAccessFile raf) throws IOException {
        return parse(raf, null, null);
    }

    public boolean parse(RandomAccessFile raf, Set<Integer> filterTrackNumbers,
            Set<Integer> filterIds) throws IOException {
        long pos = raf.getFilePointer();
        int elementId = readId(raf);
        if (elementId == mId) {
            mInnerLength = readLength(raf);
            mLength = raf.getFilePointer() - pos + mInnerLength;
            return readSection(raf, mInnerLength, filterTrackNumbers, filterIds);
        }
        return false;
    }

    @Override
    boolean read(RandomAccessFile raf) throws IOException {
        super.read(raf);
        return readSection(raf, mInnerLength, null, null);
    }

    boolean read(RandomAccessFile raf, Set<Integer> filterTrackNumbers,
            Set<Integer> filterIds) throws IOException {
        super.read(raf);
        return readSection(raf, mInnerLength, filterTrackNumbers, filterIds);
    }

    public MasterElement searchForMasterWithIntValue(int elementId,
            int findValue) throws IOException {
        // Find the positions of tracks
        for (int i = 0; i < mElements.size(); i++) {
            switch (mElements.get(i).mType) {
            case INT:
                if (mElements.get(i).id() == elementId
                        && ((IntElement) mElements.get(i)).getData() == findValue) {
                    return this;
                }
                break;
            case MASTER: {
                MasterElement res = ((MasterElement) mElements.get(i))
                        .searchForMasterWithIntValue(elementId, findValue);
                if (res != null) {
                    return res;
                }
            }
                break;
            case UNSET:
                throw new EBMLParsingException("Nested element is unset");
            case STRING:
            case BLOCK:
            case BYTES:
            default:
                break;
            }
        }
        return null;
    }

    public int searchForIntValue(int id, int defaultValue) {
        for (int i = 0; i < mElements.size(); i++) {
            switch (mElements.get(i).mType) {
            case INT:
                if (mElements.get(i).id() == id) {
                    return ((IntElement) mElements.get(i)).getData();
                }
                break;
            case MASTER:
                int r = ((MasterElement) mElements.get(i)).searchForIntValue(
                        id, defaultValue);
                if (r != defaultValue) {
                    return r;
                }
            case UNSET:
            case STRING:
            case BLOCK:
            case BYTES:
            case LONG:
            case FLOAT:
            default:
                break;
            }
        }
        return defaultValue;
    }

    public int getValueInt(int id) {
        return getValueInt(id, 0);
    }

    public int getValueInt(int id, int defaultValue) {
        ElementBase el = getElement(id);
        if (el != null && el.mType == NodeBase.Type.INT) {
            return ((IntElement) el).getData();
        }
        return defaultValue;
    }

    public long searchForLongValue(int id, long defaultValue) {
        for (int i = 0; i < mElements.size(); i++) {
            switch (mElements.get(i).mType) {
            case LONG:
                if (mElements.get(i).id() == id) {
                    return ((LongElement) mElements.get(i)).getData();
                }
                break;
            case MASTER:
                long r = ((MasterElement) mElements.get(i)).searchForLongValue(
                        id, defaultValue);
                if (r != defaultValue) {
                    return r;
                }
            case UNSET:
            case STRING:
            case BLOCK:
            case BYTES:
            case INT:
            case FLOAT:
            default:
                break;
            }
        }
        return defaultValue;
    }

    public long getValueLong(int id) {
        return getValueLong(id, 0);
    }

    public long getValueLong(int id, long defaultValue) {
        ElementBase el = getElement(id);
        if (el != null && el.mType == NodeBase.Type.LONG) {
            return ((LongElement) el).getData();
        }
        return defaultValue;
    }

    public float searchForFloatValue(int id, float defaultValue) {
        for (int i = 0; i < mElements.size(); i++) {
            switch (mElements.get(i).mType) {
            case FLOAT:
                if (mElements.get(i).id() == id) {
                    return ((FloatElement) mElements.get(i)).getData();
                }
                break;
            case MASTER:
                float r = ((MasterElement) mElements.get(i)).searchForFloatValue(
                        id, defaultValue);
                if (r != defaultValue) {
                    return r;
                }
            case UNSET:
            case STRING:
            case BLOCK:
            case BYTES:
            case INT:
            case LONG:
            default:
                break;
            }
        }
        return defaultValue;
    }

    public float getFloatLong(int id) {
        return getFloatLong(id, 0);
    }

    public float getFloatLong(int id, float defaultValue) {
        ElementBase el = getElement(id);
        if (el != null && el.mType == NodeBase.Type.FLOAT) {
            return ((FloatElement) el).getData();
        }
        return defaultValue;
    }

    public String getValueString(int id) throws UnsupportedEncodingException {
        ElementBase el = getElement(id);
        if (el != null && el.mType == NodeBase.Type.STRING) {
            return ((StringElement) el).getString();
        }
        return null;
    }

    public ByteElement getByteElement(int id) {
        ElementBase el = getElement(id);
        if (el != null && el.mType == NodeBase.Type.BYTES) {
            return (ByteElement) el;
        }
        return null;
    }

    public BlockElement getBlockElement(int id) {
        ElementBase el = getElement(id);
        if (el != null && el.mType == NodeBase.Type.BLOCK) {
            return (BlockElement) el;
        }
        return null;
    }

    public ElementBase getElement(int id) {
        for (int i = 0; i < mElements.size(); i++) {
            if (mElements.get(i).mId == id) {
                return mElements.get(i);
            }
        }
        return null;
    }

    /**
     * Pass a path of ids as Integers and it will search down and retrieve the last id element you
     * are looking for.
     * @param ids ordered list of ids to recursively look through
     * @return the element you are looking for or null if not found
     */
    public ElementBase getElementFromPath(int... ids) {
        return getElementFromPath(this, 0, ids);
    }

    public ArrayList<ElementBase> getElements() {
        return mElements;
    }

    protected boolean readSection(RandomAccessFile raf, long len,
            Set<Integer> filterTrackNumbers, Set<Integer> filterIds)
            throws IOException {
        mSearchOnceFoundElement = null;
        long upToLimit = raf.getFilePointer() + len;
        while (raf.getFilePointer() < upToLimit) {
            long position = raf.getFilePointer();
            int id = readId(raf);
            // Lookup the id and parse its block
            if (mSchema.getLookup().containsKey(id)) {

                // Only allow ids if filter is active and is in it
                if (filterIds == null || filterIds.contains(id)) {
                    NodeBase nextNode = mSchema.getLookup().get(id);
                    ElementBase element;
                    switch (nextNode.getType()) {
                    case INT:
                        element = new IntElement((IntNode) nextNode, position);
                        break;
                    case LONG:
                        element = new LongElement((LongNode) nextNode, position);
                        break;
                    case STRING:
                        element = new StringElement((StringNode) nextNode, position);
                        break;
                    case BLOCK:
                        if (filterTrackNumbers != null) {
                            // Check to see if the block's track number is
                            // allowed
                            if (BlockElement.skipBlockIfNotTrackNumber(raf,
                                    filterTrackNumbers)) {
                                continue;
                            }
                        }
                        element = new BlockElement((BlockNode) nextNode, position);
                        break;
                    case MASTER:
                        // Parse master differently
                        MasterElement el = new MasterElement(
                                (MasterNode) nextNode, position);
                        el.mSearchOnceId = mSearchOnceId;

                        if (!el.read(raf, filterTrackNumbers, filterIds)) {
                            return false;
                        }
                        mElements.add(el);
                        if (el.mSearchOnceFoundElement != null) {
                            mSearchOnceFoundElement = el.mSearchOnceFoundElement;
                            return true;
                        }
                        continue;
                    case BYTES:
                        element = new ByteElement((ByteNode) nextNode, position);
                        break;
                    case FLOAT:
                        element = new FloatElement((FloatNode) nextNode, position);
                        break;
                    default:
                        throw new EBMLParsingException(
                                "Cannot parse unset node");
                    }
                    if (!element.read(raf)) {
                        return false;
                    }
                    mElements.add(element);
                    if (mSearchOnceId == id) {
                        mSearchOnceFoundElement = element;
                        return true;
                    }

                } else {
                    // Skip this data because it did not pass through the filter
                    raf.skipBytes(readLength(raf));
                }
            } else if (id == VOID) {
                // This section is void, so ignore it
                raf.skipBytes(readLength(raf));
            } else {
                throw new EBMLParsingException(
                        "This master does not have id 0x"
                                + Integer.toHexString(id) + " in its schema");
            }
        }
        return true;
    }

    public void output() {
        output(0);
    }

    @Override
    public StringBuilder output(int level) {
        StringBuilder sb = super.output(level);
        Log.v(TAG, sb.toString() + "MASTER [" + hexId() + "]");
        for (int i = 0; i < mElements.size(); i++) {
            mElements.get(i).output(level + 1);
        }
        return null;
    }
}
