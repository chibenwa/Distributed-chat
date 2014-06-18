package Chat.Client;


import Chat.Netmessage.ChatData;
import csc4509.FullDuplexMessageWorker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by benwa on 6/7/14.
 *
 * License : GLP 2.0
 *
 * A bad ass Net manager that will manage network on client node.
 */
public class NetManager {
    /**
     *  Number of time we can not obtain our content. It means an error.
     */
    private int nbNullPointerException = 0;
    /**
     * The connection we are now using
     */
    private FullDuplexMessageWorker full;
    /**
     * If set a spare connection we can switch to in case of server fault.
     */
    private FullDuplexMessageWorker fullDuplexMessageWorkerSpare;
    /**
     * A boolean that indicates if a spare connection is set
     */
    private Boolean isSpareSet = false;
    /**
     * Tells us if the client is performing a spare switching transaction. Used by our spare switching trivial algorithm.
     */
    private Boolean isInSpareTansaction = false;
    /**
     * Tells us if the login allocation is made
     */
    private Boolean hasCompletedLogin = false;
    /**
     * Tells us if we get a response to the login request we made to the server.
     */
    private Boolean hasLoginResponse = false;
    /**
     * Pseudo set for spare connection
     */
    private String sparePseudo = "";
    /**
     * Tells if te client made a demand of user list.
     */
    private Boolean waitingUserList = false;
    /**
     * A lock to protect sensible data of concurrent access
     */
    final ReentrantLock clientStateLock = new ReentrantLock();
    /**
     *
     *
     * @return True if we have a login set
     */
    public Boolean getHasCompletedLogin() {
        return hasCompletedLogin;
    }

