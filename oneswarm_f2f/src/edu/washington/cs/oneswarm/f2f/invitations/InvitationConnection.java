package edu.washington.cs.oneswarm.f2f.invitations;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.util.logging.Logger;

import org.apache.xerces.impl.dv.util.Base64;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue.MessageQueueListener;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkConnection.ConnectionListener;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.osssl.OneSwarmSslTransportHelperFilterStream;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProtocolEndpointTCP;
import com.aelitis.azureus.core.peermanager.messaging.Message;

import edu.washington.cs.oneswarm.f2f.FriendInvitation;
import edu.washington.cs.oneswarm.f2f.FriendInvitation.Status;
import edu.washington.cs.oneswarm.f2f.Log;
import edu.washington.cs.oneswarm.f2f.invitations.InvitationManager.AuthCallback;
import edu.washington.cs.oneswarm.f2f.messaging.invitation.OSF2FAuthHandshake;
import edu.washington.cs.oneswarm.f2f.messaging.invitation.OSF2FAuthMessage;
import edu.washington.cs.oneswarm.f2f.messaging.invitation.OSF2FAuthMessageDecoder;
import edu.washington.cs.oneswarm.f2f.messaging.invitation.OSF2FAuthMessageEncoder;
import edu.washington.cs.oneswarm.f2f.messaging.invitation.OSF2FAuthRequest;
import edu.washington.cs.oneswarm.f2f.messaging.invitation.OSF2FAuthRequest.AuthType;
import edu.washington.cs.oneswarm.f2f.messaging.invitation.OSF2FAuthResponse;
import edu.washington.cs.oneswarm.f2f.messaging.invitation.OSF2FAuthStatus;

public class InvitationConnection {
    /**
     * Protocol: A->B invite + (optional pin)
     * 
     * B->DHT lookup(sha1(invite[0-19]))
     * 
     * DHT->B xor(sha1(invite[10-19]),ip:port)
     * 
     * B->A connect ip:port
     * 
     * B: verify bytes 0-9 of sha1(remote public key) with bytes[0-9] or invite
     * 
     * B->A handshake
     * 
     * A->B handshake
     * 
     * A->B request invite
     * 
     * A: look up invite code, if valid, send STATUS_INVITE_KEY_OK
     * 
     * CASE: if no additional security:
     * 
     * A: add B as friend
     * 
     * A->B STATUS_INVITE_KEY_OK
     * 
     * B: add A as friend
     * 
     * 
     * CASE: pin security, goal: Pin is sent in separate medium. A needs to
     * prove to B that it knows the PIN. B needs to prove to A that it knows the
     * PIN. Because the limited length of the pin they chain sha1 the pin to
     * make sure that brute force takes longer time.
     * 
     * A->B STATUS_INVITE_KEY_OK
     * 
     * B->A PIN_REQUEST + nounce
     * 
     * A->B hash=nounce+pin; 100x: hash=sha1(hash+nounce); send hash
     * 
     * A->B PIN_REQUEST + NOUNCE2
     * 
     * B now has 15s to compute the hash and send it back, otherwise the invite
     * is marked as expired, the 100x sha1 computation takes 1s (on my laptop)
     * 
     * B: verify hash
     * 
     * B->A hash=nounce2+pin; 100x: hash=sha1(hash+nounce2); send hash
     * 
     * A: verify hash, add B as friend
     * 
     * A->B STATUS_INVITE_KEY_OK
     * 
     * B: add A as friend
     * 
     * 
     * SECURITY DISCUSSION
     * 
     * NO PIN:
     * 
     * Case: E intercepts invite code:
     * 
     * E can use invite code to become friends with A
     * 
     * Case: E replaces invite code:
     * 
     * E can modify the code so B connects to and befriends E instead of A
     * 
     * Case: E spoofes invite to B:
     * 
     * B (if accepting the invite) will become friends with E
     * 
     * WITH PIN:
     * 
     * Case: E intercepts invite code:
     * 
     * E will connect to A, A will prove that it knows the pin. E will not have
     * enought time to brute force the pin before invite is expired
     * 
     * Case: E replaces invite code:
     * 
     * B will connect to E, E will not be able to prove that it knows the PIN
     * 
     * Case: E spoofes invite to B:
     * 
     * If B only accepts invites with PIN; E will have to know a separate way to
     * send the PIN to B while still pretending to be A
     * 
     * 
     * INVITE CODE FORMAT: bytes[0-9] is the sha1 of A's public key, this is
     * used to verify that the remote host is the expected one
     * 
     * bytes[0-19] are used to calculate the position in the DHT where the
     * ip:port (dht key is sha1 of bytes[0-19]). The value in the dht is the
     * ip:port xord with the sha1 of bytes[10-19]
     * 
     * bytes[20-28] are left untouched to make sure that at least 9 bytes of the
     * invite can not be brute forced offline byte 29 is used for flags
     * 
     */

