package Chat.Netmessage;

/**
 * Created by benwa on 6/7/14.
 *
 * Message exchanged between servers
 *
 * License : GLP 2.0
 *
 * Serializable that carry a chat message between client and server
 *
 * Error code :
 *
 *   Type 0 : No error
 *   Type 1 : The pseudo is already used
 *   Type 2 : You should ask first for a pseudo
 *   Type 3 : Client should not accept a login
 *   Type 4 : Client should not send join notification
 *   Type 5 : Client should not send Leave notification
 *   Type 6 : Message type not taken in account by protocol management
 *   Type 7 : When requesting a login, you should set the login field
 *   Type 8 : Client should not send a user list to a server
 *   Type 9 : Server provided a user list while the client never asked for
 *   Type 10 : You are already active on this node, we can not set up spare connection !
 */
public class ChatData extends NetMessage {

    /**
     *
     * Type of message : Meaning
     *
     * Type 0 : Request login
     * Type 1 : Accept login
     * Type 2 : Message
     * Type 3 : Join notification
     * Type 4 : Leave notification
     * Type 5 : Deconnection request
     * Type 7 : Demand for a list of user
     * Type 8 : Answer to a list of user
     * Type 9 : Demand spare connection
     * Type 10 : Activate spare connection
     * Type 11 : Spare switching performed server side
     * Type 12 : Private message
     * Type 13 : Demand for server list ( + answer )
     * Type 42 : Errors notifications
     */

    /**
     * The message that we want to transmit
     */
    private String message;
    /**
     * The pseudo we use
     */
    private String pseudo;
    /**
     * The pseudo we want to send a message to ( for private messages )
     */
    public String pseudoDestination;
    /**
     * Tells us if the pseudo field is set
     */
    private Boolean pseudoSet;

    /**
     * Get the message field.
     *
     * @return The message
     */

    public String getMessage() {
        return message;
    }

    /**
     *
     * Constructor
     *
     * @param _seq Sequence number
     * @param _type Type of the message
     *                   * Type 0 : Request login
     *                   * Type 1 : Accept login
     *                   * Type 2 : Message
     *                   * Type 3 : Join notification
     *                   * Type 4 : Leave notification
     *                   * Type 5 : Disconnection request
     *                   * Type 7 : Demand for a list of user
     *                   * Type 8 : Answer to a list of user
     *                   * Type 9 : Demand spare connection
     *                   * Type 10 : Activate spare connection
     *                   * Type 11 : Spare switching performed server side
     *                   * Type 12 : Private message
     *                   * Type 13 : Demand for server list ( + answer )
     *                   * Type 42 : Errors notifications
     * @param _message Message
     */

    public ChatData(int _seq, int _type, String _message) {
        super( _seq, _type);
        message = _message;
        pseudoSet = false;
        pseudo = "";
    }

    /**
     *
     * @param _seq Sequence number
     * @param _type Type of the message
     *                   * Type 0 : Request login
     *                   * Type 1 : Accept login
     *                   * Type 2 : Message
     *                   * Type 3 : Join notification
     *                   * Type 4 : Leave notification
     *                   * Type 5 : Disconnection request
     *                   * Type 7 : Demand for a list of user
     *                   * Type 8 : Answer to a list of user
     *                   * Type 9 : Demand spare connection
     *                   * Type 10 : Activate spare connection
     *                   * Type 11 : Spare switching performed server side
     *                   * Type 12 : Private message
     *                   * Type 13 : Demand for server list ( + answer )
     *                   * Type 42 : Errors notifications
     * @param _message Message
     * @param _pseudo pseudo field
     */
    public ChatData(int _seq, int _type, String _message, String _pseudo) {
        super( _seq, _type);
        message = _message;
        pseudo = _pseudo;
        pseudoSet = true;
    }

    /**
     * Tells us if pseudo field is set in this request
     *
     * @return True if the pseudo is set, false in other case
     */
    public Boolean hasPseudo() {
        return pseudoSet;
    }

    /**
     * Get the pseudo field
     *
     * @return Return pseudo
     */
    public String getPseudo() {
        return pseudo;
    }

    /**
     * Print the message for a given error ( set in the message )
     */
    public void printErrorCode() {
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
