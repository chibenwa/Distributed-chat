package Chat;

import csc4509.FullDuplexMessageWorker;
import sun.awt.Mutex;

import java.io.IOException;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by benwa on 6/5/14.
 */
public class ChatServer {
    private int port;
    private ServerSocketChannel ssc;
    private Selector selector;
    private List<ClientStruct> cliStrs;
    private  List<ConnectionStruct> serverStrs;

    final ReentrantLock selectorLock = new ReentrantLock();;

    // Stuff for elections
    // If we add servers while an election is taking place, I fear bad things to happen.
    // We will use a boolean to protect us from launching a new election and adding client while electing someone
    private Boolean isInElection = false;
    // A mutex to protect us... Gods !
    private Mutex electionMutex;
    // SocketAddress is used instead of pid. Works well until you use NAT, and are not really lucky
    private SocketAddress caw = null;
    // The father is a current connection present in serverStr. We use it instead of a Socket address for conviniance ( far much better to send a message )
    private ConnectionStruct father = null;
    // Winner have to be compared with caw and p. So that is a SocketAddress.
    private SocketAddress win = null;
    // Same thing for p ( we will obtain it once for all )
    private SocketAddress p = null;
    // Here comes integer. rec : number of Jeton for current wave received
    // lrec : number of GAGNANT received
    int rec = 0;
    int lrec = 0;
    /*
        State :
         * 0 : sleeping
         * 1 : looser
         * 2 : winner
     */
    int state = 0;

