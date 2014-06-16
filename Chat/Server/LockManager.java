package Chat.Server;

import Chat.Netmessage.InterServerMessage;
import Chat.Utils.LogicalJetonClock;
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
     * Basic constructor
     * @param _rBroadcastManager The broadcast manager that will
     */
    public LockManager(RBroadcastManager _rBroadcastManager) {
        rBroadcastManager = _rBroadcastManager;
    }
    /**
     * A boolean that indicates if we are currently using the ressource
     */
    private Boolean isUsingRessource = false;

    /**
     * The method that start doing something.
     *
     * You should overwrite it in subclass ( to get something that really do something... )
     * The task should be non blocking. It is your responsibility to use a thread.
     */
    public void startUsingRessource() {
        isUsingRessource = true;
    }

    /**
     * The method to call to release resources.
     *
     * It is your responsibility to stop what you were doing... ( threads, etc... ).
     */
    public void stopUsingRessource() {
        isUsingRessource = false;
        releaseLock();
    }

    /**
     * Basic accessor to know if we are currently using this resource.
     * @return true if we are using this resource, false in other cases.
     */
    public Boolean getIsUsingRessource() {
        return isUsingRessource;
    }
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
     * Ask for the lock. startUsingResources is then called when we receive the token.
     */
    public void askLock() {
        if( !lockAsked) {
            lockAsked = true;
            if (!hasToken) {
                nsp++;
                // TODO broadcast demand...
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
        logicalJetonClock.put(ourIdentifier, nsp);
        SocketAddress next = logicalJetonClock.getNext(dem,ourIdentifier);
        if(next == null) {
            // No followers, we are waiting for a demand
        } else {
            InterServerMessage message = new InterServerMessage(0, 3, 9);
            message.setMessage( logicalJetonClock );
            message.setNeededData( next );
            rBroadcastManager.launchBroadcast(message);
            hasToken = false;
            logicalJetonClock = null;
        }
    }

    /**
     * The Jeton we are using.
     */
    private LogicalJetonClock logicalJetonClock = null;
    /**
     * Process input to make this class evole
     */
    protected void manageRequestBroadcast(InterServerMessage interServerMessage) {
        // Message related
        Integer nsq = (Integer) interServerMessage.getMessage();
        SocketAddress q = (SocketAddress ) interServerMessage.getIdentifier();
        dem.put(q, Math.max(nsq, dem.get(q)) );
        if(hasToken && !isUsingRessource) {
            releaseLock();
        }
    }

    /**
     * Manage the reception of the token. It launches the task specified in start using resources.
     * @param interServerMessage
     */
    protected void manageTokenReception(InterServerMessage interServerMessage) {
        if( ((SocketAddress)interServerMessage.getNeededData()).toString().compareTo(ourIdentifier.toString()) == 0 ) {
            System.out.println("This token is for us");
            if (lockAsked) {
                hasToken = true;
                lockAsked = false;
                logicalJetonClock = (LogicalJetonClock) interServerMessage.getMessage();
                startUsingRessource();
            } else {
                System.out.println("Token received without asking");
            }
        } else {
            System.out.println("This token is for " + interServerMessage.getIdentifier() );
        }
    }
    /**
     * Our server identiier
     */
    private SocketAddress ourIdentifier;
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
    protected void destroyToken() {
        logicalJetonClock = null;
        lockAsked = false;
        isUsingRessource = false;
        hasToken = false;
    }
    protected void regenerateToken( ) {
        hasToken = true;
        isUsingRessource = false;
        lockAsked = false;
        if(logicalJetonClock == null) {
            logicalJetonClock = new LogicalJetonClock();
        }
    }
    private void diplayToken() {
        if(logicalJetonClock == null) {
            System.out.println("We do not have the token");
        } else {
            logicalJetonClock.display();
        }
    }
    public void display() {
        System.out.println("Our identifier : " + ourIdentifier);
        if(hasToken) {
            System.out.println("We have the token : ");
            diplayToken();
        } else {
            System.out.println("We do not have the token");
        }
        if(isUsingRessource) {
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
    private SendableHashMap dem = new SendableHashMap();

    private void demInit(ArrayList<Serializable> serversConnectedOnOurNetwork) {
        for(Serializable serializable : serversConnectedOnOurNetwork) {
            SocketAddress serverIdentifier = (SocketAddress) serializable;
            dem.put(serverIdentifier, 0);
        }
    }
    protected void makeDemInit(ArrayList<Serializable> serversConnectedOnOurNetwork) {
        if( hasToken ) {
            logicalJetonClock.generateFromServerList( serversConnectedOnOurNetwork, ourIdentifier );
        }
        demInit(serversConnectedOnOurNetwork);
    }
}
