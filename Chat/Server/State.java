package Chat.Server;

import Chat.Netmessage.ChatData;
import Chat.Netmessage.ElectionToken;
import Chat.Netmessage.InterServerMessage;
import Chat.Netmessage.NetMessage;
import Chat.Utils.ClientStruct;
import Chat.Utils.ConnectionStruct;
import com.sun.org.apache.xpath.internal.operations.Bool;
import csc4509.FullDuplexMessageWorker;

import java.io.IOException;
import java.io.Serializable;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by benwa on 6/8/14.
 *
 * License : GLP 2.0
 */
public class State {
    private final ReentrantLock serverLock = new ReentrantLock();
    private final ReentrantLock clientLock = new ReentrantLock();
    private List<ClientStruct> cliStrs;
    private  List<ConnectionStruct> serverStrs;
    private Boolean standAlone = true;
    private HashMap<String,Boolean> pseudoList;
    private ArrayList<SocketAddress> serverConnectedOnOurNetwork;

    public State() {
        cliStrs = new ArrayList<ClientStruct>();
        serverStrs = new ArrayList<ConnectionStruct>();
        pseudoList = new HashMap<String, Boolean>();
        serverConnectedOnOurNetwork = new ArrayList<SocketAddress>();
    }

    /*
    A small utility method : we construct the list of the client directly connected to this server.

    Two usage nowadays :

        - Display it on clavier demand ( it will stay for debug purpose )
        - Answer client to this question : who is connected directly to this server

     */

    public String buildClientList() {
        clientLock.lock();
        String pseudoChunk = "";
        Boolean first = true;
        for( ClientStruct cls : cliStrs) {
            if( first ) {
                first = false;
            } else {
                pseudoChunk += ", ";
            }
            pseudoChunk += cls.getPseudo();
        }
        clientLock.unlock();
        return pseudoChunk;
    }

    /*
        Another utility function,
        which will send a message
        to all clients connected
        and registrated to our
        server.
     */

    public void broadcast( ChatData mes ) {
        clientLock.lock();
        for ( ClientStruct cls : cliStrs) {
            try {
                cls.getFullDuplexMessageWorker().sendMsg(0, mes);
            } catch( IOException ioe) {
                System.out.println("Failed to broadcast message");
            }
        }
        clientLock.unlock();
    }

    /*
        Utility function used twice :

        Tells us if we already
        registered the given
        connection as a server
     */

    public Boolean isServerConnectionEstablished( FullDuplexMessageWorker fdmw ) {
        serverLock.lock();
        for( ConnectionStruct conStr : serverStrs) {
            if( conStr.getFullDuplexMessageWorker() == fdmw ) {
                serverLock.unlock();
                return true;
            }
        }
        serverLock.unlock();
        return false;
    }


    public void addServer(ClientStruct cliStr){
        serverLock.lock();
        serverStrs.add(cliStr);
        standAlone = false;
        serverLock.unlock();
    }


        /*
        Utility function...

        It is providing us our the
        connected list of servers
        recognised as servers
     */

    public String getServerList() {
        serverLock.lock();
        String res = "";
        Boolean first = true;
        for( ConnectionStruct cstr : serverStrs ) {
            if( first ) {
                first = false;
            } else {
                res += ", ";
            }
            try {
                res += cstr.getFullDuplexMessageWorker().getChannel().getRemoteAddress().toString();
            } catch(IOException ioe) {
                System.out.println("Error getting remote server address");
                ioe.printStackTrace();
            }
        }
        serverLock.unlock();
        return res;
    }

    /*
        It closed all the connections registered
        as both clients and servers. It give us
        a new clean debug environment without
        launching each servers a new time (
        quite long with compilation )

        Useless in production environments but
        so useful while testing distributed algorithms
    */

    public void reInitNetwork() {
        clientLock.lock();
        serverLock.lock();
        // No fear for dead locks as this is the only place were we lock for both clients and servers locks
        for( ConnectionStruct cstr : serverStrs) {
            try{
                cstr.getFullDuplexMessageWorker().sendMsg(2, new InterServerMessage(0,2));
            } catch(IOException ioe) {
                System.out.println("Can not send a disconnection demand");
            }
        }
        for( ClientStruct clientStruct : cliStrs) {
            try {
                clientStruct.getFullDuplexMessageWorker().close();
            } catch (IOException ioe) {
                System.out.println("Can not close client connection");
            }
        }
        serverStrs.clear();
        cliStrs.clear();
        clientLock.unlock();
        serverLock.unlock();
    }

    /*
    Check if a pseudo is available...
     */

