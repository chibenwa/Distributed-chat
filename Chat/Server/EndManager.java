package Chat.Server;

import Chat.Netmessage.InterServerMessage;

import java.net.SocketAddress;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by benwa on 6/17/14.
 *
 * TODO : integrate in our code NetManager
 * TODO : NetManager should disconnect client on sending and first receiving token
 * TODO : console logs every where !
 * TODO : safely demand ending from keyboard input
 */
public class EndManager {
    /**
     * State of our server :
     * 0 : inactive
     * 1 : active
     */
    private int state = 0;
    /**
     * Color of our server :
     * 0 : white
     * 1 : black
     */
    private int color = 0;
    /**
     * A boolean that indicates if we want to terminate ( in clean ways ) our distributed infrastructure
     */
    private Boolean endDemanded = false;
    /**
     * Message difference
     */
    private int messageCount = 0;
    /**
     * messageCount might be accessed form different threads. Protect it.
     */
    private final ReentrantLock writeLock = new ReentrantLock();
    /**
     * NetManager we will use to retrieve server list
     */
    private NetManager netManager;
    /**
     * RBroadcastManager we will use to deliver our messages
     */
    private RBroadcastManager rBroadcastManager;
    /**
     * To know if we are in charge of ending detection
     */
    private Boolean inCharge = false;
    /**
     * Basic constructor
     * @param _netManager NetManager we will use to retrieve server list
     * @param _rBroadcastManager RBroadcastManager we will use to deliver our messages
     */
    public EndManager(NetManager _netManager, RBroadcastManager _rBroadcastManager) {
        netManager = _netManager;
        rBroadcastManager = _rBroadcastManager;
    }

    /**
     * To be called whenever you send a message NOT related to ending detection.
     */
    public void notifyUserSend() {
        writeLock.lock();
        messageCount++;
        writeLock.unlock();
    }

    /**
     * To be called whenever you receive a message NOT related to ending detection.
     */
    public void notifyUserReceive() {
        writeLock.lock();
        messageCount--;
        writeLock.unlock();
        color = 1;
        state = 1;
    }

    /**
     * To be called when you finished to fetch your selector and are waiting for other messages
     */
    public void inputProcessEnded() {
        state = 0;
    }

    /**
     * Token reception
     * @param interServerMessage The message that hold the token
     */
    public void TockenReception(InterServerMessage interServerMessage) {
        if( ! endDemanded ) {
            endDemanded = true;
        }
        if( netManager.getState().getIdentifier().toString().compareTo( interServerMessage.getElectionWinner().toString() ) == 0 ) {
            // This message is addressed to us
            int messageColor = (Integer)interServerMessage.getMessage();
            int messageDiff = (Integer) interServerMessage.getNeededData();
            if( inCharge ) {
                // p = p0
                if( color ==  0 && messageColor == 0 && messageCount + messageDiff == 0) {
                    // Here we can stop with no fear.
                    netManager.shutdownOurInfrastructure();
                } else {
                    sendToken( 0, 0, netManager.getState().getFollowingTarget());
                }
            } else {
                if( color == 0 ) {
                    sendToken(messageColor, messageDiff + messageCount, netManager.getState().getFollowingTarget());
                } else {
                    sendToken(1, messageDiff + messageCount, netManager.getState().getFollowingTarget());
                }
            }
            color = 0;
        }
    }

    public void startEndingDetection() {
        if( netManager.getState().getStandAlone() ) {
            // We are stand alone. We can shutdown.
            System.exit(0);
        }
        if( inCharge ) {
            if(! endDemanded ) {
                endDemanded = true;
                sendToken( 0, 0, netManager.getState().getFollowingTarget());
            }
        }
    }

    private void sendToken(int _color, int _diff, SocketAddress _nextP) {
        InterServerMessage interServerMessage = new InterServerMessage(0,3,12);
        interServerMessage.setElectionWinner( _nextP );
        interServerMessage.setMessage( new Integer(_color));
        interServerMessage.setNeededData( new Integer(_diff));
        rBroadcastManager.launchBroadcast(interServerMessage);
    }
}
