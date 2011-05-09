package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;
import edu.washington.cs.oneswarm.ui.gwt.server.OneSwarmUIServiceImpl;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.FFMpegException.ErrorType;

public class MovieStreamInfo {
    private final static int CURRENT_VERISON = 1;

    private static Logger logger = Logger.getLogger(MovieStreamInfo.class.getName());

    private String audioCodec = "";

    private long audioRate;

    private long audioSampleRate;

    private long bitRate;

    private Set<String> container = new HashSet<String>();

    private long cropBottom;

    private long cropLeft;

    private long cropRight;

    private boolean cropSet = false;

    private long cropTop;

    private double duration = -1;
    private double frameRate;

    private long resolutionX;

    private long resolutionY;

    private String videoCodec = "";

    private long videoRate;

    private String ffmpegOut = "";

    public String getFfmpegOut() {
        return ffmpegOut;
    }

    private static final HashMap<String, SupportedContainer> flashSupportedFormats;
    static {
        flashSupportedFormats = new HashMap<String, SupportedContainer>();
        flashSupportedFormats.put("flv", new FlashSupportedContainerFLV());
        flashSupportedFormats.put("mp4", new FlashSupportedContainerMP4());
        flashSupportedFormats.put("mp3", new FlashSupportedContainerAudioOnly());
    }

    /*
     * default at 10 Mbit/s for unknown bitrates
     */
    private final static int DEFAULT_BIT_RATE = 10 * 1024 * 1024;

    public MovieStreamInfo(File file) throws InvalidPropertiesFormatException, IOException {
        // for deserialization
        FileInputStream in = new FileInputStream(file);
        Properties p = new Properties();
        p.loadFromXML(in);
        in.close();
        long version = getLongProperty(p, "version", 0);
        if (version < CURRENT_VERISON) {
            throw new IOException("version mismatch (" + version + "<" + CURRENT_VERISON);
        }
        bitRate = getLongProperty(p, "bitRate", 0);
        duration = getDoubleProperty(p, "duration", 0);
        container = getStringSetProperty(p, "container", new HashSet<String>());
        ffmpegOut = p.getProperty("ffmpegOut", "");
        videoCodec = p.getProperty("videoCodec", "");
        videoRate = getLongProperty(p, "videoRate", 0);
        audioCodec = p.getProperty("audioCodec", "");
        audioRate = getLongProperty(p, "audioRate", 0);
        audioSampleRate = getLongProperty(p, "audioSampleRate", 0);
        resolutionX = getLongProperty(p, "resolutionX", 0);
        resolutionY = getLongProperty(p, "resolutionY", 0);
        frameRate = getDoubleProperty(p, "frameRate", 0);
        cropTop = getLongProperty(p, "cropTop", 0);
        cropBottom = getLongProperty(p, "cropBottom", 0);
        cropLeft = getLongProperty(p, "cropLeft", 0);
        cropRight = getLongProperty(p, "cropRight", 0);
        cropSet = getLongProperty(p, "cropSet", 0) == 1;
        logger.fine("loaded moviesteaminfo from file: " + toString());
    }

