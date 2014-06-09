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
 */
public class EchoManager {
    protected NetManager netManager;
    private HashMap<SocketAddress, HashMap< Integer, ArrayList<Serializable> > > datasCollected;
    private HashMap<SocketAddress, HashMap< Integer, Integer> > messageCount;
    private HashMap<SocketAddress, HashMap< Integer, ClientStruct> > fathers;
    protected SocketAddress p = null;
    private int serverSequence = 0;

    public void setP(SocketAddress _p) {
        if( p == null) {
            p = _p;
        }
    }

    public EchoManager( NetManager _netManager) {
        netManager = _netManager;
        datasCollected = new HashMap<SocketAddress, HashMap<Integer, ArrayList<Serializable>>>();
        messageCount = new HashMap<SocketAddress, HashMap<Integer, Integer>>();
        fathers = new HashMap<SocketAddress, HashMap<Integer, ClientStruct>>();
    }

    public ArrayList<Serializable> processInput(InterServerMessage message, ClientStruct father) {
        SocketAddress identifier = message.getIdentifier();
        int nbEcho = message.getSeq();
        if( datasCollected.get(identifier) == null ) {
            System.out.println("First wave from " + identifier);
            // First echo wave launched by this server
            datasCollected.put( identifier, new HashMap< Integer, ArrayList<Serializable>>() );
            messageCount.put( identifier, new HashMap<Integer, Integer>());
            fathers.put( identifier, new HashMap<Integer, ClientStruct>());
        }
        HashMap< Integer, ArrayList<Serializable>> originDatas = datasCollected.get( identifier );
        HashMap<Integer, Integer> originMessagesCount = messageCount.get( identifier);
        Boolean firstWaveMessage = false;
        if( originDatas.get(nbEcho) == null ) {
            // New wave here
            System.out.println("New wave : " + nbEcho);
            originDatas.put(nbEcho, new ArrayList<Serializable>());
            originMessagesCount.put(nbEcho, 0);
            fathers.get(identifier).put(nbEcho, father);
            firstWaveMessage = true;
        }
        // Increment wave number
        originMessagesCount.put(nbEcho, originMessagesCount.get(nbEcho) + 1 );
        System.out.println("Current messages from this wave : " + originMessagesCount.get(nbEcho) );
        // Merge datas...
        ArrayList<Serializable> waveDatas = originDatas.get(nbEcho);
        ArrayList<Serializable> messageContent = (ArrayList<Serializable>) message.getMessage();
        for(Serializable serializable : messageContent) {
            if( !isInDatas(waveDatas, serializable)) {
                waveDatas.add( serializable);
            }
        }
        if( firstWaveMessage ) {
            ArrayList<Serializable> nodeDatas = getNodeDatas();
            for(Serializable data : nodeDatas) {
                if( !isInDatas(waveDatas, data))
                    waveDatas.add(data);
            }
            message.setMessage(waveDatas);
            netManager.getState().broadcastTokenWithoutFather(father, message);
        }
        if( originMessagesCount.get(nbEcho) == netManager.getState().getNbConnectedServers() ) {
            System.out.println("Last message from the wave ;-)");
            // Last message of our broadcast
            if( identifier.toString().compareTo(p.toString()) == 0) {
                // We launched it. Return it to our manager
                ArrayList<Serializable> result = waveDatas;
                // Memory cleanup
                messageCount.get( identifier).remove(nbEcho);
                datasCollected.get( identifier).remove(nbEcho);
                return result;
            } else {
               System.out.println("Send result to father ...");
               message.setMessage( waveDatas );
               try {
                   fathers.get(identifier).get(nbEcho).getFullDuplexMessageWorker().sendMsg(2, message);
               } catch( IOException ioe ) {
                   System.out.println("Failed to send report to father");
               }
               // Memory cleanup
               messageCount.get( identifier).remove(nbEcho);
               datasCollected.get( identifier).remove(nbEcho);
            }
        }

        return null;
    }

    private Boolean isInDatas( ArrayList<Serializable> datas, Serializable candidat ) {
        for( Serializable serializable : datas) {
            if( isSerializableEqual(serializable, candidat) ) {
                return true;
            }
        }
        return false;
    }

    // TODO Will b overwritten by subclasses
    protected Boolean isSerializableEqual(Serializable a, Serializable b) {
        return false;
    }

    // TODO overwrite it
    protected ArrayList<Serializable> getNodeDatas() {
        return new ArrayList<Serializable>();
    }

    protected void launchEcho( int echoId ) {
        serverSequence++;
        InterServerMessage message = new InterServerMessage(serverSequence,echoId);
        message.setIdentifier(p);
        ArrayList<Serializable> messageContent = getNodeDatas();
        message.setMessage(messageContent);
        netManager.getState().broadcastInterServerMessage(message);
    }

}
