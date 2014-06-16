package Chat.Utils;

import Chat.Server.NetManager;

/**
 * Created by benwa on 6/17/14.
 *
 * License : GLP 2.0
 *
 * A resource Visitor that enable and disable logging in a NetManager.
 */
public class LogResourceVisitor implements ResourceVisitor {
    /**
     * The NetManger on which we will start and stop logging
     */
    private NetManager netManager;

    /**
     * The basic constructor.
     *
     * @param _netManager The NetManger on which we will start and stop logging
     */
    public LogResourceVisitor(NetManager _netManager) {
        netManager = _netManager;
    }

    /**
     * enable logging
     */
    public void startUsingResource() {
        netManager.enableLogging();
    }

    /**
     * disable logging
     */
    public void stopUsingResource() {
        netManager.disableLogging();
    }
}
