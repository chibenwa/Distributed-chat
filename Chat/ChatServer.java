package Chat;

import csc4509.FullDuplexMessageWorker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
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

    public ChatServer(int _port) {
        port = _port;
        cliStrs = new ArrayList<ClientStruct>();
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
                        switch( rcv.getType()) {
                                case 0:
                                    // Here we have a demand for pseudo !
                                    //Set pseudo
                                    System.out.println("demand for pseudo : " + rcv.getPseudo());
                                    if( !rcv.hasPseudo() ) {
                                        // Return error 7 to client
                                        chdata = new ChatData(0, 6, "");
                                        chdata.setErrorCode(7);
                                        try {
                                            cliStr.getFullDuplexMessageWorker().sendMsg(0, chdata);
                                        } catch (IOException ioe) {
                                            System.out.println(loopNB + " Failed to send error for no login set in a pseudo demand");
                                            ioe.printStackTrace();
                                        }
                                        it.remove();
                                        break;
                                    }
                                    // Check if the name is already used
                                    Boolean alredyExist = false;
                                    for(ClientStruct cls : cliStrs) {
                                        if( cls.getPseudo() == rcv.getPseudo()) {
                                            alredyExist = true;
                                        }
                                    }
                                    // Notify all clients
                                    if( !alredyExist ) {
                                        System.out.println("Pseudo available");
                                        cliStrs.add( cliStr );
                                        // But first tell the client he was accepted
                                        chdata = new ChatData(0, 1, "", rcv.getPseudo());
                                        // Store the login
                                        cliStr.setPseudo( rcv.getPseudo() );
                                        // And notify every one
                                        try {
                                            cliStr.getFullDuplexMessageWorker().sendMsg(0, chdata);
                                        }catch (IOException ioe) {
                                            System.out.println(loopNB + " Failed to send ack for a pseudo demand");
                                            ioe.printStackTrace();
                                        }
                                        chdata = new ChatData(0, 3,"", rcv.getPseudo());
                                        broadcast(chdata);
                                        break;
                                    } else {
                                        // We can not allocate the pseudo as it is already taken. Notify Client.
                                        chdata = new ChatData(0, 6, "");
                                        chdata.setErrorCode(1);
                                        try {
                                            cliStr.getFullDuplexMessageWorker().sendMsg(0, chdata);
                                        }catch (IOException ioe) {
                                            System.out.println(loopNB + " Failed to send error for error for an alredy used login");
                                            ioe.printStackTrace();
                                        }
                                    }
                                case 1:
                                    // Here we have an acknolegement for pseudo -> error
                                    System.out.println("acknolegement for pseudo");
                                    chdata = new ChatData(0, 6, "");
                                    chdata.setErrorCode(3);
                                    try {
                                        cliStr.getFullDuplexMessageWorker().sendMsg(0, chdata);
                                    }catch (IOException ioe) {
                                        System.out.println(loopNB + " Failed to send error for a client who sent an ack for pseudo");
                                        ioe.printStackTrace();
                                    }
                                    break;
                                case 2:
                                    //Here we have someone who sent a message
                                    System.out.println("new message");
                                    // Broadcast it
                                    if( cliStr.hasPseudo() ) {
                                        System.out.println("new message content " + rcv.getMessage());
                                        chdata = new ChatData(0, 2, rcv.getMessage(), cliStr.getPseudo());
                                        broadcast(chdata);
                                    } else {
                                        System.out.println("Need pseudo man");
                                        // No pseudo set for this operation, even if it is required. Send an error.
                                        chdata = new ChatData(0,6,"");
                                        chdata.setErrorCode(2);
                                        try {
                                            cliStr.getFullDuplexMessageWorker().sendMsg(0, chdata);
                                        } catch (IOException ioe) {
                                            System.out.println(loopNB + " Failed to send an error for missing pseudo in sent message");
                                            ioe.printStackTrace();
                                        }
                                    }
                                    break;
                                case 3:
                                    // Here we received a join notification.
                                    System.out.println("join notification");
                                    // A client should not do that. Send error.
                                    chdata = new ChatData(0,6,"");
                                    chdata.setErrorCode(4);
                                    try {
                                        cliStr.getFullDuplexMessageWorker().sendMsg(0, chdata);
                                    } catch (IOException ioe) {
                                        System.out.println(loopNB + " Failed to send error for a client that send a join notification");
                                        ioe.printStackTrace();
                                    }
                                    break;
                                case 4:
                                    // Here we received a leave notification.
                                    System.out.println("leave notification");
                                    // A client should not do that. Send error.
                                    chdata = new ChatData(0,6,"");
                                    chdata.setErrorCode(5);
                                    try {
                                        cliStr.getFullDuplexMessageWorker().sendMsg(0, chdata);
                                    }catch (IOException ioe) {
                                        System.out.println(loopNB + " Failed to send error for a client who send a leave notification");
                                        ioe.printStackTrace();
                                    }
                                    break;
                                case 5:
                                    //Here we have a deconnection request
                                    // Notify every one
                                    System.out.println("Deconnection request handled");
                                    if( cliStr.hasPseudo() ) {

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
                                    if( rcv.hasError() ) {
                                        // Yes we have an error. Print it :
                                        rcv.printErrorCode();
                                    }
                                    break;
                                default:
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

}
