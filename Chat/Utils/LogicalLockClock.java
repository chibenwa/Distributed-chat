package Chat.Utils;

import java.io.Serializable;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by benwa on 6/16/14.
 *
 *  License : GLP 2.0
 *
 *  A class that holds data for the lockManager. We use it to maintain the Token inner structure
 */
public class LogicalLockClock extends SendableHashMap {
    /**
     * Used to generate the token using the list of available servers.
     * @param serversConnectedOnOurNetwork list of available servers.
     */
    public void generateFromServerList( ArrayList<Serializable> serversConnectedOnOurNetwork ) {
        map.clear();
        for(Serializable serializable : serversConnectedOnOurNetwork) {
            SocketAddress serverIdentifier = (SocketAddress) serializable;
            put(serverIdentifier, 0);
        }
    }

    /**
     * Get the next server that made a request for lock.
     * @param ourDemands Our demands hash map we will be compared to.
     * @param ourSocketAddress Our identifier ( to know were to begin our searches )
     * @return Null if no follower was found, a pointer to the next one in other cases
     */
    public SocketAddress getNext(SendableHashMap ourDemands, SocketAddress ourSocketAddress) {
        Key ourKey = new Key(ourSocketAddress);
        Boolean hasLocatedOurAddress = false;
        Iterator it = map.entrySet().iterator();
        while( it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            if( pairs.getKey().equals(ourKey) ) {
                hasLocatedOurAddress = true;
                continue;
            }
            if( hasLocatedOurAddress) {
                if( ourDemands.get( ( Key ) pairs.getKey() ) > get( ( Key ) pairs.getKey() ) ) {
                    System.out.println(" - - - - - - - Selecting address : " + ((Key) pairs.getKey()).getKeySocket() );
                    return ((Key) pairs.getKey()).getKeySocket();
                }
            }

        }
        it = map.entrySet().iterator();
        while( it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            if( pairs.getKey().equals(ourKey) ) {
                System.out.println("Failure to find a follower...");
                return null;
            }
            if( hasLocatedOurAddress) {
                if( ourDemands.get( ( Key ) pairs.getKey() ) > get( ( Key ) pairs.getKey() ) ) {
                    return ((Key) pairs.getKey()).getKeySocket();
                }
            }
        }
        return null;
    }

    /**
     * Display our logical clock.
     */
    public void display() {
        System.out.println("Logical clock :");
        Iterator it = map.entrySet().iterator();
        while( it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            Key key = (Key)pairs.getKey();
            int value = (Integer) pairs.getValue();
            Object o = map.get(key);
            if( o == null) {
                System.out.println("Not the same value inside the logical clock. Critic topological knowledge error.");
                continue;
            }
            System.out.println("Server : " + key.getSocketString() + " <=> " + value);
        }
    }
}
