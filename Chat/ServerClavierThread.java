package Chat;

import java.util.Scanner;

/**
 * Created by benwa on 6/6/14.
 */
public class ServerClavierThread extends Thread{
    private ChatServer chatServer;

    private Scanner sc;

    public ServerClavierThread( ChatServer _chatServer) {
        chatServer = _chatServer;
        sc = new Scanner( System.in );
    }

    public void run() {
        displayHelp();
        while(true) {
            System.out.println("Waiting for a command : ");
            int command = sc.nextInt();
            // Empty input
            sc.nextLine();
            switch( command ) {
                case 0:
                    // Print client list
                    System.out.println( "People connected to this server : " + chatServer.buildClientList() );
                    break;
                case 1:
                    // Connect to an other server
                    System.out.println("Enter the IP address of the distant server : ");
                    String ipString = sc.nextLine();
                    System.out.println();
                    int port = sc.nextInt();
                    chatServer.connectServer(ipString, port);
                    break;
                case 2:
                    // Launch an election
                    System.out.println("Launching election");
                    chatServer.launchElection();
                    break;
                case 3:
                    // Get conncted server list
                    System.out.println("Connected servers : " + chatServer.getServerList() );
                    break;
                default:
                    System.out.println("Command unrecognise...");
                    break;
            }
        }
    }

    private void displayHelp() {
        System.out.println();
        System.out.println("Enter a command to execute an action : ");
        System.out.println("0 : Display clients connected to this server");
        System.out.println("1 : Connect to another server");
        System.out.println("2 : Launch an election");
        System.out.println("3 : Display servers connected to this server");
        System.out.println();
    }

}
