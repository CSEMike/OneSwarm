package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

/**
 * Multiplexes a stream of data, and tracks what is in
 * transit across channels.
 * 
 * @author willscott
 * 
 */
public class MessageStreamMultiplexer {
    public final static Logger logger = Logger.getLogger(MessageStreamMultiplexer.class.getName());
    private Integer next;
    private final short flow;
    private final HashMap<Integer, ServiceChannelEndpoint> channels;

    private final HashMap<Integer, SequenceNumber> outstandingMessages;
    private final HashMap<Integer, Set<SequenceNumber>> channelOutstanding;
    private final static byte ss = 44;

    public MessageStreamMultiplexer(short flow) {
        this.channels = new HashMap<Integer, ServiceChannelEndpoint>();
        this.outstandingMessages = new HashMap<Integer, SequenceNumber>();
        this.channelOutstanding = new HashMap<Integer, Set<SequenceNumber>>();
        this.flow = flow;
        next = 0;
    }

    public void addChannel(ServiceChannelEndpoint s) {
        this.channels.put(s.getChannelId(), s);
        this.channelOutstanding.put(s.getChannelId(), new HashSet<SequenceNumber>());
    }

    public int onAck(OSF2FServiceDataMsg message) {
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
                    channelOutstanding.get(channelId).remove(seq);
                    seq.removeChannel(channelId);
                }
            }
            if (seq.getChannels().size() == 0) {
                outstandingMessages.remove(seq);
            }
        }
        for (Integer num : retransmissions) {
            logger.info("Non outstanding packet acked: " + num);
        }
        return numbers.size();
    }

    public SequenceNumber nextMsg() {
        int num = next++;
        SequenceNumber n = new SequenceNumber(num, flow);
        outstandingMessages.put(num, n);
        return n;
    }

    public void sendMsg(SequenceNumber msg, ServiceChannelEndpoint channel) {
        int channelId = channel.getChannelId();
        msg.addChannel(channelId);
        channelOutstanding.get(channelId).add(msg);
    }

    public boolean hasOutstanding(ServiceChannelEndpoint channel) {
        return channelOutstanding.containsKey(channel.getChannelId());
    }

    public Map<SequenceNumber, DirectByteBuffer> getOutstanding(final ServiceChannelEndpoint channel) {
        Set<SequenceNumber> outstanding = channelOutstanding.get(channel.getChannelId());
        HashMap<SequenceNumber, DirectByteBuffer> mapping = new HashMap<SequenceNumber, DirectByteBuffer>();
        for (SequenceNumber s : outstanding) {
            DirectByteBuffer msg = channel.getMessage(s);
            if (msg != null) {
                mapping.put(s, msg);
            }
        }
        return mapping;
    }

    public void removeChannel(ServiceChannelEndpoint channel) {
        int channelId = channel.getChannelId();
        channels.remove(channelId);
        Set<SequenceNumber> inFlight = channelOutstanding.get(channelId);
        if (inFlight != null) {
            for (SequenceNumber s : inFlight) {
                s.removeChannel(channelId);
            }
            channelOutstanding.remove(channelId);
        }
    }
}