    private static Logger logger = Logger.getLogger(InvitationConnection.class.getName());

    private Boolean redeeming = null;
    private final NetworkConnection connection;
    private final long connectionTime;

    private final AuthCallback callback;

    // the invitation corresponding to this
    private FriendInvitation invitation;

    private final boolean remoteSideAuthenticated = false;

    private enum ConnectionType {
        UNKNOWN, INVITING_INCOMING, INVITING_OUTGOING, REDEEMING_INCOMING, REDEEMING_OUTGOING;
    }

    private enum ProtocolStateInviting {
        NONE, HANDSHAKING, HANDSHAKE_COMPLETED, INVITE_CODE_REQUEST_SENT, INVITE_CODE_RECEIVED, AUTHENTICATED;
    }

    private enum ProtocolStateRedeeming {
        NONE, HANDSHAKING, HANDSHAKE_COMPLETED, INVITE_CODE_REQUEST_RECEIVED, INVITE_CODE_SENT, AUTHENTICATED;
    }

    private ProtocolStateInviting protocolStateInviting = ProtocolStateInviting.NONE;
    private ProtocolStateRedeeming protocolStateRedeeming = ProtocolStateRedeeming.NONE;
    private ConnectionType connectionType = ConnectionType.UNKNOWN;

    /**
     * outgoing
     * 
     * @param _manager
     * @param remoteFriendAddr
     * @param invitation
     * @param _remoteFriend
     */
    public InvitationConnection(ConnectionEndpoint remoteFriendAddr,
            final FriendInvitation invitation, AuthCallback callback) {
        if (invitation.isCreatedLocally()) {
            connectionType = ConnectionType.INVITING_OUTGOING;
            redeeming = false;
        } else {
            connectionType = ConnectionType.REDEEMING_OUTGOING;
            redeeming = true;
        }

        remoteFriendAddr
                .addProtocol(new ProtocolEndpointTCP(remoteFriendAddr.getNotionalAddress()));
        this.callback = callback;
        this.invitation = invitation;

        final byte[][] sharedSecret = new byte[2][0];
        sharedSecret[0] = OneSwarmSslTransportHelperFilterStream.SHARED_SECRET_FOR_SSL_STRING
                .getBytes();
        sharedSecret[1] = OneSwarmSslTransportHelperFilterStream.ANY_KEY_ACCEPTED_BYTES;
        this.connectionTime = System.currentTimeMillis();
        logger.fine("making outgoing connection to:\n" + remoteFriendAddr);
        this.connection = NetworkManager.getSingleton().createConnection(remoteFriendAddr,
                new OSF2FAuthMessageEncoder(), new OSF2FAuthMessageDecoder(), true, false,
                sharedSecret);
        // this.hash = getHashOf(remoteFriend.getPublicKey(),
        // this.getRemoteIp(), this.getRemotePort());
        this.connection.connect(null, false, new ConnectionListener() {
            @Override
            public void connectFailure(Throwable failure_msg) {
                logger.fine(connection + " : connect error: " + failure_msg.getMessage());
                close();
            }

            @Override
            public void connectStarted() {
            }

            @Override
            public void connectSuccess(ByteBuffer remaining_initial_data) {

                remoteKey = sharedSecret[1];

                if (redeeming) {
                    /*
                     * check that the remote public key is correct if this is a
                     * redeemed invitation
                     */
                    if (!invitation.pubKeyMatch(remoteKey)) {
                        // strange, we connected to the wrong place, close
                        sendError(OSF2FAuthStatus.STATUS_INVITE_ERR_PUB_KEY);
                        close("connected to the wrong key: remote public key="
                                + Base64.encode(remoteKey));
                        return;
                    }
                    protocolStateRedeeming = ProtocolStateRedeeming.HANDSHAKING;
                } else {
                    /*
                     * if this is an inviting outgoing connection we don't know
                     * the expected remote key, we won't be able to identify it
                     * until later
                     */
                    protocolStateInviting = ProtocolStateInviting.HANDSHAKING;
                }

                connection.getIncomingMessageQueue().registerQueueListener(
                        new IncomingQueueListener());
                NetworkManager.getSingleton().startTransferProcessing(connection);
                sendHandshake();
                enableFastMessageProcessing(true);
                logger.fine("made connection to: " + Base64.encode(remoteKey));
            }

            @Override
            public void exceptionThrown(Throwable error) {
                connectionException(error);
            }

            @Override
            public String getDescription() {
                return "connection listener: OSF2F session, outgoing";
            }
        });
    }

