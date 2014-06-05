package Chat;

import csc4509.FullDuplexMessageWorker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

/**
 * Created by benwa on 6/5/14.
 */

public class ClientNetMngr {
    // A bad ass Net manager that will do most of the job !
    private FullDuplexMessageWorker full;
    private Boolean hasCompletedLogin = false;
    private Boolean hasLoginResponse = false;
    private InetSocketAddress isa;
    private Boolean loopCondition;

    public Boolean getHasCompletedLogin() {
        return hasCompletedLogin;
    }

    public Boolean getHasLoginResponse() {
        if( hasLoginResponse ) {
            hasLoginResponse = false;
            return true;
        } else {
            return false;
        }
    }

    public ClientNetMngr(String ipAddress, int port) {
        InetAddress add;
        try {
            add = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException uhe) {
            System.out.println("Unknown host specified as server. Terminating");
            return;
        }
        isa = new InetSocketAddress(add, port);
        SocketChannel socketChannel;
        try {
            socketChannel = SocketChannel.open(isa);
        } catch( IOException ioe ) {
            System.out.println("Oh man, we cannot connect !");
            return;
        }
        // Here we are connected.
        full = new FullDuplexMessageWorker(socketChannel);
    }

    public void launch() {
        System.out.println("Listening");
        // And now listen to new messages !
        int loop = 0;
        loopCondition = true;
        while(loopCondition) {
            loop++;
            System.out.println("Loop NB : " + loop);
            full.readMessage();
            ChatData chdata;
            if( full == null ) {
                break;
            }
            try {
                chdata = (ChatData) full.getData();
            } catch( IOException ioe) {
                System.out.println("Can not read received datas");
                return;
            }
            if( chdata == null ) {
                break;
            }
            // Now see what the serveur told us :
            switch (chdata.getType()) {
                case 0:
                    System.out.println("Why did the server send us a login request ?");
                    break;
                case 1:
                    System.out.println("Demand accepted by server for login " + chdata.getPseudo() );
                    // Our login request was accepted
                    hasCompletedLogin = true;
                    hasLoginResponse = true;
                    // This value will be checked by the clavier thread. No need to do more !
                    break;
                case 2:
                    System.out.println( chdata.getDate() + " " + chdata.getPseudo() + " : " + chdata.getMessage() );
                    break;
                case 3:
                    System.out.println( chdata.getDate() + " " + chdata.getPseudo() + " joined the chat...");
                    break;
                case 4:
                    System.out.println( chdata.getDate() + " " + chdata.getPseudo() + " leaved the chat...");
                    break;
                case 5:
                    System.out.println("Why did the server send us a disconnection request ?");
                    break;
                case 6:
                    // We did something wrong and here came the error
                    if( chdata.hasError()) {
                        if (chdata.getErrorCode() == 1) {
                            System.out.println("The login is already used on this server");
                            hasLoginResponse = true;
                            // We didn't set hasCompletedLogin so the other thread will know that the login is already used
                        }
                        // Display the error and continue what we were doing !
                        chdata.printErrorCode();
                    }
                    break;
                default:
                    System.out.println("Unhandled number for message type.");
                    break;
            }
        }
    }

    public void askNewLogin(String newLogin) {
        ChatData chdata = new ChatData(0,0,"",newLogin);
        try {
            full.sendMsg(0, chdata);
        } catch( IOException ioe ) {
            System.out.println("Oh god, we failed sending the pseudo request !");
            return;
        }
    }

    public void sendMsg( String msg, String pseudo) {
        ChatData chatData = new ChatData(0,2,msg,pseudo);
        try {
            full.sendMsg(0, chatData);
        } catch( IOException ioe ) {
            System.out.println("Oh god, we failed sending the pseudo request !");
            return;
        }
    }

    public void disconnect( ) {
        ChatData chatData = new ChatData(0,5,"");
        try {
            full.sendMsg(0, chatData);
            loopCondition = false;
        } catch( IOException ioe ) {
            System.out.println("Oh god, we failed sending the pseudo request !");
            return;
        }
    }

}
