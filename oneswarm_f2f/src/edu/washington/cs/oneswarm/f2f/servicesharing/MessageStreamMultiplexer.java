package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.google.common.collect.HashMultimap;

/**
 * Multiplexes a stream of data, and tracks what is in
 * transit on each channel.
 * 
 * @author willscott
 * 
 */
public class MessageStreamMultiplexer {
    private Integer next;
    private final HashMap<Integer, ServiceChannelEndpoint> channels;

    private final HashMap<Integer, SequenceNumber> outstandingMessages;
    private final HashMultimap<Integer, SequenceNumber> channelOutstanding;
    private final static byte ss = 44;

    public MessageStreamMultiplexer() {
        this.channels = new HashMap<Integer, ServiceChannelEndpoint>();
        this.outstandingMessages = new HashMap<Integer, SequenceNumber>();
        this.channelOutstanding = HashMultimap.create();
        next = 0;
    }

    public void addChannel(ServiceChannelEndpoint s) {
        this.channels.put(s.getChannelId()[0], s);
    }

    public void onAck(OSF2FServiceDataMsg message) {
        // Parse acknowledged messages
        DirectByteBuffer payload = message.getPayload();
        HashSet<SequenceNumber> numbers = new HashSet<SequenceNumber>();
        ArrayList<Integer> retransmissions = new ArrayList<Integer>();
        SequenceNumber s = outstandingMessages.get(message.getSequenceNumber());
        if (s != null) {
            numbers.add(s);
        } else {
            retransmissions.add(message.getSequenceNumber());
        }
        while (payload.remaining(ss) > 0) {
            int num = payload.getInt(ss);
            s = outstandingMessages.get(num);
            if (s != null) {
                numbers.add(s);
            } else {
                retransmissions.add(num);
            }
        }

        for (SequenceNumber seq : numbers) {
            seq.ack();
            for (Integer channelId : seq.getChannels()) {
                if (this.channels.get(channelId).forgetMessage(seq)) {
                    channelOutstanding.remove(channelId, seq);
                    seq.removeChannel(channelId);
                }
            }
            if (seq.getChannels().size() == 0) {
                outstandingMessages.remove(seq);
            }
        }
        for (Integer num : retransmissions) {
            System.out.println("Non outstanding packet acked: " + num);
        }
    }

    public SequenceNumber nextMsg() {
        int num = next++;
        SequenceNumber n = new SequenceNumber(num);
        outstandingMessages.put(num, n);
        return n;
    }

    public void sendMsg(SequenceNumber msg, ServiceChannelEndpoint channel) {
        int channelId = channel.getChannelId()[0];
        msg.addChannel(channelId);
        channelOutstanding.put(channelId, msg);
    }

    public boolean hasOutstanding(ServiceChannelEndpoint channel) {
        return channelOutstanding.containsKey(channel.getChannelId()[0]);
    }

    public Map<SequenceNumber, DirectByteBuffer> getOutstanding(final ServiceChannelEndpoint channel) {
        Set<SequenceNumber> outstanding = channelOutstanding.get(channel.getChannelId()[0]);
        HashMap<SequenceNumber, DirectByteBuffer> mapping = new HashMap<SequenceNumber, DirectByteBuffer>();
        for (SequenceNumber s : outstanding) {
            mapping.put(s, channel.getMessage(s));
        }
        return mapping;
    }

    public void removeChannel(ServiceChannelEndpoint channel) {
        int channelId = channel.getChannelId()[0];
        channels.remove(channelId);
        for (SequenceNumber s : channelOutstanding.get(channelId)) {
            s.removeChannel(channelId);
        }
        channelOutstanding.removeAll(channelId);
    }
}
