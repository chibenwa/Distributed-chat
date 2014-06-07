package Chat.Utils;

import csc4509.FullDuplexMessageWorker;

/**
 * Created by benwa on 6/7/14.
 */
public class ConnectionStruct {
    private FullDuplexMessageWorker full;
    private static int cpt = 0;
    private int id;
    public ConnectionStruct( FullDuplexMessageWorker _full ) {
        full = _full;
        cpt++;
        id = cpt;
    }
    public int getId() {
        return id;
    }
    public FullDuplexMessageWorker getFullDuplexMessageWorker() {
        return full;
    }
}
