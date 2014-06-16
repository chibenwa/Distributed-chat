package Chat.Server;

import Chat.Netmessage.InterServerMessage;
import Chat.Utils.LogicalLockClock;
import Chat.Utils.ResourceVisitor;
import Chat.Utils.SendableHashMap;

import java.io.Serializable;
import java.net.SocketAddress;
import java.util.ArrayList;

/**
 * Created by benwa on 6/16/14.
 */
public class LockManager {
    /**
     * The broadcast manager we will use for this task...
     * I want
     * Reliable diffusion is enough. Moreover we will use the **Needed data** field,
     * so we have to make it available.
     */
    private RBroadcastManager rBroadcastManager;
    /**
     * A boolean that indicates if we are currently using the ressource
     */
    private Boolean isUsingResource = false;
    /**
     * A Boolean that indicates if we already asked a lock
     */
    private Boolean lockAsked = false;
    /**
     * nsp algorithm field
     */
    private int nsp = 0;
    /**
     * Indicates if we have the token
     */
    private Boolean hasToken = false;
    /**
     * The Token we are using.
     */
    private LogicalLockClock logicalLockClock = null;
    /**
     * Our server identifier
     */
    private SocketAddress ourIdentifier;
    /**
     * The dem hash map from the algorithm
     */
    private SendableHashMap dem = new SendableHashMap();
    /**
     * Used to personalize access to resources.
     */
    private ResourceVisitor resourceVisitor;


    /**
     * Basic constructor
     * @param _rBroadcastManager The broadcast manager that will
     */
    public LockManager(RBroadcastManager _rBroadcastManager, ResourceVisitor _resourceVisitor) {
        rBroadcastManager = _rBroadcastManager;
        resourceVisitor = _resourceVisitor;
    }


    /**
     * The method that start doing something.
     *
     * You should overwrite it in subclass ( to get something that really do something... )
     * The task should be non blocking.
     */
    protected void startUsingRessource() {
        resourceVisitor.startUsingResource();
        isUsingResource = true;

    }

    /**
     * The method to call to release resources.
     *
     * It is your responsibility to stop what you were doing... ( threads, etc... ).
     */
    public void stopUsingRessource() {
        resourceVisitor.stopUsingResource();
        isUsingResource = false;
        releaseLock();
    }

    /**
     * Ask for the lock. startUsingResources is then called when we receive the token.
     */
    public void askLock() {
        if( !lockAsked) {
            lockAsked = true;
            if (!hasToken) {
                nsp++;
                InterServerMessage message = new InterServerMessage(0, 3, 8);
                message.setMessage( nsp );
                message.setIdentifier(ourIdentifier);
                rBroadcastManager.launchBroadcast(message);
            } else {
                startUsingRessource();
            }
        } else {
            System.out.println("We already asked for this token...");
        }
    }

    /**
     * Release our lock
     */
    protected void releaseLock() {
        logicalLockClock.put(ourIdentifier, nsp);
        SocketAddress next = logicalLockClock.getNext(dem,ourIdentifier);
        if(next != null) {
            InterServerMessage message = new InterServerMessage(0, 3, 9);
            message.setMessage(logicalLockClock);
            message.setNeededData( next );
            rBroadcastManager.launchBroadcast(message);
            hasToken = false;
            logicalLockClock = null;
        }
    }



    /**
     * Manage input. Call this for Lock Request Broadcast.
     * @param interServerMessage Message that holds the lock request broadcast.
     */
    protected void manageRequestBroadcast(InterServerMessage interServerMessage) {
        // Message related
        Integer nsq = (Integer) interServerMessage.getMessage();
        SocketAddress q = interServerMessage.getIdentifier();
        dem.put(q, Math.max(nsq, dem.get(q)) );
        if(hasToken && !isUsingResource) {
            releaseLock();
        }
    }

    /**
     * Manage the reception of the token. It launches the task specified in start using resources.
     * @param interServerMessage The message we have to interpret...
     */
    protected void manageTokenReception(InterServerMessage interServerMessage) {
        if( (interServerMessage.getNeededData()).toString().compareTo(ourIdentifier.toString()) == 0 ) {
            System.out.println("This token is for us");
            if (lockAsked) {
                hasToken = true;
                lockAsked = false;
                logicalLockClock = (LogicalLockClock) interServerMessage.getMessage();
                startUsingRessource();
            } else {
                System.out.println("Token received without asking");
            }
        } else {
            System.out.println("This token is for " + interServerMessage.getIdentifier() );
        }
    }

    /**
     * Called when server is launched. It set the server identifier we will use with our waves.
     *
     * @param _ourIdentifier Server identifier
     */
    public void setOurAddress(SocketAddress _ourIdentifier) {
        if( ourIdentifier == null) {
            ourIdentifier = _ourIdentifier;
        }
    }

    /**
     * Prepare us as failed election node.
     *
     * We will not have the token
     */
    protected void destroyToken() {
        logicalLockClock = null;
        lockAsked = false;
        isUsingResource = false;
        hasToken = false;
        nsp = 0;
    }

    /**
     * Prepare us as winner election node.
     *
     * We will not have the token...
     */
    protected void regenerateToken( ) {
        hasToken = true;
        isUsingResource = false;
        lockAsked = false;
        nsp = 0;
        if(logicalLockClock == null) {
            logicalLockClock = new LogicalLockClock();
        }
    }

    /**
     * Display our token ( if we have it )
     */
    private void diplayToken() {
        if(logicalLockClock == null) {
            System.out.println("We do not have the token");
        } else {
            logicalLockClock.display();
        }
    }

    /**
     * Debug utility : display our lock state.
     */
    public void display() {
        System.out.println("Our identifier : " + ourIdentifier);
        if(hasToken) {
            System.out.println("We have the token : ");
            diplayToken();
        } else {
            System.out.println("We do not have the token");
        }
        if(isUsingResource) {
            System.out.println("We are using resources");
        } else {
            System.out.println("We do not use resources");
        }
        if(lockAsked) {
            System.out.println("We are waiting for lock");
        } else {
            System.out.println("We are not asking for lock");
        }
    }

    /**
     * Utility function. Re init dem.
     * @param serversConnectedOnOurNetwork Server list used to generate dem hash list
     */
    private void demInit(ArrayList<Serializable> serversConnectedOnOurNetwork) {
        for(Serializable serializable : serversConnectedOnOurNetwork) {
            SocketAddress serverIdentifier = (SocketAddress) serializable;
            dem.put(serverIdentifier, 0);
        }
    }

    /**
     * Re init dem parameter and regenerate token ( only the elected master did this ). Used on topological changes.
     *
     * @param serversConnectedOnOurNetwork Server list used to generate dem hash list and token
     */
    protected void makeDemInit(ArrayList<Serializable> serversConnectedOnOurNetwork) {
        if( hasToken ) {
            logicalLockClock.generateFromServerList( serversConnectedOnOurNetwork);
        }
        demInit(serversConnectedOnOurNetwork);
    }
}
