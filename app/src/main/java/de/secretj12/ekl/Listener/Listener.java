package de.secretj12.ekl.Listener;

import com.google.firebase.firestore.ListenerRegistration;

public abstract class Listener {
    protected ListenerRegistration listReg;
    protected boolean deleted;
    protected boolean connected;

    Listener() {
        deleted = false;
        connected = false;
    }

    public void delete() {
        disconnect();
        deleted = true;
    }

    boolean isDeleted() {
        return deleted;
    }

    public abstract void connect();

    public abstract void disconnect();
}
