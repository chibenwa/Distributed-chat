package Chat.Server;

import Chat.Netmessage.InterServerMessage;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by benwa on 6/8/14.
 *
 * License : GPL 2.0
 *
 * A basic reliable broadcast manager.
 */
public class RBroadcastManager {
    /**
     * A map that store the broadcast identifier for each server
     */
    private HashMap<SocketAddress, ArrayList<Integer> > RMessageReceived;
    /**
     * The netManager we will use to send messages
     */
    private NetManager netManager;
    /**
     * Our server identifier
     */
    private SocketAddress ourAddress = null;
    /**
     * Our current broadcast identifier.
     */
    private int ourSeq = 0;

    /**
     * Build a RBroadcast manager
     *
     * @param _netManager The netManager we will use to send messages
     */

    public RBroadcastManager( NetManager _netManager) {
        RMessageReceived = new HashMap<SocketAddress, ArrayList<Integer> >();
        netManager = _netManager;
    }

    /**
     * Add the server to the one that launched a R broadcast
     *
     * @param identifier The server that launched a broadcast
     */
    public void addServer(SocketAddress identifier) {
        if( RMessageReceived.get( identifier ) == null )
            RMessageReceived.put( identifier, new ArrayList<Integer>() );
    }

    /**
     * Manage a message received that happens to be a RBroadcast message.
     *
     * @param message The message just received, and related to RBroadcast.
     * @return True if this is the first time we saw this message ( it has to be taken in account ), false in other cases.
     */
    public Boolean manageInput( InterServerMessage message) {
        SocketAddress identifier = message.getIdentifier();
        int messageSeq = message.getSeq();
        if( ! processMessageIds(identifier, messageSeq)) {
            // First time we saw this message -> broadcast it...
            netManager.getState().broadcastInterServerMessage(message);
            return true;
        }
        return false;
    }

    /**
     * Using the wave identifier ( server identifier + wave number ) , see if this is the first time we saw this wave.
     *
     * @param identifier Server that launched the wave
     * @param seq Sequence that identify the wave
     * @return True if we already aw this wave, false in other cases.
     */
    private Boolean processMessageIds(SocketAddress identifier, int seq) {
        ArrayList<Integer> messageIdentifierIds = RMessageReceived.get(identifier);
        if( messageIdentifierIds == null ) {
            System.out.println("First time this server sends us a RBroadcast message");
            messageIdentifierIds = new ArrayList<Integer>();
            messageIdentifierIds.add(seq);
            RMessageReceived.put(identifier, messageIdentifierIds);
            return false;
        }
        for( Integer i : messageIdentifierIds) {
            if( i == seq ) {
                return true;
            }
        }
        messageIdentifierIds.add(seq);
        return false;
    }

    /**
     *
     * @param message
     */
    public void launchRBroadcast(InterServerMessage message) {
        // First register our message
        ArrayList<Integer> ourMessages = RMessageReceived.get(ourAddress);
        message.setSeq( ourSeq++ );
        message.setIdentifier(ourAddress);
        ourMessages.add(message.getSeq());
        netManager.getState().broadcastInterServerMessage(message);
    }

    /**
     * Called when server is launched. It set the server identifier we will use with our waves.
     *
     * @param _ourAddress Server identifier
     */
    public void setOurAddress(SocketAddress _ourAddress) {
        if( ourAddress == null) {
            ourAddress = _ourAddress;
            RMessageReceived.put( ourAddress, new ArrayList<Integer>());
        }
    }
}
