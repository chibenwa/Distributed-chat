package Chat;

import csc4509.FullDuplexMessageWorker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.channels.*;
import java.util.*;

/**
 * Created by benwa on 6/5/14.
 */
public class ChatServer {
    private int port;
    private ServerSocketChannel ssc;
    private Selector selector;
    private List<ClientStruct> cliStrs;
    private  List<ConnectionStruct> serverStrs;

    public ChatServer(int _port) {
        port = _port;
        cliStrs = new ArrayList<ClientStruct>();
        serverStrs = new ArrayList<ConnectionStruct>();
    }

    public void launch() {
        launchServer();
        selectorInit();
        asyncLoop();
    }

    protected void launchServer() {
        try {
            // Here we start listening on the given port !
            ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ServerSocket ss = ssc.socket();
            InetSocketAddress add = new InetSocketAddress(port);
            ss.bind(add);
        } catch (IOException se) {
            System.out.println("Failed to create server");
            se.printStackTrace();
            return;
        }
    }

    protected void asyncLoop() {
        int loopNB = 0;
        while (true) {
            loopNB++;
            try {
                selector.select();
            } catch (IOException ioe) {
                System.out.println(loopNB + " Failed to select Selector");
                ioe.printStackTrace();
                return;
            }
            Set selectedKeys = selector.selectedKeys();
            Iterator it = selectedKeys.iterator();
            while (it.hasNext()) {
                SelectionKey key = (SelectionKey) it.next();
                if (key.isAcceptable()) {
                    // We have a new client opening a connection.
                    System.out.println(loopNB + " New connection");
                    SocketChannel sc;
                    try {
                        sc = ssc.accept();
                    } catch (IOException ioe) {
                        System.out.println(loopNB + " Failed to open new connexion in main loop");
                        ioe.printStackTrace();
                        return;
                    }
                    try {
                        sc.configureBlocking(false);
                    } catch (IOException ioe) {
                        System.out.println(loopNB + " Failed to set non blocking for child socket");
                        ioe.printStackTrace();
                        return;
                    }
                    FullDuplexMessageWorker fdmw = new FullDuplexMessageWorker(sc);
                    try {
                        fdmw.configureNonBlocking();
                    } catch( IOException ioe ) {
                        System.out.println("FullDuplexMessageWorker can not be configured as non blocking");
                        return;
                    }
                    ClientStruct cliStr = new ClientStruct(fdmw);
                    // We register on this channel
                    try {
                        sc.register(selector, SelectionKey.OP_READ, cliStr);
                    } catch (ClosedChannelException cce) {
                        System.out.println(loopNB + " Failed to register read on new channel");
                        cce.printStackTrace();
                        return;
                    }
                } else {
                    if (key.isReadable()) {
                        ClientStruct cliStr = (ClientStruct) key.attachment();
                        FullDuplexMessageWorker fdmw = cliStr.getFullDuplexMessageWorker();
                        fdmw.readMessage();
                        if( fdmw.getMessType() == 0) {
                            ChatData chdata;
                            ChatData rcv;
                            try {
                                rcv = (ChatData) fdmw.getData();
                            }
                            catch (IOException ioe) {
                                System.out.println(loopNB + " Failed to receive message");
                                ioe.printStackTrace();
                                rcv = null;
                            }
                            // Here we have a message from a client to our server to use the Chat
                            switch (rcv.getType()) {
                                case 0:
                                    // Here we have a demand for pseudo !
                                    //Set pseudo
                                    System.out.println("demand for pseudo : " + rcv.getPseudo());
                                    if (!rcv.hasPseudo()) {
                                        // Return error 7 to client
                                        sendClientError(cliStr,7," Failed to send error for no login set in a pseudo demand");
                                        break;
                                    }
                                    // Check if the name is already used
                                    Boolean alredyExist = false;
                                    for (ClientStruct cls : cliStrs) {
                                        if (cls.getPseudo().equals(rcv.getPseudo()) ) {
                                            alredyExist = true;
                                        }
                                    }
                                    if (!alredyExist) {
                                        System.out.println("Pseudo available");
                                        // But first tell the client he was accepted
                                        sendClientMessage( fdmw, new ChatData(0, 1, "", rcv.getPseudo()), " Failed to send ack for a pseudo demand" );
                                        // Store the login
                                        cliStr.setPseudo(rcv.getPseudo());
                                        cliStrs.add(cliStr);
                                        // And notify every one
                                        broadcast(new ChatData(0, 3, "", rcv.getPseudo()));
                                        break;
                                    } else {
                                        // We can not allocate the pseudo as it is already taken. Notify Client.
                                        sendClientError(cliStr,1,"Failed to send error for error for an alredy used login");
                                    }
                                case 1:
                                    // Here we have an acknolgement for pseudo -> error
                                    sendClientError(cliStr,3,"Failed to send error for a client who sent an ack for pseudo");
                                    break;
                                case 2:
                                    //Here we have someone who sent a message
                                    System.out.println("new message");
                                    // Broadcast it
                                    if (cliStr.hasPseudo()) {
                                        System.out.println("new message content " + rcv.getMessage());
                                        broadcast( new ChatData(0, 2, rcv.getMessage(), cliStr.getPseudo()) );
                                    } else {
                                        System.out.println("Need pseudo man");
                                        // No pseudo set for this operation, even if it is required. Send an error.
                                        sendClientError(cliStr,2," Failed to send an error for missing pseudo in sent message");
                                    }
                                    break;
                                case 3:
                                    // Here we received a join notification.
                                    System.out.println("join notification");
                                    // A client should not do that. Send error.
                                    sendClientError(cliStr, 4,"Failed to send error for a client that send a join notification");
                                    break;
                                case 4:
                                    // Here we received a leave notification.
                                    System.out.println("leave notification");
                                    // A client should not do that. Send error.
                                    sendClientError(cliStr, 5, "Failed to send error for a client who send a leave notification");
                                    break;
                                case 5:
                                    //Here we have a deconnection request
                                    // Notify every one
                                    System.out.println("Deconnection request handled");
                                    if (cliStr.hasPseudo()) {
                                        chdata = new ChatData(0, 4, "", cliStr.getPseudo());
                                        broadcast(chdata);
                                    }
                                    // Close socket
                                    try {
                                        fdmw.close();
                                    } catch (IOException ioe) {
                                        System.out.println(loopNB + " Failed to close channel");
                                        ioe.printStackTrace();
                                    }
                                    // Remove the client
                                    cliStrs.remove(cliStr);
                                    break;
                                case 6:
                                    // The client send us an error
                                    if (rcv.hasError()) {
                                        // Yes we have an error. Print it :
                                        rcv.printErrorCode();
                                    }
                                    break;
                                case 7 :
                                    System.out.println("Man, a user list request !");
                                    // The client asks for a list of all users
                                    if( cliStr.hasPseudo() ) {
                                        // Ok, we now him, let send it
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
                                        System.out.println("Yes he is authentificated and we can send the user list ( we will really do it ! ) ");
                                        chdata = new ChatData(0,8,pseudoChunk, cliStr.getPseudo());
                                        sendClientMessage(fdmw, chdata, "Could not send the user list");
                                    } else {
                                        // Who's that guy? Kick him dude !
                                        System.out.println("Non authentificated user can not ask for user list...");
                                        // A client should not do that. Send error.
                                        sendClientError(cliStr, 2, "Failed to send error for a client who asked for the user list while non authentificated");
                                    }
                                    break;
                                case 8 :
                                    // The user send us the user list. That is stupid ! Let's go tell him
                                    System.out.println("User send us a list of pseudo");
                                    sendClientError(cliStr, 8, "Failed to send error for a client who send us a user list");
                                    break;
                                default:
                            }
                        } else {
                            if( fdmw.getMessType() == 1 ) {
                                // Oh dude here this is an election package !
                            } else {
                                if(fdmw.getMessType() == 2) {
                                    // Here we are synchronising messages on our ( future ) distributed Chat
                                    InterServerMessage incomingMessage;
                                    try{
                                        incomingMessage = (InterServerMessage) fdmw.getData();
                                    } catch( IOException ioe) {
                                        System.out.println("Can not retrieve InterServerMessage we are receiving");
                                        break;
                                    }
                                    // And here come a big switch
                                    switch (incomingMessage.getType()) {
                                        case 0:
                                            System.out.println("Request for server connection received");
                                            // Someone make a demand to be added to servers.
                                            if( handleServerConnectionRequest(fdmw)) {
                                                break;
                                            }
                                            // Then notify the other server that he had been correctly added :-)
                                            sendInterServerMessage(fdmw, new InterServerMessage(0,1),"Can not send ack for a server connection established");
                                            break;
                                        case 1:
                                            System.out.println("Our request for opening a new connection to another server was answered");
                                            // Someone answered our demand for server connection. Ok, so we are well connected. Add the connection to the servers connections.
                                            handleServerConnectionRequest(fdmw);
                                            break;
                                        case 42:
                                            // Error code : lets display it
                                            if( incomingMessage.hasError() ) {
                                                incomingMessage.printErrorCode();
                                            }
                                            break;
                                    }
                                }
                            }
                        }
                    }
                }
                // No other operations... More to come at this point !
                it.remove();
            }
        }
    }

