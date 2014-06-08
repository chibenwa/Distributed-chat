package Chat.Netmessage;

import java.io.Serializable;
import java.net.SocketAddress;
import java.util.Date;

/**
 * Created by benwa on 6/7/14.
 *
 * License : GLP 2.0
 */
public abstract class NetMessage implements Serializable{
    private Date date;
    private int seq;
    private int type;
    protected int errorCode = 0;
    public Date getDate() {
        return date;
    }
    public int getType() {
        return type;
    }
    public int getSeq() {
        return seq;
    }
    public NetMessage(int _seq, int _type) {
        date = new Date();
        seq = _seq;
        type = _type;
    }
    public void setErrorCode( int error ) {
        errorCode = error;
    }
    public int getErrorCode() {
        return errorCode;
    }
    public Boolean hasError() {
        return errorCode != 0;
    }
    public void setSeq(int _seq) {
        seq=_seq;
    }
}
