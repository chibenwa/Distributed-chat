package Chat.Server;

import java.io.Serializable;
import java.net.SocketAddress;
import java.util.ArrayList;

/**
 * Created by benwa on 6/9/14.
 */
public class EchoServerListManager extends EchoManager {
    public EchoServerListManager(NetManager netManager) {
        super(netManager);
    }

    // TODO Will b overwritten by subclasses
    protected Boolean isSerializableEqual(Serializable a, Serializable b) {
        SocketAddress as = (SocketAddress) a;
        SocketAddress bs = (SocketAddress) b;
        return as.toString().compareTo(bs.toString()) == 0;
    }

    // TODO overwrite it
    protected ArrayList<Serializable> getNodeDatas() {
        ArrayList<Serializable> res = new ArrayList<Serializable>();
        res.add(p);
        return res;
    }

    public void launchEcho() {
        super.launchEcho(7);
    }

}
