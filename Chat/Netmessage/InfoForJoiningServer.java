package Chat.Netmessage;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by benwa on 6/9/14.
 *
 * A class that is used into InterServerMessage serializable field to carry messages about server joining notification
 */
public class InfoForJoiningServer implements Serializable {
    // List of available servers
    public ArrayList<Serializable> serversList;
    public ArrayList<Serializable> pseudoList;
}
