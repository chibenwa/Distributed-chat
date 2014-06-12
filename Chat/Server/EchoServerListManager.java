package Chat.Server;

import java.io.Serializable;
import java.net.SocketAddress;
import java.util.ArrayList;

/**
 * Created by benwa on 6/9/14.
 */
public class EchoServerListManager extends EchoListManager {
    /**
     * Basic constructor
     * @param netManager The NetManager we will use to send our messages
     */
    public EchoServerListManager(NetManager netManager) {
        super(netManager);
    }

    /**
     * Tells if two serializable InetSocketAddress holds the same datas
     *
     * @param a First value
     * @param b Second value
     * @return True if both Serializable represent the same InetSocketAddress, false in other cases
     */
    protected Boolean isSerializableEqual(Serializable a, Serializable b) {
        SocketAddress as = (SocketAddress) a;
        SocketAddress bs = (SocketAddress) b;
        return as.toString().compareTo(bs.toString()) == 0;
    }

    /**
     * Get specific node data for EchoServerList ( Directly connected servers )
     *
     * @return Directly connected servers ( as Serializable )
     */

    protected ArrayList<Serializable> getNodeData() {
        ArrayList<Serializable> res = new ArrayList<Serializable>();
        res.add(p);
        return res;
    }

    /**
     * launches and Echo wave to get all servers connect on the Network.
     */
    public void launchEcho() {
        super.launchEcho(7);
    }

}
