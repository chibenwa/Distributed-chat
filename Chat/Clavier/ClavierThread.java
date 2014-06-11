package Chat.Clavier;

import java.util.Scanner;

/**
 * Created by benwa on 6/7/14.
 *
 * License : GLP 2.0
 *
 * Base class for all input clavier
 */

public abstract class ClavierThread extends Thread{
    /**
     * Scanner we will use to process user input
     */
    protected Scanner sc;
    /**
     * Loop condition to exit from the loop.
     */
    protected Boolean loopCondition = true;

    /**
     * Instanciate a clavier thread
     */

    public ClavierThread( ) {
        sc = new Scanner( System.in );
    }

    /**
     * Method called to start thread.
     */

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

    /**
     * Display help message. It will be overwritten
     */

    protected void displayHelp() {

    }

    /**
     * Switch statement used to process user input
     *
     * @param command integer the user passed to our program
     */

    protected void switchStatement(int command) {

    }

    /**
     *Basic init stuff we have to perform before launching the input loop. It will be overwritten
     */

    protected void init() {

    }


}
