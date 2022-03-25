package de.secretj12.ekl.Database;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import de.secretj12.ekl.Listener.ListChangeListener;
import de.secretj12.ekl.Listener.ListSettingsListener;
import de.secretj12.ekl.Listener.ListenerManager;
import de.secretj12.ekl.Listener.Receiver;

public class EKList {
    private final ListenerManager listenerManager;
    private final int listenerListID;
    private final DocumentReference listRef;
    private boolean isSettingsListenerSet;

    private String name;
    private String token;
    private boolean keepGroups = true;

    private List<Group> groups;
    private final HashMap<Group, List<Item>> itemsToGroup;
    private List<ListItem> fullList;

    EKList(DocumentReference ref) {
        listenerManager = ListenerManager.getInstance();
        listenerListID = (int) (Math.random() * 100000);
        listRef = ref;
        isSettingsListenerSet = false;
        groups = new ArrayList<>();
        fullList = new ArrayList<>();

        listSettingsListeners = new ArrayList<>();
        listChangeListeners = new ArrayList<>();
        itemsToGroup = new HashMap<>();
    }

    public String getID() {
        return listRef.getId();
    }

    private void updateListSettings(DocumentSnapshot snap) {
        String newName = snap.getString("name");
        if (!newName.equals(name)) {
            name = newName;
            listSettingsListeners.forEach(listener -> listener.onNameUpdate(name));
        }

        String newToken = snap.getString("token");
        if (!Objects.equals(token, newToken)) {
            token = newToken;
            listSettingsListeners.forEach(listener -> listener.onTokenUpdate(token));
        }

        boolean newKeepGroups = snap.getBoolean("keepGroups");
        if (keepGroups != newKeepGroups) {
            keepGroups = newKeepGroups;
            listSettingsListeners.forEach(listener -> listener.onKeepGroups(keepGroups));
        }
    }

    private void updateListOrder(QuerySnapshot snap) {
        List<Group> newGroups = snap.getDocuments()
                .stream().map(gSnap -> new Group(listRef.collection("groups").document(gSnap.getId()),
                        listenerListID,
                        gSnap.getString("name"),
                        gSnap.getLong("weight")))
                .collect(Collectors.toList());


        List<Group> newList = new ArrayList<>();
        newGroups.forEach(group -> {
            if (groups.contains(group)) {
                newList.add(groups.get(groups.indexOf(group)));
            } else {
                newList.add(group);
                listenToGroup(group);
            }
        });
        groups = newList;
        buildList();
    }

    private void listenToGroup(Group group) {
        group.setListChangeListener(items -> {
            itemsToGroup.put(group, items);
            buildList();
        }, listenerListID);
    }

    private void buildList() {
        fullList.clear();
        groups.forEach(group -> {
            fullList.add(group);
            if (itemsToGroup.containsKey(group))
                fullList.addAll(itemsToGroup.get(group));
        });
        listChangeListeners.forEach(listener -> listener.onReorder(fullList));
    }

    private int countItemsOf(Group group) {
        int countItems = 0;
        int index = fullList.indexOf(group);
        while (index + countItems + 1 < fullList.size() && fullList.get(index + countItems + 1) instanceof Item)
            countItems++;
        return countItems;
    }

    private final List<ListSettingsListener> listSettingsListeners;
    private boolean settingsListenerSet = false;

