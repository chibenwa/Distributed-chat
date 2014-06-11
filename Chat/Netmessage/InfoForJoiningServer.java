package Chat.Netmessage;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by benwa on 6/9/14.
 *
 * License : GLP 2.0
 *
 * A class that is used into InterServerMessage serializable field to carry messages about server joining notification
 */
public class InfoForJoiningServer implements Serializable {
    /**
     * List of server
     */
    public ArrayList<Serializable> serversList;
    /**
     * List of pseudo
     */
    public ArrayList<Serializable> pseudoList;
}
