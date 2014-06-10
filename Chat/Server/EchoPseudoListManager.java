package Chat.Server;

import java.io.Serializable;
import java.net.SocketAddress;
import java.util.ArrayList;

/**
 * Created by benwa on 6/9/14.
 */
public class EchoPseudoListManager extends EchoManager {

    public EchoPseudoListManager(NetManager netManager) {
        super(netManager);
    }

    // TODO Will b overwritten by subclasses
    protected Boolean isSerializableEqual(Serializable a, Serializable b) {
        String as = (String) a;
        String bs = (String) b;
        return as.compareTo(bs) == 0;
    }

    // TODO overwrite it
    protected ArrayList<Serializable> getNodeDatas() {
        return netManager.getState().getConnectedClients();
    }

    public void launchEcho() {
        super.launchEcho(6);
    }

}