    private void sendError(int code) {
        sendMessage(new OSF2FAuthStatus(OSF2FAuthMessage.CURRENT_VERSION, code));
    }

    public int getRemotePort() {
        return connection.getEndpoint().getNotionalAddress().getPort();
    }

    private void connectionException(Throwable error) {
        Log.log(LogEvent.LT_WARNING,
                "got exception in " + "OS Auth session (" + connection + "/" + getRemoteIp()
                        + ") disconnecting: " + error.getMessage() + " (from: " + error.toString()
                        + ")");
        String friendLogMessage = error.getMessage();
        boolean expectedError = false;
        if (friendLogMessage.startsWith("transport closed")) {
            expectedError = true;
        } else if (friendLogMessage.startsWith("Connection reset by peer")) {
            expectedError = true;
        } else if (friendLogMessage
                .startsWith("An existing connection was forcibly closed by the remote host")) {
            expectedError = true;
        }

        if (!expectedError) {
            StringWriter st = new StringWriter();
            error.printStackTrace(new PrintWriter(st));
            String stackTrace = st.toString();
            friendLogMessage += "\n" + stackTrace;
        }
        logger.fine("got exception: " + friendLogMessage);
        close();
    }

    private byte[] remoteKey;

    /**
     * creates a new incoming connection
     * 
     * @param _connection
     * @param _remoteFriend
     */
    public InvitationConnection(byte[] remoteKey, NetworkConnection _connection,
            AuthCallback callback) {

        this.remoteKey = remoteKey;
        this.callback = callback;

        this.connection = _connection;
        this.connectionTime = System.currentTimeMillis();
        connection.getIncomingMessageQueue().registerQueueListener(new IncomingQueueListener());

        connection.connect(true, new ConnectionListener() {
            @Override
            public void connectFailure(Throwable failure_msg) {
                logger.fine(connection + " : connect error: " + failure_msg.getMessage());
                close();
            }

            @Override
            public void connectStarted() {
                // nop
            }

            @Override
            public void connectSuccess(ByteBuffer remaining_initial_data) {
                logger.fine("incoming auth connection from: " + getRemoteIp().getHostAddress());
                logger.fine("remote key:" + Base64.encode(InvitationConnection.this.remoteKey));
                /*
                 * check if we recognize the remote key, in that case this is an
                 * incoming redeeming connection
                 */
                FriendInvitation i = InvitationConnection.this.callback
                        .getInvitationFromPublicKey(InvitationConnection.this.remoteKey);
                if (i != null) {
                    redeeming = true;
                    connectionType = ConnectionType.REDEEMING_INCOMING;
                    invitation = i;
                    protocolStateRedeeming = ProtocolStateRedeeming.HANDSHAKING;
                } else {
                    redeeming = false;
                    connectionType = ConnectionType.INVITING_INCOMING;
                    /*
                     * if this is an inviting incoming connection we don't know
                     * the expected remote key, we won't be able to identify it
                     * until later
                     */
                    protocolStateInviting = ProtocolStateInviting.HANDSHAKING;
                }

                enableFastMessageProcessing(true);
                NetworkManager.getSingleton().startTransferProcessing(connection);
                sendHandshake();
            }

            @Override
            public void exceptionThrown(Throwable error) {
                // ok, something strange happened,
                // notify connection and manager
                logger.fine("got error: " + error.getMessage());
                close("got error: " + error.getMessage());
            }

            @Override
            public String getDescription() {
                return "connection listener: OSF2F session, incoming";
            }
        });
    }

    private void sendMessage(OSF2FAuthMessage message) {
        logger.fine("sending message: " + message.getDescription());
        connection.getOutgoingMessageQueue().addMessage(message, false);
    }

    private void sendHandshake() {
        sendMessage(new OSF2FAuthHandshake((byte) 1, new byte[8]));
    }

