package Chat.Server;

import Chat.Netmessage.InterServerMessage;

import java.net.SocketAddress;
import java.util.HashMap;

/**
 * Created by benwa on 6/8/14.
 *
 * License : GPL 2.0
 *
 * A basic reliable broadcast manager.
 */
public class RBroadcastManager implements BroadcastManager{
    /**
     * A map that store the broadcast identifier for each server, and the received waves for each one of them. The last point is important to release memory...
     */
    private HashMap<SocketAddress, HashMap<Integer,Integer> > RMessageReceived;
    /**
     * The netManager we will use to send messages
     */
    protected NetManager netManager;
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
        RMessageReceived = new HashMap<SocketAddress, HashMap<Integer, Integer>>();
        netManager = _netManager;
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
        System.out.println("Wave identifier : " + identifier + " " +messageSeq);
        if( ! processMessageIds(identifier, messageSeq)) {
            // First time we saw this message -> broadcast it...
            message.setIdentifier(identifier);
            System.out.println("First time we have this message. Accept it.");
            netManager.getState().broadcastInterServerMessage(message);
            // Ugly hack... ... ...
            if(message.getType() == 5) {
                // User message. Notify that we send it.
                for (int i = 0; i < netManager.getState().getNbConnectedServers(); i++) {
                    netManager.endManager.notifyUserSend();
                }
            }
            System.out.println("Broadcast done for " + message.getIdentifier() + " " + message.getSeq() );
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
        System.out.println("Processing for " + identifier);
        HashMap<Integer, Integer> messageIdentifierIds = RMessageReceived.get(identifier);
        if( messageIdentifierIds == null ) {
            System.out.println("First time this server sends us a RBroadcast message");
            messageIdentifierIds = new HashMap<Integer, Integer>();
            messageIdentifierIds.put(seq,1);
            RMessageReceived.put(identifier, messageIdentifierIds);
            System.out.println("Process done for " + identifier);
            return false;
        }
        if( messageIdentifierIds.get(seq) != null ) {
            messageIdentifierIds.put(seq, messageIdentifierIds.get(seq) + 1);
            if( messageIdentifierIds.get(seq) >= netManager.getState().getNbConnectedServers()) {
                // Here the wave is ended. We can safely erase data about it, to avoid memory leaks.
                messageIdentifierIds.remove(seq);
                System.out.println("Resources cleaned");
            }
            return true;
        } else {
            messageIdentifierIds.put(seq,1);
            System.out.println("Process done for " + identifier);
            return false;
        }
    }

    /**
     * We use this method to send a R broadcast for a given message
     * @param message The given message
     */

    public void launchBroadcast(InterServerMessage message) {
        // First register our message
        HashMap<Integer, Integer> ourMessages = RMessageReceived.get(ourAddress);
        ourSeq++;
        message.setSeq( ourSeq );
        message.setIdentifier(ourAddress);
        ourMessages.put(message.getSeq(), 0);
        netManager.getState().broadcastInterServerMessage(message);
        System.out.println("================================ Seding RBO " + ourAddress + " " +ourSeq+ " ==========================");
    }

    /**
     * Called when server is launched. It set the server identifier we will use with our waves.
     *
     * @param _ourAddress Server identifier
     */
    public void setOurAddress(SocketAddress _ourAddress) {
        if( ourAddress == null) {
            ourAddress = _ourAddress;
            RMessageReceived.put( ourAddress, new HashMap<Integer, Integer>());
        }
    }
}
