package Chat.Server;

import Chat.Netmessage.ChatData;

import Chat.Netmessage.ChatMessage;
import Chat.Netmessage.InterServerMessage;
import Chat.Utils.ClientStruct;
import csc4509.FullDuplexMessageWorker;
import java.io.IOException;
import java.net.*;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by benwa on 6/7/14.
 *
 * License : GLP 2.0
 */
public class NetManager {

    // Things initialized in a mono threaded environment and then only read.
    private int port;
    private ServerSocketChannel ssc;

    private SocketAddress p;


    // Thread safe
    private ElectionHandler electionHandler;
    private RBroadcastManager rBroadcastManager;
    final ReentrantLock selectorLock = new ReentrantLock();
    private Selector selector;

    // Things that have to be protected more efficiently are in a separated class
    private State state;


    /*
    Initialisation of Chatserver.

    Only register the port number,
    Initialize some data structures,
    But network registration
    and async loop will be performed later
     */

    public NetManager(int _port) {
        port = _port;
        state = new State();
        electionHandler = new ElectionHandler(this);
        rBroadcastManager = new RBroadcastManager(this);
    }



    /*
        This Method makes our server
        listening the whole internet
        on the given port port.
        It also intialize some basic
        stuff for async IO.
        Then it launches the async loop
        this thread will be caught on
     */

    public void launch() {
        launchServer();
        selectorInit();
        asyncLoop();
    }



    /*
        We make here our server listening
        the internet .
        We also take time to register the
        election token.
     */

    protected void launchServer() {
        try {
            // Here we start listening on the given port !
            ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ServerSocket ss = ssc.socket();
            InetSocketAddress add = new InetSocketAddress(port);
            p = add;
            ss.bind(add);
            electionHandler.setP(add);
            rBroadcastManager.setOurAddress(add);
        } catch (IOException se) {
            System.out.println("Failed to create server");
            se.printStackTrace();
        }
    }


