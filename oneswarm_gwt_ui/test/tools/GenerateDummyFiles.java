package tools;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class GenerateDummyFiles {

    private static Random rand = new Random();

    private static byte[] buffer = new byte[256 * 1024];

    /**
     * This class will generate dummy files, the directory structure will be
     * similar to that of an itunes share
     * 
     * base/artist/album/song.mp3
     * 
     * 
     * @param args
     */
    public static void main(String[] args) {

        if (args.length != 2) {
            System.err.println("Usage: GenerateDummyFiles base_dir #files");
            System.err.println("Example: GenerateDummyFiles /tmp/dummies/ 10000");
            System.exit(0);
        }

        File baseDir = new File(args[0]);
        if (baseDir.isFile()) {
            System.err.println(args[0] + " is a file, it must be a directory!");
            System.exit(0);
        } else if (!baseDir.isDirectory()) {
            baseDir.mkdirs();
        }

        int fileNum = Integer.parseInt(args[1]);
        int numFilesCreated = 0;
        try {
            while (numFilesCreated < fileNum) {

                /*
                 * create an "artist"
                 */
                String artist = "artist " + getRandomString();
                File artistDir = new File(baseDir, artist);
                artistDir.mkdir();
                int numAlbums = rand.nextInt(20);
                for (int i = 0; i < numAlbums; i++) {

                    String album = "album " + getRandomString();
                    File albumDir = new File(artistDir, album);
                    albumDir.mkdir();
                    int numSongs = 10 + rand.nextInt(10);
                    for (int j = 0; j < numSongs; j++) {
                        numFilesCreated++;
                        String songTitle = j + " - " + getRandomString() + ".mp3";
                        File song = new File(albumDir, songTitle);
                        long time = System.currentTimeMillis();
                        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(
                                song));
                        System.out.print("creating file " + numFilesCreated + "/" + fileNum + ":\t"
                                + song.getAbsolutePath() + "\t");
                        int total = 1024 * 1024 + rand.nextInt(4 * 1024 * 1024);
                        int written = 0;
                        int toWrite;
                        while ((toWrite = Math.min(buffer.length, total - written)) > 0) {
                            rand.nextBytes(buffer);
                            out.write(buffer, 0, toWrite);
                            written += toWrite;
                        }
                        out.close();
                        long t = System.currentTimeMillis() - time;
                        System.out.println("wrote " + total / 1024 + " Kb in " + t + "ms ("
                                + (total / (1000 * t)) + " MB/s)");
                    }
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private static String getRandomString() {
        long r = rand.nextLong();
        return Long.toHexString(r);
    }

}
