package Chat.Server;

import Chat.Netmessage.ElectionToken;
import Chat.Utils.ClientStruct;
import Chat.Utils.ConnectionStruct;
import csc4509.FullDuplexMessageWorker;
import sun.awt.Mutex;
import java.io.IOException;
import java.net.SocketAddress;

/**
 * Created by benwa on 6/7/14.
 *
 * This class aims at managing the all election process
 *
 * License : GLP 2.0
 */
public class ElectionHandler {
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
    private int rec = 0;
    private int lrec = 0;
    /*
        State :
         * 0 : sleeping
         * 1 : looser
         * 2 : winner
     */
    private int state = 0;
    // To set P once only
    private Boolean isPset = false;
    //The NetManager we will use as support
    NetManager netManager;

    public ElectionHandler(NetManager _netManager) {
        electionMutex = new Mutex();
        netManager = _netManager;
    }


    /*
    We need to set the unique identifier we will use...
    It is done when we open our server.
    That is why it is not done in the constructor
     */

    public void setP ( SocketAddress add) {
        if( ! isPset ) {
            isPset = true;
            // We init p used for elections
            p = add;
        } else {
            System.out.println("P for electoral system can only be set once... ");
        }
    }

    /*
    We know if we are currently in an election

    WARNING : not thread safe.
    You need to use ElectionHandler::lock and ElectionHandler::unlock
    to protect this method. You've been warned.
     */

    public Boolean getIsInElection() {
        return isInElection;
    }


    /*
        Possibly called by many threads.
        We have to get worried...

        This method launches an election by broadcasting a Jeton.
        It also initialize the class for the next Election.
     */

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
        netManager.getState().broadcastToken(electionToken, "Problem generating an election");
    }

    /*
        In the coming code, we will have to know
        our electoral status. And optionnaly display it
        ( debug purpose )
     */

    public int getState() {
        return state;
    }

    public void displayElectoralState() {
        switch (state) {
            case 0:
                System.out.println("Election taking place");
                break;
            case 1:
                System.out.println("LoOoOoOoOoOoser BoOoOoOoOoh ... Winner is " + win.toString() );
                break;
            case 2:
                System.out.println("We are currently elected");
                break;
            default:
                System.out.println("Unknown state.. Dude, you have to worry about your stupid programm..");
                break;
        }
    }


    /*
    We manage the input message related to our election.

    This method is only used by NetManager thread ( async loop )
     */

    public void manageInput(ClientStruct cliStr) {
        FullDuplexMessageWorker fdmw = cliStr.getFullDuplexMessageWorker();
        // Oh dude here this is an election package !
        ElectionToken electionToken ;
        try{
            electionToken = (ElectionToken) fdmw.getData();
        }catch( IOException ioe) {
            System.out.println("Cannot retreive Election data");
            if( netManager.getState().isServerConnectionEstablished(fdmw) ) {
                // TODO the server went away, RBroadcast it...
                // We remove the failed server from our connections.
                netManager.getState().removeServer(cliStr);
                // In doubt launch an election ... If the removed server is either elected nor separating us from elected server.
                if( ! netManager.getState().getStandAlone() ) {
                    launchElection();
                }
            }
            return;
        }
        SocketAddress r = electionToken.getR();
        switch (electionToken.getType()) {
            case 0:
                electionMutex.lock();
                if( !isInElection) {
                    System.out.println("Someone else triggered an election. Locking...");
                    isInElection = true;
                }
                electionMutex.unlock();
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
                    netManager.getState().broadcastTokenWithoutFather(cliStr,newToken);
                }
                if( caw.toString().compareTo( r.toString() ) == 0 ) {
                    System.out.println("Answer to our current wave received");
                    rec++;
                    if( rec == netManager.getState().getNbConnectedServers() ) {
                        System.out.print("Current Jeton wave completed : ");
                        if( caw.toString().compareTo(p.toString()) == 0) {
                            // Here we actually won, so we have to broadcast it.
                            System.out.println(" We have won... Broadcast it ;-)");
                            ElectionToken winner = new ElectionToken(1);
                            winner.setR(p);
                            netManager.getState().broadcastToken(winner,"Error while broadcasting our victory");
                            // Tasks that must be performed by the winner
                            netManager.launchServerDiscovery();
                            netManager.lauchPseudoDiscovery();
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
                if( lrec < netManager.getState().getNbConnectedServers() +1 ) {
                    // The previous if prevent us from infinit loop with Gagnant broadcast !
                    // GAGNANT RECEIVED
                    System.out.println("We received a GAGNANT");
                    if (lrec == 0 || r.toString().compareTo(p.toString()) != 0) {
                        // Broadcast that we loose
                        System.out.println("We received somebody else GAGNANT");
                        ElectionToken winner = new ElectionToken(1);
                        winner.setR(r);
                        netManager.getState().broadcastToken(winner, "Error while broadcasting somebody else victory");
                    }
                    // Increment the GAGNANT token received
                    lrec++;
                    // And tell who is the winner !
                    win = r;
                    if (lrec == netManager.getState().getNbConnectedServers() + 1) {
                        System.out.println("End of the Election");
                        // End of the election.
                        if (win.toString().compareTo(p.toString()) == 0) {
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
                }
                break;
            default:
                System.out.println("Unknown election token...");
                break;
        }
    }

    /*
    We will need to lock electionMutex from other prt of the code.
     */
    protected void lock() {
        electionMutex.lock();
    }

    protected void unlock() {
        electionMutex.unlock();
    }

    /*
    Convenient methods used in NetManager thread by manageInput
     */

    private void sendElectionToken( ConnectionStruct connectionStruct, ElectionToken electionToken, String ioErrorMessage ) {
        try{
            connectionStruct.getFullDuplexMessageWorker().sendMsg(1,electionToken);
        } catch(IOException ioe) {
            System.out.println(ioErrorMessage);
        }
    }


    /*
    The goal of the election is to send messages to the elected process but we may have no direct way to it.
    One solution is to pass our message to the **father** ClientStruct, that will forward it.
    That is why we have to get it for other parts of the code :
     */
    public ConnectionStruct getFather() {
        return father;
    }

    public SocketAddress getWin() {
        return win;
    }


    /*
    When we establish a connection with another server, that is part of an elected network,
    and that we are alone ( so with no elected process to refer to ), we can can just join
    this election network. We have enough information to perform this task.

    If the task fails, we have to tell our caller so that he can launch an election.
     */

    protected Boolean joinElectoralNetwork( ClientStruct _father, SocketAddress winner ) {
        if( win == null && state == 0) {
            father = _father;
            state = 1;
            win = winner;
            return true;
        } else {
            return false;
        }
    }

}