    protected void selectorInit() {
        try {
            selector = Selector.open();
        } catch (IOException er) {
            System.out.println("Failed to open Selector");
            er.printStackTrace();
            return;
        }
        try {
            ssc.register(selector, SelectionKey.OP_ACCEPT);
        } catch (ClosedChannelException cce) {
            System.out.println("Failed to register selector for main server");
            cce.printStackTrace();
            return;
        }
    }

    protected void broadcast( ChatData mes ) {
        for ( ClientStruct cls : cliStrs) {
            try {
                cls.getFullDuplexMessageWorker().sendMsg(0, mes);
            } catch( IOException ioe) {
                System.out.println("Failed to broadcast message");
                return;
            }
        }
    }

    protected void connectServer( String ipString, int _port) {
        InetAddress add;
        try{
            add = InetAddress.getByName(ipString);
        } catch(UnknownHostException uhe) {
            System.out.println("The host server you are trying to connect is unknown to us, man... " + ipString);
            return;
        }
        InetSocketAddress isa = new InetSocketAddress(add, _port);
        SocketChannel chan;
        try{
            chan = SocketChannel.open(isa);
        }catch(IOException ioe) {
            System.out.println("Can not established a connection with " + isa);
            return;
        }
        FullDuplexMessageWorker fullDuplexMessageWorker = new FullDuplexMessageWorker(chan);
        try {
            fullDuplexMessageWorker.configureNonBlocking();
        } catch (IOException ioe) {
            System.out.println("Can not configure channel server as non blocking");
            return;
        }
        try {
            chan.register(selector, SelectionKey.OP_READ, fullDuplexMessageWorker);
        } catch(ClosedChannelException cce) {
            System.out.println("Channel closed while registering channel for inter server communication");
        }
        // Now specify to the server that WE ARE A SERVER ...
        InterServerMessage ism = new InterServerMessage(0,0);
        try {
            fullDuplexMessageWorker.sendMsg(2, "");
        } catch (IOException ioe) {
            System.out.println("Can not send a basic Hello I am a server ! ");
            return;
        }
    }

