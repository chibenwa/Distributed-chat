package Chat.Server;

import Chat.Netmessage.ChatData;
import Chat.Netmessage.ElectionToken;
import Chat.Netmessage.InterServerMessage;
import Chat.Utils.ClientStruct;
import Chat.Utils.ConnectionStruct;
import csc4509.FullDuplexMessageWorker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by benwa on 6/8/14.
 */
public class State {
    final ReentrantLock serverLock = new ReentrantLock();
    final ReentrantLock clientLock = new ReentrantLock();
    private List<ClientStruct> cliStrs;
    protected  List<ConnectionStruct> serverStrs;

    public State() {
        cliStrs = new ArrayList<ClientStruct>();
        serverStrs = new ArrayList<ConnectionStruct>();
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

    // TODO comment here

    public void addServer(ClientStruct cliStr){
        serverLock.lock();
        serverStrs.add( cliStr );
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

    // TODO : write doc

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
}
