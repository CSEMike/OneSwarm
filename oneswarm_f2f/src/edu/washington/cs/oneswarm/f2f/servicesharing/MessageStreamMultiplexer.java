package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.util.Collection;
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
    private final HashMultimap<Integer, SequenceNumber> channelMap;

    public MessageStreamMultiplexer() {
        this.channelMap = HashMultimap.create();
        next = 0;
        numChannels = 0;
    }

    public int addChannel() {
        return numChannels++;
    }

    public void onAck(SequenceNumber[] n) {
        // channelMap.remove(channel.getChannelId()[0], n);
    }

    public SequenceNumber nextMsg(ServiceChannelEndpoint channel) {
        SequenceNumber n = new SequenceNumber(next++);
        channelMap.put(channel.getChannelId()[0], n);
        return n;
    }

    public boolean hasOutstanding(ServiceChannelEndpoint channel) {
        return channelMap.containsKey(channel.getChannelId()[0]);
    }

    public Collection<DirectByteBuffer> getOutstanding(final ServiceChannelEndpoint channel) {
        Set<SequenceNumber> outstanding = channelMap.get(channel.getChannelId()[0]);
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
        channelMap.removeAll(channel.getChannelId()[0]);
    }
}
