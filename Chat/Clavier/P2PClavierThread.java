package Chat.Clavier;


/**
 * Created by benwa on 6/9/14.
 *
 * This class process input for both a server and a client.
 *
 */
public class P2PClavierThread extends ClavierThread {
    /**
     * The NetworkManager we will use to send our servers messages
     */
    private Chat.Server.NetManager serverNetManager;
    /**
     * The NetworkManager we will use to send our clients messages
     */
    private Chat.Client.NetManager clientNetManager;
    /**
     * Our pseudo
     */
    private String pseudo;

    /**
     * Constructor
     *
     * @param _serverNetManager The NetworkManager we will use to send our servers messages
     * @param _clientNetManager The NetworkManager we will use to send our clients messages
     */
    public P2PClavierThread(Chat.Server.NetManager _serverNetManager, Chat.Client.NetManager _clientNetManager) {
        super();
        serverNetManager = _serverNetManager;
        clientNetManager = _clientNetManager;
    }

    /**
     * Switch statement used to process user input
     *
     * @param command integer the user passed to our program
     */

    public void switchStatement( int command) {
        switch (command) {
            case 0:
                String _pseudo;
                // We have to change login
                _pseudo = sc.nextLine();
                // Notify the server !
                setPseudo(_pseudo);
                break;
            case 1:
                System.out.print("Your message : ");
                String message = sc.nextLine();
                // Send
                clientNetManager.sendMsg(message, pseudo);
                break;
            case 2:
                // Disconnection claimed
                clientNetManager.disconnect();
                loopCondition = false;
                serverNetManager.reInitNetwork();
                break;
            case 3:
                displayHelp();
                break;
            case 4:
                clientNetManager.askForUserList();
                break;
            case 5:
                if( ! clientNetManager.getIsSpareSet()) {
                    System.out.print("Ip : ");
                    String ipS = sc.nextLine();
                    System.out.println("Port : ");
                    int __port = sc.nextInt();
                    clientNetManager.establishSpareConnection(ipS, __port, pseudo);
                } else {
                    System.out.println("Spare connection is already active !");
                }
                break;
            case 6:
                // Private message
                System.out.print("Destination : ");
                String dest = sc.nextLine();
                System.out.print("Message : ");
                message = sc.nextLine();
                clientNetManager.sendPrivateMessage(pseudo, dest, message);
                break;
            case 7 :
                // Server list
                clientNetManager.askForServerList();
                break;
            case 8 :
                System.out.print("Enter the IP address of the distant server : ");
                String ipString = sc.nextLine();
                System.out.print("Now enter the port you want to connect to : ");
                int port = sc.nextInt();
                serverNetManager.connectServer(ipString, port);
                break;
            default :
                System.out.println("Come on, try to do something useful ! ");
                break;
        }
    }

    /**
     * Display help message for client
     */

    protected void displayHelp() {
        System.out.println();
        System.out.println("Enter a command to execute an action : ");
        System.out.println("Press 0 to change your login.");
        System.out.println("Press 1 to send a message to all people present on the chat");
        System.out.println("Press 2 if you want to be disconnected from server");
        System.out.println("Press 3 to see this help again");
        System.out.println("Press 4 to see every users on the Chat");
        System.out.println("Press 5 to set a spare connection");
        System.out.println("Press 6 to send a private message");
        System.out.println("Press 7 to obtain server list");
        System.out.println("Press 8 to connect to an other target");
        System.out.println();
    }

    /**
     * Establish our pseudo with the server
     *
     * @param _pseudo Our new pseudo
     */

    private void setPseudo(String _pseudo) {
        clientNetManager.askNewLogin(_pseudo);
        while( !clientNetManager.getHasLoginResponse() ) {
            try{
                Thread.sleep(10);
            } catch( InterruptedException intEx) {
                System.out.println("Interrupted");
            }
        }
        // Here we have a response from the serveur. Check it
        if( clientNetManager.getHasCompletedLogin() ) {
            pseudo = _pseudo;
        } else {
            // Nice try but already used
            System.out.println("Please enter a pseudo : ");
            _pseudo = sc.nextLine();
            // Recursive call. In normal use cases, it should ot explose our stack
            setPseudo(_pseudo);
        }
    }

    /**
     * Basic init stuff we have to perform before launching the input loop
     */

    protected void init() {
        System.out.println("Please enter a pseudo : ");
        String _pseudo = sc.nextLine();
        setPseudo(_pseudo);
        System.out.println("");
    }
}
