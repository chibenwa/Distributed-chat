package Chat.Server;


import Chat.Clavier.ServerClavierThread;

/**
 * Created by benwa on 6/7/14.
 */
public class Main {
    public static void main(String[] argv) {
        if (argv.length != 1) {
            System.out.println("Use : asyncServer <port> <string to transmit>");
            return;
        }
        int port = Integer.parseInt(argv[0]);
        NetManager netManager = new NetManager(port);
        ServerClavierThread serverClavierThread = new ServerClavierThread(netManager);
        serverClavierThread.start();
        netManager.launch();
    }
}
