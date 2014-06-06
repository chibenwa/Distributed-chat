package Chat;

import csc4509.FullDuplexMessageWorker;

/**
 * Created by benwa on 6/5/14.
 */
public class ClientStruct extends ConnectionStruct {
    private String pseudo;
    private Boolean pseudoSet = false;
    public Boolean hasPseudo() {
        return pseudoSet;
    }
    public void setPseudo( String s) {
        pseudo = s;
        pseudoSet = true;
    }
    public String getPseudo() {
        return pseudo;
    }
    public ClientStruct( FullDuplexMessageWorker _full ) {
        super( _full );
    }
}