    /*
        The loop our main thread will be caught in.

        It accepts connections, and manages read.
        It dispatches read, thanks to the
        FullMessageWorkersType between these categories :

            - Message from client
            - Message related to an election
            - Other message transiting between two servers

        Note : We need to do this dispatch, as we need
        to cast the right object
     */

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
                    makeRegistration(sc);
                } else {
                    if (key.isReadable()) {
                        ClientStruct cliStr = (ClientStruct) key.attachment();
                        FullDuplexMessageWorker fdmw = cliStr.getFullDuplexMessageWorker();
                        fdmw.readMessage();
                        switch (fdmw.getMessType()) {
                            case 0:
                                // Here we have a message coming from the client
                                handleClientMessage(cliStr);
                                break;
                            case 1:
                                // Here this is a message related to elections
                                electionHandler.manageInput(cliStr);
                                break;
                            case 2:
                                // Here we get a server message
                                manageServersMessages(cliStr);
                                break;
                            default:

                                break;
                        }
                    } else {
                        System.out.println("Key is unfortunetly not readable...");
                    }
                }
                // No other operations... More to come at this point !
                it.remove();
            }
        }
    }


    /*
        Basic stuff with Selector
        initialisation.
     */

    private void selectorInit() {
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
        }
    }


    /*
    This method is only called
    buy the clavier thread.

    It establish a connection
    with another server and
    notifies it we are a server.
     */

    public void connectServer( String ipString, int _port) {
        electionHandler.lock();
        System.out.println("Lock succeed");
        if( electionHandler.getIsInElection()) {
            electionHandler.unlock();
            System.out.println("We are in a f***ing election dude. No way...");
            return;
        }
        electionHandler.unlock();
        System.out.println("Passed unlock succeed");
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
        System.out.println("Channel opened");
        FullDuplexMessageWorker fullDuplexMessageWorker = makeRegistration(chan);
        if( fullDuplexMessageWorker == null ) {
            return;
        }
        // Now specify to the server that WE ARE A SERVER ...
        sendInterServerMessage(fullDuplexMessageWorker, new InterServerMessage(0,0), "Can not send a basic Hello I am a server ! " );
    }

    /*
        Utility function. Called twice.

        It is here both for code reuse
        and readability.

        It simply add the given connection
        to our connected server pool if
        it not there yet. In other cases,
        it sends an error.
     */

    private Boolean handleServerConnectionRequest(ClientStruct cliStr) {
        Boolean res = state.isServerConnectionEstablished(cliStr.getFullDuplexMessageWorker());
        if( res ) {
            System.out.println("Connection already established. Sending error.");
            // Send error
            sendServerError(cliStr.getFullDuplexMessageWorker(), 1, "Error while sending error for a double established server connection warning 1");
        } else {
            state.addServer(cliStr);
        }
        return res;
    }


    /*
    The five following methods are
    always called. They were written
    to improve readability by removing
    exception handling of the logic,
    but keep a strong error management
    ( which is quitte helpful with
    things as tricky as networking
    using hand made protocols with
    hand made parsers ).
     */

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
        mes.setIdentifier(p);
        mes.setElectionWinner( electionHandler.getWin() );
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

    private FullDuplexMessageWorker makeRegistration(SocketChannel chan) {
        FullDuplexMessageWorker fullDuplexMessageWorker = new FullDuplexMessageWorker(chan);
        ClientStruct str = new ClientStruct(fullDuplexMessageWorker);
        try {
            fullDuplexMessageWorker.configureNonBlocking();
        } catch (IOException ioe) {
            System.out.println("Can not configure channel server as non blocking");
            return null;
        }
        // http://stackoverflow.com/questions/1057224/thread-is-stuck-while-registering-channel-with-selector-in-java-nio-server
        // More than 2 hours lost and one of the strangest bug I have ever made : https://benwa.minet.net/article/46
        selectorLock.lock();
        try{
            selector.wakeup();
            try {
                chan.register(selector, SelectionKey.OP_READ, str);
            } catch(ClosedChannelException cce) {
                System.out.println("Channel closed while registering channel for inter server communication");
            }
        } finally {
            selectorLock.unlock();
        }
        return fullDuplexMessageWorker;
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
        state.reInitNetwork();
    }


    /*
        A quite verbose method that analyses
        clients messages and took appropriate decisions.

        It part of the async loop, but
        separated for readability.
     */

    private  void handleClientMessage(ClientStruct cliStr) {
        FullDuplexMessageWorker fdmw = cliStr.getFullDuplexMessageWorker();
        ChatData chdata;
        ChatData rcv;
        try {
            rcv = (ChatData) fdmw.getData();
        } catch (IOException ioe) {
            // We removed the fail client from our pool of clients...
            System.out.println(" Failed to receive message");
            if( cliStr.hasPseudo() ) {
                if (state.getStandAlone()) {
                    // No need to use a complex diffusion algorithm, we are stand alone...
                    chdata = new ChatData(0, 4, "", cliStr.getPseudo());
                    state.broadcast(chdata);
                } else {
                    sendRClientLeave(cliStr.getPseudo());
                }
            }
            return;
        }
        // Here we have a message from a client to our server to use the Chat
        switch (rcv.getType()) {
            case 0:
                // Here we have a demand for pseudo !
                //Set pseudo
                System.out.println("demand for pseudo : " + rcv.getPseudo());
                if (!rcv.hasPseudo()) {
                    // Return error 7 to client
                    sendClientError(cliStr, 7, " Failed to send error for no login set in a pseudo demand");
                    break;
                }
                // Check if the name is already used
                if ( ! state.isPseudoTaken(rcv.getPseudo()) ) {
                    System.out.println("Pseudo available");
                    // But first tell the client he was accepted
                    sendClientMessage(fdmw, new ChatData(0, 1, "", rcv.getPseudo()), " Failed to send ack for a pseudo demand");
                    // Store the login
                    cliStr.setPseudo(rcv.getPseudo());
                    state.addClient(cliStr);
                    state.addPseudo(rcv.getPseudo());
                    // And notify every one

                    if( state.getStandAlone() ) {
                        // No need to use a complex diffusion algorithm, we are stand alone...
                        state.broadcast(new ChatData(0, 3, "", rcv.getPseudo()));
                    } else {
                        sendRClientJoin(rcv.getPseudo());
                    }
                    break;
                } else {
                    // We can not allocate the pseudo as it is already taken. Notify Client.
                    sendClientError(cliStr, 1, "Failed to send error for error for an alredy used login");
                }
            case 1:
                // Here we have an acknolgement for pseudo -> error
                sendClientError(cliStr, 3, "Failed to send error for a client who sent an ack for pseudo");
                break;
            case 2:
                //Here we have someone who sent a message
                System.out.println("new message");
                // Broadcast it
                if (cliStr.hasPseudo()) {
                    System.out.println("new message content " + rcv.getMessage());
                    state.broadcast(new ChatData(0, 2, rcv.getMessage(), cliStr.getPseudo()));
                    if( ! state.getStandAlone() ) {
                        sendRMessage( rcv.getMessage(), cliStr.getPseudo());
                    }
                } else {
                    System.out.println("Need pseudo man");
                    // No pseudo set for this operation, even if it is required. Send an error.
                    sendClientError(cliStr, 2, " Failed to send an error for missing pseudo in sent message");
                }
                break;
            case 3:
                // Here we received a join notification.
                System.out.println("join notification");
                // A client should not do that. Send error.
                sendClientError(cliStr, 4, "Failed to send error for a client that send a join notification");
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
                    state.removePseudo(cliStr.getPseudo());
                    if( state.getStandAlone() ) {
                        // No need to use a complex diffusion algorithm, we are stand alone...
                        chdata = new ChatData(0, 4, "", cliStr.getPseudo());
                        state.broadcast(chdata);
                    } else {
                        sendRClientLeave(cliStr.getPseudo());
                    }
                }
                // Close socket
                try {
                    fdmw.close();
                } catch (IOException ioe) {
                    System.out.println(" Failed to close channel");
                    ioe.printStackTrace();
                }
                // Remove the client
                state.removeClient(cliStr);
                break;
            case 7:
                System.out.println("Man, a user list request !");
                // The client asks for a list of all users
                if (cliStr.hasPseudo()) {
                    // Ok, we now him, let send it
                    System.out.println("Yes he is authentificated and we can send the user list ( we will really do it ! ) ");
                    chdata = new ChatData(0, 8, state.getClientsString(), cliStr.getPseudo());
                    sendClientMessage(fdmw, chdata, "Could not send the user list");
                } else {
                    // Who's that guy? Kick him dude !
                    System.out.println("Non authentificated user can not ask for user list...");
                    // A client should not do that. Send error.
                    sendClientError(cliStr, 2, "Failed to send error for a client who asked for the user list while non authentificated");
                }
                break;
            case 8:
                // The user send us the user list. That is stupid ! Let's go tell him
                System.out.println("User send us a list of pseudo");
                sendClientError(cliStr, 8, "Failed to send error for a client who send us a user list");
                break;
            case 9:
                // The user want to be set as spare
                if( state.isPseudoTaken(rcv.getPseudo())) {
                    // The pseudo is taken, we can set spare connection
                    sendClientMessage(cliStr.getFullDuplexMessageWorker(), new ChatData(0,10,""),"Failed to send Ack for spared connection registring");
                    cliStr.setPseudo(rcv.getPseudo());
                    System.out.println("Client spare " + rcv.getPseudo() + " registered");
                }
                break;
            case 10:
                // The client wants to perform switching...
                System.out.println("Traiting client switching");
                if( !cliStr.hasPseudo()) {
                    cliStr.setPseudo(rcv.getPseudo());
                }
                state.addClient(cliStr);
                sendClientMessage(cliStr.getFullDuplexMessageWorker(), new ChatData(0,11,""), "Failed to send Ack for spare connection activation");
                break;
            case 42:
                // The client send us an error
                if (rcv.hasError()) {
                    // Yes we have an error. Print it :
                    rcv.printErrorCode();
                }
                break;
            default:
        }
    }


    /*
        A quite verbose method that analyses
        servers messages and took appropriate decisions.

        It part of the async loop, but
        separated for readability.
     */

    private void manageServersMessages(ClientStruct cliStr) {
        FullDuplexMessageWorker fdmw = cliStr.getFullDuplexMessageWorker();
        // Here we are synchronising messages on our ( future ) distributed Chat
        InterServerMessage incomingMessage;
        try{
            incomingMessage = (InterServerMessage) fdmw.getData();
        } catch( IOException ioe) {
            System.out.println("Can not retrieve InterServerMessage we are receiving");
            if( state.isServerConnectionEstablished(fdmw) ) {
                // We remove the failed server from our connections.
                state.removeServer(cliStr);
                // In doubt launch an election ... If the removed server is either elected nor separating us from elected server.
                if( ! state.getStandAlone() ) {
                    electionHandler.launchElection();
                }
            }
            return;
        }
        // And here come a big switch
        switch (incomingMessage.getType()) {
            case 0:
                System.out.println("DBG Rcv 0");
                electionHandler.lock();
                if( electionHandler.getIsInElection() ) {
                    electionHandler.unlock();
                    // We are in election, we can not add a server !
                    System.out.println("Server add demand while in election. Sending error");
                    sendServerError(fdmw,3, "IO ERROR SERVER CODE 3");
                } else {
                    electionHandler.unlock();
                    System.out.println("Request for server connection received");
                    // Someone make a demand to be added to servers.
                    if (handleServerConnectionRequest(cliStr)) {
                        break;
                    }
                    sendRServerJoin( incomingMessage.getIdentifier() );
                    // Then notify the other server that he had been correctly added :-)
                    sendInterServerMessage(fdmw, new InterServerMessage(0, 1), "Can not send ack for a server connection established");
                }
                break;
            case 1:
                System.out.println("DBG Rcv 1");
                electionHandler.lock();
                if(electionHandler.getIsInElection()) {
                    electionHandler.unlock();
                    System.out.println("Server add reply while in election. Sending error");
                    sendServerError(fdmw,4, "IO ERROR SERVER CODE 4");
                } else {
                    electionHandler.unlock();
                    System.out.println("Our request for opening a new connection to another server was answered");
                    // Someone answered our demand for server connection. Ok, so we are well connected. Add the connection to the servers connections.
                    if( handleServerConnectionRequest(cliStr) ) {
                        break;
                    }
                    manageElectoralStateOnServerConnection( incomingMessage.getElectionWinner(), cliStr );
                }
                break;
            case 2:
                // Remote server demands us to close the connection. Let's do it
                state.removeServer(cliStr);
                try {
                    fdmw.close();
                } catch(IOException ioe) {
                    System.out.println("We could not disconnect server as requested...");
                }
            case 3:
                // R diffusion message
                if( rBroadcastManager.manageInput( incomingMessage) ) {
                    // We have to process this message : it was accepted
                    manageServerMessageSubtype(incomingMessage);
                }
                break;
            case 42:
                // Error code : lets display it
                if( incomingMessage.hasError() ) {
                    incomingMessage.printErrorCode();
                }
                break;
        }
    }

    /*
        We need to propagate a few methods to our ElectionHandler
     */

    public void launchElection() {
        electionHandler.launchElection();
    }

    public void displayElectoralState() {
        electionHandler.displayElectoralState();
    }

    /*
        We have one main issue while connecting two servers :
        They might be from different Networks.
        To detect it, we just have to attach the winner value to the InterServerMessage.
            * If it is null for both sides, we don't care, no election occured yet.No way to worry.
            * If the value is the same, this is a connection for two servers of the same network. No way to worry.
            * If the value is different, we are from different networks. We will process to these steps :
                * First connect the networks
                - The added value will launch an election if it is not the only node of its network
                - In other cases we will launch an election
     */

    private void manageElectoralStateOnServerConnection(SocketAddress otherServerElectedIdentifier, ClientStruct potentialFather) {
        if( electionHandler.getWin() == null && otherServerElectedIdentifier == null) {
            // No way to worry, both sides do not have elected someone...
            // But wait, now that we are linked, we can elect someone, no ? It would not be stupid...
            System.out.println("============================ Set server for both unelected networks");
            electionHandler.launchElection();
        } else {
            if( electionHandler.getWin() == null && state.getNbConnectedServers() == 1 ) {
                // We are connected to a network that have an elected node
                // We are alone. So we can join it with no fear and no complexity added
                System.out.println("================================ Joining electoral system for both networks");
                if( !electionHandler.joinElectoralNetwork(potentialFather, otherServerElectedIdentifier) ) {
                    System.out.println("Error while joining network. We will start a new Election.");
                    electionHandler.launchElection();
                }
                return;
            }
            if(otherServerElectedIdentifier == null ) {
                // Other side network do not have an elected node. We do not know the amount of node of this network at this given moment
                // ( even if deductible from message : things might have changed )
                // The best option is to launch a new election so that we obtain a only elected node for the two freshly joined networks
                System.out.println("=========================== Unknown topology on other side. Launching election");
                electionHandler.launchElection();
                return;
            }
            if( otherServerElectedIdentifier.toString().compareTo( electionHandler.getWin().toString() ) != 0 ) {
                // Both networks do not have the same elected winner. We should uniform that !
                System.out.println("============================== Different winner on both networks");
                electionHandler.launchElection();
            }
        }
    }

    protected State getState() {
        return state;
    }

    public String buildClientList() {
        return state.buildClientList();
    }

    public String getServerList() {
        return state.getServerList();
    }

    private void manageServerMessageSubtype( InterServerMessage incomingMessage) {
        ChatData chatData;
        switch (incomingMessage.getSubType() ) {
            case 0:
                System.out.println("Not a message set up for Diffusion algoritms : Lack of subtype");
                break;
            case 1:
                // Client joining notification
                chatData = new ChatData(0,3,"", (String)incomingMessage.getMessage() );
                state.broadcast(chatData);
                state.addPseudo((String)incomingMessage.getMessage());
                break;
            case 2:
                // Client leave notification ...
                chatData = new ChatData(0,4,"", (String)incomingMessage.getMessage() );
                state.broadcast(chatData);
                state.removePseudo((String) incomingMessage.getMessage());
                break;
            case 3:
                // Client message forwarded
                ChatMessage chatMessage = (ChatMessage) incomingMessage.getMessage();
                chatData = new ChatData(0,2,chatMessage.message, chatMessage.pseudo);
                state.broadcast(chatData);
                break;
            case 4:
                // Server tells us it just joined our network
                System.out.println("Bouilla !!!!!!!!!!!!!");
                SocketAddress itsAddress = incomingMessage.getIdentifier();
                System.out.println( itsAddress + " just joined us!");
                break;
            default:
                // Unknown message subtype received
                System.out.println("Unknown message subtype received");
                break;
        }
    }

    public void sendRClientJoin(String pseudo) {
        InterServerMessage message = new InterServerMessage(0, 3, 1);
        message.setMessage( pseudo );
        rBroadcastManager.launchRBroadcast(message);
        state.broadcast(new ChatData(0, 3, "", pseudo));
    }

    public void sendRClientLeave(String pseudo) {
        InterServerMessage message = new InterServerMessage(0, 3, 2);
        message.setMessage( pseudo );
        rBroadcastManager.launchRBroadcast(message);
        state.broadcast(new ChatData(0, 4, "", pseudo));
    }

    public void sendRMessage(String messageContent, String pseudo) {
        InterServerMessage message = new InterServerMessage(0, 3, 3);
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.pseudo = pseudo;
        chatMessage.message = messageContent;
        message.setMessage(chatMessage);
        rBroadcastManager.launchRBroadcast(message);
    }

    public void sendRServerJoin(SocketAddress socketAddress) {
        InterServerMessage message = new InterServerMessage(0, 3, 4);
        message.setMessage( socketAddress );
        rBroadcastManager.launchRBroadcast(message);
    }
}