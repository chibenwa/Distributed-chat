package Chat;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by benwa on 6/5/14.
 */
public class ChatData implements Serializable {
    private Date date;
    private int seq;
    private String message;
    /*
     * Type 0 : Request login
     * Type 1 : Accept login
     * Type 2 : Message
     * Type 3 : Join notification
     * Type 4 : Leave notification
     * Type 5 : Deconnection request
     * Type 6 : Errors notifications
     * Type 7 : Demand for a list of user
     * Type 8 : Answer to a list of user
     */
    private int type;
    private String pseudo; // set by server only
    private Boolean pseudoSet;
    /*
        Type 0 : No error
        Type 1 : The pseudo is already used
        Type 2 : You should ask first for a pseudo
        Type 3 : Client should not accept a login
        Type 4 : Client should not send join notification
        Type 5 : Client should not send Leave notification
        Type 6 : Message type not taken in account by protocole management
        Type 7 : When requesting a login, you should set the login field
        Type 8 : Client should not send a user list to a server
        Type 9 : Server provided a user list while the client never asked for
     */
    private int errorCode = 0;
    public Date getDate() {
        return date;
    }
    public int getType() {
        return type;
    }
    public String getMessage() {
        return message;
    }
    public int getSeq() {
        return seq;
    }
    public ChatData(int _seq, int _type, String _message) {
        date = new Date();
        seq = _seq;
        type = _type;
        message = _message;
        pseudoSet = false;
        pseudo = "";
    }
    public ChatData(int _seq, int _type, String _message, String _pseudo) {
        date = new Date();
        seq = _seq;
        type = _type;
        message = _message;
        pseudo = _pseudo;
        pseudoSet = true;
    }
    public Boolean hasPseudo() {
        return pseudoSet;
    }
    public String getPseudo() {
        return pseudo;
    }
    public void setErrorCode( int error ) {
        errorCode = error;
    }
    public int getErrorCode() {
        return errorCode;
    }
    public Boolean hasError() {
        return errorCode == 0;
    }
    public void printErrorCode() {
        /*
        Type 0 : No error
        Type 1 : The pseudo is already used
        Type 2 : You should ask first for a pseudo
        Type 3 : Client should not accept a login
        Type 4 : Client should not send join notification
        Type 5 : Client should not send Leave notification
        Type 6 : Message type not taken in account by protocole management
        Type 7 : When requesting a login, you should set the login field
        Type 8 : Client should not send a user list to a server
        Type 9 : Server provided a user list while the client never asked for
        */
        switch(errorCode) {
            case 0 :
                System.out.println("No error");
                break;
            case 1 :
                System.out.println("The pseudo is already used");
                break;
            case 2 :
                System.out.println("You should ask first for a pseudo");
                break;
            case 3 :
                System.out.println("Client should not accept a login");
                break;
            case 4 :
                System.out.println("Client should not send join notification");
                break;
            case 5 :
                System.out.println("Client should not send Leave notification");
                break;
            case 6 :
                System.out.println("Message type not taken in account by protocole management");
                break;
            case 7:
                System.out.println("When requesting a login, you should set the login field");
                break;
            case 8:
                System.out.println("Client should not send a user list to a server");
                break;
            case 9:
                System.out.println("Server provided a user list while the client never asked for");
            default:
                System.out.println("None specified error code");
                break;
        }
    }
}