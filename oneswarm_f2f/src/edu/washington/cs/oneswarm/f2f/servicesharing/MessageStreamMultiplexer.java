package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
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
    private Integer numChannels;
    private final HashMultimap<ServiceChannelEndpoint, SequenceNumber> channelMap;
    private final static byte ss = 44;

    public MessageStreamMultiplexer() {
        this.channelMap = HashMultimap.create();
        next = 0;
        numChannels = 0;
    }

    public int addChannel(ServiceChannelEndpoint s) {
        return numChannels++;
    }

    public void onAck(OSF2FServiceDataMsg message) {
        // Parse acknowledged messages into an integer array.
        DirectByteBuffer payload = message.getPayload();
        HashSet<Integer> numbers = new HashSet<Integer>();
        numbers.add(message.getSequenceNumber());
        while (payload.remaining(ss) > 0) {
            numbers.add(payload.getInt(ss));
        }

        // Remove + forget them.
        for (Entry<ServiceChannelEndpoint, SequenceNumber> s : channelMap.entries()) {
            if (numbers.contains(s.getValue().getNum())) {
                s.getKey().forgetMessage(s.getValue());
                channelMap.remove(s.getKey(), s.getValue());
            }
        }
    }

    public SequenceNumber nextMsg(ServiceChannelEndpoint channel) {
        SequenceNumber n = new SequenceNumber(next++);
        channelMap.put(channel, n);
        return n;
    }

    public boolean hasOutstanding(ServiceChannelEndpoint channel) {
        return channelMap.containsKey(channel);
    }

    public Collection<DirectByteBuffer> getOutstanding(final ServiceChannelEndpoint channel) {
        Set<SequenceNumber> outstanding = channelMap.get(channel);
        return Collections2.transform(outstanding,
                new Function<SequenceNumber, DirectByteBuffer>() {

                    @Override
                    public DirectByteBuffer apply(SequenceNumber s) {
                        return channel.getMessage(s);
                    }
        });
    }

    public void removeChannel(ServiceChannelEndpoint channel) {
        numChannels--;
        channelMap.removeAll(channel);
    }
}
