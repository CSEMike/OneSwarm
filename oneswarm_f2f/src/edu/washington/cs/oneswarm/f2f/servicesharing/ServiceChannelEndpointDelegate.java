package edu.washington.cs.oneswarm.f2f.servicesharing;

public interface ServiceChannelEndpointDelegate {
    public void channelDidConnect(ServiceChannelEndpoint sender);

    public void channelDidClose(ServiceChannelEndpoint sender);

    public void channelIsReady(ServiceChannelEndpoint sender);

    public boolean channelGotMessage(ServiceChannelEndpoint sender, OSF2FServiceDataMsg msg);

    public boolean writesMessages();
}
