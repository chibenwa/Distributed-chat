package Chat.Clavier;

import Chat.Server.NetManager;

/**
 * Created by benwa on 6/7/14.
 *
 * License : GLP 2.0
 *
 *  Base class for all input clavier for server
 */

public class ServerKeyboardThread extends KeyboardThread {

    /**
     * The NetworkManager we will use to send our messages
     */

    NetManager netManager;

    /**
     * Constructor
     *
     * @param _netManager The NetworkManager we will use to send our messages
     */

    public ServerKeyboardThread(NetManager _netManager) {
        super();
        netManager = _netManager;
    }

    /**
     * Switch statement used to process user input
     *
     * @param command integer the user passed to our program
     */

    public void switchStatement( int command) {
        switch( command ) {
            case 0:
                // Print client list
                System.out.println( "People connected to this server : " + netManager.buildClientList() );
                break;
            case 1:
                // Connect to an other server
                System.out.print("Enter the IP address of the distant server : ");
                String ipString = sc.nextLine();
                System.out.print("Now enter the port you want to connect to : ");
                int port = sc.nextInt();
                netManager.connectServer(ipString, port);
                break;
            case 2:
                // Launch an election
                System.out.println("Launching election");
                netManager.launchElection();
                break;
            case 3:
                // Get connected server list
                System.out.println("Connected servers : " + netManager.getServerList() );
                break;
            case 4:
                // Re initialize network dude
                netManager.reInitNetwork();
                break;
            case 5:
                // Display Electoral status
                netManager.displayElectoralState();
                break;
            case 6:
                //netManager.launchServerDiscovery();
                netManager.launchPseudoDiscovery();
                break;
            case 7:
                netManager.reInitVectorialClock();
            //    netManager.displayOurVectorialClock();
                break;
            case 8:
                netManager.displayCMessageBag();
                break;
            case 9:
                netManager.displayLockState();
                break;
            case 10:
                netManager.startUsingResource();
                break;
            case 11:
                netManager.stopIsingResource();
                break;
            case 12:
                netManager.shutdownOurInfrastructure();
                break;
            case 13:
                netManager.safeShutDown();
                break;
            default:
                System.out.println("Command unrecognise...");
                break;
        }
    }

    /**
     * Display help message for server
     */

    protected void displayHelp() {
        System.out.println();
        System.out.println("Enter a command to execute an action : ");
        System.out.println("0 : Display clients connected to this server : Debug : not thread safe");
        System.out.println("1 : Connect to another server");
        System.out.println("2 : Launch an election : Debug : not thread safe");
        System.out.println("3 : Display servers directly connected to this server : Debug : not thread safe");
        System.out.println("4 : Re initialize network on this node ( all clients' and servers' connections will be closed ) : Debug : not thread safe");
        System.out.println("5 : Display electoral state : Debug : not thread safe");
        System.out.println("6 : Debug button : Debug : not thread safe");
        System.out.println("7 : Re-init vectorial clock : Debug : not thread safe");
        System.out.println("8 : Display C message bag : Debug : not thread safe");
        System.out.println("9 : Display lock state");
        System.out.println("10 : Lock");
        System.out.println("11 : Unlock");
        System.out.println("12 : Shutdown our infrastructure ( violent ) and Debug : not thread safe");
        System.out.println("13 : Shutdown our infrastructure ( soft way... ) ");
        System.out.println();
    }

}
