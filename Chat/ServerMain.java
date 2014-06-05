package Chat;

/**
 * Created by benwa on 6/5/14.
 */
public class ServerMain {

    public static void main(String[] argv) {
        if (argv.length != 1) {
            System.out.println("Use : asyncServer <port> <string to transmit>");
            return;
        }
        int port = Integer.parseInt(argv[0]);
        ChatServer chatServer = new ChatServer(port);
        chatServer.launch();
    }

}
