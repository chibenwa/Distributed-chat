package Chat;

import java.util.Scanner;

/**
 * Created by benwa on 6/5/14.
 */
public class ClientThread extends Thread{
    private ClientNetMngr clientNetMngr;
    private String pseudo;
    private Scanner sc;
    /*
        In this thread we will manage user input
     */

    public ClientThread( ClientNetMngr _clientNetMngr) {
        clientNetMngr = _clientNetMngr;
        sc = new Scanner(System.in);
    }

    public void run() {
        /* First ask for pseudo */
        System.out.println("Please enter a pseudo : ");
        String _pseudo = sc.nextLine();
        setPseudo(_pseudo);
        System.out.println("");
        displayHelp();
        Boolean loopCondition = true;
        while(loopCondition) {
            System.out.print("Enter a command : ");
            int command = sc.nextInt();
            sc.nextLine();
            switch (command) {
                case 0:
                    // We have to change login
                    _pseudo = sc.nextLine();
                    // Notify the server !
                    setPseudo(_pseudo);
                    break;
                case 1:
                    System.out.print("Your message : ");
                    String message = sc.nextLine();
                    // Send
                    clientNetMngr.sendMsg(message, pseudo);
                    break;
                case 2:
                    // Disconnection claimed
                    clientNetMngr.disconnect();
                    loopCondition = false;
                    break;
                case 3:
                    displayHelp();
                    break;
                case 4:
                    clientNetMngr.askForUserList();
                    break;
                default :
                    System.out.println("Come on, try to do something usefull ! ");
                    break;
            }
        }
    }

    private void displayHelp() {
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

    private void setPseudo(String _pseudo) {
        clientNetMngr.askNewLogin(_pseudo);
        while( !clientNetMngr.getHasLoginResponse() ) {
            try{
                Thread.sleep(10);
            } catch( InterruptedException intEx) {
                System.out.println("Interrupted");
            }
        }
        // Here we have a response from the serveur. Check it
        if( clientNetMngr.getHasCompletedLogin() ) {
            pseudo = _pseudo;
        } else {
            // Nice try but already used
            System.out.println("Please enter a pseudo : ");
            _pseudo = sc.nextLine();
            // Recursive call. In normal use cases, it should ot explose our stack
            setPseudo(_pseudo);
        }
    }
}
