package Chat.Server;


import Chat.Clavier.ServerKeyboardThread;

/**
 * Created by benwa on 6/7/14.
 *
 * License : GLP 2.0
 *
 * Main class for server.
 */
public class Main {
    public static void main(String[] argv) {
        if (argv.length != 1) {
            System.out.println("Use : asyncServer <port> <string to transmit>");
            return;
        }
        int port = Integer.parseInt(argv[0]);
        NetManager netManager = new NetManager(port);
        ServerKeyboardThread serverClavierThread = new ServerKeyboardThread(netManager);
        serverClavierThread.start();
        netManager.launch();
    }
}

/*
    I put here my roadMap :

    // TODO 7 : Causal diffusion for chat messages
    // TODO : Release memory in EchoManager
    // TODO : Secure type casting of thing received from the network...)

 */
