package Chat.Netmessage;

import java.net.SocketAddress;

/**
 * Created by benwa on 6/7/14.
 *
 * License : GLP 2.0
 */
public class ElectionToken  extends NetMessage{

    /*
        The election process uses as criterion the couple ip address and port ( unique util you do stupid things like NAT. Come on man, do not share protocol layers, that is really ugly.
     */
    SocketAddress r;

    /*
        Type :

        * 0 : JETON
        * 1 : GAGNANT

     */

    public ElectionToken( int _type ) {
        super(0, _type);
    }
    public void setR( SocketAddress _r) {
        r =_r;
    }
    public SocketAddress getR() {
        return r;
    }
}
