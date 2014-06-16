package Chat.Utils;

/**
 * Created by benwa on 6/16/14.
 *
 * License : GLP 2.0
 *
 * Basic interface to specify to LockManager how to begin using a resource, and how to stop using it without having to overwrite our LockManager.
 * We will use the visitor pattern.
 */
public interface ResourceVisitor {
    /**
     * Start using the resource.
     */
    public void startUsingResource();

    /**
     * Stop using the resource
     */
    public void stopUsingResource();
}
