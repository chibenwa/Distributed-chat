package Chat.Server;

import Chat.Netmessage.InterServerMessage;

import java.net.SocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by benwa on 6/17/14.
 *
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
    private Boolean inCharge = true;
    /**
     * Timer used to schedule token sending
     */
    final Timer timer = new Timer();

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
            System.out.println("Shutdown requested...");
            endDemanded = true;
            netManager.getState().setClientsInactive();
        }
        if( state == 1) {
            System.out.println("Euh man, we are active");
        }
        if( netManager.getState().getIdentifier().toString().compareTo( interServerMessage.getElectionWinner().toString() ) == 0 ) {
            // This message is addressed to us
            int messageColor = (Integer)interServerMessage.getMessage();
            int messageDiff = (Integer) interServerMessage.getNeededData();
            if( inCharge ) {
                System.out.println("We are in charge : color : " + color + ", Message color : " + messageColor + ", messageDiff : " + messageDiff
                + ", messageCount : " + messageCount);
                // p = p0
                if( color ==  0 && messageColor == 0 && messageCount + messageDiff == 0) {
                    // Here we can stop with no fear.
                    System.out.println("We can stop with no fear");
                    System.out.println("Next : " +netManager.getState().getFollowingTarget() );
                    netManager.shutdownOurInfrastructure();
                } else {
                    System.out.println("Can not stop. Sending new token.");
                    System.out.println("Next : " +netManager.getState().getFollowingTarget() );
                    sendToken( 0, 0, netManager.getState().getFollowingTarget());
                }
            } else {
                if( color == 0 ) {
                    System.out.println("Token received. Color white : Message color : " + messageColor + ", messageDiff : " + messageDiff
                            + ", messageCount : " + messageCount);
                    System.out.println("Next : " +netManager.getState().getFollowingTarget() );
                    sendToken(messageColor, messageDiff + messageCount, netManager.getState().getFollowingTarget());
                } else {
                    System.out.println("Token received. Color black");
                    sendToken(1, messageDiff + messageCount, netManager.getState().getFollowingTarget());
                }
            }
            color = 0;
        } else {
            System.out.println("Message not for us. For : " + interServerMessage.getElectionWinner() +" But we are : " + netManager.getState().getIdentifier() );
        }
    }

    /**
     * Start the detection of ending.
     */
    public void startEndingDetection() {
        if( netManager.getState().getStandAlone() ) {
            // We are stand alone. We can shutdown.
            System.exit(0);
        }
        if( inCharge ) {
            if(! endDemanded ) {
                endDemanded = true;
                netManager.getState().setClientsInactive();
                sendToken( 0, 0, netManager.getState().getFollowingTarget());
                System.out.println("Sending first token.");
                System.out.println("Next : " +netManager.getState().getFollowingTarget() );
            }
        }
    }

    /**
     * Send a token
     * @param _color Color of the token
     * @param _diff Difference between received and sent message hold in the token
     * @param _nextP Next target for token
     */
    private void sendToken(int _color, int _diff, SocketAddress _nextP) {
        final int myCol = _color;
        final SocketAddress myNext = _nextP;
        final int myDiff = _diff;
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                InterServerMessage interServerMessage = new InterServerMessage(0,3,11);
                interServerMessage.setElectionWinner( myNext );
                interServerMessage.setMessage( new Integer(myCol));
                interServerMessage.setNeededData( new Integer(myDiff));
                rBroadcastManager.launchBroadcast(interServerMessage);
            }
        };
        timer.schedule(timerTask,10);
    }

    /**
     * Reset our end Manager
     */
    protected void reset() {
        state = 0;
        color = 0;
        endDemanded = false;
        messageCount = 0;
        inCharge = netManager.getElectoralState() == 2;
        if(netManager.getState().getStandAlone()) {
            inCharge = true;
        }
    }
}
