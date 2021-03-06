package Chat.Utils;

import csc4509.FullDuplexMessageWorker;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by benwa on 6/7/14.
 *
 * License : GLP 2.0
 *
 * A connection structure that represent a client connection
 */
public class ClientStruct {
    /**
     * Number of IOerror we faced on this connection since the last time it worked.
     */
    private int nbIOError = 0;
    /**
     * FullDuplexMessageWorker you have to use to speak to this client
     */
    private FullDuplexMessageWorker full;
    /**
     * Client pseudo
     */
    private String pseudo;
    /**
     * Is pseudo set ?
     */
    private Boolean pseudoSet = false;
    /**
     * Indicates if this client is active.
     * We can not receive messages from inactive clients
     */
    private Boolean active = true;

    /**
     * Use this to know if a client is active
     * @return True if the client is active, false in other cases.
     */
    public Boolean getActive() {
        return active;
    }

    /**
     * Switch client to inactive mode.
     */
    public void setInactive() {
        active = false;
    }
    /**
     * A write lock to protect our channel from concurrent write
     */
    private final ReentrantLock writeLock = new ReentrantLock();

    /**
     * Lock our channel.
     */
    public void lock() {
        writeLock.lock();
    }

    /**
     * Unlock our channel
     */
    public void unlock() {
        writeLock.unlock();
    }
    /**
     * Accessor for pseudoSet.
     *
     * @return True if a pseudo is set for this client, false in other cases
     */
    public Boolean hasPseudo() {
        return pseudoSet;
    }


    /**
     * Setter for pseudo
     *
     * @param s The pseudo to attach to connection.
     */
    public void setPseudo( String s) {
        pseudo = s;
        pseudoSet = true;
    }

    /**
     * Pseudo accessor
     *
     * @return The pseudo attached to the connection
     */
    public String getPseudo() {
        return pseudo;
    }

    /**
     * Basic costructor
     *
     * @param _full FullDuplexMessageWorker you have to use to speak to this client
     */
    public ClientStruct( FullDuplexMessageWorker _full ) {
        full = _full;
    }

    /**
     * Accessor for full
     *
     * @return FullDuplexMessageWorker you have to use to speak to this client
     */
    public FullDuplexMessageWorker getFullDuplexMessageWorker() {
        return full;
    }

    public void resetIoError() {
        nbIOError = 0;
    }

    /**
     * To be called on IO error. Increment io error number
     * @return True if we need to close the connection, false if it can still wait.
     */
    public Boolean addIOError() {
        nbIOError++;
        if( nbIOError > 0) {
            // Just change this value to be less mean
            return true;
        }
        return false;
    }
}
