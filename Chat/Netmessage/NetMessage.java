package Chat.Netmessage;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by benwa on 6/7/14.
 *
 * License : GLP 2.0
 *
 * Base of all message transiting via Network
 */
public abstract class NetMessage implements Serializable{
    /**
     * Date the message was sent
     */

    private Date date;

    /**
     * Sequence order.
     */

    private int seq;

    /**
     * Type of the message. It is detailed in subclasses
     */

    private int type;

    /**
     * Error code of the message. Detailed in subclasses
     */

    protected int errorCode = 0;

    /**
     *
     * Get the date the message was sent
     *
     * @return the message was sent
     */

    public Date getDate() {
        return date;
    }

    /**
     * Gives the message type
     *
     * @return Returns the message type
     */

    public int getType() {
        return type;
    }

    /**
     * Returns sequence order
     *
     * @return Sequence order
     */

    public int getSeq() {
        return seq;
    }

    /**
     * Constructor
     *
     * @param _seq Sequence number
     * @param _type Message type
     */

    public NetMessage(int _seq, int _type) {
        date = new Date();
        seq = _seq;
        type = _type;
    }

    /**
     * Set the error code. Error code is detailed in subclasses
     *
     * @param error The error
     */
    public void setErrorCode( int error ) {
        errorCode = error;
    }

    /**
     * Get the error code. Error code is detailed in subclasses
     *
     * @return Error code
     */

    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Tells if an error is set
     *
     * @return True if there is an error, false in other cases
     */

    public Boolean hasError() {
        return errorCode != 0;
    }

    /**
     * Set sequence number
     *
     * @param _seq Sequence number
     */

    public void setSeq(int _seq) {
        seq=_seq;
    }

    /**
     * Print the error message associated with the error code.
     *
     * Overwritten in subclasses
     */

    public void  printErrorCode() {

    }
}
