package Chat.Server;

import Chat.Netmessage.InterServerMessage;
import Chat.Utils.VectorialClock;

import java.io.Serializable;
import java.net.SocketAddress;
import java.util.*;

/**
 * Created by benwa on 6/12/14.
 *
 * License : GLP 2.0
 *
 * A class to manage C Broadcast
 */

public class CBroadcastManager {
    /**
     * The RBroadcast manager we will use to send a R broadcast wave ( needed for a C broadcast )
     */
    private RBroadcastManager rBroadcastManager;
    /**
     * The vectorial clock of our current node
     */
    private VectorialClock ourVectorialClock;
    /**
     * The collection of not yet C delivered messages.
     */
    private Set<InterServerMessage> messageBag;
    /**
     * Our server identifier
     */
    private SocketAddress ourIdentifier;
    /**
     * InterServerMessages that were C accepted after last C Broadcast message reception
     */
    private ArrayList<InterServerMessage> cAcceptedMessages;

    /**
     * Basic constructor
     *
     * @param _rBroadcastManager RBroadcast manager we will use to send a R broadcast wave ( needed for a C broadcast )
     */

    CBroadcastManager(RBroadcastManager _rBroadcastManager) {
        messageBag = new HashSet<InterServerMessage>();
        ourVectorialClock = new VectorialClock();
        rBroadcastManager = _rBroadcastManager;
        cAcceptedMessages = new ArrayList<InterServerMessage>();
    }


    /**
     * Called when server is launched. It set the server identifier we will use with our waves.
     *
     * @param _ourIdentifier Server identifier
     */
    public void setOurAddress(SocketAddress _ourIdentifier) {
        if( ourIdentifier == null) {
            ourIdentifier = _ourIdentifier;
            ourVectorialClock.put(ourIdentifier, 0);
        }
    }

    /**
     * Launches a C Broadcast for the given message
     *
     * @param message the message to C Broadcast
     */

    public void launchBroadcast(InterServerMessage message) {
        ourVectorialClock.put(ourIdentifier, ourVectorialClock.get(ourIdentifier) + 1);
        message.setNeededData(ourVectorialClock);
        rBroadcastManager.launchBroadcast(message);
    }

    /**
     * Process input message. Return true if one ( or more ) message was C accepted after receiving this message.
     *
     * Accepted messages can be retrieved thanks to getAcceptedMessages.
     *
     * @param message The message we just received.
     * @return Return true if one ( or more ) message was C accepted after receiving this message. False in other cases.
     */
    public Boolean manageInput( InterServerMessage message) {
        if( ! rBroadcastManager.manageInput(message) ) {
            System.out.println("Paquet not R accepted");
            return false;
        }
        System.out.println("Paquet R accepted. Add it to message  Box. Here is its vectorial clock.");
        messageBag.add(message);
        Boolean needToProcessMessageBag = true;
        Boolean result = false;
        VectorialClock messageVectorialClock = (VectorialClock) message.getNeededData();
        messageVectorialClock.display();
        while(needToProcessMessageBag) {
            needToProcessMessageBag = false;
            // Iterate on message Bag
            for(InterServerMessage interServerMessage : messageBag) {
                if( ((VectorialClock)interServerMessage.getNeededData()).isNext(ourVectorialClock, interServerMessage.getIdentifier(), ourIdentifier) ) {
                    System.out.println("Message C accepted !!!!!!! : " + interServerMessage.getSubType() );
                    // We C accept the message
                    cAcceptedMessages.add(interServerMessage);
                    // and remove it from the bag.
                    messageBag.remove(interServerMessage);
                    // We accepted one message, we will have to iterate again
                    needToProcessMessageBag = true;
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * Accessor on C Accepted messages ( since last reception ).
     * C accepted messages are reset inside this class after this call, you can get them only once.
     *
     * @return C accepted messages since last call.
     */
    public ArrayList<InterServerMessage> getCAcceptedMessages() {
        ArrayList<InterServerMessage> result = cAcceptedMessages;
        cAcceptedMessages = new ArrayList<InterServerMessage>();
        return result;
    }

    protected void reInitVectorialClock(ArrayList<Serializable> serversConnectedOnOurNetwork) {
        ourVectorialClock.clear();
        for(Serializable serializable : serversConnectedOnOurNetwork) {
            SocketAddress serverIdentifier = (SocketAddress) serializable;
            ourVectorialClock.put(serverIdentifier, 0);
        }
    }

    protected void displayVectorialClock() {
        ourVectorialClock.display();
    }
}
