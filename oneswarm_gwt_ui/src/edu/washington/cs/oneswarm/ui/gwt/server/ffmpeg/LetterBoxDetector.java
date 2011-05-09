package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import sun.awt.AWTAutoShutdown;

public class LetterBoxDetector {

    private static Logger logger = Logger.getLogger(LetterBoxDetector.class.getName());

    /**
     * tries to detect the letter box boundaries in movies to suggest a good
     * crop
     * 
     * @param args
     */
    public static void main(String[] args) {

    }

    private static SuggestedCrop getCropSummary(List<String> frames, double fraction)
            throws IOException {
        List<SuggestedCrop> samples = new LinkedList<SuggestedCrop>();
        for (String file : frames) {

            FileInputStream in = new FileInputStream(new File(file));

            ImageIO.setUseCache(false);

            /*
             * note, ImageIO.read() causes an AWT-Thread to stick
             * 
             * if you fix this, fix FFMpegTools:310 as well
             */
            BufferedImage base = ImageIO.read(in);
            in.close();
            BufferedImage im = new BufferedImage(base.getWidth(null), base.getHeight(null),
                    BufferedImage.TYPE_INT_ARGB);
            // Copy image to buffered image
            Graphics2D g = im.createGraphics();

            // Paint the image onto the buffered image
            g.drawImage(base, 0, 0, null);
            g.dispose();

            CannyEdgeDetector d = new CannyEdgeDetector();
            float low = 7.5f;
            d.setLowThreshold(low);
            float high = 10f;
            d.setHighThreshold(high);
            d.setSourceImage(im);

            d.process();
            // BufferedImage edges = d.getEdgesImage();
            // ImageIO.write(edges, "png", new File(name + "_edges_" + low +
            // "_"
            // + high + ".png"));
            int[] data = d.getRawPixels();
            int width = d.getWidth();
            int heigth = d.getHeight();

            SuggestedCrop crop = new SuggestedCrop(width, heigth, data, 0.75);
            logger.finer("file=" + file + " crop=" + crop);
            samples.add(crop);

            /**
             * There seems to be a weird interaction with using ImageIO if we
             * don't mark this thread as free. (The AWT-Shutdown thread will
             * hang around forever when we try to quit until it is forceably
             * shutdown). Calling this seems to prevent that, although I'm not
             * sure if we've addressed the root problem.
             */
            try {
                AWTAutoShutdown.notifyToolkitThreadFree();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        SuggestedCrop result = SuggestedCrop.getCropSummary(samples, fraction);
        logger.fine("final: " + result);
        return result;
    }

    public static void setCropInfo(MovieStreamInfo mediaInfo, File mediaFile) {
        /*
         * settings
         */
        int maxSearchLength = 1 * 15;
        int frames = 10;
        double frameRate = frames / (double) maxSearchLength;
        double minFractionOfFramesNeeded = 0.3;

        SuggestedCrop res = null;
        File tempDir = null;
        List<String> screenShots = new LinkedList<String>();
        try {
            String tempPrefix = Long.toHexString(System.currentTimeMillis());

            File tmpFile = File.createTempFile(tempPrefix, ".xml");
            tmpFile.deleteOnExit();
            tempDir = tmpFile.getParentFile();

            if (mediaInfo == null) {
                throw new RuntimeException("mediainfo is null");
            }
            if (!mediaFile.exists()) {
                throw new RuntimeException("media file does not exist " + mediaFile);
            }
            if (mediaInfo.getDuration() <= 0) {
                throw new RuntimeException("mediainfo duration is 0");
            }

            if (mediaInfo.getDuration() < maxSearchLength) {
                frameRate = frames / mediaInfo.getDuration();
            }
            // ~/app/bin/ffmpeg -i file -r 0.1 -vframes 10 test_%03d.png

            String tmpFileName = tempDir.getCanonicalPath() + File.separator + tempPrefix;
            final Process ffmpeg = Runtime.getRuntime().exec(
                    new String[] { FFMpegTools.getFFMpegPath(), "-i", mediaFile.getCanonicalPath(),
                            "-r", "" + frameRate, "-vframes", "" + frames,
                            tmpFileName + "_%03d.png" });
            /*
             * add a thread that kill ffmpeg if it takes longer than 60 sec to
             * terminate
             */
            Thread ffmpegTerminatorThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(60 * 1000);
                        if (ffmpeg != null) {
                            ffmpeg.destroy();
                        }
                    } catch (InterruptedException e) {
                    }
                }
            });
            ffmpegTerminatorThread.setName("FFMpeg terminator thread");
            ffmpegTerminatorThread.setDaemon(true);
            ffmpegTerminatorThread.start();

