package Chat.Clavier;

import Chat.Client.NetManager;

/**
 * Created by benwa on 6/7/14.
 *
 * License : GLP 2.0
 */
public class ClientClavierThread extends ClavierThread {

    NetManager netManager;
    String pseudo;

    public ClientClavierThread( NetManager _netManager) {
        super();
        netManager = _netManager;
    }

    protected void displayHelp() {
        System.out.println("This is the help message for this application");
        System.out.println("Here are the commands :");
        System.out.println("Press 0 to change your login.");
        System.out.println("Press 1 to send a message to all people present on the chat");
        System.out.println("Press 2 if you want to be disconnected from server");
        System.out.println("Press 3 to see this help again");
        System.out.println("Press 4 to see every users on the Chat");
        System.out.println("Et oui je suis une sorte de répondeur!");
        System.out.println("");
    }

    protected void switchStatement(int command) {
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
                netManager.sendMsg(message, pseudo);
                break;
            case 2:
                // Disconnection claimed
                netManager.disconnect();
                loopCondition = false;
                break;
            case 3:
                displayHelp();
                break;
            case 4:
                netManager.askForUserList();
                break;
            default :
                System.out.println("Come on, try to do something usefull ! ");
                break;
        }
    }

    private void setPseudo(String _pseudo) {
        netManager.askNewLogin(_pseudo);
        while( !netManager.getHasLoginResponse() ) {
            try{
                Thread.sleep(10);
            } catch( InterruptedException intEx) {
                System.out.println("Interrupted");
            }
        }
        // Here we have a response from the serveur. Check it
        if( netManager.getHasCompletedLogin() ) {
            pseudo = _pseudo;
        } else {
            // Nice try but already used
            System.out.println("Please enter a pseudo : ");
            _pseudo = sc.nextLine();
            // Recursive call. In normal use cases, it should ot explose our stack
            setPseudo(_pseudo);
        }
    }

    protected void init() {
        System.out.println("Please enter a pseudo : ");
        String _pseudo = sc.nextLine();
        setPseudo(_pseudo);
        System.out.println("");
    }

}
