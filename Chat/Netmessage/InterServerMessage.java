package Chat.Netmessage;

/**
 * Created by benwa on 6/7/14.
 */
public class InterServerMessage extends NetMessage {
        /*

    Type definition !

        Type 0 : Hello I am a server...
        Type 1 : I noticed that you are a server !
        Type 2 : Demand to close connection ...
        Type 3 : Add demand received while in election. Please try later.
        Type 4 : Add answer received while in election. Please try later.
        Type 42 : Error
     */

    /*

    Error definition !

        0 : No error
        1 : The server already established a connection with the distant server. Making a new one is both pointless AND dangerous

     */

    public InterServerMessage(int _seq, int _type) {
        super( _seq, _type);
    }

    public void printErrorCode() {
        switch (errorCode) {
            case 0 :
                System.out.println("No error");
                break;
            case 1 :
                System.out.println("The server already established a connection with the distant server. Making a new one is both pointless AND dangerous");
                break;
            default:
                System.out.println("Unhandled error");
                break;
        }
    }
}