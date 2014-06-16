package Chat.Utils;

import Chat.Server.NetManager;

/**
 * Created by benwa on 6/17/14.
 */
public class LogResourceVisitor implements ResourceVisitor {
    private NetManager netManager;
    public LogResourceVisitor(NetManager _netManager) {
        netManager = _netManager;
    }
    public void startUsingResource() {
        netManager.enableLogging();
    }
    public void stopUsingResource() {
        netManager.disableLogging();
    }
}
