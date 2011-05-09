package edu.washington.cs.oneswarm.ui.gwt.rpc;

import com.google.gwt.user.client.rpc.IsSerializable;

public class OneSwarmException extends Exception implements IsSerializable {

    private static final long serialVersionUID = -1936628445420196185L;
    private String message;

    public OneSwarmException() {

    }

    public OneSwarmException(String message) {
        this.message = message;
    }

    public OneSwarmException(Throwable e) {
        this.message = e.getMessage();
    }

    public OneSwarmException(Throwable t, boolean getOriginalCause) {
        if (getOriginalCause) {
            while (t.getCause() != null) {
                t = t.getCause();
            }
        }
        this.message = t.getMessage();

    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
