package edu.washington.cs.oneswarm.ui.gwt.rpc;

public class SharedServiceDTO extends ServiceDTO {
    private static final long serialVersionUID = 1L;
    public String address;

    public SharedServiceDTO(String name, String searchKey, String address, int port) {
        super(name, searchKey, port);
        this.address = address;
    }

    public SharedServiceDTO() {
        super();
    }

    public SharedServiceDTO(boolean dummy) {
        super(dummy);
        this.address = "";
    }

    @Override
    public void validate() {
        super.validate();
        validateString(address, 1, "invalid address");
    }

    public String toString() {
        return super.toString() + " " + address;
    }
}