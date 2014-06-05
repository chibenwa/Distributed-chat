package Chat;

/**
 * Created by benwa on 6/5/14.
 */
public class ClientMain {

    public static void main( String[] argv ) {
        // Use ClientMain <ip address> <port>
        if( argv.length != 2) {
            System.out.println("Use : ClientMain <ip addresse> <port>");
        }
        String ip = argv[0];
        int port = Integer.parseInt(argv[1]);
        ClientNetMngr clientNetMngr = new ClientNetMngr(ip, port);
        // Here we launch the clavier thread
        ClientThread clientThread = new ClientThread(clientNetMngr);
        clientThread.start();
        // And know we start listening
        clientNetMngr.launch();
    }

}