    public MovieStreamInfo(String ffmpegStdErr) throws FFMpegException {
        ffmpegOut = ffmpegStdErr;
        logger.finest("creating movie stream info, stderr:\n" + ffmpegStdErr);
        // find the duration line
        String[] lines = ffmpegStdErr.split("\n");

        boolean durationDone = false;
        boolean audioDone = false;
        boolean videoDone = false;
        boolean containerDone = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // String[] lineSplit = line.split(" ");
            // System.out.println("'" + line + "'");
            try {
                if (!durationDone) {
                    durationDone = this.parseDurationLine(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (!containerDone) {
                    containerDone = this.parseContainerLine(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (!audioDone) {
                    audioDone = this.parseAudioLine(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (!videoDone) {
                    videoDone = this.parseVideoLine(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // for (int j = 0; j < lineSplit.length; j++) {
            // String field = lineSplit[j];
            // System.out.println("'" + field + "'");
            // if()
            // }
        }

        if (duration < 0) {
            final FFMpegException mpegException = new FFMpegException(ErrorType.OTHER,
                    "Unable to parse ffmpeg output");
            mpegException.setStdErr(ffmpegStdErr);
            throw mpegException;
        }
        logger.fine("created moviestreaminfo: " + toString());
    }

    public long getBitRate() {
        return bitRate;
    }

    public long getCropBottom() {
        return cropBottom;
    }

    public long getCropLeft() {
        return cropLeft;
    }

    public long getCropRight() {
        return cropRight;
    }

    public long getCropTop() {
        return cropTop;
    }

    public double getDuration() {
        return duration;
    }

    public long getResolutionX() {
        return resolutionX;
    }

    public long getResolutionY() {
        return resolutionY;
    }

    public boolean hasAudio() {
        return audioCodec.length() > 0;
    }

    public boolean hasVideo() {
        return videoCodec.length() > 0;
    }

    public boolean isCropSet() {
        return cropSet;
    }

    /**
     * returns true is the video is ready to be viewed in flash without
     * convertion
     * 
     * @return
     */
    public boolean isFlashReady() {
        logger.fine("checking video for flash support");

        SupportedContainer fsc = getContainerRules(this.container);
        if (fsc == null) {
            logger.fine("no flash support for container: " + createCommaSeparated(container));
            return false;
        }
        logger.finer("found flash supported container '" + fsc.getName() + "', checking codecs");
        return fsc.isSupported(this);
    }

    private static SupportedContainer getContainerRules(Set<String> containers) {
        for (String container : containers) {
            if (flashSupportedFormats.containsKey(container)) {
                return flashSupportedFormats.get(container);
            }
        }
        return null;
    }

    private boolean parseAudioLine(String l) {
        String t = l.trim();
        if (t.startsWith("Stream")) {
            if (t.contains("Audio:")) {
                System.out.println("audio line: '" + l + "'");
                String[] s = t.split("Audio:");
                if (s.length == 2) {
                    String entries = s[1].trim();
                    String[] eSplit = entries.split(", ");
                    if (eSplit.length > 0) {
                        this.audioCodec = eSplit[0];
                        System.out.println("acodec: " + this.audioCodec);
                    }
                    if (eSplit.length > 1) {
                        if (eSplit[1].contains("Hz")) {
                            this.audioSampleRate = Long.parseLong(eSplit[1].replace(" Hz", ""));
                        }
                    }
                    if (eSplit.length > 4) {
                        this.audioRate = parseBitRate(eSplit[4]);
                        System.out.println("a rate: " + audioRate);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private long parseBitRate(String line) {
        String[] vRate = line.split(" ");
        if (vRate.length > 1) {
            if (vRate[1].equals("mb/s")) {
                return Math.round(Double.parseDouble(vRate[0]) * 1024 * 1024);
            } else if (vRate[1].equals("kb/s")) {
                return Math.round(Double.parseDouble(vRate[0]) * 1024);
            } else if (vRate[1].equals("b/s")) {
                return Math.round(Double.parseDouble(vRate[0]));
            } else if (vRate[0].equals("N/A")) {
                return DEFAULT_BIT_RATE;
            }
        }
        return 0;
    }

    private boolean parseContainerLine(String l) {
        String t = l.trim();
        if (t.startsWith("Input")) {
            System.out.println("parsing container line: " + t);
            String[] split = t.split(" ");
            if (split.length > 3) {
                this.container = setFromCommaSeparated(split[2]);
                System.out.println("got container: " + container);
                return true;
            }
        }
        return false;
    }

    private boolean parseDurationLine(String line) {
        String[] lineSplit = line.split(" ");
        boolean d = false;
        boolean b = false;
        /*
         * find the duration field
         */
        for (int i = 0; i < lineSplit.length; i++) {
            if ("Duration:".equals(lineSplit[i])) {
                if (lineSplit.length > i + 2) {
                    d = true;
                    this.duration = parseLength(lineSplit[i + 1]);
                }
            }
        }
        for (int i = 0; i < lineSplit.length; i++) {
            if ("bitrate:".equals(lineSplit[i])) {
                if (lineSplit.length > i + 2) {
                    b = true;
                    this.bitRate = parseBitRate(lineSplit[i + 1] + " " + lineSplit[i + 2]);
                } else if (lineSplit.length > i + 1) {
                    if ("N/A".equals(lineSplit[i + 1])) {
                        b = true;
                        this.bitRate = DEFAULT_BIT_RATE;
                    }
                }
            }
        }

        return b && d;

    }

    public Map<String, String> getAsUiMap() {
        Map<String, String> v = new HashMap<String, String>();
        v.put("ffmpegOut", getFfmpegOut());

        if (isFlashReady()) {
            String rules = getContainerRules(container).name;
            v.put(" Flash ready", "yes, with '" + rules + "' rules");
        } else {
            v.put(" Flash ready", "no, convert needed");
        }
        v.put(" Duration", StringTools.trim(getDuration(), 1) + " s");
        v.put(" Container", getContainerString() + "");
        v.put(" Bit rate", StringTools.formatRate(getBitRate(), "bit/s"));
        if (hasVideo()) {
            v.put("Video codec", getVideoCodec());
            if (getVideoRate() > 0) {
                v.put("Video bit rate", StringTools.formatRate(getVideoRate(), "bit/s"));
            }
            v.put("Video Resolution", getResolutionX() + "x" + getResolutionY());
            v.put("Video frame rate", getFrameRate() + "");
            if (isCropSet()) {
                v.put("Video crop", "(" + getCropLeft() + "," + getCropRight() + "," + getCropTop()
                        + "," + getCropBottom() + ")");
            }
        }
        if (hasAudio()) {
            v.put("Audio codec", getAudioCodec());
            if (getAudioRate() > 0) {
                v.put("Audio bit rate", StringTools.formatRate(getAudioRate(), "bit/s"));
            }
            v.put("Audio sample rate", getAudioSampleRate() + " Hz");
        }
        return v;
    }

    private double parseLength(String str) {
        logger.finest("looking at length line: " + str);
        // remove the last ,
        if (str.endsWith(",")) {
            str = str.replaceAll(",", "");
        }
        String[] split = str.split(":");
        if (split.length == 3) {
            double seconds = Double.parseDouble(split[2]);
            int minutes = Integer.parseInt(split[1]);
            int hours = Integer.parseInt(split[0]);
            logger.finest("h=" + hours + " m=" + minutes + " s=" + seconds);
            double ret = 3600 * hours + 60 * minutes + seconds;
            logger.finest("h=" + hours + " m=" + minutes + " s=" + seconds + " ret=" + ret);
            return ret;
        }
        return -1;

    }

    private boolean parseVideoLine(String l) {
        String t = l.trim();
        if (t.startsWith("Stream")) {
            if (t.contains("Video:")) {
                System.out.println("video line: '" + l + "'");
                String[] s = t.split("Video:");
                if (s.length == 2) {
                    String entries = s[1].trim();
                    String[] eSplit = entries.split(", ");
                    if (eSplit.length > 0) {
                        /*
                         * parse the codec field
                         */
                        this.videoCodec = eSplit[0];
                        System.out.println("vcodec: " + this.videoCodec);
                    }
                    if (eSplit.length > 2) {
                        /*
                         * and the resolution field
                         */
                        String res[] = eSplit[2].split(" ");
                        if (res.length > 0) {
                            String[] rSplit = res[0].split("x");
                            if (rSplit.length == 2) {
                                resolutionX = Integer.parseInt(rSplit[0]);
                                resolutionY = Integer.parseInt(rSplit[1]);
                                System.out.println("res: " + resolutionX + "x" + resolutionY);
                            }
                        }
                    }
                    if (eSplit.length > 3) {
                        long vRate = parseBitRate(eSplit[3]);
                        if (vRate > 0) {
                            this.videoRate = vRate;
                            System.out.println("video bit rate: " + videoRate);
                        } else {
                            // this could be frame rate, test
                            String[] fr = eSplit[3].split(" ");
                            if (fr.length > 1 && fr[1].contains("tb(r)")) {
                                frameRate = Double.parseDouble(fr[0]);
                                System.out.println("frame rate: " + frameRate);
                            }
                        }
                    }
                    if (eSplit.length > 4) {
                        /*
                         * and the frame rate
                         */
                        String[] fr = eSplit[4].split(" ");
                        if (fr.length > 0) {
                            try {
                                frameRate = Double.parseDouble(fr[0]);
                            } catch (Exception e) {
                                frameRate = -1.0;
                            }
                            System.out.println("frame rate: " + frameRate);
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public void setCrop(int top, int bottom, int left, int rigth) {
        this.cropTop = top;
        this.cropBottom = bottom;
        this.cropLeft = left;
        this.cropRight = rigth;
        this.cropSet = true;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public String toString() {
        return "container=" + createCommaSeparated(container) + " vcodec=" + videoCodec
                + " acodec=" + audioCodec + " duration=" + duration + " bitrate=" + bitRate
                + " crop=(" + cropTop + "," + cropBottom + "," + cropLeft + "," + cropRight + ")";
    }

    public Properties getAsProperties() {
        Properties p = new Properties();
        p.setProperty("version", CURRENT_VERISON + "");
        p.setProperty("bitRate", "" + bitRate);
        p.setProperty("duration", "" + duration);
        p.setProperty("videoCodec", videoCodec);
        p.setProperty("videoRate", "" + videoRate);
        p.setProperty("audioCodec", audioCodec);
        p.setProperty("audioRate", "" + audioRate);
        p.setProperty("audioSampleRate", "" + audioSampleRate);
        p.setProperty("resolutionX", "" + resolutionX);
        p.setProperty("resolutionY", "" + resolutionY);
        p.setProperty("frameRate", "" + frameRate);
        p.setProperty("cropSet", "" + (cropSet ? 1 : 0));
        p.setProperty("cropTop", "" + cropTop);
        p.setProperty("cropBottom", "" + cropBottom);
        p.setProperty("cropLeft", "" + cropLeft);
        p.setProperty("cropRight", "" + cropRight);
        p.setProperty("container", createCommaSeparated(container));
        p.setProperty("ffmpegOut", ffmpegOut);
        return p;
    }

    public void writeToFile(File f) throws IOException {
        Properties p = getAsProperties();
        FileOutputStream out = new FileOutputStream(f);
        p.storeToXML(out, "");
        out.close();
    }

    private static String createCommaSeparated(Set<String> strings) {
        StringBuilder b = new StringBuilder();
        for (String s : strings) {
            b.append(s + ",");
        }
        return b.toString();
    }

    private static double getDoubleProperty(Properties p, String key, double defaultVal) {
        if (p.containsKey(key)) {
            return Double.parseDouble(p.getProperty(key));
        } else {
            return defaultVal;
        }
    }

    private static long getLongProperty(Properties p, String key, long defaultVal) {
        if (p.containsKey(key)) {
            return Long.parseLong(p.getProperty(key));
        } else {
            return defaultVal;
        }
    }

    private static Set<String> getStringSetProperty(Properties p, String key, Set<String> defaultVal) {
        if (p.containsKey(key)) {
            String commaSeparated = p.getProperty(key);
            return setFromCommaSeparated(commaSeparated);
        } else {
            return defaultVal;
        }
    }

    public static void main(String[] args) {

        StringBuilder b = new StringBuilder();
        BufferedReader in;
        try {
            OneSwarmUIServiceImpl.loadLogger();
            logger.setLevel(Level.ALL);
            in = new BufferedReader(new FileReader(new File("/tmp/ffmpeg.out")));

            String line;
            while ((line = in.readLine()) != null) {
                b.append(line + "\n");
            }
            MovieStreamInfo m = new MovieStreamInfo(b.toString());
            m.writeToFile(new File("/tmp/m"));
            System.out.println(m.toString());
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FFMpegException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static Set<String> setFromCommaSeparated(String commaSeparated) {
        Set<String> set = new HashSet<String>();
        if (commaSeparated.contains(",")) {
            String[] split = commaSeparated.split(",");
            for (String s : split) {
                if (s.length() > 0) {
                    set.add(s);
                }
            }
        } else {
            set.add(commaSeparated);
        }
        return set;
    }

    static abstract class SupportedContainer {
        public static enum VideoHandler {
            FLASH, SILVERLIGHT, HTML5;
        }

        private final String name;
        private final VideoHandler videoHandler;

        private SupportedContainer(String name, VideoHandler videoHandler) {
            this.name = name;
            this.videoHandler = videoHandler;
        }

        public String getName() {
            return name;
        }

        public abstract boolean isSupported(MovieStreamInfo m);

        public abstract String[] getSupportedAudioCodecs();

        public abstract String[] getSupportedVideoCodecs();

        protected boolean isVideoCodecSupported(MovieStreamInfo m) {
            for (String supportedCodec : getSupportedVideoCodecs()) {
                if (m.videoCodec.equals(supportedCodec)) {
                    return true;
                }
            }
            return false;
        }

        protected boolean isAudioCodecSupported(MovieStreamInfo m) {
            for (String supportedCodec : getSupportedAudioCodecs()) {
                if (m.audioCodec.equals(supportedCodec)) {
                    return true;
                }
            }
            return false;
        }
    }

    static class FlashSupportedContainerFLV extends SupportedContainer {
        public final static String[] SUPPORTED_VIDEO_CODECS = new String[] { "flv", "vp6f", "h264" };
        public final static String[] SUPPORTED_AUDIO_CODECS = new String[] { "nellymoser", "mp3",
                "aac" };

        public FlashSupportedContainerFLV() {
            super("flv", VideoHandler.FLASH);
        }

        @Override
        public boolean isSupported(MovieStreamInfo m) {
            /*
             * this is only for flv containers
             */
            if (m.container.contains("flv")) {
                logger.finer("checking for flash support, flv: " + toString());

                /*
                 * if the file has video it must be supported by flash to be
                 * flash ready
                 */
                if (m.hasVideo() && (isVideoCodecSupported(m) == false)) {
                    logger.finer("format not supported, hasVideo=" + m.hasVideo()
                            + " codec_supported=" + isVideoCodecSupported(m));
                    return false;
                }
                logger.finest("video is ok, codec=" + m.videoCodec);

                /*
                 * same goes for audio
                 */
                if (m.hasAudio() && (isAudioCodecSupported(m) == false)) {
                    logger.finer("format not supported, hasAudio=" + m.hasAudio()
                            + " codec_supported=" + isAudioCodecSupported(m));
                    return false;
                }

                /*
                 * special check for 48000 Hz mp3, not supported...
                 */
                if (m.audioCodec.equals("mp3")) {
                    if (m.audioSampleRate == 48000) {
                        logger.fine("mp3 with 48000 Hz sample rate is not allowed");
                        return false;
                    } else {
                        logger.finest("mp3 sample rate ok (" + m.audioSampleRate + " Hz)");
                    }
                }
                logger.finest("audio is ok, codec=" + m.audioCodec);
                logger.fine("format is flv supported");
                return true;
            }
            logger.finer("container not flv");

            return false;
        }

        @Override
        public String[] getSupportedAudioCodecs() {
            return SUPPORTED_AUDIO_CODECS;
        }

        @Override
        public String[] getSupportedVideoCodecs() {
            return SUPPORTED_VIDEO_CODECS;
        }
    }

    static class FlashSupportedContainerMP4 extends SupportedContainer {
        public final static String[] SUPPORTED_VIDEO_CODECS = new String[] { "h264" };
        public final static String[] SUPPORTED_AUDIO_CODECS = new String[] { "aac" };

        public FlashSupportedContainerMP4() {
            super("mp4", VideoHandler.FLASH);

        }

        @Override
        public boolean isSupported(MovieStreamInfo m) {
            /*
             * this is only for flv containers
             */
            if (m.container.contains("mp4")) {
                logger.finer("checking for flash support, mp4: " + toString());

                /*
                 * if the file has video it must be supported by flash to be
                 * flash ready
                 */
                if (m.hasVideo() && (isVideoCodecSupported(m) == false)) {
                    logger.finer("format not supported, hasVideo=" + m.hasVideo()
                            + " codec_supported=" + isVideoCodecSupported(m));
                    return false;
                }
                logger.finest("video is ok, codec=" + m.videoCodec);

                /*
                 * same goes for audio
                 */
                if (m.hasAudio() && (isAudioCodecSupported(m) == false)) {
                    logger.finer("format not supported, hasAudio=" + m.hasAudio()
                            + " codec_supported=" + isAudioCodecSupported(m));
                    return false;
                }
                logger.finest("audio is ok, codec=" + m.audioCodec);

                logger.fine("format is flash mp4 supported");
                return true;
            }
            logger.finer("container not mp4");

            return false;
        }

        @Override
        public String[] getSupportedAudioCodecs() {
            return SUPPORTED_AUDIO_CODECS;
        }

        @Override
        public String[] getSupportedVideoCodecs() {
            return SUPPORTED_VIDEO_CODECS;
        }
    }

    static class FlashSupportedContainerAudioOnly extends SupportedContainer {

        private final Set<String> containers = new HashSet<String>();
        public final static String[] SUPPORTED_VIDEO_CODECS = new String[] { "" };
        public final static String[] SUPPORTED_AUDIO_CODECS = new String[] { "mp3" };// ,
                                                                                     // "aac"
                                                                                     // };

        public FlashSupportedContainerAudioOnly() {
            super("AudioOnly", VideoHandler.FLASH);
            for (String audioContainer : SUPPORTED_AUDIO_CODECS) {
                containers.add(audioContainer);
            }
        }

        @Override
        public boolean isSupported(MovieStreamInfo m) {
            /*
             * this is only for audio only containers
             */
            boolean containerSupported = false;
            for (String container : containers) {
                if (m.container.contains(container)) {
                    containerSupported = true;
                }
            }
            if (containerSupported) {
                logger.finer("checking for flash support, audio only containers: " + toString());

                /*
                 * if the file has video it won't work with audio only
                 * containers...
                 */
                if (m.hasVideo()) {
                    logger.finer("format not supported, hasVideo=" + m.hasVideo());
                    return false;
                }

                /*
				 * 
				 */
                if (m.hasAudio() && (isAudioCodecSupported(m) == false)) {
                    logger.finer("format not supported, hasAudio=" + m.hasAudio()
                            + " codec_supported=" + isAudioCodecSupported(m));
                    return false;
                }
                logger.finest("audio is ok, codec=" + m.audioCodec);

                logger.fine("format is flash audio supported");
                return true;
            }
            logger.finer("container not in (" + createCommaSeparated(containers) + ")");

            return false;
        }

        @Override
        public String[] getSupportedAudioCodecs() {
            return SUPPORTED_AUDIO_CODECS;
        }

        @Override
        public String[] getSupportedVideoCodecs() {
            return SUPPORTED_VIDEO_CODECS;
        }
    }

    /**
     * if the video is in h246 it is really expensive to reencode to flash, much
     * better to just repackage to mp4
     * 
     * @author isdal
     * 
     */
    static class RepackSupportedContainer extends SupportedContainer {
        public final static String[] SUPPORTED_VIDEO_CODECS = new String[] { "h264" };
        /*
         * it could be supported by more containers, might add more
         */
        public final static String[] SUPPORTED_CONTAINERS = new String[] { "mkv", "avi" };

        public RepackSupportedContainer() {
            super("repack", VideoHandler.FLASH);
        }

        @Override
        public boolean isSupported(MovieStreamInfo m) {

            /*
             * current ffmpeg doesn't support this due to bug 807
             */
            if (true)
                return false;

            boolean supportedContainer = false;

            for (String container : SUPPORTED_CONTAINERS) {
                if (m.container.contains(container)) {
                    supportedContainer = true;
                }
            }

            if (supportedContainer) {
                logger.finer("checking for repackage support: " + toString());

                /*
                 * if the file has video it must be supported by flash to be
                 * flash ready
                 */
                if (m.hasVideo() && (isVideoCodecSupported(m) == false)) {
                    logger.finer("format not supported, hasVideo=" + m.hasVideo()
                            + " codec_supported=" + isVideoCodecSupported(m));
                    return false;
                }
                logger.finest("video is ok, codec=" + m.videoCodec);

                logger.fine("format is repack supported");
                return true;
            }
            logger.finer("container not repack supported");

            return false;
        }

        @Override
        public String[] getSupportedAudioCodecs() {
            return new String[0];
        }

        @Override
        public String[] getSupportedVideoCodecs() {
            return SUPPORTED_VIDEO_CODECS;
        }
    }

    public String getAudioCodec() {
        return audioCodec;
    }

    public long getAudioRate() {
        return audioRate;
    }

    public long getAudioSampleRate() {
        return audioSampleRate;
    }

    public Set<String> getContainer() {
        return container;
    }

    public String getContainerString() {
        return createCommaSeparated(container);
    }

    public double getFrameRate() {
        return frameRate;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public long getVideoRate() {
        return videoRate;
    }
}
