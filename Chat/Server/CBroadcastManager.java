package Chat.Server;

import Chat.Netmessage.InterServerMessage;
import Chat.Utils.VectorialClock;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

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
        messageBag = new TreeSet<InterServerMessage>();
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
        messageBag.add(message);
        Boolean needToProcessMessageBag = true;
        Boolean result = false;
        while(needToProcessMessageBag) {
            needToProcessMessageBag = false;
            // Iterate on message Bag
            for(InterServerMessage interServerMessage : messageBag) {
                if( ((VectorialClock)interServerMessage.getNeededData()).isNext(ourVectorialClock, interServerMessage.getIdentifier(), ourIdentifier) ) {
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
        cAcceptedMessages.clear();
        return result;
    }

    protected void setOurVectorialClock(VectorialClock vectorialClock) {
        ourVectorialClock = vectorialClock;
    }

    /**
     * Used by EchoVectorialClockManager to return our
     * @return
     */
    protected VectorialClock getOurVectorialClock() {
        return ourVectorialClock;
    }

}
