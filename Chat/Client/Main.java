package Chat.Client;


import Chat.Clavier.ClientClavierThread;

import java.util.Scanner;

/**
 * Created by benwa on 6/7/14.
 *
 * License : GLP 2.0
 */
public class Main {
    public static void main( String[] argv ) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Choose your server hostname : ");
        String ip = scanner.nextLine();
        System.out.print("Choose your port : ");
        int port = scanner.nextInt();
        NetManager clientNetMngr = new NetManager(ip, port);
        // Here we launch the clavier thread
        ClientClavierThread clientClavierThread = new ClientClavierThread(clientNetMngr);
        clientClavierThread.start();
        // And know we start listening
        clientNetMngr.launch();
    }
}
