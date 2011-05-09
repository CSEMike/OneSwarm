package edu.washington.cs.oneswarm.f2f.messaging;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class SearchResponseSerializeTester {

    public static void main(String[] args) {
        try {
            Random r = new Random();
            int NUM = 10000;
            Set<Integer> randNum = new HashSet<Integer>(NUM);

            final byte VERSION = 0;
            for (int i = VERSION; i < NUM; i++) {
                int search = new Random().nextInt();
                randNum.add(search);
                int channelId = new Random().nextInt();
                randNum.add(channelId);
                int path = new Random().nextInt();
                randNum.add(path);

                OSF2FHashSearchResp resp = new OSF2FHashSearchResp(VERSION, search, channelId, path);

                final DirectByteBuffer[] data = resp.getData();

                OSF2FHashSearchResp resp2 = (OSF2FHashSearchResp) (new OSF2FHashSearchResp(VERSION,
                        0, 0, 0).deserialize(data[0], VERSION));

                boolean error = false;
                if (resp.getChannelID() != resp2.getChannelID()) {
                    error = true;
                } else if (resp.getSearchID() != resp2.getSearchID()) {
                    error = true;
                } else if (resp.getPathID() != resp2.getPathID()) {
                    error = true;
                }

                if (error) {
                    System.out.println("error: ");
                    System.out.println(resp.getDescription());
                    System.out.println(resp2.getDescription());
                    System.exit(0);
                }
            }
            System.out.println("rand dist: " + randNum.size() + "/" + NUM);
        } catch (MessageException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
