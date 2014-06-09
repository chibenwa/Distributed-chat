package Chat.Server;


import Chat.Clavier.ServerClavierThread;

/**
 * Created by benwa on 6/7/14.
 *
 * License : GLP 2.0
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

/*
    I put here my roadMap :

    // TODO 4 : Echo to retrieve trivially client list
    // TODO 5 : Echo to retrieve trivially server list
    // TODO 7 : Causal diffusion for chat messages
    // TODO 8 : P2P chat from combining both server and client

 */
