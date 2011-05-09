package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.MovieStreamInfo;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.StreamReader;

public class MoveStreamInfoTest {

    public static void main(String[] args) {
        try {

            File dir = new File("/Users/isdal/Documents/Azureus Downloads/");
            if (dir.exists()) {
                File[] files = dir.listFiles();
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    if (file.isFile()) {
                        // ffmpeg -i swing.avi -vcodec png -vframes 1 -an -f
                        // rawvideo -s 320x240 swing1.png
                        int seekTo = 2;
                        Process ffmpeg = Runtime
                                .getRuntime()
                                .exec(new String[] { "/opt/local/bin/ffmpeg", "-i",
                                        file.getCanonicalPath(), "-vcodec", "png", "-ss",
                                        seekTo + "", "-vframes", "1", "-an", "-f", "rawvideo", "-" });

                        BufferedReader stdErr = new BufferedReader(new InputStreamReader(
                                ffmpeg.getErrorStream()));
                        // dump anything showing up on stdout
                        StreamReader r = new StreamReader(ffmpeg.getInputStream());

                        StringBuffer output = new StringBuffer();
                        String line;
                        while ((line = stdErr.readLine()) != null) {
                            output.append(line + "\n");
                        }
                        stdErr.close();
                        int exitVal = ffmpeg.waitFor();
                        System.out.println("exited with status: " + exitVal);
                        if (exitVal == 0) {
                            MovieStreamInfo m = new MovieStreamInfo(output.toString());
                            System.out.println(file.getName());
                            System.out.println(m.toString());
                            if (m.getDuration() > 0) {
                                byte[] img = r.read();

                                OutputStream o = new FileOutputStream(new File(
                                        file.getCanonicalPath() + ".png"));
                                o.write(img);
                                o.close();
                            }

                        }
                    }
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FFMpegException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
