package Chat.Server;

import Chat.Netmessage.InterServerMessage;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by benwa on 6/8/14.
 *
 * License : GPL 2.0
 */
public class RBroadcastManager {
    private HashMap<SocketAddress, ArrayList<Integer> > RMessageReceived;
    private NetManager netManager;
    private SocketAddress ourAddress = null;
    private int ourSeq = 0;

    public RBroadcastManager( NetManager _netManager) {
        RMessageReceived = new HashMap<SocketAddress, ArrayList<Integer> >();
        netManager = _netManager;
    }

    // To be called on server joining !
    public void addServer(SocketAddress identifier) {
        if( RMessageReceived.get( identifier ) == null )
            RMessageReceived.put( identifier, new ArrayList<Integer>() );
    }

    public Boolean manageInput( InterServerMessage message) {
        return manageRInputWithAnswer( message);
    }

    public Boolean manageRInputWithAnswer(InterServerMessage message) {
        SocketAddress identifier = message.getIdentifier();
        int messageSeq = message.getSeq();
        if( ! processMessageIds(identifier, messageSeq)) {
            // First time we saw this message -> broadcast it...
            netManager.getState().broadcastInterServerMessage(message);
            return true;
        }
        return false;
    }

    private Boolean processMessageIds(SocketAddress identifier, int seq) {
        ArrayList<Integer> messageIdentifierIds = RMessageReceived.get(identifier);
        if( messageIdentifierIds == null ) {
            System.out.println("Null ID founded in RMessageReceived warning 1. We add it. ");
            messageIdentifierIds = new ArrayList<Integer>();
            messageIdentifierIds.add(seq);
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

    public void launchRBroadcast(InterServerMessage message) {
        // First register our message
        ArrayList<Integer> ourMessages = RMessageReceived.get(ourAddress);
        message.setSeq( ourSeq++ );
        message.setIdentifier(ourAddress);
        ourMessages.add(message.getSeq());
        netManager.getState().broadcastInterServerMessage(message);

    }

    public void setOurAddress(SocketAddress _ourAddress) {
        if( ourAddress == null) {
            ourAddress = _ourAddress;
            RMessageReceived.put( ourAddress, new ArrayList<Integer>());
        }
    }
}
