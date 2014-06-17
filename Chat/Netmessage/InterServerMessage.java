package Chat.Netmessage;

import java.io.Serializable;
import java.net.SocketAddress;

/**
 * Created by benwa on 6/7/14.
 *
 * A message that is exchanged between servers
 *
 * Error definition !
 *
 *  0 : No error
 *  1 : The server already established a connection with the distant server. Making a new one is both pointless AND dangerous
 *  3 : Add demand received while in election. Please try later.
 *  4 : Add answer received while in election. Please try later.
 *
 * License : GLP 2.0
 */
public class InterServerMessage extends NetMessage {

    private int subType = 0;

    /**
     * The message that we carry. Cast is done thanks to subType field.
     */

    private Serializable message;


    /**
     * A unique identifier to identify the server this message is coming from
     */

    private SocketAddress identifier;

    /**
     * The identifier of the election winner of the network. Is used to recognize a server as part of our network
     * if establishing a connection.
     */
    private SocketAddress electionWinner;

    /**
     * Another Serialized field to carry the dat to hold. It is mainly used by C broadcast message to transmit vectorial clock used by this algorithm
     * ( so messages used with C Broadcast can not use this field... ).
     */
    private Serializable neededData = null;

    /**
     * Constructor
     *
     * @param _seq Sequence number
     * @param _type Type
     *              Type 0 : Hello I am a server...
     *              Type 1 : I noticed that you are a server !
     *              Type 2 : Demand to close connection ...
     *              Type 3 : R diffusion of a message
     *              Type 4 : F diffusion of a message
     *              Type 5 : C diffusion
     *              Type 6 : Echo wave pseudos
     *              Type 7 : Echo wave servers
     *              Type 42 : Error
     */
    public InterServerMessage(int _seq, int _type) {
        super( _seq, _type);
    }

    /**
     * Constructor
     *
     * @param _seq Sequence number
     * @param _type Type
     *              Type 0 : Hello I am a server...
     *              Type 1 : I noticed that you are a server !
     *              Type 2 : Demand to close connection ...
     *              Type 3 : R diffusion of a message
     *              Type 4 : F diffusion of a message
     *              Type 5 : C diffusion
     *              Type 6 : Echo wave pseudos
     *              Type 7 : Echo wave servers
     *              Type 42 : Error
     * @param _subType
     *              Type 0 : no SubType
     *              Type 1 : Client join
     *              Type 2 : Client leave
     *              Type 3 : Client message
     *              Type 4 : Coming in your network
     *              Type 5 : private message forward
     *              Type 6 : Set your list of servers ( The elected server use this message to synchronize server data after its election, after an Echo wave servers )
     *              Type 7 : Set the list of clients ( The elected server use this message to synchronize server data after its election, after an Echo wave servers )
     *              Type 8 : Diffusion of a MUTEX request
     *              Type 9 : Somebody give us the token
     *              Type 10 : Shutdown Request
     *              Type 11 : Ending detection
     */

    public InterServerMessage(int _seq, int _type, int _subType) {
        super( _seq, _type);
        subType = _subType;
    }

    /**
     * Display the error message
     */

    public void printErrorCode() {
        switch (errorCode) {
            case 0 :
                System.out.println("No error");
                break;
            case 1 :
                System.out.println("The server already established a connection with the distant server. Making a new one is both pointless AND dangerous");
                break;
            case 3 :
                System.out.println("Add demand received while in election. Please try later.");
                break;
            case 4 :
                System.out.println("Add answer received while in election. Please try later.");
                break;
            default:
                System.out.println("Unhandled error");
                break;
        }
    }

    /**
     * Set the identifier of the server that first send this message
     *
     * @param _identifier Identifier of the origin of the message
     */

    public void setIdentifier( SocketAddress _identifier ) {
        identifier = _identifier;
    }

    /**
     * Access the identifier of the server sender
     * @return identifier of the server sender
     */

    public SocketAddress getIdentifier() {
        return identifier;
    }

    /**
     * Set the value of the identifier of the last election winner
     *
     * This value is used on inter server connection, to know if oth servers belong to the same network.
     *
     * @param _electionWinner identifier of the last election winner
     */

    public void setElectionWinner( SocketAddress _electionWinner) {
        electionWinner = _electionWinner;
    }

    /**
     * Access the value of the identifier of the last election winner
     *
     * @return identifier of the last election winner
     */

    public SocketAddress getElectionWinner() {
        return electionWinner;
    }


    /**
     *
     * Returns message subtype
     *
     * @return message subtype
     */

    public int getSubType(){
        return subType;
    }

    /**
     *
     * Set the message in the InterServerMessage
     *
     * @param _message the given message
     */

    public void setMessage( Serializable _message) {
        message = _message;
    }

    /**
     *
     * Set the message in the InterServerMessage
     *
     * @return the given message
     */

    public Serializable getMessage() {
        return message;
    }

    /**
     * Needed data setter
     * @param _neededData Needed Data field to set for this message.
     */
    public void setNeededData(Serializable _neededData) {
        neededData = _neededData;
    }

    /**
     * Needed data accessor
     * @return Needed Data field for this message
     */
    public Serializable getNeededData() {
        return neededData;
    }

}