    public ChatServer(int _port) {
        port = _port;
        cliStrs = new ArrayList<ClientStruct>();
        serverStrs = new ArrayList<ConnectionStruct>();
        electionMutex = new Mutex();
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
            // We init p used for elections
            p = add;
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
                                        System.out.println("Yes he is authentificated and we can send the user list ( we will really do it ! ) ");
                                        chdata = new ChatData(0,8,buildClientList(), cliStr.getPseudo());
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
                                electionMutex.lock();
                                if( !isInElection) {
                                    System.out.println("Someone else triggered an election. Locking...");
                                    isInElection = true;
                                }
                                electionMutex.unlock();
                                // Oh dude here this is an election package !
                                ElectionToken electionToken;
                                try{
                                    electionToken = (ElectionToken) fdmw.getData();
                                }catch( IOException ioe) {
                                    System.out.println("Cannot retreive Election data");
                                    break;
                                }
                                SocketAddress r = electionToken.getR();
                                switch (electionToken.getType()) {
                                    case 0:
                                        // JETON RECEIVED
                                        System.out.println("We have received a Jeton");
                                        if( caw == null ) {
                                            System.out.println("Initialising for this election");
                                            // First message received by a non candidate process. We need to do some init :
                                            state = 0;
                                            rec = 0;
                                            lrec = 0;
                                            father = null;
                                            win = null;
                                        }
                                        if(caw == null || r.toString().compareTo( caw.toString() ) < 0 ) {
                                            System.out.println("Stronger wave founded. leaving the current one.");
                                            // Our message is weaker than the other. We are replaced.
                                            caw = r;
                                            rec = 0;
                                            // Luck, I am your father !
                                            father = cliStr;
                                            // Propagate the stronger wave
                                            ElectionToken newToken = new ElectionToken(0);
                                            newToken.setR(r);
                                            System.out.println("Propagatting our new wave");
                                            for( ConnectionStruct connectionStruct : serverStrs) {
                                                if( connectionStruct != father) {
                                                    sendElectionToken( connectionStruct, newToken, "Error while sending the new token");
                                                }
                                            }
                                        }
                                        if( caw.toString().compareTo( r.toString() ) == 0 ) {
                                            System.out.println("Answer to our current wave received");
                                            rec++;
                                            if( rec == serverStrs.size() ) {
                                                System.out.print("Current Jeton wave completed : ");
                                                if( caw.toString().compareTo(p.toString()) == 0) {
                                                    // Here we actually won, so we have to broadcast it.
                                                    System.out.print(" We have won... Broadcast it ;-)");
                                                    ElectionToken winner = new ElectionToken(1);
                                                    winner.setR(p);
                                                    broadcastToken(winner,"Error while broadcasting our victory");
                                                } else {
                                                    System.out.println("Answer dad");
                                                    // All our neighbours have answered us so we can reply to our father
                                                    ElectionToken answer = new ElectionToken(0);
                                                    answer.setR(caw);
                                                    sendElectionToken(father,answer, "Answer while returning token to father");
                                                }
                                            }
                                        }
                                        break;
                                    case 1:
                                        // GAGNANT RECEIVED
                                        System.out.println("We received a GAGNANT");
                                        if(lrec == 0 || r.toString().compareTo(p.toString()) != 0) {
                                            // Broadcast that we loose
                                            System.out.println("We received somebody else GAGNANT");
                                            ElectionToken winner = new ElectionToken(1);
                                            winner.setR(r);
                                            broadcastToken(winner, "Error while broadcasting somebody else victory");
                                        }
                                        // Increment the GAGNANT token received
                                        lrec++;
                                        // And tell who is the winner !
                                        win = r;
                                        if( lrec == serverStrs.size() ) {
                                            System.out.println("End of the Election");
                                            // End of the election.
                                            if( win.toString().compareTo(p.toString()) == 0 ) {
                                                System.out.println("We won");
                                                state = 2;
                                            } else {
                                                System.out.println("We lost");
                                                state = 1;
                                            }
                                            electionMutex.lock();
                                            // We are no more in an electoral state. Unlock it dude.
                                            isInElection = false;
                                            // Here we are preparing stuff for next election
                                            // caw must be null t the beginning of the election for not candidates. In other case, the behaviour can't be predicted.
                                            caw = null;
                                            electionMutex.unlock();
                                        }

                                        break;
                                    default:

                                        break;
                                }
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
                                            System.out.println("DBG Rcv 0");
                                            electionMutex.lock();
                                            if( isInElection ) {
                                                electionMutex.unlock();
                                                // We are in election, we can not add a server !
                                                System.out.println("Server add demand while in election. Sending error");
                                                sendServerError(fdmw,3, "IO ERROR SERVER CODE 3");
                                            } else {
                                                electionMutex.unlock();
                                                System.out.println("Request for server connection received");
                                                // Someone make a demand to be added to servers.
                                                if (handleServerConnectionRequest(cliStr)) {
                                                    break;
                                                }
                                                // Then notify the other server that he had been correctly added :-)
                                                sendInterServerMessage(fdmw, new InterServerMessage(0, 1), "Can not send ack for a server connection established");
                                            }
                                            break;
                                        case 1:
                                            System.out.println("DBG Rcv 1");
                                            electionMutex.lock();
                                            if(isInElection) {
                                                electionMutex.unlock();
                                                System.out.println("Server add reply while in election. Sending error");
                                                sendServerError(fdmw,4, "IO ERROR SERVER CODE 4");
                                            } else {
                                                electionMutex.unlock();
                                                System.out.println("Our request for opening a new connection to another server was answered");
                                                // Someone answered our demand for server connection. Ok, so we are well connected. Add the connection to the servers connections.
                                                handleServerConnectionRequest(cliStr);
                                            }
                                            break;
                                        case 2:
                                            // Remote server demands us to close the connection. Let's do it
                                            Boolean present = false;
                                            ConnectionStruct toRemove = null;
                                            for( ConnectionStruct cstr : serverStrs) {
                                                if( cstr.getFullDuplexMessageWorker() == fdmw ) {
                                                    present = true;
                                                    toRemove = cstr;
                                                }
                                            }
                                            if(present) {
                                                serverStrs.remove(toRemove);
                                            }
                                            try {
                                                fdmw.close();
                                            } catch(IOException ioe) {
                                                System.out.println("We could not disconnect server as requested...");
                                            }
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

    public String buildClientList() {
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
        return pseudoChunk;
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

    public void connectServer( String ipString, int _port) {
        electionMutex.lock();
        System.out.println("Lock succeed");
        if( isInElection) {
            electionMutex.unlock();
            System.out.println("We are in a f***ing election dude. No way...");
            return;
        }
        electionMutex.unlock();
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
        FullDuplexMessageWorker fullDuplexMessageWorker = new FullDuplexMessageWorker(chan);
        ClientStruct str = new ClientStruct(fullDuplexMessageWorker);
        try {
            fullDuplexMessageWorker.configureNonBlocking();
        } catch (IOException ioe) {
            System.out.println("Can not configure channel server as non blocking");
            return;
        }
        System.out.println("FullDuplexMessageWorker both created and non blocking");
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
        System.out.println("Channel registered");
        // Now specify to the server that WE ARE A SERVER ...
        System.out.println("Sending request : Basic hello world, I am a server");
        sendInterServerMessage(fullDuplexMessageWorker, new InterServerMessage(0,0), "Can not send a basic Hello I am a server ! " );
        System.out.println("Sended");
    }

    private Boolean isServerConnectionEstablished( FullDuplexMessageWorker fdmw ) {
        for( ConnectionStruct conStr : serverStrs) {
            if( conStr.getFullDuplexMessageWorker() == fdmw ) {
                return true;
            }
        }
        return false;
    }

    private Boolean handleServerConnectionRequest(ClientStruct cliStr) {
        Boolean res = isServerConnectionEstablished(cliStr.getFullDuplexMessageWorker());
        if( res ) {
            System.out.println("Connection already established. Sending error.");
            // Send error
            sendServerError(cliStr.getFullDuplexMessageWorker(), 1, "Error while sending error for a double established server connection warning 1");
        } else {
            serverStrs.add( cliStr );
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

    public void launchElection() {
        // Will be call by an other thread -> mutex !
        electionMutex.lock();
        if( isInElection ) {
            electionMutex.unlock();
            System.out.println("We are in an election process, we can not generate a new election dude...");
            return;
        }
        electionMutex.unlock();
        // Ok, do some init
        win = null;
        father = null;
        caw = p;
        rec = 0;
        lrec = 0;
        state = 0;
        ElectionToken electionToken = new ElectionToken(0);
        electionToken.setR(p);
        broadcastToken( electionToken, "Problem generating an election");
    }

    public String getServerList() {
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
        return res;
    }

    public void reInitNetwork() {
        /*
        Useless in production environments but so useful while testing distributed algorithms
         */
        for( ConnectionStruct cstr : serverStrs) {
            sendInterServerMessage( cstr.getFullDuplexMessageWorker(), new InterServerMessage(0,2),"Can not send a disconnection demand" );
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
    }

    private void sendElectionToken( ConnectionStruct connectionStruct, ElectionToken electionToken, String ioErrorMessage ) {
        try{
            connectionStruct.getFullDuplexMessageWorker().sendMsg(1,electionToken);
        } catch(IOException ioe) {
            System.out.println(ioErrorMessage);
        }
    }

    private void broadcastToken( ElectionToken electionToken, String ioErrorMessage) {
        for( ConnectionStruct connectionStruct : serverStrs) {
            try {
                connectionStruct.getFullDuplexMessageWorker().sendMsg(1, electionToken);
            } catch (IOException ioe) {
                System.out.println(ioErrorMessage);
            }
        }
    }

}