    public Boolean isPseudoTaken(String pseudo) {
        clientLock.lock();
        Boolean alredyExist = false;
        for (ClientStruct cls : cliStrs) {
            if (cls.getPseudo().equals(pseudo)) {
                alredyExist = true;
            }
        }
        clientLock.unlock();
        return alredyExist;
    }

    public void addClient(ClientStruct cliStr) {
        clientLock.lock();
        cliStrs.add(cliStr);
        clientLock.unlock();
    }

    public void removeClient(ClientStruct cliStr) {
        clientLock.lock();
        cliStrs.remove(cliStr);
        clientLock.unlock();
    }

    public void removeServer(ClientStruct toRemove) {
        serverLock.lock();
        serverStrs.remove(toRemove);
        if( cliStrs.size() ==  0 ) {
            standAlone = true;
        }
        serverLock.unlock();
    }

    public int getNbConnectedServers() {
        return serverStrs.size();
    }

    public void broadcastToken( ElectionToken electionToken, String ioErrorMessage) {
        // No need to protect this : accessed from only one thread, and not disturbed thanks to election lock
        for( ConnectionStruct connectionStruct : serverStrs) {
            try {
                connectionStruct.getFullDuplexMessageWorker().sendMsg(1, electionToken);
            } catch (IOException ioe) {
                System.out.println(ioErrorMessage);
            }
        }
    }

    public void broadcastTokenWithoutFather(ClientStruct father, ElectionToken newToken) {
        // No need to protect this : accessed from only one thread, and not disturbed thanks to election lock
        for( ConnectionStruct connectionStruct : serverStrs) {
            if( connectionStruct != father) {
                try {
                    connectionStruct.getFullDuplexMessageWorker().sendMsg(1, newToken);
                } catch (IOException ioe) {
                    System.out.println("Error while sending the new token");
                }
            }
        }
    }

    public void broadcastTokenWithoutFather(ClientStruct father, InterServerMessage newToken) {
        // No need to protect this : accessed from only one thread, and not disturbed thanks to election lock
        for( ConnectionStruct connectionStruct : serverStrs) {
            if( connectionStruct != father) {
                try {
                    connectionStruct.getFullDuplexMessageWorker().sendMsg(2, newToken);
                } catch (IOException ioe) {
                    System.out.println("Error while sending the new token");
                }
            }
        }
    }

    public void broadcastInterServerMessage( InterServerMessage mes) {
        for( ConnectionStruct connectionStruct : serverStrs) {
            try {
                connectionStruct.getFullDuplexMessageWorker().sendMsg(2, mes);
            } catch (IOException ioe) {
                System.out.println("Error while broadcasting server message");
            }
        }
    }

    public Boolean getStandAlone() {
        return standAlone;
    }

    public void setPseudoList( ArrayList<String> _pseudoList) {
        pseudoList = new HashMap<String, Boolean>();
        for( String pseudo : _pseudoList) {
            pseudoList.put(pseudo, true);
        }
    }

    public void addPseudo( String newPseudo){
            pseudoList.put(newPseudo, true);
    }

    public void removePseudo( String pseudo ) {
        pseudoList.remove( pseudo );
    }

    public Boolean isPseudoUsed(String pseudo) {
        Boolean res = pseudoList.get(pseudo);
        if( res == null) {
            return false;
        }
        return res;
    }

    public String getClientsString() {
        String res = "";
        Iterator it = pseudoList.entrySet().iterator();
        Boolean isFirst = true;
        while( it.hasNext()) {
            Map.Entry pairs = ( Map.Entry) it.next();
            if( !isFirst ) {
                res += ", ";
            } else {
                isFirst  =false;
            }
            res += pairs.getKey();
        }
        return res;
    }

    public ClientStruct getClientByPseudo(String pseudo ) {
        for( ClientStruct clientStruct : cliStrs) {
            if( clientStruct.getPseudo().compareTo(pseudo) == 0 ) {
                return clientStruct;
            }
        }
        return null;
    }

    public void setServerConnectedOnOurNetwork( ArrayList<Serializable> input) {
        serverConnectedOnOurNetwork.clear();
        for(Serializable serializable : input) {
            serverConnectedOnOurNetwork.add( (SocketAddress) serializable);
        }
    }

    public void addServerConnectedOnOurNetwork( SocketAddress socketAddress) {
        for( SocketAddress address : serverConnectedOnOurNetwork) {
            if( address.toString().compareTo(socketAddress.toString()) == 0 ) {
                return;
            }
        }
        serverConnectedOnOurNetwork.add(socketAddress);
    }

    public String getServerConnectedOnOurNetworkString() {
        String res = "";
        Boolean isFirst = true;
        for( SocketAddress address : serverConnectedOnOurNetwork) {
            if( isFirst) {
                isFirst = false;
            } else {
                res += ", ";
            }
            res+=address.toString();
        }
        return res;
    }
}
