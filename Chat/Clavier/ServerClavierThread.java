package Chat.Clavier;

import Chat.Server.NetManager;

/**
 * Created by benwa on 6/7/14.
 *
 * License : GLP 2.0
 */

public class ServerClavierThread extends ClavierThread {

    NetManager netManager;

    public ServerClavierThread( NetManager _netManager) {
        super();
        netManager = _netManager;
    }

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
                netManager.lauchPseudoDiscovery();
                break;
            default:
                System.out.println("Command unrecognise...");
                break;
        }
    }

    protected void displayHelp() {
        System.out.println();
        System.out.println("Enter a command to execute an action : ");
        System.out.println("0 : Display clients connected to this server");
        System.out.println("1 : Connect to another server");
        System.out.println("2 : Launch an election");
        System.out.println("3 : Display servers directly connected to this server");
        System.out.println("4 : Re initialize network on this node ( all clients' and servers' connections will be closed )");
        System.out.println("5 : Display electoral state");
        System.out.println("6 : Debug button");
        System.out.println();
    }

}
