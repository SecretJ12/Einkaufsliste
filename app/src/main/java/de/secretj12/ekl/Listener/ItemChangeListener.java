package de.secretj12.ekl.Listener;

public interface ItemChangeListener {
    void onNameChange(String name);

    void onStateChanged(boolean cancelled);
}
