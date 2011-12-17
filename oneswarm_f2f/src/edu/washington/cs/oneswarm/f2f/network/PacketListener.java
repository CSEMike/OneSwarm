package edu.washington.cs.oneswarm.f2f.network;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelMsg;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearchResp;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FSearchResp;

public interface PacketListener {

    public boolean packetReadyForAzureusQueue(OSF2FChannelMsg message);

    public void packetAddedToForwardQueue(FriendConnection source, FriendConnection destination,
            OSF2FSearch sourceMessage, OSF2FSearchResp setupMessage, boolean searcherSide,
            OSF2FChannelMsg message);

    public void packetAddedToTransportQueue(FriendConnection destination,
            OSF2FSearch sourceMessage, OSF2FSearchResp setupMessage, boolean searcherSide,
            OSF2FChannelMsg message);

    public void packetArrivedAtFinalDestination(FriendConnection source, OSF2FHashSearch search,
            OSF2FHashSearchResp response, OSF2FChannelDataMsg msg, boolean searcherSide);

}
