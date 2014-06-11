package Chat.Utils;

import csc4509.FullDuplexMessageWorker;

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
