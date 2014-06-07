package Chat.Clavier;

import java.util.Scanner;

/**
 * Created by benwa on 6/7/14.
 */
public class ClavierThread extends Thread{

    protected Scanner sc;
    protected Boolean loopCondition = true;

    public ClavierThread( ) {
        sc = new Scanner( System.in );
    }

    public void run() {
        init();
        displayHelp();
        while(loopCondition) {
            System.out.println("Waiting for a command : ");
            int command = sc.nextInt();
            // Empty input
            sc.nextLine();
            switchStatement( command );
        }
    }

    // Will be overide
    protected void displayHelp() {

    }

    // Will be overide
    protected void switchStatement(int command) {

    }

    protected void init() {

    }


}
