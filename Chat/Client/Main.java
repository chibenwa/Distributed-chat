package Chat.Client;


import Chat.Clavier.ClientClavierThread;

/**
 * Created by benwa on 6/7/14.
 */
public class Main {
    public static void main( String[] argv ) {
        // Use ClientMain <ip address> <port>
        if( argv.length != 2) {
            System.out.println("Use : ClientMain <ip addresse> <port>");
        }
        String ip = argv[0];
        int port = Integer.parseInt(argv[1]);
        NetManager clientNetMngr = new NetManager(ip, port);
        // Here we launch the clavier thread
        ClientClavierThread clientClavierThread = new ClientClavierThread(clientNetMngr);
        clientClavierThread.start();
        // And know we start listening
        clientNetMngr.launch();
    }
}
