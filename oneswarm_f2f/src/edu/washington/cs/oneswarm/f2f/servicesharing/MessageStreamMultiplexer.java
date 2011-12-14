package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
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
        numbers.add(outstandingMessages.get(message.getSequenceNumber()));
        while (payload.remaining(ss) > 0) {
            numbers.add(outstandingMessages.get(payload.getInt(ss)));
        }

        for (SequenceNumber s : numbers) {
            this.channels.get(s.getChannel()).forgetMessage(s);
            channelOutstanding.remove(s.getChannel(), s);
            outstandingMessages.remove(s.getNum());
        }
    }

    public SequenceNumber nextMsg(ServiceChannelEndpoint channel) {
        int num = next++;
        int chan = channel.getChannelId()[0];
        SequenceNumber n = new SequenceNumber(num, chan);
        outstandingMessages.put(num, n);
        channelOutstanding.put(chan, n);
        return n;
    }

    public boolean hasOutstanding(ServiceChannelEndpoint channel) {
        return channelOutstanding.containsKey(channel.getChannelId()[0]);
    }

    public Collection<DirectByteBuffer> getOutstanding(final ServiceChannelEndpoint channel) {
        Set<SequenceNumber> outstanding = channelOutstanding.get(channel.getChannelId()[0]);
        return Collections2.transform(outstanding,
                new Function<SequenceNumber, DirectByteBuffer>() {

                    @Override
                    public DirectByteBuffer apply(SequenceNumber s) {
                        return channel.getMessage(s);
                    }
        });
    }

    public void removeChannel(ServiceChannelEndpoint channel) {
        channels.remove(channel.getChannelId()[0]);
        channelOutstanding.removeAll(channel.getChannelId()[0]);
    }
}
