package Chat.Netmessage;

import java.io.Serializable;

/**
 * Created by benwa on 6/8/14.
 *
 * A structure used inside InterServerMessage to carry client message across our network of servers
 *
 */

public class ChatMessage implements Serializable {
    public String message;
    public String pseudo;
    public String dest;
}