    private Boolean isServerConnectionEstablished( FullDuplexMessageWorker fdmw ) {
        for( ConnectionStruct conStr : serverStrs) {
            if( conStr.getFullDuplexMessageWorker() == fdmw ) {
                return true;
            }
        }
        return false;
    }

    private Boolean handleServerConnectionRequest(FullDuplexMessageWorker fdmw) {
        Boolean res = isServerConnectionEstablished(fdmw);
        if( res ) {
            System.out.println("Connection already established. Sending error.");
            // Send error
            sendServerError(fdmw,1,"Error while sending error for a double established server connection warning 1");
        } else {
            // First add it
            ConnectionStruct connectionStruct = new ConnectionStruct(fdmw);
            serverStrs.add( connectionStruct );
        }
        return res;
    }

    private void sendClientError( ClientStruct cliStr, int errorCode, String ioErrorMessage) {
        ChatData chdata = new ChatData(0, 6, "");
        chdata.setErrorCode(errorCode);
        sendClientMessage(cliStr.getFullDuplexMessageWorker(), chdata, ioErrorMessage);
    }

    private void sendServerError( FullDuplexMessageWorker full, int errorCode, String ioErrorMessage) {
        InterServerMessage ism = new InterServerMessage(0,42);
        ism.setErrorCode(errorCode);
        sendInterServerMessage(full, ism, ioErrorMessage);
    }

    private void sendInterServerMessage( FullDuplexMessageWorker full,  InterServerMessage mes, String ioErrorMessage ) {
        try {
            full.sendMsg(2, mes);
        } catch (IOException ioe) {
            System.out.println(ioErrorMessage);
            ioe.printStackTrace();
        }
    }

    private void sendClientMessage( FullDuplexMessageWorker full, ChatData chatData, String ioErrorMessage ) {
        try{
            full.sendMsg(0,chatData);
        } catch( IOException ioe) {
            System.out.println(ioErrorMessage);
            ioe.printStackTrace();
        }
    }
}