    public synchronized void setListSettingsListener(ListSettingsListener listener, int listenerFragmentID) {
        if (!settingsListenerSet) {
            settingsListenerSet = true;
            listenerManager.registerListener(listenerListID, listenerFragmentID, new Receiver<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot data) {
                    updateListSettings(data);
                    isSettingsListenerSet = true;
                }
            }, listRef);
        }

        listSettingsListeners.add(listener);
        if (isSettingsListenerSet) {
            listener.onNameUpdate(name);
            listener.onTokenUpdate(token);
            listener.onKeepGroups(keepGroups);
        }
    }

    private final List<ListChangeListener<ListItem>> listChangeListeners;
    private boolean changeListenerSet = false;

    public void setListChangeListener(ListChangeListener<ListItem> listener, int listenerFragmentID) {
        if (!changeListenerSet) {
            changeListenerSet = true;

            listenerManager.registerListener(listenerListID, listenerFragmentID, new Receiver<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot data) {
                    updateListOrder(data);
                    for (int i = 0; i < groups.size(); i++) {
                        groups.get(i).checkUpdate(data.getDocuments().get(i));
                    }
                }
            }, listRef.collection("groups"));
        }

        listChangeListeners.add(listener);
        listener.onReorder(fullList);
    }

    public void setGroupsChangeListener(ListChangeListener<Group> listener, int listenerFragmentID) {
        setListChangeListener(items -> listener.onReorder(groups), listenerFragmentID);
    }

    public void createToken(Receiver<String> receiver) {
        StringBuilder tokenBuilder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            tokenBuilder.append((char) (((int) (Math.random() * 25)) + 'A'));
        }
        setToken(new Receiver() {
            @Override
            public void onSuccess(Object data) {
                receiver.onSuccess(token);
            }

            @Override
            public void onFailure() {
                receiver.onFailure();
            }
        }, listRef.getId() + "-" + tokenBuilder.toString());
    }

    public void resetToken(Receiver receiver) {
        setToken(receiver, null);
    }

    private void setToken(Receiver receiver, final String token) {
        listRef.update("token", token)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            EKList.this.token = token;
                            receiver.onSuccess(null);
                        } else
                            receiver.onFailure();
                    }
                });
    }

    public void setKeepGroups(boolean keep) {
        keepGroups = keep;
        listRef.update("keepGroups", keep);
    }

    public void connect(int fragmentID) {
        listenerManager.connect(listenerListID, fragmentID);
    }

    public void disconnect(int fragmentID) {
        listenerManager.disconnect(listenerListID, fragmentID);
    }

    public void delete(int fragmentID) {
        listenerManager.delete(listenerListID, fragmentID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EKList ekList = (EKList) o;
        return Objects.equals(listRef, ekList.listRef);
    }

    public void addGroup(String name, Receiver receiver) {
        if (!changeListenerSet) {
            listRef.collection("groups")
                    .orderBy("weight")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (!task.isSuccessful()) {
                                receiver.onFailure();
                                return;
                            }

                            updateListOrder(task.getResult());
                            addGroupHelper(name, receiver);
                        }
                    });
            return;
        } else
            addGroupHelper(name, receiver);
    }

    private void addGroupHelper(String name, Receiver receiver) {
        DocumentReference groupRef = listRef.collection("groups")
                .document();

        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        long weight = calculateNewWeight();
        map.put("weight", weight);
        groupRef.set(map)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            receiver.onFailure();
                            return;
                        }
                    }
                });
    }

    private long calculateNewWeight() {
        long highestWeight = groups.size() == 0 ?
                -1000000000 :
                groups.stream().reduce((group1, group2) -> group1.getWeight() > group2.getWeight() ? group1 : group2)
                        .get().getWeight();

        return highestWeight + (1000000000) + (long) (Math.random() * 2000 - 1000);
    }

    public void updateGroup(Group movedGroup, Group groupAbove) {
        long weight;
        if (groupAbove == null) {
            long lowest = groups.size() == 0 ?
                    0 :
                    groups.stream().reduce((group1, group2) -> group1.getWeight() < group2.getWeight() ? group1 : group2)
                            .get().getWeight();

            weight = lowest - (1000000000) + (long) (Math.random() * 2000 - 1000);
        } else {
            int indexOfAbove = groups.indexOf(groupAbove);
            if (indexOfAbove == groups.size() - 1)
                weight = calculateNewWeight();
            else {
                long weightabove = groupAbove.getWeight();
                long weightbelow = groups.get(indexOfAbove + 1).getWeight();
                if (weightabove == weightbelow - 1) {
                    //Todo big reorder no place in between
                }
                weight = weightabove + (weightbelow - weightabove) / 2;
                weight += (long) Math.random() * ((weightbelow - weightabove) / 10) - ((weightbelow - weightabove) / 20);
            }
        }
        movedGroup.setWeight(weight);
    }

    public void clearList() {
        if (!isSettingsListenerSet) {
            listRef.get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            updateListSettings(documentSnapshot);
                            clearListHelper();
                        }
                    });
        } else
            clearListHelper();
    }

    private void clearListHelper() {
        buildList();
        if (!keepGroups) {
            groups.forEach(group -> group.delete());
        } else {
            groups.forEach(group -> group.clearItems());
        }
    }
}