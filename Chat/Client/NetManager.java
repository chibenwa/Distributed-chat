package Chat.Client;

import Chat.Netmessage.ChatData;
import csc4509.FullDuplexMessageWorker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

/**
 * Created by benwa on 6/7/14.
 */
public class NetManager {
        // A bad ass Net manager that will do most of the job !
        private FullDuplexMessageWorker full;
        private Boolean hasCompletedLogin = false;
        private Boolean hasLoginResponse = false;
        private InetSocketAddress isa;
        private Boolean loopCondition;
        private Boolean waitingUserList = false;

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

        public NetManager(String ipAddress, int port) {
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
                //    System.out.println("Loop NB : " + loop);
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
                    case 7:
                        System.out.println("Why did the server send us a user list request ?");
                        break;
                    case 8:
                        if( waitingUserList ) {
                            System.out.println( chdata.getDate() + " Peoples on the chat : " + chdata.getMessage() );
                            waitingUserList = false;
                        } else {
                            // Inform the server he made a mistake : we never asked for what he provided us...
                            ChatData informServer = new ChatData(0,6,"");
                            informServer.setErrorCode(9);
                            sendMessage(informServer, "Problem telling the server we didn't need the user list");
                        }
                        break;
                    default:
                        System.out.println("Unhandled number for message type : "+chdata.getType());
                        break;
                }
            }
        }

        public void askNewLogin(String newLogin) {
            sendMessage( new ChatData(0,0,"",newLogin), "Oh god, we failed sending the pseudo request !" );
        }

        public void sendMsg( String msg, String pseudo) {
            sendMessage( new ChatData(0,2,msg,pseudo), "Oh god, we failed sending the pseudo request !" );
        }

        public void disconnect( ) {
            sendMessage( new ChatData(0,5,""), "Oh god, we failed sending the pseudo request !" );
        }

        public void askForUserList() {
            waitingUserList = true;
            System.out.println("sending user list request");
            sendMessage(new ChatData(0,7,""), "Oh god, we failed sending the user list request !");
        }

        private void sendMessage(ChatData chatData, String ioErrorMessage) {
            try {
                full.sendMsg(0, chatData);
            } catch( IOException ioe ) {
                System.out.println(ioErrorMessage);
                return;
            }
        }
}
