package Chat.P2P;

import Chat.Clavier.P2PClavierThread;

import java.util.Scanner;

/**
 * Created by benwa on 6/9/14.
 *
 * License : GLP 2.0
 */

public class Main {
    public static void main( String[] argv ) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Choose your port : ");
        int port = scanner.nextInt();
        Chat.Server.NetManager serverNetMngr = new Chat.Server.NetManager( port );
        //TODO here launch server in the thread
        ServerThread serverThread = new ServerThread(serverNetMngr);
        serverThread.start();
        Chat.Client.NetManager clientNetMngr = new Chat.Client.NetManager("127.0.0.1", port);
        // Here we launch the clavier thread
        P2PClavierThread p2pClavierThread = new P2PClavierThread(serverNetMngr, clientNetMngr);
        p2pClavierThread.start();
        // And know we start listening
        clientNetMngr.launch();
    }
}
