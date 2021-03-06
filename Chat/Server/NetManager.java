package Chat.Server;

import Chat.Netmessage.*;

import Chat.Utils.ClientStruct;
import Chat.Utils.LogResourceVisitor;
import Chat.Utils.ResourceVisitor;
import csc4509.FullDuplexMessageWorker;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.net.*;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by benwa on 6/7/14.
 *
 * License : GLP 2.0
 *
 * A bad ass Net manager that will manage network on server node.
 */
public class NetManager {

    // Things initialized in a mono threaded environment and then only read.
    /**
     * Server listening port
     */
    private int port;
    /**
     * ServerSocketChannel we are accepting connections on.
     */
    private ServerSocketChannel ssc;
    /**
     * A tool for logging our conversation
     */
    private static Logger logger = Logger.getLogger(NetManager.class);
    /**
     * Do we have to log?
     */
    private Boolean isLoggingEnable = false;
    /**
     * A class that manage safely a lock across our server network.
     */
    private LockManager lockManager;
    /**
     * Election handler that manages elections
     */
    private ElectionHandler electionHandler;
    /**
     * Reliable broadcast manager...
     */
    private RBroadcastManager rBroadcastManager;
    /**
     * Causal broadcast manager.
     */
    private CBroadcastManager cBroadcastManager;
    /**
     * An attribute that will be used to retrieve server list across the network
     */
    private EchoServerListManager echoServerListManager;
    /**
     * An attribute that will be used to retrieve connected pseudo list across the network
     */
    private EchoPseudoListManager echoPseudoListManager;
    private EchoTokenListManager echoTokenListManager;
    /**
     * A class that provides end detection for our application.
     */
    protected EndManager endManager;
    /**
     * A lock to protect our use of the selector.
     */
    final ReentrantLock selectorLock = new ReentrantLock();
    /**
     * The selector we will use for async read and open.
     */
    private Selector selector;

    /**
     * Other data that give us the server state...
     */
    private State state;
    /**
     * A Boolean that holds information about scheduled shutdown requests.
     */
    private Boolean isShutdownScheduled = false;
    private Boolean isResourceReleaseScheduled = false;
    private Boolean isResourceTakeScheduled = false;
    private Boolean isResourceDisplayScheduled = false;
    final ReentrantLock schedulerLock = new ReentrantLock();
    /**
     * Accessor to get the lock manager
     * @return Our lock manager
     */
    protected LockManager getLockManager() {
        return lockManager;
    }

    /**
     * Enable logging
     */
    public void enableLogging() {
        isLoggingEnable = true;
    }

    /**
     * Disable logging
     */
    public void disableLogging() {
        isLoggingEnable = false;
    }

    /**
     * Initialisation of the Network manager.
     *
     * Only register the port number, Initialize some data structures.
     * But network registration and async loop will be performed later
     *
     * @param _port The port we will later launch the server on.
     */

    public NetManager(int _port) {
        port = _port;
        state = new State(this);
        electionHandler = new ElectionHandler(this);
        rBroadcastManager = new RBroadcastManager(this);
        echoServerListManager = new EchoServerListManager(this);
        echoPseudoListManager = new EchoPseudoListManager(this);
        cBroadcastManager = new CBroadcastManager(rBroadcastManager);
        ResourceVisitor resourceVisitor = new LogResourceVisitor(this);
        lockManager = new LockManager(rBroadcastManager, resourceVisitor);
        endManager = new EndManager(this, rBroadcastManager);
        echoTokenListManager = new EchoTokenListManager(this);
    }

    /**
     * This Method makes our server listening the whole internet on the given port port.
     * It also initialize some basic stuff for async IO.
     * Then it launches the async loop this thread will be caught on.
     */

    public void launch() {
        launchServer();
        selectorInit();
        asyncLoop();
    }

    /**
     * We make here our server listening the internet .
     * We also take time to register the election token.
     */

