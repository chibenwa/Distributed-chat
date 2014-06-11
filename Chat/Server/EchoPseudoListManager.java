package Chat.Server;

import java.io.Serializable;
import java.net.SocketAddress;
import java.util.ArrayList;

/**
 * Created by benwa on 6/9/14.
 *
 * License : GLP 2.0
 *
 * An echo manager that collect connected clients across our network.
 */
public class EchoPseudoListManager extends EchoManager {

    /**
     * Basic constructor
     *
     * @param netManager The NetManager we will use to send messages
     */
    public EchoPseudoListManager(NetManager netManager) {
        super(netManager);
    }

    /**
     * Tells us if two serialized pseudo are equals
     *
     * @param a First value
     * @param b Second value
     * @return True if they happens to be tha same value, false in other cases
     */
    protected Boolean isSerializableEqual(Serializable a, Serializable b) {
        String as = (String) a;
        String bs = (String) b;
        return as.compareTo(bs) == 0;
    }

    /**
     * Data to collect on this node. Here this is simply the pseudo list.
     *
     * @return pseudo list
     */
    protected ArrayList<Serializable> getNodeDatas() {
        return netManager.getState().getConnectedClients();
    }

    /**
     * launches and Echo wave to get all clients connect on the Network.
     */
    public void launchEcho() {
        super.launchEcho(6);
    }

}