            BufferedReader stdErr = new BufferedReader(new InputStreamReader(
                    ffmpeg.getErrorStream()));
            // read anything showing up on stdout
            StreamReader ffmpegStdOutReader = new StreamReader(ffmpeg.getInputStream());

            // and read from stderr at the same time
            StringBuffer ffmpegStdErr = new StringBuffer();

            int lineNum = 0;
            try {
                String line;
                while ((line = stdErr.readLine()) != null) {
                    // ffmpeg can send out megabytes on stderr, only save first
                    // 100
                    // lines
                    if (lineNum++ < 100) {
                        ffmpegStdErr.append(line + "\n");
                        logger.finest(line);
                    }
                }
                stdErr.close();
                byte[] b = ffmpegStdOutReader.read();
                logger.finest(new String(b));
            } catch (IOException e) {

                throw new FFMpegException(FFMpegException.ErrorType.OTHER,
                        "Got IO error while reading from ffmpeg", e);

            } catch (InterruptedException e) {
                throw new FFMpegException(FFMpegException.ErrorType.INTERUPT,
                        "Got interupted while reading from ffmpeg", e);
            }

            final int exitVal;
            try {
                exitVal = ffmpeg.waitFor();
                ffmpegTerminatorThread.interrupt();
            } catch (InterruptedException e) {
                throw new FFMpegException(FFMpegException.ErrorType.INTERUPT,
                        "Got interupted while waiting for ffmpeg to complete", e);
            }

            if (exitVal == 0) {
                DecimalFormat myFormat = new DecimalFormat("000");

                for (int i = 1; i <= frames; i++) {
                    String fileName = tmpFileName + "_" + myFormat.format(new Integer(i)) + ".png";
                    screenShots.add(fileName);
                    logger.finest("'" + fileName + "'");
                }
                res = getCropSummary(screenShots, minFractionOfFramesNeeded);
                mediaInfo.setCrop(res.top.index, res.bottom.index, res.left.index, res.rigth.index);
                logger.fine("new mediaInfo: " + mediaInfo);
                return;
            } else {
                throw new FFMpegException(FFMpegException.ErrorType.OTHER, exitVal,
                        ffmpegStdErr.toString());
            }

        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (FFMpegException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            System.err.println(e1.getStdErr());
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            for (String string : screenShots) {
                File f = new File(string);
                if (f.exists()) {
                    f.delete();
                    logger.finest("deleting: " + f);
                }
            }
        }