    public Boolean getHasLoginResponse() {
        if (hasLoginResponse) {
            hasLoginResponse = false;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Basic constructor. It opens a connection to the server.
     *
     * @param ipAddress Server hostname
     * @param port Server listening port
     */
    public NetManager(String ipAddress, int port) {
        InetAddress add;
        try {
            add = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException uhe) {
            System.out.println("Unknown host specified as server. Terminating");
            return;
        }
        InetSocketAddress isa = new InetSocketAddress(add, port);
        SocketChannel socketChannel;
        try {
            socketChannel = SocketChannel.open(isa);
        } catch (IOException ioe) {
            System.out.println("Oh man, we cannot connect !");
            return;
        }
        // Here we are connected.
        full = new FullDuplexMessageWorker(socketChannel);
    }

    /**
     * Once ready, call this to launch the network manager. It will wait for server answer.
     */

    public void launch() {
        System.out.println("Listening");
        // And now listen to new messages !
        Boolean loopCondition = true;
        while (loopCondition) {
            full.readMessage();
            ChatData chdata;
            if (full == null) {
                break;
            }
            try {
                chdata = (ChatData) full.getData();
            } catch( NullPointerException nlp ) {
                System.out.println("Can not read received datas ( Null pointer exception ) ");
                if( manageIOError() ) {
                    return;
                }
                continue;
            } catch (IOException ioe) {
                System.out.println("Can not read received datas");
                if( manageIOError() ) {
                    return;
                }
                continue;
            }
            if (chdata == null) {
                break;
            }
            // Now see what the server told us :
            switch (chdata.getType()) {
                case 0:
                    System.out.println("Why did the server send us a login request ?");
                    break;
                case 1:
                    System.out.println("Demand accepted by server for login " + chdata.getPseudo());
                    // Our login request was accepted
                    hasCompletedLogin = true;
                    hasLoginResponse = true;
                    // This value will be checked by the clavier thread. No need to do more !
                    break;
                case 2:
                    System.out.println(chdata.getDate() + " " + chdata.getPseudo() + " : " + chdata.getMessage());
                    break;
                case 3:
                    System.out.println(chdata.getDate() + " " + chdata.getPseudo() + " joined the chat...");
                    break;
                case 4:
                    System.out.println(chdata.getDate() + " " + chdata.getPseudo() + " leaved the chat...");
                    break;
                case 5:
                    System.out.println("Why did the server send us a disconnection request ?");
                    break;
                case 7:
                    System.out.println("Why did the server send us a user list request ?");
                    break;
                case 8:
                    clientStateLock.lock();
                    if (waitingUserList) {
                        System.out.println(chdata.getDate() + " Peoples on the chat : " + chdata.getMessage());
                        waitingUserList = false;
                        clientStateLock.unlock();
                    } else {
                        // Inform the server he made a mistake : we never asked for what he provided us...
                        ChatData informServer = new ChatData(0, 42, "");
                        informServer.setErrorCode(9);
                        clientStateLock.unlock();
                        sendMessage(informServer, "Problem telling the server we didn't need the user list");
                    }
                    break;
                case 6:
                case 42:
                    // We did something wrong and here came the error
                    if (chdata.hasError()) {
                        if (chdata.getErrorCode() == 1) {
                            System.out.println("The login is already used on this server");
                            hasLoginResponse = true;
                            // We didn't set hasCompletedLogin so the other thread will know that the login is already used
                        }
                        // Display the error and continue what we were doing !
                        chdata.printErrorCode();
                    }
                    break;
                case 12:
                    // We received a private message
                    System.out.println(chdata.getPseudo() +" ( as private ) : " + chdata.getMessage() );
                    break;
                case 13:
                    System.out.println("Server list : " + chdata.getMessage());
                    break;
                default:
                    System.out.println("Unhandled number for message type : " + chdata.getType());
                    break;
            }
        }
    }

    /**
     * Called from clavier thread. Ask the server for a new pseudo
     *
     * @param newLogin this new pseudo
     */

    public void askNewLogin(String newLogin) {
        sendMessage(new ChatData(0, 0, "", newLogin), "Oh god, we failed sending the pseudo request !");
    }

    /**
     * Called by clavier thread. It sends a massage to the server.
     * @param msg Our message
     * @param pseudo Our pseudo
     */

    public void sendMsg(String msg, String pseudo) {
        sendMessage(new ChatData(0, 2, msg, pseudo), "Oh god, we failed sending our message ! To ");
    }

    /**
     * Called by the clavier thread. Used to demand to server to close our connection.
     */

    public void disconnect() {
        sendMessage(new ChatData(0, 5, ""), "Oh god, we failed sending the pseudo request !");
    }

    /**
     * Called by clavier thread. It is used to ask the server to provide us the client list.
     */

    public void askForUserList() {
        clientStateLock.lock();
        waitingUserList = true;
        clientStateLock.unlock();
        System.out.println("sending user list request");
        sendMessage(new ChatData(0, 7, ""), "Oh god, we failed sending the user list request !");
    }

    /**
     *
     * Utility function. It is used to send a packet to the server
     *
     * @param chatData Data to send
     * @param ioErrorMessage Error in case of IO error
     */

    private void sendMessage(ChatData chatData, String ioErrorMessage) {
        clientStateLock.lock();
        try {
            full.sendMsg(0, chatData);
        } catch (IOException ioe) {
            System.out.println(ioErrorMessage);
            manageIOError();
        }
        clientStateLock.unlock();
    }

    /**
     * Returns us if spare connection is set.
     *
     * @return True if a spare connection is set, and false in other cases
     */

    public Boolean getIsSpareSet() {
        return isSpareSet;
    }

    /**
     * This method can be called to set a spare connection with a server.
     *
     * @param ip Server IP
     * @param _port Server port
     * @param _pseudo Our pseudo.
     */

    public void establishSpareConnection(String ip, int _port, String _pseudo) {
        clientStateLock.lock();
        InetAddress add;
        try {
            add = InetAddress.getByName(ip);
        } catch (UnknownHostException uhe) {
            System.out.println("Unknown host specified as server. Terminating");
            return;
        }
        InetSocketAddress _isa = new InetSocketAddress(add, _port);
        SocketChannel socketChannel;
        try {
            socketChannel = SocketChannel.open(_isa);
        } catch (IOException ioe) {
            System.out.println("Oh man, we cannot connect spare connection !");
            return;
        }
        // Here we are connected.
        fullDuplexMessageWorkerSpare = new FullDuplexMessageWorker(socketChannel);
        try{
            fullDuplexMessageWorkerSpare.sendMsg(0,new ChatData(0,9,"",_pseudo));
        } catch( IOException ioe) {
            System.out.println("Failed to send spare connection demand");
        }
        isSpareSet = true;
        sparePseudo = _pseudo;
        clientStateLock.unlock();
    }

    /**
     * Switch our main connection to spare connection.
     */

    public void switchToSpareConnection() {
        clientStateLock.lock();
        if( isSpareSet) {
            isInSpareTansaction = true;
            isSpareSet = false;
            try {
                fullDuplexMessageWorkerSpare.sendMsg(0, new ChatData(0, 10, "", sparePseudo));
            } catch (IOException ioe) {
                System.out.println("Failed to send spare switching demand");
                return;
            }
            fullDuplexMessageWorkerSpare.readMessage();
            ChatData answer;
            try{
                answer = ( ChatData ) fullDuplexMessageWorkerSpare.getData();
            } catch (IOException ioe ) {
                System.out.println("Error while waiting ack");
                return;
            }
            if( answer.getType() == 11 ) {
                try {
                    full.close();
                } catch(IOException ioe) {
                    System.out.println("Can not close full");
                }
                full = fullDuplexMessageWorkerSpare;
                isInSpareTansaction = false;
                System.out.println("Switch done !");
            } else {
                System.out.println("Wrong message type not expected");
            }
        }
        clientStateLock.unlock();
    }

    /**
     * Called by clavier thread. Sends a private message
     *
     * @param pseudo Your pseudo
     * @param dest Pseudo you want to send a message to
     * @param message Message you want to send
     */

    public void sendPrivateMessage( String pseudo, String dest, String message) {
        ChatData chatData = new ChatData(0,12,message,pseudo);
        chatData.pseudoDestination = dest;
        sendMessage(chatData, "Error sending private message ! ");
    }

    /**
     * Called by clavier thread. Ask our server for the list of servers
     */

    public void askForServerList() {
        sendMessage(new ChatData(0,13,""),"Error requesting server list ");
    }

    /**
     * Manage an IO error :
     * Perform switching if a spare connection is established.
     * If we have more than 5 error, we shut down our client...
     *
     * @return True if we need to shut down the client, false in other cases
     */

    private Boolean manageIOError() {
        nbNullPointerException++;
        if( nbNullPointerException> 5) {
            return true;
        }
        switchToSpareConnection();
        while( isInSpareTansaction ) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ie) {
                System.out.println("Interrupted...");
            }
        }
        return false;
    }
}
