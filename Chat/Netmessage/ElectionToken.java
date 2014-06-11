package Chat.Netmessage;

import java.net.SocketAddress;

/**
 * Created by benwa on 6/7/14.
 *
 * A message used during elections.
 *
 * License : GLP 2.0
 */
public class ElectionToken  extends NetMessage{

    /**
        The election process uses as criterion the couple ip address and port ( unique util you do stupid things like NAT. Come on man, do not share protocol layers, that is really ugly.
     */
    SocketAddress r;

    /**
     *
     * @param _type Election token type
     *              * 0 : JETON
     *              * 1 : GAGNANT
     */

    public ElectionToken( int _type ) {
        super(0, _type);
    }

    /**
     * Set sender id ( r in the algorithm )
     *
     * @param _r sender id
     */
    public void setR( SocketAddress _r) {
        r =_r;
    }

    /**
     * Get sender id
     *
     * @return sender id
     */
    public SocketAddress getR() {
        return r;
    }
}
