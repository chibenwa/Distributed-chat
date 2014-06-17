package Chat.Utils;

import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by benwa on 6/12/14.
 *
 * License : GLP 2.0
 *
 * A class that implements a Serializable vectorial clock for causal diffusion.
 */

public class VectorialClock extends SendableHashMap {

    /**
     * Tells if the current object is causally before the passed argument
     *
     * @param vectorialClock Vectorial clock we are compared with.
     * @return True if our object is causally before the one passed in parameter
     */

    public Boolean isCausalBefore( VectorialClock vectorialClock) {
        Iterator it = vectorialClock.map.entrySet().iterator();
        while( it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            Key key = (Key)pairs.getKey();
            Integer value = (Integer) pairs.getValue();
            Object o = map.get(key);
            if( o == null) {
                System.out.println("Not the same value inside the vectorial clock. Critic topological knowledge error.");
                return false;
            }
            if( value > (Integer) map.get(key) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells if the current object is causally after the passed argument
     *
     * @param vectorialClock Vectorial clock we are compared with.
     * @return True if our object is causally after the one passed in parameter
     */
    public Boolean isCausalAfter( VectorialClock vectorialClock) {
        Iterator it = vectorialClock.map.entrySet().iterator();
        while( it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            Key key = (Key)pairs.getKey();
            Integer value = (Integer) pairs.getValue();
            Object o = map.get(key);
            if( o == null) {
                System.out.println("Not the same value inside the vectorial clock. Critic topological knowledge error.");
                return false;
            }
            if( value < this.get(key) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * We check if this message is part of a message that we can accept.
     *
     * @param localVectorialClock We compared the received vectorial clock with the one we already have on our C broadcast manager
     * @param initiator Identifier of the process that sent the C Broadcast
     * @param ourLocalAddress Our server identifier
     * @return True if the current message can be accepted regarding its ( this one ! ) vectorial clock
     */

    public Boolean isNext( VectorialClock localVectorialClock, SocketAddress initiator, SocketAddress ourLocalAddress ) {
        // Iterate on local vectorial clock
        Key localAddressKey = new Key( ourLocalAddress);
        Key initiatorKey = new Key(initiator);
        if( this.get( initiator) != localVectorialClock.get(initiator)+1) {
            return false;
        }
        Iterator it = localVectorialClock.map.entrySet().iterator();
        while( it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            Key key = (Key)pairs.getKey();
            Integer value = (Integer) pairs.getValue();
            Object o = map.get(key);
            if( o == null) {
                System.out.println("Not the same value inside the vectorial clock. Critic topological knowledge error.");
                return false;
            }
            if( key.equals(localAddressKey) ) {
                return true;
            }
            if( key.equals(initiatorKey) ) {
                // Hack because I had problems with equality...
                Integer loc = value + 1;
                if( loc.toString().compareTo( map.get(initiatorKey).toString() ) == 0){
                    continue;
                }
                return false;
            }
            if( value > this.get(key) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * We increment the value associated with this socket address.
     *
     * @param socketKey value to increment
     */

    public void incrementKey( SocketAddress socketKey) {
        map.put( new Key(socketKey), (Integer) map.get( new Key(socketKey)) + 1);
    }

    /**
     * A method that clear our vectorial clock
     */
    public void clear() {
        map.clear();
    }

    /**
     * A method to display our vectorial class.
     * Debug purposes.
     */
    public void display() {
        System.out.println("Vectorial clock :");
        Iterator it = map.entrySet().iterator();
        while( it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            Key key = (Key)pairs.getKey();
            int value = (Integer) pairs.getValue();
            Object o = map.get(key);
            if( o == null) {
                System.out.println("Not the same value inside the vectorial clock. Critic topological knowledge error.");
                continue;
            }
            System.out.println("Server : " + key.getSocketString() + " <=> " + value);
        }
    }
}