    protected void launchServer() {
        try {
            // Here we start listening on the given port !
            ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ServerSocket ss = ssc.socket();
            InetSocketAddress add = new InetSocketAddress(port);
            ss.bind(add);
            electionHandler.setP(add);
            rBroadcastManager.setOurAddress(add);
            cBroadcastManager.setOurAddress(add);
            echoServerListManager.setP(add);
            echoPseudoListManager.setP(add);
            echoTokenListManager.setP(add);
            lockManager.setOurAddress(add);
            state.setIdentifier(add);
        } catch (IOException se) {
            System.out.println("Failed to create server");
            se.printStackTrace();
        }
    }

    /**
     *     The loop our main thread will be caught in.
     *
     * It accepts connections, and manages read.
     * It dispatches read, thanks to the FullMessageWorkersType between these categories :
     *        - Message from client
     *        - Message related to an election
     *        - Other message transiting between two servers
     *
     * Note : We need to do this dispatch, as we need to cast the right object
     */

    protected void asyncLoop() {
        while (true) {
            try {
                selector.select(10);
            } catch (IOException ioe) {
                System.out.println(" Failed to select Selector");
                ioe.printStackTrace();
                return;
            }
            Set selectedKeys = selector.selectedKeys();
            Iterator it = selectedKeys.iterator();
            while (it.hasNext()) {
                SelectionKey key = (SelectionKey) it.next();
                if (key.isAcceptable()) {
                    // We have a new client opening a connection.
                    System.out.println(" New connection");
                    SocketChannel sc;
                    try {
                        sc = ssc.accept();
                    } catch (IOException ioe) {
                        System.out.println(" Failed to open new connexion in main loop");
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
            // Here we manage scheduled jobs.
            schedulerLock.lock();
            if(isShutdownScheduled) {
                isShutdownScheduled = false;
                endManager.startEndingDetection();
            }
            if(isResourceDisplayScheduled) {
                isResourceDisplayScheduled = false;
                lockManager.display();
            }
            if(isResourceTakeScheduled) {
                isResourceTakeScheduled = false;
                lockManager.askLock();
            }
            if(isResourceReleaseScheduled) {
                isResourceReleaseScheduled = false;
                lockManager.stopUsingRessource();
            }
            schedulerLock.unlock();
        }
    }

    /**
     *     Basic stuff with Selector initialisation.
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

    /**
     * This method is only called buy the clavier thread.
     *
     * It establish a connection with another server and notifies it we are a server.
     *
     * @param ipString Hostname to connect to
     * @param _port  Listening port to connect to
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
        ClientStruct clientStruct = makeRegistration(chan);
        if( clientStruct == null ) {
            return;
        }
        // Now specify to the server that WE ARE A SERVER ...
        InterServerMessage response = new InterServerMessage(0,0);
        response.setElectionWinner( electionHandler.getWin() );
        sendInterServerMessage(clientStruct, response, "Can not send a basic Hello I am a server ! " );
    }

    /**
     * Send a message with this an error code set, displaying a message on io failure
     *
     * @param cliStr Client to send data to
     * @param errorCode Error code you want to send
     * @param ioErrorMessage Message to display on io error while sending it
     */

    private void sendClientError( ClientStruct cliStr, int errorCode, String ioErrorMessage) {
        ChatData chdata = new ChatData(0, 6, "");
        chdata.setErrorCode(errorCode);
        sendClientMessage(cliStr, chdata, ioErrorMessage);
    }

    /**
     * Send an error message to a server
     *
     * @param clientStruct Server we will send the error to
     * @param errorCode Error code you want to send
     * @param ioErrorMessage Message to display on io error while sending it
     */

    private void sendServerError( ClientStruct clientStruct, int errorCode, String ioErrorMessage) {
        InterServerMessage ism = new InterServerMessage(0,42);
        ism.setErrorCode(errorCode);
        sendInterServerMessage(clientStruct, ism, ioErrorMessage);
    }

    /**
     * Send a message to an other server.
     *
     * @param clientStruct server we will send the error to
     * @param mes Message to send
     * @param ioErrorMessage Message to display on io error while sending it
     */

    protected void sendInterServerMessage( ClientStruct clientStruct,  InterServerMessage mes, String ioErrorMessage ) {
        //mes.setElectionWinner( electionHandler.getWin() );
        try {
            clientStruct.lock();
            clientStruct.getFullDuplexMessageWorker().sendMsg(2, mes);
            clientStruct.unlock();
        } catch (IOException ioe) {
            System.out.println(ioErrorMessage);
            manageIOErrorOnServerConnection(clientStruct);
        } catch (NullPointerException npe) {
            System.out.println(ioErrorMessage);
            manageIOErrorOnServerConnection(clientStruct);
        }
    }

    /**
     * Send a message to a client.
     *
     * @param clientStruct The client connection structure we will use to send our message
     * @param chatData Message to send
     * @param ioErrorMessage Message to display on io error while sending it
     */

    protected void sendClientMessage( ClientStruct clientStruct, ChatData chatData, String ioErrorMessage ) {
        try{
            clientStruct.lock();
            clientStruct.getFullDuplexMessageWorker().sendMsg(0, chatData);
            clientStruct.unlock();
        } catch( IOException ioe) {
            System.out.println(ioErrorMessage);
            manageIOErrorOnClientConnection( clientStruct);
        }
    }

    /**
     * Build the FullDuplexMessageWorker, configure the channel as non blocking, then register the channel.
     *
     * @param chan The channel we want to register.
     * @return The FullDuplexMessageWorker we will use to speak across the channel we just registered
     */

    private ClientStruct makeRegistration(SocketChannel chan) {
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
        return str;
    }

    /**
     * It closed all the connections registered as both clients and servers.
     * It give us a new clean debug environment without launching each servers a new time ( quite long with compilation )
     * Useless in production environments but so useful while testing distributed algorithms
    */

    public void reInitNetwork() {
        state.reInitNetwork();
    }


    /**
     *     A quite verbose method that analyses clients messages and took appropriate decisions.
     *    It part of the async loop, but separated for readability.
     *
     *    @param cliStr Client connection structure we will read data on
     */

    private  void handleClientMessage(ClientStruct cliStr) {
        if(!cliStr.getActive()) {
            System.out.println("Client is inactive");
            return;
        }
        FullDuplexMessageWorker duplexMessageWorker = cliStr.getFullDuplexMessageWorker();
        ChatData localChatData;
        ChatData rcv;
        try {
            Serializable serializable = duplexMessageWorker.getData();
            if(serializable == null) {
                System.out.println(" Failed to receive message");
                manageIOErrorOnClientConnection( cliStr );
                return;
            }
            rcv = (ChatData) serializable;
        } catch (IOException ioe) {
            // We removed the fail client from our pool of clients...
            System.out.println(" Failed to receive message");
            manageIOErrorOnClientConnection( cliStr );
            return;
        }
        // Reset error suite number to 0 as we succeded into obtaining data from this client
        cliStr.resetIoError();
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
                if ( ! state.isPseudoUsed(rcv.getPseudo()) ) {
                    System.out.println("Pseudo available");
                    // But first tell the client he was accepted
                    sendClientMessage(cliStr, new ChatData(0, 1, "", rcv.getPseudo()), " Failed to send ack for a pseudo demand");
                    // Store the login
                    cliStr.setPseudo(rcv.getPseudo());
                    state.addClient(cliStr);
                    state.addPseudo(rcv.getPseudo());
                    // And notify every one
                    if( state.getStandAlone() ) {
                        // No need to use a complex diffusion algorithm, we are stand alone...
                        state.broadcast(new ChatData(0, 3, "", rcv.getPseudo()));
                    } else {
                        sendCClientJoin(rcv.getPseudo());
                    }
                    break;
                } else {
                    // We can not allocate the pseudo as it is already taken. Notify Client.
                    sendClientError(cliStr, 1, "Failed to send error for error for an alredy used login");
                }
            case 1:
                // Here we have an acknowledgment for pseudo -> error
                sendClientError(cliStr, 3, "Failed to send error for a client who sent an ack for pseudo");
                break;
            case 2:
                //Here we have someone who sent a message
                System.out.println("new message");
                // Broadcast it
                if (cliStr.hasPseudo()) {
                    System.out.println("new message content " + rcv.getMessage());
                    state.broadcast(new ChatData(0, 2, rcv.getMessage(), cliStr.getPseudo()));
                    if( isLoggingEnable )
                        logger.info( cliStr.getPseudo() + " : " + rcv.getMessage() );
                    if( ! state.getStandAlone() ) {
                        sendCMessage(rcv.getMessage(), cliStr.getPseudo());
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
                //Here we have a disconnection request
                System.out.println("Disconnection request handled");
                sendClientLeave(cliStr);
                closeSocket( duplexMessageWorker );
                break;
            case 7:
                System.out.println("Man, a user list request !");
                // The client asks for a list of all users
                if (cliStr.hasPseudo()) {
                    // Ok, we now him, let send it
                    System.out.println("Yes he is authentificated and we can send the user list ( we will really do it ! ) ");
                    localChatData = new ChatData(0, 8, state.getClientsString(), cliStr.getPseudo());
                    sendClientMessage(cliStr, localChatData, "Could not send the user list");
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
                    sendClientMessage(cliStr, new ChatData(0,10,""),"Failed to send Ack for spared connection registring");
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
                sendClientMessage(cliStr, new ChatData(0,11,""), "Failed to send Ack for spare connection activation");
                break;
            case 12:
                // The client send a private message
                String dest = rcv.pseudoDestination;
                if( state.isPseudoTaken( dest ) ) {
                    localChatData = new ChatData(0,12,rcv.getMessage(), rcv.getPseudo());
                    localChatData.pseudoDestination = dest;
                    sendClientMessage(state.getClientByPseudo(dest), localChatData, "Failed send private message to client directly connected");
                } else {
                    sendCPrivateMessage(rcv.getMessage(), rcv.getPseudo(), dest);
                }
                if( isLoggingEnable )
                        logger.info( rcv.getPseudo() + " send to " + dest + " : " + rcv.getMessage());
                break;
            case 13:
                // Answer from our demand of servers list
                System.out.println("Demand of list of server");

                String serverListString;
                if( state.getStandAlone() ) {
                    serverListString = state.getIdentifier().toString();
                } else {
                    serverListString = state.getServerConnectedOnOurNetworkString();
                }
                sendClientMessage(cliStr,new ChatData(0,13,serverListString),"Error sending answer to client");
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


    /**
     * A quite verbose method that analyses servers messages and took appropriate decisions.
     * It part of the async loop, but separated for readability.
     *
     * @param cliStr The server connection structure we will read data on
     */

    private void manageServersMessages(ClientStruct cliStr) {
        FullDuplexMessageWorker fdmw = cliStr.getFullDuplexMessageWorker();
        // Here we are synchronising messages on our ( future ) distributed Chat
        InterServerMessage incomingMessage;
        try{
            Serializable serializable = fdmw.getData();
            if( serializable == null) {
                System.out.println("Can not retrieve InterServerMessage we are receiving 2");
                manageIOErrorOnServerConnection(cliStr);
                return;
            }
            incomingMessage = (InterServerMessage) serializable;
        } catch( IOException ioe) {
            System.out.println("Can not retrieve InterServerMessage we are receiving");
            manageIOErrorOnServerConnection(cliStr);
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
                    sendServerError(cliStr,3, "IO ERROR SERVER CODE 3");
                } else {
                    electionHandler.unlock();
                    System.out.println("Request for server connection received");
                    state.addServer(cliStr);
                    InterServerMessage response = new InterServerMessage(0, 1);
                    response.setElectionWinner( electionHandler.getWin() );
                    sendInterServerMessage(cliStr, response, "Can not send ack for a server connection established");
                }
                break;
            case 1:
                System.out.println("DBG Rcv 1");
                electionHandler.lock();
                if(electionHandler.getIsInElection()) {
                    electionHandler.unlock();
                    System.out.println("Server add reply while in election. Sending error");
                    sendServerError(cliStr,4, "IO ERROR SERVER CODE 4");
                } else {
                    electionHandler.unlock();
                    System.out.println("Our request for opening a new connection to another server was answered");
                    state.addServer(cliStr);
                    manageElectoralStateOnServerConnection( incomingMessage.getElectionWinner() );
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
                if( state.getNbConnectedServers() > 0 ) {
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Launching election on server disconnection");
                    electionHandler.launchElection();
                } else {
                    System.out.println("A server leaved us and we switched to stand alone...");
                    state.switchToStandAlone();
                }

                break;
            case 3:
                // R diffusion message
                System.out.println("R broadcast detected");
                if( rBroadcastManager.manageInput( incomingMessage) ) {
                    // We have to process this message : it was accepted
                    System.out.println("R Broadcast accepted");
                    manageServerMessageSubtype(incomingMessage);
                }
                break;
            case 5 :
                endManager.notifyUserReceive();
                // C diffusion
                System.out.println("C broadcast detected");
                if( cBroadcastManager.manageInput( incomingMessage) ) {
                    System.out.println("Results for C broadcast : input messages");
                    ArrayList<InterServerMessage> acceptedMessages = cBroadcastManager.getCAcceptedMessages();
                    for( InterServerMessage acceptedMessage : acceptedMessages) {
                        System.out.println("Managing message subtype : " + acceptedMessage.getSubType());
                        manageServerMessageSubtype(acceptedMessage);
                    }
                }
                // We have ended to manage our
                endManager.inputProcessEnded();
                break;
            case 6 :
                System.out.println("Working with retrieve clients requests");
                ArrayList<Serializable> result = echoPseudoListManager.processInput(incomingMessage, cliStr);
                if( result != null ) {
                    System.out.println(" ############################################# ");
                    for (Serializable serializable : result) {
                        System.out.println((String) serializable);
                    }
                    System.out.println(" ############################################# ");
                    state.setPseudoList( result );
                    launchRPseudoSet(result);
                }
                break;
            case 7:
                System.out.println("Working with server retrieve request ");
                ArrayList<Serializable> res = echoServerListManager.processInput(incomingMessage, cliStr);
                if( res != null ) {
                    System.out.println(" ############################################# ");
                    for (Serializable serializable : res) {
                        System.out.println(( serializable).toString());
                    }
                    System.out.println(" ############################################# ");
                    state.setServerConnectedOnOurNetwork(res);
                    launchRServerSet(res);
                    reInitVectorialClock();
                    lockManager.makeDemInit(res);
                }
                break;
            case 8 :
                System.out.println("Working with token retrieve request ");
                ArrayList<Serializable> resu = echoTokenListManager.processInput(incomingMessage, cliStr);
                if( resu != null ) {
                    System.out.println(" ############################################# ");
                    System.out.println( resu.size() );
                    System.out.println(" ############################################# ");
                    if(resu.size() == 0) {
                        lockManager.createToken();
                        break;
                    }
                    if(resu.size() == 1) {
                        break;
                    }
                    if(resu.size() == 2) {
                        System.out.println("Bouillla !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        break;
                    }
                    launchRServerSet(resu);

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

    /**
     * Proxy method to launch an election
     */

    public void launchElection() {
        electionHandler.launchElection();
    }

    /**
     * Proxy method to display Electoral state in the terminal
     */

    public void displayElectoralState() {
        electionHandler.displayElectoralState();
    }

    /**
     * Accessor for the state of our server
     *
     * @return State of our server
     */
    protected State getState() {
        return state;
    }

    /**
     * Proxy method to obtain client pseudo list String
     *
     * @return pseudo list String
     */

    public String buildClientList() {
        return state.buildClientList();
    }

    /**
     * Proxy method to get server list string
     *
     * @return server list string
     */

    public String getServerList() {
        return state.getServerList();
    }

    /**
     * Manage server sub message. called by manageServerMessages
     * @param incomingMessage Message to manage
     */

    private void manageServerMessageSubtype( InterServerMessage incomingMessage) {
        ChatData chatData;
        switch (incomingMessage.getSubType() ) {
            case 0:
                System.out.println("Not a message set up for Diffusion algoritms : Lack of subtype");
                break;
            case 1:
                // Client joining notification
                String pseudo = (String)incomingMessage.getMessage();
                System.out.println("Joining notif r broadcasted "+pseudo);
                if( ! state.isPseudoTaken(pseudo) ) {
                    chatData = new ChatData(0, 3, "", pseudo);
                    state.broadcast(chatData);
                    state.addPseudo((String) incomingMessage.getMessage());
                    if( isLoggingEnable )
                        logger.info( incomingMessage.getMessage()+ " joined the Chat.");
                }
                System.out.println();
                break;
            case 2:
                // Client leave notification ...
                pseudo = (String)incomingMessage.getMessage();
                System.out.println("Leave notif r broadcasted");
                chatData = new ChatData(0,4,"", pseudo );
                state.broadcast(chatData);
                state.removePseudo((String) incomingMessage.getMessage());
                System.out.println();
                if( isLoggingEnable )
                    logger.info(  incomingMessage.getMessage()+ " leaved the Chat.");
                break;
            case 3:
                // Client message forwarded
                System.out.println("Message r broadcasted");
                ChatMessage chatMessage = (ChatMessage) incomingMessage.getMessage();
                chatData = new ChatData(0,2,chatMessage.message, chatMessage.pseudo);
                state.broadcast(chatData);
                System.out.println();
                if( isLoggingEnable )
                    logger.info( chatMessage.pseudo+ " : " + chatMessage.message);
                break;
            case 4:
                // Server tells us it just joined our network
                SocketAddress itsAddress = incomingMessage.getIdentifier();
                System.out.println( itsAddress + " just joined us!");
                state.addServerConnectedOnOurNetwork(itsAddress);
                break;
            case 5:
                // Private message
                chatMessage = (ChatMessage) incomingMessage.getMessage();
                String dest = chatMessage.dest;
                if( state.isPseudoTaken( dest ) ) {
                    ChatData chdata = new ChatData(0,12,chatMessage.message, chatMessage.pseudo);
                    chdata.pseudoDestination = dest;
                    sendClientMessage(state.getClientByPseudo(dest), chdata, "Failed send private message to client directly connected ( after broadcast ) ");
                }
                if( isLoggingEnable )
                        logger.info( chatMessage.pseudo + " send to " + dest + " : " + chatMessage.message );
                break;
            case 6:
                // The winner set up our server list
                ArrayList<Serializable> listOfServers = ( ArrayList<Serializable>) incomingMessage.getMessage();
                state.setServerConnectedOnOurNetwork(listOfServers);
                System.out.println("Job done, I now use master's list of servers");
                reInitVectorialClock();
                lockManager.makeDemInit(listOfServers);
                break;
            case 7:
                // The winner set up our client list
                ArrayList<Serializable> listOfPseudo = ( ArrayList<Serializable>) incomingMessage.getMessage();
                state.setPseudoList(listOfPseudo);
                System.out.println("Job done, I now use master's list of pseudos");
                break;
            case 8:
                System.out.println("We received a lock request for lock");
                lockManager.manageRequestBroadcast(incomingMessage);
                break;
            case 9:
                System.out.println("We received the token for lock");
                lockManager.manageTokenReception(incomingMessage);
                break;
            case 10:
                System.out.println("A broadcast request for shut down was received.");
                System.exit(0);
                break;
            case 11 :
                System.out.println();
                System.out.println();
                System.out.println("Ending detection received : ");
                endManager.TockenReception(incomingMessage);
                break;
            default:
                // Unknown message subtype received
                System.out.println("Unknown message subtype received");
                break;
        }
    }

    /**
     * Send a R broadcast message to notify that a client joined our network
     *
     * @param pseudo Pseudo of the client that just joined our network
     */

    public void sendCClientJoin(String pseudo) {
        InterServerMessage message = new InterServerMessage(0, 5, 1);
        message.setMessage( pseudo );
        cBroadcastManager.launchBroadcast(message);
        state.broadcast(new ChatData(0, 3, "", pseudo));
        if( isLoggingEnable )
            logger.info(  pseudo + " joined the Chat.");
    }

    /**
     * Send a R broadcast message to notify that a client leaved our network
     *
     * @param pseudo Pseudo of the client that just leaved our network
     */

    public void sendCClientLeave(String pseudo) {
        InterServerMessage message = new InterServerMessage(0, 5, 2);
        message.setMessage( pseudo );
        cBroadcastManager.launchBroadcast(message);
        state.broadcast(new ChatData(0, 4, "", pseudo));
        if( isLoggingEnable )
            logger.info(  pseudo + " leaved the Chat.");
    }

    /**
     * Send a R broadcast message to send a message form one of our clients
     *
     * @param messageContent Message content
     * @param pseudo The pseudo of the client that sent the message
     */

    public void sendCMessage(String messageContent, String pseudo) {
        InterServerMessage message = new InterServerMessage(0, 5, 3);
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.pseudo = pseudo;
        chatMessage.message = messageContent;
        message.setMessage(chatMessage);
        cBroadcastManager.launchBroadcast(message);
    }

    /**
     * RBroadcast a private message ( that is addressed to a client connected on an other server ).
     *
     * @param messageContent Message content
     * @param pseudo Sender pseudo
     * @param dest Destination pseudo
     */

    public void sendCPrivateMessage(String messageContent, String pseudo, String dest) {
        InterServerMessage message = new InterServerMessage(0, 5, 5);
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.pseudo = pseudo;
        chatMessage.message = messageContent;
        chatMessage.dest = dest;
        message.setMessage(chatMessage);
        cBroadcastManager.launchBroadcast(message);
    }

    /**
     * Proxy method to launch an echo to get the list of server that are on our network
     */

    public void launchServerDiscovery() {
        echoServerListManager.launchEcho();
    }

    /**
     * Method to export your server list ( that is up to date ) on the network via a R broadcast.
     *
     * @param serverList Server list to export
     */

    private void launchRServerSet(ArrayList<Serializable> serverList) {
        InterServerMessage message = new InterServerMessage(0, 3, 6);
        message.setMessage( serverList );
        rBroadcastManager.launchBroadcast(message);
    }

    /**
     * Proxy method to launch a pseudo list discovery on our network
     */

    public void launchPseudoDiscovery() {
        echoPseudoListManager.launchEcho();
        echoTokenListManager.launchEcho();
    }

    /**
     * Method to export your pseudo list ( that is up to date ) on the network via a R broadcast.
     *
     * @param pseudoList Pseudo list to export
     */

    private void launchRPseudoSet(ArrayList<Serializable> pseudoList) {
        InterServerMessage message = new InterServerMessage(0, 3, 7);
        message.setMessage( pseudoList );
        rBroadcastManager.launchBroadcast(message);
    }

    /**
     * A method to send a message to all clients to shutdown them.
     */
    public void shutdownOurInfrastructure() {
        InterServerMessage message = new InterServerMessage(0, 3, 10);
        rBroadcastManager.launchBroadcast(message);
        System.exit(0);
    }

    /**
     * Manage IOError on client connection
     *
     * On 5 errors, disconnect client and send a client leave
     */

    protected void manageIOErrorOnClientConnection(ClientStruct clientStruct) {
        if( clientStruct.addIOError() ) {
            sendClientLeave(clientStruct);
            state.removeClient(clientStruct);
            closeSocket(clientStruct.getFullDuplexMessageWorker());
        }
    }

    /**
     * Send the appropriate client leave, depending of stand alone mode
     *
     * @param cliStr The client structure which client is leaving us.
     */
    private void sendClientLeave(ClientStruct cliStr) {
        if (cliStr.hasPseudo()) {
            state.removePseudo(cliStr.getPseudo());

            if( !state.getStandAlone() ) {
                sendCClientLeave(cliStr.getPseudo());
            } else {
                ChatData chdata = new ChatData(0, 4, "", cliStr.getPseudo());
                state.broadcast(chdata);
            }
        }
        // Remove the client
        state.removeClient(cliStr);
    }

    /**
     * Close the socket associated with our FullDuplexMessageWorker
     *
     * @param fdmw The FullDuplexMessageWorker we will close
     */

    private void closeSocket(FullDuplexMessageWorker fdmw) {
        try {
            fdmw.close();
        } catch (IOException ioe) {
            System.out.println(" Failed to close channel");
            ioe.printStackTrace();
        }
    }

    /**
     * Convenient methods used in NetManager thread by manageInput to launch an election
     *
     * @param connectionStruct Connection structure to send the data to
     * @param electionToken ElectionToken to broadcast
     * @param ioErrorMessage Message to display on io error
     */

    protected void sendElectionToken( ClientStruct connectionStruct, ElectionToken electionToken, String ioErrorMessage ) {
        try{
            connectionStruct.getFullDuplexMessageWorker().sendMsg(1,electionToken);
        } catch(IOException ioe) {
            System.out.println(ioErrorMessage);
        }
    }

    /**
     * Manage IOError on server connection
     *
     * On 5 errors, disconnect server and send and launch an election as topology changed
     */

    protected void manageIOErrorOnServerConnection(ClientStruct clientStruct) {
        if( clientStruct.addIOError() ) {
            System.out.println("Hello pussy");
            // We remove the failed server from our connections.
            state.removeServer(clientStruct);
            // In doubt launch an election ... If the removed server is either elected nor separating us from elected server.
            if( ! state.getStandAlone() ) {
                electionHandler.launchElection();
            } else {
                System.out.println("Switching standalone");
                state.switchToStandAlone();
                electionHandler.switchStandAlone();
                if( state.getStandAlone() ) {
                    System.out.println("Seul et célibataire !");
                }
            }
            closeSocket(clientStruct.getFullDuplexMessageWorker());
        }
    }

    /**
     * Called when we establish a new connection with another server.
     * We identify if it belongs to the same network ( based on the identifier of the election winner )
     * If we are part of the same network, nothing is done. If we are not part of the same network, we elect a new election winner.
     *
     * The goal of this is simple : we should have only one winner per network. Both network with more that one node have an election winner.
     * So another thing : If the node is alone, the most simple thing to do is the exact same thing...
     *
     * @param otherServerElectedIdentifier The elected winner of the other node, used to know if we belong to the same network.
     */

    private void manageElectoralStateOnServerConnection(SocketAddress otherServerElectedIdentifier) {
        if (electionHandler.getWin() == null && otherServerElectedIdentifier == null) {
            // No way to worry, both sides do not have elected someone...
            // But wait, now that we are linked, we can elect someone, no ? It would not be stupid...
            System.out.println("============================ Set server for both unelected networks");
            electionHandler.launchElection();
        } else {
            if (electionHandler.getWin() == null ) {
                electionHandler.launchElection();
                return;
            }
            if (otherServerElectedIdentifier == null) {
                // Other side network do not have an elected node. We do not know the amount of node of this network at this given moment
                // ( even if deductible from message : things might have changed )
                // The best option is to launch a new election so that we obtain a only elected node for the two freshly joined networks
                System.out.println("=========================== Unknown topology on other side. Launching election");
                electionHandler.launchElection();
                return;
            }
            if (otherServerElectedIdentifier.toString().compareTo(electionHandler.getWin().toString()) != 0) {
                // Both networks do not have the same elected winner. We should uniform that !
                System.out.println("============================== Different winner on both networks");
                electionHandler.launchElection();
            }
        }
    }

    /**
     * Use the connected server on our network to re init our vectoral clock
     */
    public void reInitVectorialClock() {
        cBroadcastManager.reInitVectorialClock( state.getServerConnectedOnOurNetwork() );
    }

    /**
     * Proxy method to display our vectorial clock ( Debug purposes).
     */
    public void displayOurVectorialClock() {
        cBroadcastManager.displayVectorialClock();
    }

    /**
     * Display the waiting messages in C Broadcast manager.
     */
    public void displayCMessageBag() {
        cBroadcastManager.displayMessageBag();
    }

    /**
     * Debug utility : display our lock manager state
     */
    public void displayLockState() {
//        lockManager.display();
        schedulerLock.lock();
        isResourceDisplayScheduled = true;
        schedulerLock.unlock();
    }

    /**
     * Start using resource. Ask for token, and we will start using resource on reception.
     */
    public void startUsingResource() {
//        lockManager.askLock();
        schedulerLock.lock();
        isResourceTakeScheduled = true;
        schedulerLock.unlock();
    }

    /**
     * Stop using resource. We will give the token to somebody else if we were asked for.
     */
    public void stopIsingResource() {
        schedulerLock.lock();
//        lockManager.stopUsingRessource();
        isResourceReleaseScheduled = true;
        schedulerLock.unlock();
    }

    public void safeShutDown() {
        schedulerLock.lock();
        if(state.getStandAlone()) {
            isShutdownScheduled = true;
            return;
        }
        if(  electionHandler.getState() == 2) {
          //  endManager.startEndingDetection();
            isShutdownScheduled = true;
        } else {
            System.out.println("No way, man, you are not the master...");
        }
        schedulerLock.unlock();

    }

    public int getElectoralState() {
        return electionHandler.getState();
    }
}