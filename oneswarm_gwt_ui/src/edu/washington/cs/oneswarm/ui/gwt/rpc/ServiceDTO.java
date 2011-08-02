package edu.washington.cs.oneswarm.ui.gwt.rpc;

import com.google.gwt.user.client.rpc.IsSerializable;

public abstract class ServiceDTO implements IsSerializable {

    private boolean dummy = false;
    private String name;
    private int port;
    private String searchKey;

    public ServiceDTO() {
    }

    public ServiceDTO(boolean dummy) {
        this.dummy = dummy;
        this.name = "";
        this.searchKey = "";
    }

    public ServiceDTO(String name, String searchKey, int port) {
        this.name = name;
        this.searchKey = searchKey;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public int getPort() {
        return port;
    }

    public String getSearchKey() {
        return searchKey;
    }

    public boolean isDummy() {
        return dummy;
    }

    public void setDummy(boolean dummy) {
        this.dummy = dummy;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPort(String value) {
        int port = 0;
        try {
            port = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(value + " is not a valid port number.");
        }
        validatePort(port);
        this.port = port;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    public String toString() {
        return name + " " + searchKey + " " + port;
    }

    public void validate() {
        validateString(name, 1, "invalid name");
        validateString(searchKey, 1, "invalid search key");
        validatePort(this.port);
    }

    private void validatePort(int port) {
        if (port <= 0 || port > 64 * 1024 - 1) {
            throw new NumberFormatException("only ports between 1 and 65535 are allowed.");
        }
    }

    protected void validateString(String string, int minLength, String errorMessage) {
        if (string == null || string.length() < minLength) {
            throw new RuntimeException(errorMessage);
        }
    }

}