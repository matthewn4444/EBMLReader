# EBMLReader / MKV Reader (0.1.0) [Java]
This project is a library project to parse MKV files that follow the EBML binary
format. This project is not complete and was mainly created to parse attachments
and subtitles.

You can read the documentation for EBML, the MKV format at:
[http://www.matroska.org/technical/specs/index.html](http://www.matroska.org/technical/specs/index.html)

## What can this library do?

This library can read an MKV file and parse out all the subtitles and attachments
quickly and efficently in Java. You can use the code in a normal Java application
or in an Android application (which is what I am using it for).


For now there is not much support for video and audio metadata. The data is parsed
but I did not implement a simple way to read the data. One would have to inherit
EBMLReader.java and get the data from the *mTracksHeader* variable.

### Subtitles

The current subtitle support is easy enough to read through the file quickly
and output SRT or ASS (SSA) subtitles and write them to disk or convert to WebVTT.
You can then later do whatever you want with the subtitles. You can also read
all the metadata of the subtitles in a simple class with a couple of getter
functons.


The idea with subtitles in this program is to read through tracks and cues to
find the locations of subtitle entries scattered throughout the video file and
read them. Later you can query subtitles that you just read.


This means you can stream subtitles while playing a video (say if you were
to ChromeCast the video and gradually send the subtitles to the receiver). You
can change the location to parse the subtitles are while they are being extracted
in case the user seeks the video.

### Attachments

You can extract all the attachments from a MKV file. It will not allocate the
data until you ask for it as it will just store the position and length of the
binary data.

## Sample Code

    // Please run the code in a thread or AsyncTask (in Android)
    // You must follow the order of function calls to read the subtitles and
    // attachments successfully
    EBMLReader reader = null;
    try {
        reader = new EBMLReader("/path/to/my/video.mkv");

        // Check to see if this is a valid MKV file
        // The header contains information for where all the segments are located
        if (!reader.readHeader()) {
            System.out.println("This is not an mkv file!");
            return;
        }

        // Read the tracks. This contains the details of video, audio and subtitles
        // in this file
        reader.readTracks();

        // Extract the attachments: fonts, images etc
        // This function takes a couple of milliseconds usually less than 500ms
        reader.readAttachments();
        List<Attachments.FileAttachment> attachments = reader.getAttachments();

        // Write each attachment to file
        for (Attachments.FileAttachment attachment : attachments) {
            File filepath = new File("/path/output/files/"+ attachment.getName());
            FileOutputStream fos = new FileOutputStream(filepath);
            try {
                // This will now allocate and copy data
                byte[] buffer = attachment.getData();
                fos.write(buffer);
            } finally {
                fos.close();
            }
        }

        // Check if there are any subtitles in this file
        int numSubtitles = reader.getSubtitles().size();
        if (numSubtitles == 0) {
            System.out.println("There are no subtitles in this file!");
            return;
        }
        System.out.println("There are " + numSubtitles + " subtitles in this file");

        // You need this to find the clusters scattered across the file to find
        // video, audio and subtitle data
        reader.readCues();

        // OPTIONAL: You can read the header of the subtitle if it is ASS/SSA format
        for (int i = 0; i < reader.getSubtitles().size(); i++) {
            if (reader.getSubtitles().get(i) instanceof SSASubtitles) {
                SSASubtitles subs = (SSASubtitles) reader.getSubtitles().get(i);
                System.out.println(subs.getHeader());
            }
        }

        // Read all the subtitles from the file
        // If you want you can implement to read x-number of subtitles at a time,
        // this will read all of it
        // This will read all the subtitles if there are more than one
        // Performance-wise, this will take some time because it needs to read
        // most of the file.
        while (reader.readNextSubtitle());

        // If you had to seek the video while the subtitles are still extracting,
        // you can use reader.moveSubtitleIteratorAfterTime(<time>) to extract
        // at that time location

        // OPTIONAL: we get the subtitle data that was just read
        for (int i = 0; i < reader.getSubtitles().size(); i++) {
            List<Caption> subtitles = reader.getSubtitles().get(i).readUnreadSubtitles();
            // Do want you like with partial read of subtitles, you can technically
            // write the subtitles to file here
        }

        // Write the subtitles to file
        for (int i = 0; i < reader.getSubtitles().size(); i++) {
            Subtitles subs = reader.getSubtitles().get(i);
            if (subs instanceof SSASubtitles) {
                subs.writeFile("/path/to/output/subtitles.ass");
            } else {
                subs.writeFile("/path/to/output/subtitles.srt");
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
    } finally {
        try {
            // Remember to close this!
            reader.close();
        } catch (Exception e) {}
    }


## Integration with an Android Studio project

1. You can clone the project in the root of your project here:
    ``https://github.com/matthewn4444/EBMLReader.git``

    Or add it as a submodule
    ``git submodule add https://github.com/matthewn4444/EBMLReader.git``


2. In your project's **settings.gradle**:
    ``include ':ebmlreader'``

3. In your app's **build.gradle** add this line to dependencies:
    ``compile project(':ebmlreader')``

4. You are now ready to use this library

## Future Work/TODO

- Implement wrapper classes for video and audio extraction
- Test more MKV files for limitations in this library
- Implement Chapters parsing
- Maybe implement an easier way to read parsed data
