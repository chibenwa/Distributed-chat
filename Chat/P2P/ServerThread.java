package Chat.P2P;

import Chat.Server.NetManager;

/**
 * Created by benwa on 6/9/14.
 *
 * This class allow you to launch a server network listener in a separated thread for P2P agent
 */

public class ServerThread extends Thread {
    private NetManager netManager;

    /**
     * Build the ServerThread
     *
     * @param _netManager Server network manager
     */

    ServerThread(NetManager _netManager) {
        netManager = _netManager;
    }

    /**
     * Launches server network listener in a separate thread for P2P agent
     */

    public void run() {
        netManager.launch();
    }

}
