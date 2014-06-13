package Chat.Utils;

import java.io.Serializable;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by benwa on 6/12/14.
 *
 * License : GLP 2.0
 *
 * A class that implements a Serializable vectorial clock for causal diffusion.
 */

public class VectorialClock implements Serializable {

    /**
     * The hash map that holds data.
     */

    HashMap map = new HashMap();

    /**
     * A static class to allow us to serialize our hash map.
     */

    static class Key implements Serializable {
        /**
         * The real data hold by this Key.
         */
        private SocketAddress keySocket;

        /**
         * Basic constructor.
         *
         * @param keySocket The SocketAddress do build Key arround.
         */
        Key(SocketAddress keySocket)
        {
            this.keySocket = keySocket;
        }

        /**
         * Calculate key hash code
         *
         * @return The hash code of the key.
         */
        @Override
        public int hashCode()
        {
            return keySocket.hashCode();
        }

        /**
         * Test if our key is equal to an other object.
         *
         * @param obj The object to test equality with.
         * @return True if both Keys hold the same data, false in other cases.
         */
        @Override
        public boolean equals(Object obj)
        {
            Key otherKey = (Key) obj;
            return keySocket.equals(otherKey.keySocket);
        }

        public String getSocketString() {
            return keySocket.toString();
        }
    }

    /**
     * Proxy method to put a value inside the vectorial clock.
     *
     * @param key key where to put the value
     * @param value the value to put
     */

    public void put( SocketAddress key, Integer value) {
        map.put(new Key(key), value);
    }

    /**
     * Proxy method to get a value inside the vectorial clock
     *
     * @param key Socket address that points to this value
     * @return The value associated with this Socket address
     */

    public Integer get( SocketAddress key) {
        return (Integer ) map.get( new Key(key) );
    }

    /**
     * Utility to get a value inside the vectorial clock
     *
     * @param key Key that points to this value
     * @return The value associated with this Key
     */

    private Integer get( Key key) {
        return (Integer ) map.get( key );
    }

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
                continue;
            }
            if( key.equals(initiatorKey) ) {
                continue;
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
