package Chat.Netmessage;

import java.io.Serializable;
import java.net.SocketAddress;

/**
 * Created by benwa on 6/7/14.
 *
 * License : GLP 2.0
 */
public class InterServerMessage extends NetMessage {
        /*

    Type definition !

        Type 0 : Hello I am a server...
        Type 1 : I noticed that you are a server !
        Type 2 : Demand to close connection ...
        Type 3 : R diffusion of a message
        Type 4 : F diffusion of a message
        Type 5 : C diffusion
        Type 6 : Echo wave pseudos
        Type 7 : Echo wave servers
        Type 42 : Error
     */

    /*

    Error definition !

        0 : No error
        1 : The server already established a connection with the distant server. Making a new one is both pointless AND dangerous
        3 : Add demand received while in election. Please try later.
        4 : Add answer received while in election. Please try later.

     */

    private int subType = 0;
    private Serializable message;

    /*
        Type 0 : no SubType
        Type 1 : Client join
        Type 2 : Client leave
        Type 3 : Client message
        Type 4 : Coming in your network
        Type 5 : private message forward
     */

    // A unique identifier to identify the server this message is coming from
    private SocketAddress identifier;

    // The identity of the election winner
    private SocketAddress electionWinner;

    public InterServerMessage(int _seq, int _type) {
        super( _seq, _type);
    }

    public InterServerMessage(int _seq, int _type, int _subType) {
        super( _seq, _type);
        subType = _subType;
    }

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

    /*
        Will be used to identify a server during the diffusion process
     */
    public void setIdentifier( SocketAddress _identifier ) {
        identifier = _identifier;
    }
    public SocketAddress getIdentifier() {
        return identifier;
    }
    public void setElectionWinner( SocketAddress _electionWinner) {
        electionWinner = _electionWinner;
    }
    public SocketAddress getElectionWinner() {
        return electionWinner;
    }
    public int getSubType(){ return subType; }
    public void setMessage( Serializable _message) {
        message = _message;
    }
    public Serializable getMessage() {
        return message;
    }
}
