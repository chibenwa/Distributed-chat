package Chat.Server;

import Chat.Netmessage.InterServerMessage;
import Chat.Utils.ClientStruct;

import java.io.IOException;
import java.io.Serializable;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by benwa on 6/8/14.
 *
 * License : GLP 2.0
 *
 * This class is used to manage an echo on our networks.
 * It collect data on all node of our network and return it to the initiator.
 *
 * As we would collect different data, this class will be overwritten, with minor changes.
 */
public class EchoManager {
    /**
     * The NetManager we will use to send messages
     */
    protected NetManager netManager;
    /**
     * A hash map that holds data we collected for each wave
     */
    private HashMap<SocketAddress, HashMap<Integer, ArrayList<Serializable>>> datasCollected;
    /**
     * Number of messages we received for each wave
     */
    private HashMap<SocketAddress, HashMap<Integer, Integer>> messageCount;
    /**
     * The server we should return our collected data to.
     */
    private HashMap<SocketAddress, HashMap<Integer, ClientStruct>> fathers;
    /**
     * We should be able to recognize our identifier
     */
    protected SocketAddress p = null;
    /**
     * Identity of the Message. Couple identifier (p) and serverSequence allow us to identify a wave.
     */
    private int serverSequence = 0;

    /**
     * Set our server wave identifier
     *
     * @param _p Identifier of this server
     */
    public void setP(SocketAddress _p) {
        if (p == null) {
            p = _p;
        }
    }

    /**
     * Initialize our EchoManager
     *
     * @param _netManager The NetManager we will use to send messages
     */
    public EchoManager(NetManager _netManager) {
        netManager = _netManager;
        datasCollected = new HashMap<SocketAddress, HashMap<Integer, ArrayList<Serializable>>>();
        messageCount = new HashMap<SocketAddress, HashMap<Integer, Integer>>();
        fathers = new HashMap<SocketAddress, HashMap<Integer, ClientStruct>>();
    }

    /**
     * While the listening thread will spot a Echo message, it will call this method to make the Echo manager aware of it.
     *
     * @param message The message that happened to be an echo message.
     * @param father The server connection that sent us this message
     * @return Data collected from the echo from the message our server should deal with. If the Echo is not over, or if we are not at the origin of this echo, null pointer is returned.
     */
    public ArrayList<Serializable> processInput(InterServerMessage message, ClientStruct father) {
        SocketAddress identifier = message.getIdentifier();
        int nbEcho = message.getSeq();
        if (datasCollected.get(identifier) == null) {
            System.out.println("First wave from " + identifier);
            // First echo wave launched by this server
            datasCollected.put(identifier, new HashMap<Integer, ArrayList<Serializable>>());
            messageCount.put(identifier, new HashMap<Integer, Integer>());
            fathers.put(identifier, new HashMap<Integer, ClientStruct>());
        }
        HashMap<Integer, ArrayList<Serializable>> originData = datasCollected.get(identifier);
        HashMap<Integer, Integer> originMessagesCount = messageCount.get(identifier);
        Boolean firstWaveMessage = false;
        if (originData.get(nbEcho) == null) {
            // New wave here
            System.out.println("New wave : " + nbEcho);
            originData.put(nbEcho, new ArrayList<Serializable>());
            originMessagesCount.put(nbEcho, 0);
            fathers.get(identifier).put(nbEcho, father);
            firstWaveMessage = true;
        }
        // Increment wave number
        originMessagesCount.put(nbEcho, originMessagesCount.get(nbEcho) + 1);
        System.out.println("Current messages from this wave : " + originMessagesCount.get(nbEcho));
        // Merge data...
        ArrayList<Serializable> waveData = originData.get(nbEcho);
        ArrayList<Serializable> messageContent = (ArrayList<Serializable>) message.getMessage();
        for (Serializable serializable : messageContent) {
            if (!isInData(waveData, serializable)) {
                waveData.add(serializable);
            }
        }
        if (firstWaveMessage) {
            ArrayList<Serializable> nodeDatas = getNodeData();
            for (Serializable data : nodeDatas) {
                if (!isInData(waveData, data))
                    waveData.add(data);
            }
            message.setMessage(waveData);
            netManager.getState().broadcastTokenWithoutFather(father, message);
        }
        if (originMessagesCount.get(nbEcho) == netManager.getState().getNbConnectedServers()) {
            System.out.println("Last message from the wave ;-)");
            // Last message of our broadcast
            if (identifier.toString().compareTo(p.toString()) == 0) {
                // We launched it. Return it to our manager
                messageCount.get(identifier).remove(nbEcho);
                datasCollected.get(identifier).remove(nbEcho);
                return waveData;
            } else {
                System.out.println("Send result to father ...");
                message.setMessage(waveData);
                try {
                    fathers.get(identifier).get(nbEcho).getFullDuplexMessageWorker().sendMsg(2, message);
                } catch (IOException ioe) {
                    System.out.println("Failed to send report to father");
                }
                messageCount.get(identifier).remove(nbEcho);
                datasCollected.get(identifier).remove(nbEcho);
            }
        }
        return null;
    }

    /**
     * Utility used to know if a serialized data is contained in a an array of serialized data
     *
     * @param data Array of value we want to look inside
     * @param candidate The value we want to find in the data
     * @return True if candidate is in data, false in other cases
     */
    private Boolean isInData(ArrayList<Serializable> data, Serializable candidate) {
        for (Serializable serializable : data) {
            if (isSerializableEqual(serializable, candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * Test equality between two Serializable. It uses cast depending on the type of data collected by the echo manager.
     * Overwritten in subclasses
     *
     * @param a First value
     * @param b Second value
     * @return True if a contains the same data as b, false in other cases
     */

    protected Boolean isSerializableEqual(Serializable a, Serializable b) {
        return false;
    }

    /**
     * Data to collect on this node. Again these data depends of the type of Echo we are dealing with
     * Overwritten in subclasses
     *
     * @return Data to collect on this node.
     */

    protected ArrayList<Serializable> getNodeData() {
        return new ArrayList<Serializable>();
    }

    /**
     * Launch an echo to retrieve data across the network
     * Used by subclasses to provide a method for that with no parameters
     *
     * @param echoId is the type of echo we want to launch.
     */
    protected void launchEcho(int echoId) {
        serverSequence++;
        InterServerMessage message = new InterServerMessage(serverSequence, echoId);
        message.setIdentifier(p);
        ArrayList<Serializable> messageContent = getNodeData();
        message.setMessage(messageContent);
        netManager.getState().broadcastInterServerMessage(message);
    }

}

