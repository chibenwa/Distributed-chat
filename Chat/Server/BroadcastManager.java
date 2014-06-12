package Chat.Server;

import Chat.Netmessage.InterServerMessage;

import java.net.SocketAddress;

/**
 * Created by benwa on 6/12/14.
 *
 * License : GLP 2.0
 *
 * Common interface for broadcast managers
 */
public interface BroadcastManager {

    /**
     * Launches a Broadcast for the given message
     *
     * @param message the message to Broadcast
     */

    public void launchBroadcast(InterServerMessage message);

    /**
     * Process input message. Return true if one ( or more ) message was accepted after receiving this message.
     *
     * @param message The message we just received.
     * @return Return true if one ( or more ) message was accepted after receiving this message. False in other cases.
     */

    public Boolean manageInput( InterServerMessage message);

    /**
     * Called when server is launched. It set the server identifier we will use with our waves.
     *
     * @param _ourAddress Server identifier
     */

    public void setOurAddress(SocketAddress _ourAddress);
}
