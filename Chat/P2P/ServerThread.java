package Chat.P2P;

import Chat.Server.NetManager;

/**
 * Created by benwa on 6/9/14.
 */
public class ServerThread extends Thread {
    private NetManager netManager;
    ServerThread(NetManager _netManager) {
        netManager = _netManager;
    }

    public void run() {
        netManager.launch();
    }

}
