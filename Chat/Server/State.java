package Chat.Server;

import Chat.Netmessage.ChatData;
import Chat.Netmessage.ElectionToken;
import Chat.Netmessage.InterServerMessage;
import Chat.Utils.ClientStruct;
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
    /**
     * The network manage we will use to send messages
     */
    private NetManager netManager;
    /**
     * A lock to protect data about servers
     */
    private final ReentrantLock serverLock = new ReentrantLock();
    /**
     * A lock to protect data about clients
     */
    private final ReentrantLock clientLock = new ReentrantLock();
    /**
     * List of connection structures for clients
     */
    private List<ClientStruct> cliStrs;
    /**
     * List of connection structures for servers
     */
    private  List<ClientStruct> serverStrs;
    /**
     * A value to know if the server runs in stand alone mode or if it is in connected mode ( to other servers )
     */
    private Boolean standAlone = true;
    /**
     * The list of used pseudo across our network
     */
    private HashMap<String,Boolean> pseudoList;
    /**
     * The list of connected servers ( on our network )
     */
    private ArrayList<SocketAddress> serverConnectedOnOurNetwork;

    /**
     * Basic constructor
     */
    public State(NetManager _netManager) {
        netManager = _netManager;
        cliStrs = new ArrayList<ClientStruct>();
        serverStrs = new ArrayList<ClientStruct>();
        pseudoList = new HashMap<String, Boolean>();
        serverConnectedOnOurNetwork = new ArrayList<SocketAddress>();
    }

    /**
     * A small utility method : we construct the list of the client directly connected to this server.
     *
     * Two usage nowadays :
     *
     *    - Display it on clavier demand ( it will stay for debug purpose )
     *    - Answer client to this question : who is connected directly to this server
     *
     * @return String with all directly connected pseudos
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


    /**
     * Another utility function,
     * which will send a message
     * to all clients connected
     * and registered to our
     * server.
     *
     * @param mes message to broadcast
     */


    public void broadcast( ChatData mes ) {
        clientLock.lock();
        for ( ClientStruct cls : cliStrs) {
            netManager.sendClientMessage(cls, mes, "Failed to broadcast message");
        }
        clientLock.unlock();
    }

    /**
     * Utility function used twice :

     Tells us if we already
     registered the given
     connection as a server
     * @param fdmw The tested connection
     * @return True if the connection is already established, false in other cases
     */

    public Boolean isServerConnectionEstablished( FullDuplexMessageWorker fdmw ) {
        serverLock.lock();
        for( ClientStruct conStr : serverStrs) {
            if( conStr.getFullDuplexMessageWorker() == fdmw ) {
                serverLock.unlock();
                return true;
            }
        }
        serverLock.unlock();
        return false;
    }


    /**
     *
     * We add a server Struct which represent a server connection to our list of connected servers
     *
     * @param cliStr Server connection structure to add to our list of connected servers
     */

    public void addServer(ClientStruct cliStr){
        serverLock.lock();
        serverStrs.add(cliStr);
        standAlone = false;
        serverLock.unlock();
    }

    /**
     * Utility function...
     *
     * It is providing us our the
     * connected list of servers
     * recognised as servers
     *
     * @return connected list of servers recognised as servers
     */

    public String getServerList() {
        serverLock.lock();
        String res = "";
        Boolean first = true;
        for( ClientStruct cstr : serverStrs ) {
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

    /**
     * It closed all the connections registered
     * as both clients and servers. It give us
     * a new clean debug environment without
     * launching each servers a new time (
     * quite long with compilation )
     *
     * Useless in production environments but
     * so useful while testing distributed algorithms
     */

    public void reInitNetwork() {
        clientLock.lock();
        serverLock.lock();
        // No fear for dead locks as this is the only place were we lock for both clients and servers locks
        for( ClientStruct cstr : serverStrs) {
            netManager.sendInterServerMessage(cstr, new InterServerMessage(0,2), "Can not send a disconnection demand");
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

    /**
     *
     * Check if a pseudo is available... ( Locally )
     *
     * @param pseudo pseudo to check
     * @return true if a client with this pseudo is directly connected
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

    /**
     * Add a client Connection structure to our list of connected clients
     *
     * @param cliStr client Connection structure to add
     */

    public void addClient(ClientStruct cliStr) {
        clientLock.lock();
        cliStrs.add(cliStr);
        clientLock.unlock();
    }

    /**
     * Remove a connected client from directly connected clients list
     *
     * @param cliStr connected client to remove
     */

    public void removeClient(ClientStruct cliStr) {
        clientLock.lock();
        cliStrs.remove(cliStr);
        clientLock.unlock();
    }

    /**
     * Remove a connected server from directly connected servers list
     *
     * @param toRemove connected server to remove
     */

    public void removeServer(ClientStruct toRemove) {
        serverLock.lock();
        serverStrs.remove(toRemove);
        if( serverStrs.size() ==  0 ) {
            standAlone = true;
        }
        serverLock.unlock();
    }

    /**
     * We get the number of directly connected servers
     *
     * @return the number of directly connected servers
     */

    public int getNbConnectedServers() {
        return serverStrs.size();
    }

    /**
     *
     * We broadcast the given broadcast election token to all the directly connected servers
     *
     * @param electionToken Election token to broadcast
     * @param ioErrorMessage Server to display on error
     */

    protected void broadcastToken( ElectionToken electionToken, String ioErrorMessage) {
        // No need to protect this : accessed from only one thread, and not disturbed thanks to election lock
        for( ClientStruct connectionStruct : serverStrs) {
            netManager.sendElectionToken(connectionStruct, electionToken, ioErrorMessage );
        }
    }

    /**
     *
     * Same thing than above ( broadcastToken ) but we do not send it to father
     *
     * @param father ignored
     * @param newToken Election token to broadcast
     */

    protected void broadcastTokenWithoutFather(ClientStruct father, ElectionToken newToken) {
        // No need to protect this : accessed from only one thread, and not disturbed thanks to election lock
        for( ClientStruct connectionStruct : serverStrs) {
            if( connectionStruct != father) {
                netManager.sendElectionToken(connectionStruct, newToken, "Error while sending the new token" );
            }
        }
    }

    /**
     * Same utility than above, but for a InterserverMessage
     *
     * @param father ignored
     * @param newToken InterServerMessage to broadcast
     */

    public void broadcastTokenWithoutFather(ClientStruct father, InterServerMessage newToken) {
        // No need to protect this : accessed from only one thread, and not disturbed thanks to election lock
        for( ClientStruct connectionStruct : serverStrs) {
            if( connectionStruct != father) {
                netManager.sendInterServerMessage(connectionStruct,newToken,"Error while sending the new token");
            }
        }
    }

    /**
     * We broadcast the interserver message to all directly connected servers.
     *
     * @param mes InterServerMessage to broadcast
     */

    public void broadcastInterServerMessage( InterServerMessage mes) {
        for( ClientStruct connectionStruct : serverStrs) {
            netManager.sendInterServerMessage(connectionStruct, mes, "Error while broadcasting server message");
            System.out.print(".");
        }
        System.out.println();
    }

    /**
     * Accessor for standAlone
     *
     * @return True if the server is standalone, false in other cases
     */

    public Boolean getStandAlone() {
        return standAlone;
    }

    /**
     *
     * We do the difference between both pseudo lists, to know how to send join and leave notifications,
     * and then we set the list of pseudos available on our network.
     *
     * @param _pseudoList The pseudo list we want to replace connected pseudo on our network with
     */

    public void setPseudoList( ArrayList<Serializable> _pseudoList) {
        ArrayList<String> toNotifyDisconnection = new ArrayList<String>();
        ArrayList<String> toNotifyArrival = new ArrayList<String>();
        for(Serializable serializable : _pseudoList) {
           // if( !isPseudoTaken( (String) serializable ) ) {
           if( !isPseudoUsed( (String) serializable ) ) {
                // absent from this server. Add it.
                String actualPseudo = (String) serializable;
                toNotifyArrival.add( actualPseudo);
                broadcast(new ChatData(0, 3, "", actualPseudo));
            }
        }
        Iterator it = pseudoList.entrySet().iterator();
        while( it.hasNext() ) {
            Map.Entry pairs  = ( Map.Entry) it.next();
            String localPseudo =(String) pairs.getKey();
            if(!foundEntry(_pseudoList, localPseudo)) {
                // The client leaved
                toNotifyDisconnection.add( localPseudo );
                broadcast(new ChatData(0, 4, "", localPseudo ));
            }
        }
        for(String string : toNotifyArrival) {
            pseudoList.put(string, true);
        }
        for(String string : toNotifyDisconnection) {
            pseudoList.remove(string);
        }
    }

    /**
     * This method make the state come back to a standalone mode
     */

    public void switchToStandAlone() {
        ArrayList<Serializable> localClientsPseudo = new ArrayList<Serializable>();
        for(ClientStruct clientStruct : cliStrs) {
            localClientsPseudo.add(clientStruct.getPseudo());
        }
        setPseudoList(localClientsPseudo);
        serverConnectedOnOurNetwork.clear();
        standAlone = true;
    }

    /**
     * Tell us if the value of the String entry is contained in the array of Serializable
     *
     * @param list List that can contain the entity
     * @param entry Entry to check
     * @return True if found, else in other cases
     */

    private Boolean foundEntry(ArrayList<Serializable> list, String entry) {
        for(Serializable serializable : list) {
            if( entry.compareTo((String)serializable) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add the given pseudo to the list of pseudo connected to our network
     *
     * @param newPseudo pseudo to add
     */

    public void addPseudo( String newPseudo){
            pseudoList.put(newPseudo, true);
    }

    /**
     * Remove he given pseudo to the list of pseudo connected to our network
     *
     * @param pseudo to remove
     */

    public void removePseudo( String pseudo ) {
        pseudoList.remove( pseudo );
    }

    /**
     * Tells us if the pseudo is used on our network
     *
     * @param pseudo Pseudo to test
     * @return True if the pseudo is used, false in other cases
     */

    public Boolean isPseudoUsed(String pseudo) {
        Boolean res = pseudoList.get(pseudo);
        if( res == null) {
            return false;
        }
        return res;
    }

    /**
     * @return The string that contains all the clients' pseudo that are connected on our network
     */

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

    /**
     *
     * Get the connection structure with the given pseudo.
     *
     * It is used for private message delivery
     *
     * @param pseudo The given pseudo
     * @return The connection structure that holds this pseudo
     */

    public ClientStruct getClientByPseudo(String pseudo ) {
        for( ClientStruct clientStruct : cliStrs) {
            if( clientStruct.getPseudo().compareTo(pseudo) == 0 ) {
                return clientStruct;
            }
        }
        return null;
    }

    /**
     * We reset the list of servers connected on our network.
     *
     * @param input The new list of connected servers.
     */

    public void setServerConnectedOnOurNetwork( ArrayList<Serializable> input) {
        serverConnectedOnOurNetwork.clear();
        for(Serializable serializable : input) {
            serverConnectedOnOurNetwork.add( (SocketAddress) serializable);
        }
    }

    /**
     * Add a server to the list of servers that are connected on our network.
     *
     * @param socketAddress Server id to add
     */

    public void addServerConnectedOnOurNetwork( SocketAddress socketAddress) {
        for( SocketAddress address : serverConnectedOnOurNetwork) {
            if( address.toString().compareTo(socketAddress.toString()) == 0 ) {
                return;
            }
        }
        serverConnectedOnOurNetwork.add(socketAddress);
    }

    /**
     * Give us the list of servers connected on our network
     *
     * @return The list of servers that are connected on our network
     */

    public ArrayList<Serializable> getServerConnectedOnOurNetwork() {
        ArrayList<Serializable> res = new ArrayList<Serializable>();
        for( SocketAddress address : serverConnectedOnOurNetwork) {
            res.add( address);
        }
        return res;
    }

    /**
     * Get the string that contains all servers ids that are connected on our network. Used to be display or send to clients
     *
     * @return the string that contains all servers ids that are connected on our network
     */

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

    /**
     * Get the list of directly connected clients.
     *
     * It is used to obtain node specific data on a pseudo request broadcast.
     *
     * @return Returns the list of directly connected clients.
     */

    public ArrayList<Serializable> getConnectedClients() {
        ArrayList<Serializable> res = new ArrayList<Serializable>();
        for(ClientStruct clientStruct : cliStrs) {
            res.add( clientStruct.getPseudo());
        }
        return res;
    }


}