        mediaInfo.setCrop(0, 0, 0, 0);
    }

    static class CropResult {
        final int index;
        final double ratio;

        public CropResult(int index, double ratio) {
            this.index = index;
            this.ratio = ratio;
        }

        public String toString() {
            double r = ((int) (100 * ratio)) / 100.0;
            return "(" + index + "," + r + ")";
        }
    }

    static class SuggestedCrop {
        final CropResult bottom;
        int[] dotsPerCol;
        int[] dotsPerRow;
        final CropResult left;
        final CropResult rigth;
        final CropResult top;

        public SuggestedCrop(CropResult top, CropResult bottom, CropResult left, CropResult rigth) {
            this.top = top;
            this.bottom = bottom;
            this.left = left;
            this.rigth = rigth;
        }

        public SuggestedCrop(int width, int heigth, int[] edgeImage, double breakingRatio) {
            dotsPerRow = new int[heigth];
            dotsPerCol = new int[width];
            // HashMap<Integer, Integer> counts = new HashMap<Integer,
            // Integer>();

            for (int i = 0; i < edgeImage.length; i++) {
                int row = i / width;
                int col = i % heigth;
                if (edgeImage[i] == -1) {
                    dotsPerRow[row]++;
                    dotsPerCol[col]++;
                }
            }
            this.top = analyseStart(dotsPerRow, breakingRatio, width);
            this.bottom = analyseEnd(dotsPerRow, breakingRatio, width);
            this.left = analyseStart(dotsPerCol, breakingRatio, heigth);
            this.rigth = analyseEnd(dotsPerCol, breakingRatio, heigth);

        }

        public String toString() {
            return "top=" + top + " bottom=" + bottom + " left=" + left + " right=" + rigth;
        }

        private static CropResult analyseEnd(final int[] values, final double breakingRatio,
                final int maxDots) {
            // find the lower crop line, it has to be in the last 25% of
            // the image, walk the image backwards to detect the last line
            // above limit
            int maxFill = 0;
            int maxIndex = 0;
            double ratio = 0;
            for (int i = values.length - 1; i > (3 * values.length) / 4; i--) {
                if (maxFill < values[i]) {
                    maxFill = values[i];
                    maxIndex = i;
                    ratio = ((double) maxFill) / maxDots;
                    if (ratio > breakingRatio) {
                        /*
                         * return the band size, that is the number of pixels to
                         * crop
                         */
                        return new CropResult(values.length - maxIndex, ratio);
                    }
                }
            }
            return null;
        }

        private static CropResult analyseStart(final int[] values, final double breakingRatio,
                final int maxDots) {
            // find the upper crop line, it has to be in the top 25% of
            // the image
            int maxFill = 0;
            int maxIndex = 0;
            double ratio = 0;
            for (int i = 0; i < values.length / 4; i++) {
                if (maxFill < values[i]) {
                    maxFill = values[i];
                    maxIndex = i;
                    ratio = ((double) maxFill) / maxDots;
                    if (ratio > breakingRatio) {
                        return new CropResult(maxIndex, ratio);
                    }
                }
            }

            return null;
        }

        public static SuggestedCrop getCropSummary(List<SuggestedCrop> samples, double fraction) {

            try {
                CropResult t = getMajority(samples, "top", fraction);
                CropResult b = getMajority(samples, "bottom", fraction);
                CropResult l = getMajority(samples, "left", fraction);
                CropResult r = getMajority(samples, "rigth", fraction);
                return new SuggestedCrop(t, b, l, r);
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        private static CropResult getMajority(List<SuggestedCrop> samples, String field,
                double fraction) throws IllegalArgumentException, SecurityException,
                IllegalAccessException, NoSuchFieldException {
            Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
            Map<Integer, Double> ratioSum = new HashMap<Integer, Double>();
            for (SuggestedCrop s : samples) {
                CropResult r = (CropResult) SuggestedCrop.class.getDeclaredField(field).get(s);
                if (r != null) {
                    if (!counts.containsKey(r.index)) {
                        counts.put(r.index, 0);
                        ratioSum.put(r.index, 0.0);
                    }
                    counts.put(r.index, counts.get(r.index) + 1);
                    ratioSum.put(r.index, ratioSum.get(r.index) + r.ratio);
                }
            }
            for (Integer idx : counts.keySet()) {
                int count = counts.get(idx);
                if (count > samples.size() * fraction) {
                    return new CropResult(idx, ratioSum.get(idx) / count);
                }
            }
            return new CropResult(0, 0);
        }

    }
}
