package Chat.Utils;

import java.io.Serializable;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by benwa on 6/16/14.
 *
 * License : GLP 2.0
 *
 * A hash map that is the base for Serializable Hash map to send across network.
 */
public class SendableHashMap implements Serializable{

    /**
     * The hash map that holds data.
     */

    HashMap map = new LinkedHashMap();

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

        public SocketAddress getKeySocket() {
            return keySocket;
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

    protected Integer get( Key key) {
        return (Integer ) map.get( key );
    }
}
