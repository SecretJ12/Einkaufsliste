package de.secretj12.ekl.Listener;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListenerManager {
    private static ListenerManager instance;

    private final Map<Integer, Map<Integer, List<Listener>>> registered;

    public static ListenerManager getInstance() {
        if (instance == null)
            instance = new ListenerManager();
        return instance;
    }

    private ListenerManager() {
        registered = new HashMap<>();
    }

    public Listener registerListener(int listID, int fragmentID, Receiver<DocumentSnapshot> receiver, DocumentReference docRef) {
        return registerListener(listID, fragmentID, new DocumentListener(receiver, docRef));
    }

    public Listener registerListener(int listID, int fragmentID, Receiver<QuerySnapshot> receiver, CollectionReference colRef) {
        return registerListener(listID, fragmentID, new CollectionListener(receiver, colRef));
    }

    public Listener registerListener(int listID, int fragmentID, Listener listener) {
        if (!registered.containsKey(listID))
            registered.put(listID, new HashMap<>());
        if (!registered.get(listID).containsKey(fragmentID))
            registered.get(listID).put(fragmentID, new ArrayList<>());

        List<Listener> listeners = registered.get(listID).get(fragmentID);
        listeners.add(listener);
        registered.get(listID).put(fragmentID, listeners);
        listener.connect();
        return listener;
    }

    public void connect(int listID, int fragmentID) {
        if (!registered.containsKey(listID) || !registered.get(listID).containsKey(fragmentID))
            return;

        List<Listener> listeners = registered.get(listID).get(fragmentID);
        for (int i = 0; i < listeners.size(); ) {
            Listener listener = listeners.get(i);
            if (listener.isDeleted())
                listeners.remove(i);
            else {
                listener.connect();
                i++;
            }
        }
        registered.get(listID).put(fragmentID, listeners);
    }

    public void disconnect(int listID, int fragmentID) {
        if (!registered.containsKey(listID) || !registered.get(listID).containsKey(fragmentID))
            return;

        List<Listener> listeners = registered.get(listID).get(fragmentID);
        for (int i = 0; i < listeners.size(); ) {
            Listener listener = listeners.get(i);
            if (listener.isDeleted())
                listeners.remove(i);
            else {
                listener.disconnect();
                i++;
            }
        }
        registered.get(listID).put(fragmentID, listeners);
    }

    public void delete(int listID) {
        registered.remove(listID);
    }

    public void delete(int listID, int fragmentID) {
        registered.get(listID).remove(fragmentID);
    }
}
