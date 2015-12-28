package com.matthewn4444.ebml;

import android.util.Log;

import com.matthewn4444.ebml.Attachments.FileAttachment;
import com.matthewn4444.ebml.elements.BlockElement;
import com.matthewn4444.ebml.elements.ElementBase;
import com.matthewn4444.ebml.elements.IntElement;
import com.matthewn4444.ebml.elements.MasterElement;
import com.matthewn4444.ebml.node.IntNode;
import com.matthewn4444.ebml.node.MasterNode;
import com.matthewn4444.ebml.node.NodeBase;
import com.matthewn4444.ebml.node.StringNode;
import com.matthewn4444.ebml.subtitles.Subtitles;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class EBMLReader {
    private static final String TAG = "EBMLReader";

    public static final int ID = 0x1A45DFA3;

    public static final int VERSION = 0x4286;
    public static final int READ_VERSION = 0x42F7;
    public static final int MAX_ID_LENGTH = 0x42F2;
    public static final int MAX_SIZE_LENGTH = 0x42F3;
    public static final int DOC_TYPE = 0x4282;
    public static final int DOC_VERSION = 0x4287;
    public static final int DOC_READ_VERSION = 0x4285;

    protected static MasterNode EBML_ROOT = new MasterNode(ID);

    private static final Object InitLock = new Object();

    protected final RandomAccessFile mRanAccFile;
    private boolean mIsOpened;

    protected MasterElement mEmblHeader;
    protected MasterElement mSegmentHeader;
    protected MasterElement mCuesHeader;
    protected MasterElement mTracksHeader;
    protected MasterElement mAttachmentsHeader;

    protected int mVideoTrackIndex;

    protected ArrayList<Subtitles> mSubtitles;
    protected final Set<Integer> mSubtitleTrackNumbers = new HashSet<>();

    protected ArrayList<FileAttachment> mAttachments;

    // Keep track of all the subs (if exists) or cluster positions for seeking
    protected ArrayDeque<Cluster.Entry> mClusterVidEntries;
    protected ArrayDeque<Cluster.Entry> mClusterSubEntries;

    protected Iterator<Cluster.Entry> mCurrentClusterEntry;

    // Read cluster faster by only parsing subtitle related elements
    protected Set<Integer> mClusterBlockReadOnlyEl;

    protected long mPositionOffset;
    protected long mCuesPosition;
    protected long mChaptersPosition;
    protected long mTracksPosition;
    protected long mAttachmentsPosition;

    // Create the ebml tree to parse the file
    private static void init() {
        if (EBML_ROOT.getLookup().isEmpty()) {
            synchronized (InitLock) {
                if (EBML_ROOT.getLookup().isEmpty()) {
                    EBML_ROOT.addNode(new IntNode(VERSION));
                    EBML_ROOT.addNode(new IntNode(READ_VERSION));
                    EBML_ROOT.addNode(new IntNode(MAX_ID_LENGTH));
                    EBML_ROOT.addNode(new IntNode(MAX_SIZE_LENGTH));
                    EBML_ROOT.addNode(new StringNode(DOC_TYPE));
                    EBML_ROOT.addNode(new IntNode(DOC_VERSION));
                    EBML_ROOT.addNode(new IntNode(DOC_READ_VERSION));

                    Segment.init();
                    Cues.init();
                    Tracks.init();
                    Cluster.init();
                    Attachments.init();
                }
            }
        }
    }

    /**
     * Open a file to read.
     * Please use close() later
     * @param path file to open
     * @throws IOException
    */
    public EBMLReader(String path) throws IOException {
        mIsOpened = true;
        mRanAccFile = new RandomAccessFile(path, "r");
    }

    /**
     * Pass an already allocated RandomAccessFile to be read.
     * Please use close() later
     * @param randomAccessFile to be read
     */
    public EBMLReader(RandomAccessFile randomAccessFile) {
        mIsOpened = true;
        mRanAccFile = randomAccessFile;
    }

    /**
     * Closes the RandomAccessFile, you will receive errors trying to read this file
     * @throws IOException
     */
    public void close() throws IOException {
        if (mIsOpened) {
            mRanAccFile.close();
            mIsOpened = false;
        }
    }

    /**
     * Check to see if this class reader is opened
     * @return if file is opened and can be read
     */
    public boolean isOpened() {
        return mIsOpened;
    }

    /**
     * Read header of the file.
     * You should use this first to see if this file is an MKV file and supports EBML.
     * This also reads the segment information for seek head which will contain all the ids and
     * positions for each segment in the file (such as tracks, attachments, chapters etc).
     * @return if this file is an MKV file
     * @throws IOException
     */
    public boolean readHeader() throws IOException {
        if (mEmblHeader != null) {
            return true;
        }

        init();
        mEmblHeader = new MasterElement(EBML_ROOT);
        mSegmentHeader = new MasterElement(Segment.HEADER);
        mCuesPosition = 0;
        mChaptersPosition = 0;

        // Parse the header content
        if (!mEmblHeader.parse(mRanAccFile)) {
            return false;
        }

        // See if segment is next
        if (Segment.ID != mRanAccFile.readInt()) {
            throw new EBMLParsingException("Segment does not follow the EBML header, is this a valid mkv?");
        }

        // Skip the segment size of file, not sure how to handle this
        long anchor = mRanAccFile.getFilePointer();
        if (mRanAccFile.readByte() == 0x01) {
            mRanAccFile.skipBytes(7);
        } else {
            mRanAccFile.seek(anchor);
        }

        // Parse the segment information
        mPositionOffset = mRanAccFile.getFilePointer();
        if (!mSegmentHeader.parse(mRanAccFile)) {
            throw new EBMLParsingException("Unable to parse segment properly");
        }
        return true;
    }

    /**
     * Read the segment tracks
     * This would give you information about the video, audio and other tracks such as subtitles
     * This allocates a list of subtitles where you can query with getSubtitles().
     * Getting readable video and audio data is not supported yet.
     * @throws IOException
     */
    public void readTracks() throws IOException {
        // TODO implement video and audio classes to wrap these tracks for easier getters
        if (mTracksHeader == null) {
            findTracksPosition();

            mRanAccFile.seek(mTracksPosition);
            mTracksHeader = new MasterElement(Tracks.HEADER);
            if (!mTracksHeader.parse(mRanAccFile)) {
                throw new EBMLParsingException("Unable to parse tracks properly");
            }

            // TODO audio tracks if needed

            // Build the subtitles from track and record valid track numbers
            mSubtitles = new ArrayList<>();
            mSubtitleTrackNumbers.clear();
            for (ElementBase el : mTracksHeader.getElements()) {
                MasterElement master = (MasterElement) el;
                int type = master.getValueInt(Tracks.TYPE);
                if (type == Tracks.Type.VIDEO) {
                    mVideoTrackIndex = master.getValueInt(Tracks.NUMBER);
                } else if (type == Tracks.Type.SUBTITLE) {
                    Subtitles subs = Subtitles.CreateSubsFromBlockGroup((MasterElement) el);
                    assert(subs != null);
                    mSubtitles.add(subs);
                    mSubtitleTrackNumbers.add(subs.getTrackNumber());
                }

            }
        }
    }

    /**
     * Read the segment attachments
     * This will read the attachment data which you can get with getAttachments().
     * @throws IOException
     */
    public void readAttachments() throws IOException {
        if (mAttachmentsHeader == null) {
            findAttachmentsPosition();

            mRanAccFile.seek(mAttachmentsPosition);
            mAttachmentsHeader = new MasterElement(Attachments.HEADER);
            if (!mAttachmentsHeader.parse(mRanAccFile)) {
                throw new EBMLParsingException("Unable to parse attachments properly");
            }

            mAttachments = new ArrayList<>();
            for (ElementBase el : mAttachmentsHeader.getElements()) {
                mAttachments.add(new FileAttachment((MasterElement) el, mRanAccFile));
            }
        }
    }

    /**
     * Read the segment cues
     * Cues contains a lot of information of clusters for each track (video, audio and subtitles).
     * This is mainly used to get the locations of clusters for subtitles
     * You need to run this before readNextSubtitle() and getting subtitle information
     * Performance-wise does pretty quickly but should not be done on the ui-thread
     * @throws IOException
     */
    public void readCues() throws IOException {
        if (mCuesHeader == null) {
            if (findCuesPosition()) {
                mRanAccFile.seek(mCuesPosition);
                mCuesHeader = new MasterElement(Cues.HEADER);
                if (!mCuesHeader.parse(mRanAccFile)) {
                    throw new EBMLParsingException("Unable to parse cues properly");
                }

                mClusterVidEntries = new ArrayDeque<>();
                mClusterSubEntries = new ArrayDeque<>();
                Cluster.Entry currentVidEntry = null;
                Cluster.Entry currentSubEntry = null;

                // Read the cues for subtitle track entries and record the cluster position
                for (ElementBase element : mCuesHeader.getElements()) {
                    MasterElement master = (MasterElement) element;
                    int cueTime = master.getValueInt(Cues.TIME, 0);
                    MasterElement trackMaster = (MasterElement) master.getElement(Cues.TRACK_POSITIONS);

                    int trackNumber = trackMaster.getValueInt(Cues.TRACK);
                    int address = trackMaster.getValueInt(Cues.CLUSTER_POSITION);
                    if (address == 0) throw new EBMLParsingException("Cannot parse the address from cues");
                    address += mPositionOffset;

                    // For more details why video and subtitle tracks are recorded, read readNextSubtitle()
                    if (mClusterSubEntries.isEmpty() && trackNumber == mVideoTrackIndex) {
                        // Record video entries: Reading cues for cluster positions does not guarantee all addresses!!
                        if (currentVidEntry != null) {
                            // Do not put into list if address is same as previous
                            if (currentVidEntry.mStartAddress == address) {
                                continue;
                            }
                            currentVidEntry.mEndAddress = address;
                            currentVidEntry.mNextTimecode = cueTime;
                        }
                        currentVidEntry = new Cluster.Entry(cueTime, address);
                        mClusterVidEntries.add(currentVidEntry);
                    } else if (mSubtitleTrackNumbers.contains(trackNumber)) {
                        int relativePos = trackMaster.getValueInt(Cues.RELATIVE_POSITION);
                        if (currentSubEntry != null) {
                            // Do not put into list if address and relative address is same as previous
                            if (currentSubEntry.mStartAddress == address
                                    && currentSubEntry.mRelativePosition == relativePos) {
                                continue;
                            }
                            currentSubEntry.mEndAddress = address;
                            currentSubEntry.mNextTimecode = cueTime;
                        }
                        currentSubEntry = new Cluster.Entry(cueTime, address, relativePos);
                        mClusterSubEntries.add(currentSubEntry);
                    }
                }
                if (!mClusterSubEntries.isEmpty()) {
                    mClusterVidEntries.clear();
                }
            } else {
                throw new EBMLParsingException("Cannot find cues in file");
            }
        }
    }

    /**
     * Change the location to parse subtitles when user seeks in video
     * Mainly helps to stream subtitles because subtitles may take too long to parse out of the file.
     * You move the subtitle parsing pointer to sometime later in the file depending on the timecode
     * of the video.
     * @param time to move the subtitle iterator, matches seek time in milliseconds
     */
    public void moveSubtitleIteratorAfterTime(long time) {
        Iterator<Cluster.Entry> it;
        if (!mClusterSubEntries.isEmpty()) {
            it = mClusterSubEntries.iterator();
        } else if (!mClusterVidEntries.isEmpty()) {
            it = mClusterVidEntries.iterator();
        } else {
            return;
        }

        int numIterations = 0;
        int lastTimecode = 0;
        while (it.hasNext()) {
            Cluster.Entry entry = it.next();
            if (lastTimecode <= time && time <= entry.mTimecode) {
                it = mClusterVidEntries.iterator();
                for (int i = 0; i < numIterations - 1; i++) {
                    it.next();
                }
                mCurrentClusterEntry = it;
                return;
            }
            numIterations++;
            lastTimecode = entry.mTimecode;
        }
        Log.w(TAG, "Time specified was outside the subtitle time, nothing happens");
    }

    /**
     * Reads the cluster for 2 different algorithms depending if cues has direct location of subtitles,
     * otherwise we scan through the entire cluster for subtitles (this is obviously slower).
     * This function will read through cluster until at least one subtitle is found. Which then you
     * can get the unread subtitle text from the Subtitles class.
     *
     * You can get unread subtitles by going using Subtitles.readUnreadSubtitles();
     *
     * Algorithm 1: Cues has the subtitle track relative position
     *      - Use the positions (cluster and relative) that cues specified from readCues()
     *      - Read the header info (id and length) of the cluster it is in and then seek by relative position
     *      - Read the Cluster block for information of the subtitle and append to the subtitle with corresponding
     *        track number
     *      - Read next subtitles if the timecode is the same
     *      - End loop when we at least read one subtitle block
     *
     * Algorithm 2: No subtitle position was given by cues point
     *      - Use the cluster position from readCues()
     *      - Parse the entire cluster and ignore unwanted elements to speed up parsing
     *      - Parse the next few clusters until the next cluster's address in mClusterVidsPositions
     *      - Loop through the elements inside cluster
     *      - First line will always be the timecode while the rest will be subtitle blocks, append them
     *        in their corresponding subtitle track number
     *      - End loop when we at least read one subtitle block
     *
     * @return if there is still anymore subtitles left
     * @throws IOException
     */
    public boolean readNextSubtitle() throws IOException {
        if (!mClusterSubEntries.isEmpty()) {
            // Algorithm 1: cues gives us the addresses for each subtitle, just read those
            if (mCurrentClusterEntry == null) {
                mCurrentClusterEntry = mClusterSubEntries.iterator();
            }
            if (!mCurrentClusterEntry.hasNext()) {
                mCurrentClusterEntry = mClusterSubEntries.iterator();
            }

            // Subs positions are available in Cues, we can pin point them and get them faster
            Cluster.Entry entry = mCurrentClusterEntry.next();
            mCurrentClusterEntry.remove();
            MasterElement clusterEl = new MasterElement(Cluster.ENTRY);
            int timecode = entry.mTimecode;

            // Scan till after the id and length to properly get the position of the subtitle track
            mRanAccFile.seek(entry.mStartAddress);
            if (clusterEl.parseOnlyIdAndLength(mRanAccFile) == 0) {
                throw new EBMLException("Unable to parse cluster header info");
            }

            // Go directly to the subtitle track data and parse the block
            long address = mRanAccFile.getFilePointer() + entry.mRelativePosition;
            mRanAccFile.seek(address);
            MasterElement blockGroup = new MasterElement(Cluster.BLOCK_GROUP_NODE);
            if (!blockGroup.parse(mRanAccFile)) {
                throw new EBMLParsingException("Cannot parse block group");
            }

            // Get the block track number and put it in the correct subtitle track
            BlockElement block = blockGroup.getBlockElement(Cluster.BLOCK_ID);
            int blockTrackNumber = block.getTrackNumber();
            for (Subtitles sub : mSubtitles) {
                if (sub.getTrackNumber() == blockTrackNumber) {
                    sub.appendBlock(block, timecode - block.getTimecode(),
                            blockGroup.getValueInt(Cluster.BLOCK_DURATION));
                    // We must read all the subtitles with same timecode, will return the number of subs found
                    return !(mCurrentClusterEntry.hasNext() && entry.mNextTimecode == timecode)
                            || readNextSubtitle();
                }
            }
            throw new EBMLParsingException("Cannot parse block group for subtitles, is file corrupted?");
        } else if (!mClusterVidEntries.isEmpty()) {
            // Algorithm 2: search through each cluster for subtitles
            if (mClusterBlockReadOnlyEl == null) {
                mClusterBlockReadOnlyEl = new HashSet<>();
                mClusterBlockReadOnlyEl.add(Cluster.BLOCK_ID);
                mClusterBlockReadOnlyEl.add(Cluster.BLOCK_GROUP);
                mClusterBlockReadOnlyEl.add(Cluster.BLOCK_DURATION);
                mClusterBlockReadOnlyEl.add(Cluster.TIMECODE);
            }
            if (mCurrentClusterEntry == null) {
                mCurrentClusterEntry = mClusterVidEntries.iterator();
            }
            if (!mCurrentClusterEntry.hasNext()) {
                mCurrentClusterEntry = mClusterVidEntries.iterator();
            }

            // Seek to the next cluster position
            boolean parsedAtLeastOneSub = false;
            Cluster.Entry entry = mCurrentClusterEntry.next();
            mCurrentClusterEntry.remove();
            mRanAccFile.seek(entry.mStartAddress);
            while (true) {
                // Keep parsing till we reach the next cluster position set from Cues
                MasterElement clusterEl = new MasterElement(Cluster.ENTRY);
                if (!clusterEl.parse(mRanAccFile, mSubtitleTrackNumbers, mClusterBlockReadOnlyEl)) {
                    // End of clusters
                    return true;
                }

                // Sort each subtitle entry inside this cluster into its subtitle track
                int timecode = 0;
                for (ElementBase el : clusterEl.getElements()) {
                    if (el.getType() == NodeBase.Type.MASTER) {
                        BlockElement block = ((MasterElement) el).getBlockElement(Cluster.BLOCK_ID);
                        if (block != null) {
                            int blockTrackNumber = block.getTrackNumber();
                            for (Subtitles sub : mSubtitles) {
                                if (sub.getTrackNumber() == blockTrackNumber) {
                                    sub.appendBlock(block, timecode,
                                            ((MasterElement) el).getValueInt(Cluster.BLOCK_DURATION));
                                    parsedAtLeastOneSub = true;
                                }
                            }
                        }
                    } else if (el.getType() == NodeBase.Type.INT) {
                          timecode = ((IntElement) el).getData();
                    } else {
                        throw new EBMLParsingException("Parsing cluster for subtitles gained useless data");
                    }
                }

                // Once we reach the next cluster position set from Cues, we can end the loop
                // The last entry will have an end address of 0, so scan till end
                if (entry.mEndAddress > 0 && mRanAccFile.getFilePointer() >= entry.mEndAddress) {
                    break;
                }
            }
            // Read the next sub if didnt find one now
            return parsedAtLeastOneSub || readNextSubtitle();
        }
        return false;
    }

    /**
     * Get the subtitles after parsing the video file.
     * You must call the functions in the order:
     *      readHeader()
     *      readTracks()
     *          - at this point you can get how many subtitle tracks are in this file, but no info in it
     *      readCues()
     *      readNextSubtitle() till it returns false
     *      readUnreadSubtitles()
     *          - now all the subtitles have been parsed and you can get the information
     * @return a readable list of subtitles class
     */
    public ArrayList<Subtitles> getSubtitles() {
        return mSubtitles;
    }

    /**
     * Get the attachments after parsing the tracks
     * You must call the functions in the order:
     *      readHeader()
     *      readTracks()
     *      readAttachments()
     * @return a list of attachments
     */
    public ArrayList<FileAttachment> getAttachments() {
        return mAttachments;
    }

//      TODO only need this if you find a video without clusters
//    private void findClusterPosition() throws IOException {
//        if (mClusterPosition != 0) {
//            return;
//        }
//        mClusterPosition = findPositionForSegmentEntry(Cluster.ID);
//        if (mClusterPosition == 0) {
//            // There was no cluster located in segment, so we have to find it another way
//            if (findCuesPosition()) {
//                // Cues will easily tell us the location of the first cluster
//                mRanAccFile.seek(mCuesPosition);
//                mCuesHeader = new MasterElement(Cues.HEADER);
//                ElementBase el = mCuesHeader.parseElementIdOnce(mRanAccFile, Cues.CLUSTER_POSITION);
//                if (el == null) {
//                    throw new EBMLException("Failed to find first cluster location in cues.");
//                }
//                mCuesHeader.output();
//                mClusterPosition = ((IntElement)el).getData() + mPositionOffset;
//            } else {
                // No Cues so we have to fallback and scan the file for it
                // The algorithm consists of scanning areas between major segments
                // until end of file or the first cluster

                // Get all positions in segment entry after max chapters or tracks
//                findChaptersPosition();
//                ArrayList<Long> positions = new ArrayList<Long>();
//                long startScanPosition = Math.max(mChaptersPosition, mTracksPosition);
//                for (ElementBase el : mSegmentHeader.getElements()) {
//                    long pos = ((MasterElement) el).getValueInt(Segment.SEEK_POSITION) + mPositionOffset;
//                    if (pos >= startScanPosition) {
//                        positions.add(pos);
//                    }
//                }
//                Collections.sort(positions);
//                positions.add(mRanAccFile.length());
//
//                // Start scanning for the cluster
//                for (int i = 0; i < positions.size() - 1; i++) {
//                    mRanAccFile.seek(positions.get(i));
//
//                    // Skip 4 bytes for the master id; we guarantee id based on position
//                    // then skip the entire body to find the data in between
//                    ElementBase.readId(mRanAccFile);
//                    int len = ElementBase.readLength(mRanAccFile);
//                    mRanAccFile.skipBytes(len);
//
//                    // Scan the in between segments data for the cluster
//                    int clusterId[] = { 0x1F, 0x43, 0xB6, 0x75 };
//                    while(mRanAccFile.getFilePointer() < positions.get(i + 1)) {
//                        if (mRanAccFile.readByte() == clusterId[0]) {
//                            if (mRanAccFile.readByte() == clusterId[1]) {
//                                if (mRanAccFile.readByte() == clusterId[2]) {
//                                    if (mRanAccFile.readByte() == clusterId[3]) {
//                                        mClusterPosition = mRanAccFile.getFilePointer() - 4;
//                                        return;
//                                    } else {
//                                        mRanAccFile.seek(mRanAccFile.getFilePointer() - 3);
//                                    }
//                                } else {
//                                    mRanAccFile.seek(mRanAccFile.getFilePointer() - 2);
//                                }
//                            } else {
//                                mRanAccFile.seek(mRanAccFile.getFilePointer() - 1);
//                            }
//                        }
//                    }
//                }
//                throw new EBMLParsingException("Cannot find cluster id");
//            }
//        }
//    }

    private void findTracksPosition() throws IOException {
        if (mTracksPosition == 0) {
            mTracksPosition = findPositionForSegmentEntry(Tracks.ID);
        }
    }

    private boolean findCuesPosition() throws IOException {
        if (mCuesPosition == 0) {
            mCuesPosition = findPositionForSegmentEntry(Cues.ID);
        }
        return true;
    }

    private void findChaptersPosition() throws IOException {
        if (mChaptersPosition == 0) {
            mChaptersPosition = findPositionForSegmentEntry(Chapters.ID);
        }
    }

    private void findAttachmentsPosition() throws IOException {
        if (mAttachmentsPosition == 0) {
            mAttachmentsPosition = findPositionForSegmentEntry(Attachments.ID);
        }
    }

    private int findPositionForSegmentEntry(int id) throws IOException {
        MasterElement entry = mSegmentHeader.searchForMasterWithIntValue(
                Segment.SEEK_ID, id);
        if (entry == null) {
            return 0;
        }
        return (int) (entry.getValueInt(Segment.SEEK_POSITION) + mPositionOffset);

    }
}
