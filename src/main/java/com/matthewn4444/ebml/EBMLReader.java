package com.matthewn4444.ebml;

import android.util.Log;

import com.matthewn4444.ebml.Attachments.FileAttachment;
import com.matthewn4444.ebml.elements.BlockElement;
import com.matthewn4444.ebml.elements.ElementBase;
import com.matthewn4444.ebml.elements.IntElement;
import com.matthewn4444.ebml.elements.LongElement;
import com.matthewn4444.ebml.elements.MasterElement;
import com.matthewn4444.ebml.node.IntNode;
import com.matthewn4444.ebml.node.MasterNode;
import com.matthewn4444.ebml.node.NodeBase;
import com.matthewn4444.ebml.node.StringNode;
import com.matthewn4444.ebml.subtitles.Subtitles;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    public static final int NS_TO_MS = 1000000;

    protected static MasterNode EBML_ROOT = new MasterNode(ID);

    private static final Object InitLock = new Object();

    protected final RandomAccessFile mRanAccFile;
    protected final List<MasterElement> mSegmentHeaders = new ArrayList<>();
    private boolean mIsOpened;

    protected MasterElement mEmblHeader;
    protected MasterElement mInfoHeader;
    protected MasterElement mCuesHeader;
    protected MasterElement mTracksHeader;
    protected MasterElement mAttachmentsHeader;

    protected int mVideoTrackIndex;

    protected ArrayList<Subtitles> mSubtitles;
    protected final Set<Integer> mSubtitleTrackNumbers = new HashSet<>();

    protected ArrayList<FileAttachment> mAttachments;

    protected ArrayList<AudioTrack> mAudioTracks;

    // Keep track of all the video cues for getting subtitles
    protected ArrayList<Cluster.Entry> mCueFrames;
    protected boolean mHasCueSubtitlesPos;

    // Read cluster faster by only parsing subtitle related elements
    protected Set<Integer> mClusterBlockReadOnlyEl;

    protected float mDurationMs;

    protected long mPositionOffset;
    protected long mCuesPosition;
    protected long mChaptersPosition;
    protected long mTracksPosition;
    protected long mInfoPosition;
    protected long mAttachmentsPosition;

    protected long mTracksLength;
    protected long mAttachmentsLength;
    protected long mCuesLength;
    protected long mChaptersLength;

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

                    Info.init();
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
        mHasCueSubtitlesPos = false;
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
     * Gets the position of cues in this file
     * @return cues offset
     */
    public long getCuesPosition() {
        if (!mSegmentHeaders.isEmpty()) {
            try {
                findCuesPosition();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mCuesPosition;
    }

    /**
     * Gets the position of chapters in this file
     * @return chapters offset
     */
    public long getChaptersPosition() {
        if (!mSegmentHeaders.isEmpty()) {
            try {
                findChaptersPosition();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mChaptersPosition;
    }

    /**
     * Gets the position of tracks in this file
     * @return tracks offset
     */
    public long getTracksPosition() {
        if (!mSegmentHeaders.isEmpty()) {
            try {
                findTracksPosition();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mTracksPosition;
    }

    /**
     * Gets the position of attachments in this file
     * @return attachments offset
     */
    public long getAttachmentsPosition() {
        if (!mSegmentHeaders.isEmpty()) {
            try {
                findAttachmentsPosition();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mAttachmentsPosition;
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
        mSegmentHeaders.clear();
        mEmblHeader = new MasterElement(EBML_ROOT, 0);
        mCuesPosition = 0;
        mChaptersPosition = 0;

        // Parse the header content
        synchronized (mRanAccFile) {
            mRanAccFile.seek(0);
            if (!mEmblHeader.parse(mRanAccFile)) {
                return false;
            }

            // See if segment is next
            if (Segment.ID != mRanAccFile.readInt()) {
                throw new EBMLParsingException("Segment does not follow the EBML header, is this a valid mkv?");
            }

            // Detect when we get to the seek-head
            scanForId(Segment.SEEK_HEAD, 10);

            // Parse the segment information
            mSegmentHeaders.add(new MasterElement(Segment.HEADER, mRanAccFile.getFilePointer()));
            if (!mSegmentHeaders.get(0).parse(mRanAccFile)) {
                throw new EBMLParsingException("Unable to parse segment seek header properly");
            }
        }

        // Handle cases for segment info at the bottom of the file
        long seekHeaderPosition = findPositionFromSegmentEntry(Segment.SEEK_HEAD);
        if (seekHeaderPosition != 0) {
            synchronized (mRanAccFile) {
                mRanAccFile.seek(seekHeaderPosition);
                mSegmentHeaders.add(new MasterElement(Segment.HEADER, seekHeaderPosition));
                if (!mSegmentHeaders.get(1).parse(mRanAccFile)) {
                    throw new EBMLParsingException("Unable to parse segment seek header properly again");
                }
            }
        }

        // Parse the info segment
        if (mInfoHeader == null) {
            findInfoPosition();

            synchronized (mRanAccFile) {
                mRanAccFile.seek(mInfoPosition);
                mInfoHeader = new MasterElement(Info.HEADER, mRanAccFile.getFilePointer());
                if (!mInfoHeader.parse(mRanAccFile)) {
                    throw new EBMLParsingException("Unable to parse info properly");
                }
            }

            // Get the duration of the video
            int timescale = mInfoHeader.searchForIntValue(Info.TIMECODE_SCALE, NS_TO_MS);
            mDurationMs = mInfoHeader.searchForFloatValue(Info.DURATION, 0) / timescale * NS_TO_MS;
        }
        return true;
    }

    /**
     * Gets the tracks length region
     * @return the length of the region
     * @throws IOException
     */
    public long getTracksDataLength() throws IOException {
        findTracksPosition();
        synchronized (mRanAccFile) {
            mRanAccFile.seek(mTracksPosition);
            if ((mTracksLength = MasterElement.parseUpToLength(mRanAccFile, Tracks.ID)) == 0) {
                throw new EBMLParsingException("Getting tracks read length in the wrong location");
            }
        }
        return mTracksLength;
    }

    /**
     * Gets the attachments length region
     * @return the length of the region
     * @throws IOException
     */
    public long getAttachmentsDataLength() throws IOException {
        findAttachmentsPosition();
        if (mAttachmentsPosition == 0) {
            return 0;
        }
        synchronized (mRanAccFile) {
            mRanAccFile.seek(mAttachmentsPosition);
            if ((mAttachmentsLength = MasterElement.parseUpToLength(mRanAccFile, Attachments.ID)) == 0) {
                throw new EBMLParsingException("Getting attachments read length in the wrong location");
            }
        }
        return mAttachmentsLength;
    }

    /**
     * Gets the cues length region
     * @return the length of the region
     * @throws IOException
     */
    public long getCuesDataLength() throws IOException {
        findCuesPosition();
        synchronized (mRanAccFile) {
            mRanAccFile.seek(mCuesPosition);
            if ((mCuesLength = MasterElement.parseUpToLength(mRanAccFile, Cues.ID)) == 0) {
                throw new EBMLParsingException("Getting cues read length in the wrong location");
            }
        }
        return mCuesLength;
    }

    /**
     * Gets the chapters length region
     * @return the length of the region
     * @throws IOException
     */
    public long getChaptersDataLength() throws IOException {
        findChaptersPosition();
        synchronized (mRanAccFile) {
            mRanAccFile.seek(mChaptersPosition);
            if ((mChaptersLength = MasterElement.parseUpToLength(mRanAccFile, Chapters.ID)) == 0) {
                throw new EBMLParsingException("Getting chapters read length in the wrong location");
            }
        }
        return mChaptersLength;
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

            synchronized (mRanAccFile) {
                mRanAccFile.seek(mTracksPosition);
                mTracksHeader = new MasterElement(Tracks.HEADER, mRanAccFile.getFilePointer());
                if (!mTracksHeader.parse(mRanAccFile)) {
                    throw new EBMLParsingException("Unable to parse tracks properly");
                }

                // Build the subtitles and audio tracks and record valid track numbers
                mSubtitles = new ArrayList<>();
                mAudioTracks = new ArrayList<>();
                mSubtitleTrackNumbers.clear();
                for (ElementBase el : mTracksHeader.getElements()) {
                    MasterElement master = (MasterElement) el;
                    int type = master.getValueInt(Tracks.TYPE);
                    if (type == Tracks.Type.VIDEO) {
                        mVideoTrackIndex = master.getValueInt(Tracks.NUMBER);
                    } else if (type == Tracks.Type.SUBTITLE) {
                        MasterElement masterSubTrack = (MasterElement) el;

                        // Check to see if this subtitle element has compression
                        boolean hasCompression = false;
                        el = masterSubTrack.getElementFromPath(Tracks.CONTENT_ENCODINGS_ENTRY,
                                Tracks.CONTENT_ENCODING, Tracks.CONTENT_COMPRESSION);
                        if (el != null) {
                            // We have compression in subtitles
                            if (((MasterElement) el).getElements().size() > 0) {
                                throw new UnsupportedOperationException("Have not implemented more complex compression for subtitles!");
                            }
                            hasCompression = true;
                        }

                        Subtitles subs = Subtitles.CreateSubsFromBlockGroup(masterSubTrack, hasCompression);
                        assert (subs != null);
                        mSubtitles.add(subs);
                        mSubtitleTrackNumbers.add(subs.getTrackNumber());
                    } else if (type == Tracks.Type.AUDIO) {
                        AudioTrack audioTrack = AudioTrack.fromMasterTrackElement((MasterElement) el);
                        assert (audioTrack != null);
                        mAudioTracks.add(audioTrack);
                    }
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
            if (mAttachmentsPosition == 0) {
                Log.v(TAG, "There are no attachments");
                return;
            }

            synchronized (mRanAccFile) {
                mRanAccFile.seek(mAttachmentsPosition);
                mAttachmentsHeader = new MasterElement(Attachments.HEADER, mRanAccFile.getFilePointer());
                if (!mAttachmentsHeader.parse(mRanAccFile)) {
                    throw new EBMLParsingException("Unable to parse attachments properly");
                }
            }

            mAttachments = new ArrayList<>();
            for (ElementBase el : mAttachmentsHeader.getElements()) {
                mAttachments.add(new FileAttachment((MasterElement) el, mRanAccFile));
            }
        }
    }

    /**
     * Gets the amount of video cues inside the video file
     * Use this to get the total amount of cues to parse the subtitles
     * @return the amount of video cues
     */
    public int getCuesCount() {
        return mCueFrames.size();
    }

    /**
     * Get the start address of a cue
     * @param index position of the cue
     * @return start address
     */
    public long getCueStartAddress(int index) {
        return mCueFrames.get(index).mStartAddress;
    }

    /**
     * Get the end address of a cue
     * @param index position of the cue
     * @return end address
     */
    public long getCueEndAddress(int index) {
        return mCueFrames.get(index).mEndAddress;
    }

    /**
     * Get the start time of a cue
     * @param index position of the cue
     * @return start time
     */
    public int getCueTimecode(int index) {
        return mCueFrames.get(index).mTimecode;
    }

    /**
     * Get the next time code of a cue
     * @param index position of the cue
     * @return next time code
     */
    public int getCueEndTimecode(int index) {
        return mCueFrames.get(index).mNextTimecode;
    }

    /**
     * Gets the subtitle inside the cue entry.
     * If the video has subtitle information inside the cues, it will read it much faster,
     * older videos will not so we will have to scan through majority of the file.
     * This operation is slow and should be done on a background thread.
     * Once the subtitle is parsed out, it will be placed into a subtitle object then you can
     * use getSubtitles().get(i).readUnreadSubtitles() to get the read subtitle
     * On the dev side, you should keep a Set<Integer> to keep track of which cue entries
     * you have read so you dont need to run this function on the same entry again.
     * @param index of the cue frame
     * @return if there are any subtitles parsed
     * @throws IOException
     */
    public boolean readSubtitlesInCueFrame(int index) throws IOException {
        Cluster.Entry entry = mCueFrames.get(index);
        if (!entry.mHasParsed) {
            if (mHasCueSubtitlesPos) {
                // There should be subtitle entries inside some video entries, read part of the cluster
                if (entry.mSubEntries != null) {
                    // This entry has subtitles!
                    for (Cluster.Entry subEntry : entry.mSubEntries) {
                        long pos = -1;
                        int timecode = subEntry.mTimecode;
                        synchronized (mRanAccFile) {
                            MasterElement clusterEl = new MasterElement(Cluster.ENTRY, subEntry.mStartAddress);

                            // Scan till after the id and length to properly get the position of the subtitle track
                            mRanAccFile.seek(subEntry.mStartAddress);
                            if (clusterEl.parseOnlyIdAndLength(mRanAccFile) == 0) {
                                throw new EBMLException("Unable to parse cluster header info");
                            }

                            // Go directly to the subtitle track data and parse the block
                            mRanAccFile.skipBytes(subEntry.mRelativePosition);
                            pos = mRanAccFile.getFilePointer();
                        }
                        MasterElement blockGroup = new MasterElement(Cluster.BLOCK_GROUP_NODE, pos);
                        if (!blockGroup.parse(mRanAccFile)) {
                            synchronized (mRanAccFile) {
                                // Rare case if author used simpleblock instead of block group since
                                // simple block has no duration, making the subtitle useless, ignore it
                                mRanAccFile.seek(pos);
                                int id = ElementBase.readId(mRanAccFile);
                                if (id == Cluster.SIMPLE_BLOCK) {
                                    continue;
                                }
                                throw new EBMLParsingException("Cannot parse block group");
                            }
                        }

                        // Get the block track number and put it in the correct subtitle track
                        BlockElement block = blockGroup.getBlockElement(Cluster.BLOCK_ID);
                        int blockTrackNumber = block.getTrackNumber();
                        boolean sorted = false;
                        for (Subtitles sub : mSubtitles) {
                            if (sub.getTrackNumber() == blockTrackNumber) {
                                sorted = true;
                                sub.appendBlock(block, timecode - block.getTimecode(),
                                        blockGroup.getValueInt(Cluster.BLOCK_DURATION));
                            }
                        }
                        if (!sorted) {
                            throw new EBMLParsingException("Cannot parse block group for subtitles, is file corrupted?");
                        }
                    }
                    entry.mHasParsed = true;
                    return true;
                }
            } else {
                synchronized (mRanAccFile) {
                    // Cues did not tell us any subtitle locations, we need to read the entire cluster
                    boolean parsedAtLeastOneSub = false;
                    mRanAccFile.seek(entry.mStartAddress);

                    while (true) {
                        // Keep parsing till we reach the next cluster position set from Cues
                        MasterElement clusterEl = new MasterElement(Cluster.ENTRY, mRanAccFile.getFilePointer());
                        if (!clusterEl.parse(mRanAccFile, mSubtitleTrackNumbers, mClusterBlockReadOnlyEl)) {
                            // End of clusters
                            if (entry.mEndAddress != mCuesPosition - 1) {
                                throw new EBMLParsingException("Unable to parse cluster header info");
                            }
                            break;
                        }

                        // Sort each subtitle entry inside this cluster into its subtitle track
                        int timecode = 0;
                        for (ElementBase el : clusterEl.getElements()) {
                            if (el.getType() == NodeBase.Type.MASTER) {
                                BlockElement block = ((MasterElement) el).getBlockElement(Cluster.BLOCK_ID);
                                if (block != null) {
                                    int blockTrackNumber = block.getTrackNumber();
                                    boolean sorted = false;
                                    for (Subtitles sub : mSubtitles) {
                                        if (sub.getTrackNumber() == blockTrackNumber) {
                                            sorted = true;
                                            sub.appendBlock(block, timecode,
                                                    ((MasterElement) el).getValueInt(Cluster.BLOCK_DURATION));
                                            parsedAtLeastOneSub = true;
                                        }
                                    }
                                    if (!sorted) {
                                        throw new EBMLParsingException("Cannot parse block group for subtitles, is file corrupted?");
                                    }
                                }
                            } else if (el.getType() == NodeBase.Type.INT) {
                                timecode = ((IntElement) el).getData();
                            } else {
                                throw new EBMLParsingException("Parsing cluster for subtitles gained useless data");
                            }
                        }

                        // Once we reach the next cluster position set from Cues, we can end the loop
                        // The last entry will have an end address right before the cues, so scan till end
                        if (entry.mEndAddress != mCuesPosition - 1 && mRanAccFile.getFilePointer() >= entry.mEndAddress) {
                            break;
                        }
                    }
                    entry.mHasParsed = true;
                    return parsedAtLeastOneSub;
                }
            }
        }
        return false;
    }

    /**
     * Find the cue entry index within the time provided
     * Finds the index using binary search
     * @param time specified to search for the index
     * @return the index
     */
    public int getCueIndexAtTime(int time) {
        int low = 0;
        int high = mCueFrames.size() - 1;

        if (time > mCueFrames.get(high).mTimecode) {
            return high;
        }

        if (high < 0) {
            throw new IllegalArgumentException("The array cannot be empty");
        }
        if (time > mCueFrames.get(high).mTimecode) {
            throw new IllegalArgumentException("Input time is above last time");
        }

        while (low < high) {
            int mid = (low + high) / 2;
            if (mCueFrames.get(mid).mTimecode < time) {
                low = mid+1;
            } else {
                high = mid;
            }
        }
        return Math.max(high-1, 0);
    }

    /**
     * Find the cue entry index within the address provided
     * Finds the index using binary search
     * @param address specified to search for the index
     * @return the index
     */
    public int getCueIndexFromAddress(long address) {
        int low = 0;
        int high = mCueFrames.size() - 1;

        if (address > mCueFrames.get(high).mStartAddress) {
            return high;
        }

        if (high < 0) {
            throw new IllegalArgumentException("The array cannot be empty");
        }

        while (low < high) {
            int mid = (low + high) / 2;
            if (mCueFrames.get(mid).mStartAddress < address) {
                low = mid+1;
            } else {
                high = mid;
            }
        }
        return Math.max(high-1, 0);
    }

    /**
     * Query if this cue entry has any subtitles to be parsed. Once you use readSubtitlesInCueFrame
     * on a cue entry, it will be marked as read regardless if it has any subtitles.
     * @param index which cue entry
     * @return if you can parse this entry
     */
    public boolean canParseSubtitlesFromCueAt(int index) {
        Cluster.Entry entry = mCueFrames.get(index);
        return !entry.mHasParsed && entry.mSubEntries != null && !entry.mSubEntries.isEmpty();
    }

    /**
     * Read the segment cues
     * Cues contains a few locations for some clusters for video and some mkv files can
     * also contain information directly to each subtitle entry.
     * This is mainly used to get the locations of clusters for subtitles
     * Performance-wise does pretty quickly but should not be done on the ui-thread
     * @throws IOException
     */
    public void readCues() throws IOException {
        if (mCuesHeader == null) {
            findCuesPosition();
            if (mCuesPosition > 0) {
                synchronized (mRanAccFile) {
                    mRanAccFile.seek(mCuesPosition);
                    mCuesHeader = new MasterElement(Cues.HEADER, mCuesPosition);
                    if (!mCuesHeader.parse(mRanAccFile)) {
                        throw new EBMLParsingException("Unable to parse cues properly");
                    }
                }

                mCueFrames = new ArrayList<>();
                Cluster.Entry currentVidEntry = null;
                Cluster.Entry currentSubEntry = null;

                // Read the cues for subtitle track entries and record the cluster position
                for (ElementBase element : mCuesHeader.getElements()) {
                    MasterElement master = (MasterElement) element;
                    int cueTime = master.getValueInt(Cues.TIME, 0);
                    MasterElement trackMaster = (MasterElement) master.getElement(Cues.TRACK_POSITIONS);

                    int trackNumber = trackMaster.getValueInt(Cues.TRACK);
                    long address = trackMaster.getValueLong(Cues.CLUSTER_POSITION);
                    if (address == 0) throw new EBMLParsingException("Cannot parse the address from cues");
                    address += mPositionOffset;

                    // Record each entry into a list to relate time with data
                    if (trackNumber == mVideoTrackIndex) {
                        // Record video entries: Reading cues for cluster positions does not guarantee all addresses!!
                        if (currentVidEntry != null) {
                            // Do not put into list if address is same as previous
                            if (currentVidEntry.mStartAddress == address) {
                                continue;
                            }
                            currentVidEntry.mEndAddress = address - 1;
                            currentVidEntry.mNextTimecode = cueTime;
                        }
                        currentVidEntry = new Cluster.Entry(cueTime, address);
                        mCueFrames.add(currentVidEntry);
                    } else if (mSubtitleTrackNumbers.contains(trackNumber)) {
                        assert currentVidEntry != null;
                        if (address != currentVidEntry.mStartAddress) {
                            // Add a new cue entry here for more fine tune control over subtitles since we didn't specify a cluster entry here before
                            currentVidEntry.mEndAddress = address - 1;
                            currentVidEntry.mNextTimecode = cueTime;
                            currentVidEntry = new Cluster.Entry(cueTime, address);
                            mCueFrames.add(currentVidEntry);
                        }

                        mHasCueSubtitlesPos = true;
                        int relativePos = trackMaster.getValueInt(Cues.RELATIVE_POSITION);
                        if (currentSubEntry != null) {
                            // Do not put into list if address and relative address is same as previous
                            if (currentSubEntry.mStartAddress == address
                                    && currentSubEntry.mRelativePosition == relativePos) {
                                continue;
                            }
                            currentSubEntry.mEndAddress = address - 1;
                            currentSubEntry.mNextTimecode = cueTime;
                        }
                        currentSubEntry = new Cluster.Entry(cueTime, address, relativePos);
                        currentVidEntry.addSubtitle(currentSubEntry);
                    }
                }

                // Set the last cue entry with the total duration as next and address before cues as end address
                if (currentVidEntry != null) {
                    currentVidEntry.mNextTimecode = (int) Math.floor(getDuration());
                    currentVidEntry.mEndAddress = mCuesPosition - 1;
                }
            } else {
                throw new EBMLParsingException("Cannot find cues in file");
            }
        }
    }

    /**
     * Quickly reads only the header of the cues to find the position of the first cluster entry
     * which leads you do the video data
     * @return address for the first video frame (cluster)
     * @throws IOException
     */
    public long readVideoStartAddressFromCues() throws IOException {
        synchronized (mRanAccFile) {
            getCuesDataLength();
            MasterElement pointNode = new MasterElement(Cues.POINT_NODE, mCuesPosition);
            LongElement el = (LongElement) pointNode.parseElementIdOnce(mRanAccFile, Cues.CLUSTER_POSITION);
            return el.getData() + mPositionOffset;
        }
    }

    /**
     * Get the duration in milliseconds of this video
     * @return duration in ms
     */
    public float getDuration() {
        return mDurationMs;
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

    /**
     * Get the audio tracks after parsing the header
     * You must call the functions in the order:
     *      readHeader()
     *      readTracks()
     * @return a readable list of the audio tracks
     */
    public ArrayList<AudioTrack> getAudioTracks() {
        return mAudioTracks;
    }

    private void scanForId(int id, int attempts) throws IOException {
        int b1 = (id >> 24) & 0xFF;
        int b2 = (id >> 16) & 0xFF;
        int b3 = (id >> 8) & 0xFF;
        int b4 = id & 0xFF;

        while (attempts > 0) {
            if ((mRanAccFile.readByte() & 0xFF) == b1) {
                if ((mRanAccFile.readByte() & 0xFF) == b2 && (mRanAccFile.readByte() & 0xFF) == b3
                        && (mRanAccFile.readByte() & 0xFF) == b4) {
                    mRanAccFile.seek(mRanAccFile.getFilePointer() - 4);
                    mPositionOffset = mRanAccFile.getFilePointer();
                    return;
                }
                mRanAccFile.seek(mRanAccFile.getFilePointer() - 1);
            }
            attempts--;
        }
        throw new EBMLException("Unable to find segment seek header properly");
    }

    private void findInfoPosition() throws IOException {
        if (mInfoPosition == 0) {
            mInfoPosition = findPositionFromSegmentEntry(Info.ID);
        }
    }

    private void findTracksPosition() throws IOException {
        if (mTracksPosition == 0) {
            mTracksPosition = findPositionFromSegmentEntry(Tracks.ID);
        }
    }

    private void findCuesPosition() throws IOException {
        if (mCuesPosition == 0) {
            mCuesPosition = findPositionFromSegmentEntry(Cues.ID);
        }
    }

    private void findChaptersPosition() throws IOException {
        if (mChaptersPosition == 0) {
            mChaptersPosition = findPositionFromSegmentEntry(Chapters.ID);
        }
    }

    private void findAttachmentsPosition() throws IOException {
        if (mAttachmentsPosition == 0) {
            mAttachmentsPosition = findPositionFromSegmentEntry(Attachments.ID);
        }
    }

    private long findPositionFromSegmentEntry(int id) throws IOException {
        for (MasterElement segmentHeader : mSegmentHeaders) {
            MasterElement entry = segmentHeader.searchForMasterWithIntValue(
                    Segment.SEEK_ID, id);
            if (entry != null) {
                return (entry.getValueLong(Segment.SEEK_POSITION) + mPositionOffset);
            }
        }
        return 0;
    }
}
