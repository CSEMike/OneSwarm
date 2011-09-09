package edu.uw.cse.netlab.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ByteManip {
    public static String ntoa(int ip) {
        long a = (ip & 0xFF000000) >>> 24;
        long b = (ip & 0x00FF0000) >>> 16;
        long c = (ip & 0x0000FF00) >>> 8;
        long d = (ip & 0x000000FF) >>> 0;

        return a + "." + b + "." + c + "." + d;
    }

    public static int aton(String ip) {
        String[] toks = ip.split("\\.");
        int a = Integer.parseInt(toks[0]);
        int b = Integer.parseInt(toks[1]);
        int c = Integer.parseInt(toks[2]);
        int d = Integer.parseInt(toks[3]);

        return (int) ((a << 24) | (b << 16) | (c << 8) | d);
    }

    public static long btol(byte[] b) {
        long l = 0;
        for (int i = 0; i < 8; i++) {
            l <<= 8;
            l ^= (long) b[i] & 0xFF;
        }
        return l;
    }

    public static byte[] ltob(long l) {
        byte[] b = new byte[8];
        for (int i = 0; i < 8; i++) {
            b[7 - i] = (byte) (l >>> (i * 8));
        }
        return b;
    }

    public static final int btoi(byte[] b) {
        return (b[0] << 24) + ((b[1] & 0xFF) << 16) + ((b[2] & 0xFF) << 8) + (b[3] & 0xFF);
    }

    public static final byte[] itob(int value) {
        return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8),
                (byte) value };
    }

    public static byte[] objectToBytes(Object s) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(s);
        oos.close();
        return baos.toByteArray();
    }

    public static Object objectFromBytes(byte[] bytes) throws IOException {
        ByteArrayInputStream baos = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(baos);
        try {
            return ois.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static final void main(String[] args) throws Exception {
        String foo = "foo";
        while (true) {
            objectFromBytes(objectToBytes(foo));
            System.gc();
        }
    }
}
