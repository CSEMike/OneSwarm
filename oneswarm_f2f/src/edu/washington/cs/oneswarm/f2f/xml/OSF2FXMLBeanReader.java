/**
 * 
 */
package edu.washington.cs.oneswarm.f2f.xml;

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.Debug;

import edu.washington.cs.oneswarm.f2f.friends.FriendManager;

public class OSF2FXMLBeanReader<T> implements Runnable {
    public static interface OSF2FXMLBeanReaderCallback<T> {
        public void readObject(T object);

        public void completed();
    }

    private final static Logger logger = Logger.getLogger(OSF2FXMLBeanReader.class.getName());
    private final Semaphore lock;
    private final OSF2FXMLBeanReaderCallback<T> cb;
    private final ClassLoader cl;
    private final Class<T> c;
    private final String filename;

    public OSF2FXMLBeanReader(ClassLoader classloader, Class<T> c, String filename, Semaphore lock,
            OSF2FXMLBeanReaderCallback<T> callback) {
        this.lock = lock;
        this.cb = callback;
        this.cl = classloader;
        this.c = c;
        this.filename = filename;
    }

    @Override
    public void run() {
        try {

            boolean classesLoaded = false;
            while (!classesLoaded) {
                try {
                    logger.fine("trying to load objects from disk: " + filename);
                    cl.loadClass(c.getCanonicalName());
                    classesLoaded = true;
                } catch (ClassNotFoundException e) {
                    Debug.out("cannot load class: " + c.getCanonicalName() + ", trying to sleep...");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            }
            logger.finer("reading objects from disk");
            File friendsFile = new File(FriendManager.OSF2F_DIR, filename);
            if (friendsFile.isFile()) {
                logger.finer("reading from file '" + friendsFile + "'");
                T[] objects = readFriendsFromFile(friendsFile);

                for (T o : objects) {
                    cb.readObject(o);
                }
                logger.fine("read " + objects.length + " objects");
            } else {
                logger.warning("File not found: " + friendsFile.getPath());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ClassCastException e) {
            e.printStackTrace();
        } finally {
            logger.fine("releasing lock");
            lock.release();
            cb.completed();
        }
    }

    @SuppressWarnings("unchecked")
    private T[] readFriendsFromFile(File friendsFile) throws FileNotFoundException {
        XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(
                new FileInputStream(friendsFile)), this, new ExceptionListener() {
            @Override
            public void exceptionThrown(Exception e) {
                e.printStackTrace();
            }
        }, cl);

        T[] o = (T[]) new Object[0];
        try {
            o = (T[]) decoder.readObject();
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            logger.severe("Error parsing friends file (corrupt?): " + e.toString());
        }
        decoder.close();
        return o;
    }
}