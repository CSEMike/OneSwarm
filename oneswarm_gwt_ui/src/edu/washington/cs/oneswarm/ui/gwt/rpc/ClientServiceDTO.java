package edu.washington.cs.oneswarm.ui.gwt.rpc;

public class ClientServiceDTO extends ServiceDTO {

    private static final long serialVersionUID = 1L;

    public ClientServiceDTO() {
        super();
    }

    public ClientServiceDTO(String name, String searchKey, int localPort) {
        super(name, searchKey, localPort);
    }

    public ClientServiceDTO(boolean dummy) {
        super(dummy);
    }

}