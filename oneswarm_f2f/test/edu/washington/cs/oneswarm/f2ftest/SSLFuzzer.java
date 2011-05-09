package edu.washington.cs.oneswarm.f2ftest;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;

import edu.washington.cs.oneswarm.test_tools.StreamReader;

public class SSLFuzzer {
    // msg_type is always 1
    private final static byte msg_type = 1;
    // SSL 3
    private final static byte major_version = 3;
    private final static byte minor_version = 0;
    // the cipher length (2 bytes) is always divisible by 3
    private final static byte[] cipher_spec_length = { 0, 3 };
    // session id is either of length 0 or of length 16
    private final static byte[] session_id_length = { 0, 0 };
    // challenge length is always 32
    private final static byte[] challenge_length = { 0, 32 };

    public static final byte[] SSL_HANDSHAKE_BYTES = new byte[] { 0, 0, msg_type, major_version,
            minor_version, cipher_spec_length[0], cipher_spec_length[1], session_id_length[0],
            session_id_length[1], challenge_length[0], challenge_length[1] };

    /**
     * Will connect to an oneswarm instance and send random crap to the ssl
     * port, but it will format the initial bytes in such a way that is passes
     * the ssl packet matcher
     * 
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        Random random = new Random();

        try {
            int NUM = 5;
            for (int i = 0; i < NUM; i++) {
                byte[] randomBytes = new byte[1024 * 1024];

                random.nextBytes(randomBytes);

                Socket s = new Socket(args[0], Integer.parseInt(args[1]));
                System.out.println("connected, iter: " + (i + 1) + "/" + NUM);

                InputStream inputStream = s.getInputStream();
                StreamReader streamReader = new StreamReader(inputStream);

                DataOutputStream outputStream = new DataOutputStream(s.getOutputStream());
                outputStream.write(SSL_HANDSHAKE_BYTES);
                outputStream.write(randomBytes);
                outputStream.close();
                System.out.println("done writing bytes");

                System.out.println("read: '" + new String(streamReader.read()) + "'");
            }
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