    public void enableFastMessageProcessing(boolean enable) {
        logger.finer(this + ": setting fast message processing=" + enable);
        if (enable) {
            NetworkManager.getSingleton().upgradeTransferProcessing(connection);
        } else {

            // always enable this
            // NetworkManager.getSingleton().upgradeTransferProcessing(connection
            // );
        }
    }

    protected void close() {
        logger.fine("closing connection");
        connection.close();
        callback.closed(this);
    }

    public InetAddress getRemoteIp() {
        return connection.getEndpoint().getNotionalAddress().getAddress();
    }

    private void handleIncomingHandshake(OSF2FAuthHandshake authHandshake) {
        logger.fine("handshake received");
        if (invitation != null) {
            invitation.setStatus(Status.STATUS_CONNECTED);
        }
        if (redeeming) {
            /*
             * well, the other party is in charge
             */
            updateRedeemingStatus(ProtocolStateRedeeming.HANDSHAKE_COMPLETED);
        } else {
            updateInvitingStatus(ProtocolStateInviting.HANDSHAKE_COMPLETED);
            /*
             * request the key
             */
            this.sendMessage(new OSF2FAuthRequest(OSF2FAuthMessage.CURRENT_VERSION,
                    OSF2FAuthRequest.AuthType.KEY, null));
            updateInvitingStatus(ProtocolStateInviting.INVITE_CODE_REQUEST_SENT);
        }
    }

    public void handleAuthRequest(OSF2FAuthRequest message) {
        AuthType type = message.getAuthType();
        switch (type) {
        case KEY:
            /*
             * this is a key request, can only happen if we did an outgoing
             * connection
             */
            if (!redeeming) {
                sendError(OSF2FAuthStatus.STATUS_INVITE_ERR_PROTOCOL);
                close("got key request for inviting connection");
                return;
            }
            if (protocolStateRedeeming != ProtocolStateRedeeming.HANDSHAKE_COMPLETED) {
                sendError(OSF2FAuthStatus.STATUS_INVITE_ERR_PROTOCOL);
                close("hot key request before handshake completed, closing connection");
                return;
            }
            // send over the key
            updateRedeemingStatus(ProtocolStateRedeeming.INVITE_CODE_REQUEST_RECEIVED);
            sendMessage(new OSF2FAuthResponse(OSF2FAuthMessage.CURRENT_VERSION, AuthType.KEY,
                    invitation.getKey()));
            updateRedeemingStatus(ProtocolStateRedeeming.INVITE_CODE_SENT);
            break;
        default:
            sendError(OSF2FAuthStatus.STATUS_INVITE_ERR_PROTOCOL);
            close("unknown auth request type: " + type);
            break;
        }
    }

    private void close(String string) {
        logger.fine("closing: " + string);
        close();
    }

    private void handleAuthStatus(OSF2FAuthStatus message) {

        if (message.getStatus() == OSF2FAuthStatus.STATUS_INVITE_KEY_OK) {
            if (redeeming) {
                /*
                 * outgoing
                 */
                if (protocolStateRedeeming != ProtocolStateRedeeming.INVITE_CODE_SENT) {
                    sendError(OSF2FAuthStatus.STATUS_INVITE_ERR_PROTOCOL);
                    close("Got authenticated but never sent invite code, closing");
                    return;
                }

                int securityLevel = invitation.getSecurityLevel();
                switch (securityLevel) {
                case FriendInvitation.SECURITY_LEVEL_LOW:
                    // all fine, just add remote side
                    authenticated();
                    break;
                case FriendInvitation.SECURITY_LEVEL_PIN:
                    // we need to authenticate the remote side as well
                    if (remoteSideAuthenticated) {
                        authenticated();
                    } else {
                        Debug.out("got status without remote side authenticated");
                    }
                    break;
                default:
                    break;
                }
            } else {
                /*
                 * incoming:
                 */
                sendError(OSF2FAuthStatus.STATUS_INVITE_ERR_PROTOCOL);
                close("got auth=authenticated when not expecting it, closing");
                return;
            }
        } else {
            sendError(OSF2FAuthStatus.STATUS_INVITE_ERR_PROTOCOL);
            close("got non OK authstatus message: " + message.getDescription());
        }
    }

