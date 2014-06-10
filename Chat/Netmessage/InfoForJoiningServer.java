package Chat.Netmessage;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by benwa on 6/9/14.
 */
public class InfoForJoiningServer implements Serializable {
    // List of available servers
    public ArrayList<Serializable> serversList;
    public ArrayList<Serializable> pseudoList;
}
