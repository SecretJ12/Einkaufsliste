package de.secretj12.ekl.Database;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.secretj12.ekl.Listener.ItemChangeListener;

public class Item extends ListItem {
    private final DocumentReference itemRef;
    private String name;
    private boolean cancelled;
    private long weight;

    private final Map<Integer, ItemChangeListener> itemChangeListeners;
    private int itemChangeListenerIds = 0;

    Item(DocumentReference itemRef, String name, boolean cancelled, long weight) {
        this.itemRef = itemRef;
        this.name = name;
        this.cancelled = cancelled;
        this.weight = weight;

        itemChangeListeners = new HashMap<>();
    }

    @Override
    public int setItemListener(ItemChangeListener listener) {
        itemChangeListeners.put(itemChangeListenerIds, listener);
        listener.onNameChange(name);
        listener.onStateChanged(cancelled);
        return itemChangeListenerIds++;
    }

    @Override
    public void resetItemListener(int id) {
        itemChangeListeners.remove(id);
    }

    void checkUpdate(DocumentSnapshot snap) {
        String name = snap.getString("name");
        if (!name.equals(this.name)) {
            this.name = snap.getString("name");
            itemChangeListeners.entrySet().forEach(entry -> entry.getValue().onNameChange(name));
        }

        boolean cancelled = snap.getBoolean("cancelled");
        if (cancelled != this.cancelled) {
            this.cancelled = cancelled;
            itemChangeListeners.entrySet().forEach(entry -> entry.getValue().onStateChanged(cancelled));
        }
        this.cancelled = cancelled;

        this.weight = snap.getLong("weight");
    }

    long getWeight() {
        return weight;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof Item && Objects.equals(itemRef, ((Item) obj).itemRef);
    }

    @Override
    public void delete() {
        itemRef.delete();
    }

    @Override
    public void setCancel(boolean cancelled) {
        itemRef.update("cancelled", cancelled);
    }

    void setWeight(long weight) {
        itemRef.update("weight", weight);
    }

    String getName() {
        return name;
    }

    @Override
    public boolean isEnableAble() {
        return cancelled;
    }

    @Override
    public boolean isCancelAble() {
        return !cancelled;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setName(String name) {
        itemRef.update("name", name);
    }

    @Override
    public String toString() {
        return "Item(" + name + "){" + itemRef.getId() + "}";
    }
}