    private void authenticated() {
        invitation.setStatus(Status.STATUS_AUTHENTICATED);
        invitation.setRemotePublicKey(remoteKey);
        invitation.setLastConnectDate(System.currentTimeMillis());
        invitation.setLastConnectIp(getRemoteIp().getHostAddress());
        invitation.setLastConnectPort(getRemotePort());
        try {
            callback.authenticated(invitation);
        } catch (InvalidKeyException e) {
            invitation.setStatus(Status.STATUS_INVALID);
            e.printStackTrace();
        }
    }

    private void updateInvitingStatus(ProtocolStateInviting newStatus) {
        logger.finer("updating status: type=" + connectionType.name() + " old_status="
                + protocolStateInviting.name() + " new_status=" + newStatus.name());
        protocolStateInviting = newStatus;
    }

    private void updateRedeemingStatus(ProtocolStateRedeeming newStatus) {
        logger.finer("updating status: type=" + connectionType.name() + " old_status="
                + protocolStateRedeeming.name() + " new_status=" + newStatus.name());
        protocolStateRedeeming = newStatus;
    }

    private void handleAuthResponse(OSF2FAuthResponse message) {
        AuthType type = message.getAuthType();
        switch (type) {
        case KEY:
            byte[] key = message.getResponse();
            if (redeeming) {
                /*
                 * we really shouldn't get a key unless this is an inviting
                 * connection
                 */
                sendError(OSF2FAuthStatus.STATUS_INVITE_ERR_PROTOCOL);
                close("got key response in redeeming connection, closing");
                return;
            }
            /*
             * incoming then...
             */
            // check that we are in the right state:
            if (protocolStateInviting != ProtocolStateInviting.INVITE_CODE_REQUEST_SENT) {
                sendError(OSF2FAuthStatus.STATUS_INVITE_ERR_PROTOCOL);
                close("got invite key but never requested it...");
                return;
            }
            updateInvitingStatus(ProtocolStateInviting.INVITE_CODE_RECEIVED);

            this.invitation = callback.getInvitationFromInviteKey(new HashWrapper(key));

            /*
             * check that we acutally sent this...
             */
            if (invitation == null || !invitation.keyEquals(key)) {
                String ip = getRemoteIp().getHostAddress();
                callback.banIp(ip);
                sendError(OSF2FAuthStatus.STATUS_INVITE_ERR_INV_KEY);
                logger.fine("incoming auth connection denied: " + ip);
                close("invalid auth response");
                return;
            }

            int securityLevel = invitation.getSecurityLevel();
            switch (securityLevel) {
            case FriendInvitation.SECURITY_LEVEL_LOW:
                authenticated();
                sendMessage(new OSF2FAuthStatus(OSF2FAuthMessage.CURRENT_VERSION,
                        OSF2FAuthStatus.STATUS_INVITE_KEY_OK));
                updateInvitingStatus(ProtocolStateInviting.AUTHENTICATED);
                break;
            case FriendInvitation.SECURITY_LEVEL_PIN:
                sendError(OSF2FAuthStatus.STATUS_INVITE_ERR_PROTOCOL);
                close("Security level pin not implemented yet");
                break;
            default:
                sendError(OSF2FAuthStatus.STATUS_INVITE_ERR_PROTOCOL);
                Debug.out("unknown security type");
                break;
            }
            break;

        default:
            break;
        }
    }

    private long lastMessageRecvTime = System.currentTimeMillis();

    private class IncomingQueueListener implements MessageQueueListener {
        private long packetNum = 0;

        @Override
        public void dataBytesReceived(int byte_count) {
            lastMessageRecvTime = System.currentTimeMillis();

        }

        @Override
        public boolean messageReceived(Message message) {
            packetNum++;
            lastMessageRecvTime = System.currentTimeMillis();
            logger.finer(" got message: " + message.getDescription() + "\t::"
                    + InvitationConnection.this);
            if (message instanceof OSF2FAuthResponse) {
                handleAuthResponse((OSF2FAuthResponse) message);
            } else if (message instanceof OSF2FAuthRequest) {
                handleAuthRequest((OSF2FAuthRequest) message);
            } else if (message instanceof OSF2FAuthStatus) {
                handleAuthStatus((OSF2FAuthStatus) message);
            } else if (message instanceof OSF2FAuthHandshake) {
                handleIncomingHandshake((OSF2FAuthHandshake) message);
            }

            else {
                Debug.out("unknown message: " + message.getDescription());
            }

            return (true);
        }

        @Override
        public void protocolBytesReceived(int byte_count) {
            // TODO Auto-generated method stub

        }
    }
}
