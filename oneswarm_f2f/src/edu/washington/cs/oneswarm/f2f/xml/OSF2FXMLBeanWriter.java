/**
 * 
 */
package edu.washington.cs.oneswarm.f2f.xml;

import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;

import edu.washington.cs.oneswarm.f2f.friends.FriendManager;

public class OSF2FXMLBeanWriter<T> implements Runnable {

    private final static Logger logger = Logger.getLogger(OSF2FXMLBeanWriter.class.getName());
    private final static int NUM_BACKUP_FILES_TO_KEEP = 10;
    private final static int KEEP_EVERY_X_BACKUP_FILE = 20;
    private static final File OSF2F_DIR = FriendManager.OSF2F_DIR;
    private boolean allowDecreasedSize;
    private final String filename;
    private final Semaphore lock;
    private final boolean makeBackup;

    private final T[] objects;

    public OSF2FXMLBeanWriter(T[] objects, Semaphore lock, String filename, boolean makeBackup,
            boolean allowDecreasedSize) {
        this.makeBackup = makeBackup;
        this.allowDecreasedSize = allowDecreasedSize;
        this.objects = objects;
        this.lock = lock;
        this.filename = filename;
    }

    public void run() {

        try {
            if (!OSF2F_DIR.isDirectory()) {
                boolean success = OSF2F_DIR.mkdirs();
                if (!success) {
                    System.err.println("ERROR: failed to create dir: "
                            + OSF2F_DIR.getAbsolutePath());
                }
            }
            logger.finest("waiting for semaphore");
            lock.acquire();
            logger.finest("got semaphore");

            File f = File.createTempFile("osf2f_", filename);
            f.deleteOnExit();
            // Serialize object into XML
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
            XMLEncoder encoder = new XMLEncoder(out);

            encoder.writeObject(objects);
            encoder.close();
            out.close();
            logger.finer("wrote " + objects.length + " objects to disk: " + f.getAbsolutePath());

            File oldFile = new File(OSF2F_DIR, filename);
            if (oldFile.isFile() && makeBackup) {
                cleanBackupFiles(OSF2F_DIR);
                java.text.DecimalFormat nft = new java.text.DecimalFormat("#0000.###");
                nft.setDecimalSeparatorAlwaysShown(false);

                int previousBackupFile = getLargestId(OSF2F_DIR);
                File backupFile = new File(oldFile.getCanonicalPath() + ".backup"
                        + nft.format(previousBackupFile + 1));
                logger.finest("moving: " + oldFile.getAbsolutePath() + " to "
                        + backupFile.getAbsolutePath());
                renameOrCopy(oldFile, backupFile);
                oldFile = new File(OSF2F_DIR, filename);
            }

            if (System.getProperty("oneswarm.experimental.config.file") != null) {
                allowDecreasedSize = true;
            }

            // check if we wrote everything out completely or if an error
            // occurred
            if (oldFile.length() < f.length() + 1000 || allowDecreasedSize) {
                logger.finest("moving: " + f.getAbsolutePath() + " to " + oldFile.getAbsolutePath());
                renameOrCopy(f, oldFile);
            } else {
                (new Exception()).printStackTrace();
                System.err
                        .println("the new version of the friends file is significantly smaller than the old version. Not overwriting. Old: "
                                + oldFile.length() + " New: " + f.length());
            }

        } catch (IOException e) {
            Debug.out("unable to write friend file to disk", e);
        }

        catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            lock.release();
        }
    }

    private void cleanBackupFiles(File folder) {
        String[] backupFiles = getBackupFilesList(folder);
        for (String f : backupFiles) {
            logger.finest(f);
        }

        logger.finest("deleting the oldest ones, keeping " + NUM_BACKUP_FILES_TO_KEEP);
        for (int i = 0; i < backupFiles.length - NUM_BACKUP_FILES_TO_KEEP; i++) {
            String file = backupFiles[i];
            if (!(getIdOfBackupFile(file) % KEEP_EVERY_X_BACKUP_FILE == 0)) {
                new File(folder, file).delete();
                logger.finer("deleting: " + file);
            }
        }
    }

    private String[] getBackupFilesList(File folder) {
        String[] backupFiles = folder.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(filename + ".backup");
            }
        });
        sortBackupFilesById(backupFiles);
        return backupFiles;
    }

    private int getIdOfBackupFile(String fn) {
        try {
            return Integer.parseInt(fn.replace(filename + ".backup", ""));
        } catch (NumberFormatException e) {
            logger.fine("got number format exception for: " + fn);
        }
        return -1;
    }

    private int getLargestId(File folder) {
        String[] filenames = getBackupFilesList(folder);
        if (filenames.length == 0) {
            return 0;
        }
        String filename = filenames[filenames.length - 1];
        return getIdOfBackupFile(filename);
    }

    /*
     * for testing friend file backups/deletes
     */
    public static void main(String[] args) {

        try {
            File friendsDir = new File("/tmp/friendsTest/");
            friendsDir.mkdirs();

            for (int i = 9000; i < 10100; i++) {
                java.text.DecimalFormat nft = new java.text.DecimalFormat("#0000.###");
                nft.setDecimalSeparatorAlwaysShown(false);

                File backupFile = new File(friendsDir, "osf2f.friends.backup" + nft.format(i));
                backupFile.createNewFile();
            }
            // logger.finest("largest id: " + getLargestId(friendsDir));
            // cleanBackupFiles(friendsDir);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void renameOrCopy(File toRenameOrCopy, File dst) throws IOException {
        if (toRenameOrCopy.renameTo(dst) == false) {
            if (FileUtil.copyFile(toRenameOrCopy, dst) == false) {
                throw new IOException("couldn't rename or copy: " + toRenameOrCopy.toString()
                        + " to " + dst.toString());
            }
            toRenameOrCopy.delete();
        }
    }

    private void sortBackupFilesById(String[] filenames) {
        Arrays.sort(filenames, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return getIdOf(o1) - getIdOf(o2);
            }

            private int getIdOf(String fn) {
                try {
                    int id = Integer.parseInt(fn.replace(filename + ".backup", ""));
                    return id;
                } catch (NumberFormatException e) {
                    logger.fine("got number format exception for: " + fn);
                }
                return -1;
            }
        });
    }
}