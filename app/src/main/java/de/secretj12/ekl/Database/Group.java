package de.secretj12.ekl.Database;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
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

import de.secretj12.ekl.Listener.ItemChangeListener;
import de.secretj12.ekl.Listener.ListChangeListener;
import de.secretj12.ekl.Listener.Listener;
import de.secretj12.ekl.Listener.ListenerManager;
import de.secretj12.ekl.Listener.Receiver;

public class Group extends ListItem {
    private final ListenerManager listenerManager;
    private final int listenerID;
    private final DocumentReference groupRef;

    private String name;
    private boolean allItemsCancelled;
    private List<Item> items;
    private long weight;

    private final Map<Integer, ItemChangeListener> itemChangeListeners;
    private final List<ListChangeListener<Item>> listChangeListeners;
    private int itemChangeListenerIDs = 0;

    Group(DocumentReference groupRef, int listenerID, String name, long weight) {
        listenerManager = ListenerManager.getInstance();
        this.groupRef = groupRef;
        this.listenerID = listenerID;
        this.name = name;
        this.weight = weight;

        items = new ArrayList<>();
        allItemsCancelled = false;
        itemChangeListeners = new HashMap<>();
        listChangeListeners = new ArrayList<>();
    }

    int getSize() {
        return items.size();
    }

    public boolean equals(DocumentReference ref) {
        return ref.equals(groupRef);
    }

    @Override
    public int setItemListener(ItemChangeListener listener) {
        itemChangeListeners.put(itemChangeListenerIDs, listener);
        listener.onNameChange(name);
        listener.onStateChanged(allItemsCancelled);
        return itemChangeListenerIDs++;
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

        this.weight = snap.getLong("weight");
    }

    long getWeight() {
        return this.weight;
    }

    private boolean groupChangeListenerSet = false;
    private Listener groupChangeListener;

    void setListChangeListener(ListChangeListener<Item> listener, int fragmentID) {
        if (!groupChangeListenerSet) {
            groupChangeListenerSet = true;
            groupChangeListener = listenerManager.registerListener(listenerID, fragmentID, new Receiver<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot data) {
                    updateGroupOrder(data);
                    for (int i = 0; i < items.size(); i++) {
                        items.get(i).checkUpdate(data.getDocuments().get(i));
                    }
                }
            }, groupRef.collection("items"));
        }
        listChangeListeners.add(listener);
        if (items.size() != 0)
            listener.onReorder(items);
    }

    void deleteGroupChangeListener() {
        if (groupChangeListener != null)
            groupChangeListener.delete();
    }

    private void updateGroupOrder(QuerySnapshot data) {
        List<Item> newItems = data.getDocuments().stream()
                .map(item -> new Item(
                        groupRef.collection("items").document(item.getId()),
                        item.getString("name"),
                        item.getBoolean("cancelled"),
                        item.getLong("weight")
                ))
                .collect(Collectors.toList());

        List<Item> newList = new ArrayList<>();
        newItems.forEach(item -> {
            if (items.contains(item)) {
                newList.add(items.get(items.indexOf(item)));
            } else {
                newList.add(item);
            }
        });
        items = newList;

        boolean newAllItemsCancelled = data.getDocuments().stream()
                .map(item -> item.getBoolean("cancelled"))
                .reduce(true, (a, b) -> a & b) && data.getDocuments().size() != 0;

        if (newAllItemsCancelled != allItemsCancelled) {
            allItemsCancelled = newAllItemsCancelled;
            itemChangeListeners.entrySet().forEach(entry -> entry.getValue().onStateChanged(newAllItemsCancelled));
        }
        allItemsCancelled = newAllItemsCancelled;
        listChangeListeners.forEach(listener -> listener.onReorder(items));
    }

    public void addItem(String name, Receiver receiver) {
        if (!groupChangeListenerSet) {
            groupRef.collection("items")
                    .orderBy("weight")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (!task.isSuccessful()) {
                                receiver.onFailure();
                                return;
                            }

                            updateGroupOrder(task.getResult());
                            addItemHelper(name, receiver);
                        }
                    });
        } else
            addItemHelper(name, receiver);
    }

    private void addItemHelper(String name, Receiver receiver) {
        DocumentReference itemRef = groupRef.collection("items").document();

        Map<String, Object> map = new HashMap<>();
        map.put("cancelled", false);
        map.put("name", name);
        long weight = calculateNewWeight();
        map.put("weight", weight);
        itemRef.set(map)
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

    public void addExistingItem(Item item, Item itemAbove) {
        if (!groupChangeListenerSet) {
            groupRef.collection("items")
                    .orderBy("weight")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            updateGroupOrder(task.getResult());
                            addExistingItemHelper(item, itemAbove);
                        }
                    });
        } else
            addExistingItemHelper(item, itemAbove);
    }

    private void addExistingItemHelper(Item item, Item itemAbove) {
        DocumentReference itemRef = groupRef.collection("items").document();

        Map<String, Object> map = new HashMap<>();
        map.put("name", item.getName());
        map.put("cancelled", item.isCancelled());
        long weight = calculateWeight(itemAbove);
        map.put("weight", weight);
        itemRef.set(map);
    }

    private long calculateNewWeight() {
        long highestWeight = items.size() == 0 ?
                -1000000000 :
                items.stream().reduce((item1, item2) -> item1.getWeight() > item2.getWeight() ? item1 : item2)
                        .get().getWeight();

        return highestWeight + (1000000000) + (long) (Math.random() * 2000 - 1000);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Group && Objects.equals(groupRef, ((Group) o).groupRef);
    }

    @Override
    public void delete() {
        Log.e("group", "delete group");
        groupRef.delete();
    }

    @Override
    public boolean isEnableAble() {
        return items.stream()
                .map(item -> item.isCancelled())
                .reduce(false, (a, b) -> a | b) && items.size() != 0;
    }

    @Override
    public boolean isCancelAble() {
        return !items.stream()
                .map(item -> item.isCancelled())
                .reduce(true, (a, b) -> a & b) && items.size() != 0;
    }

    @Override
    public boolean isCancelled() {
        return allItemsCancelled;
    }

    @Override
    public void setName(String name) {
        groupRef.update("name", name);
    }

    @Override
    public void setCancel(boolean cancelled) {
        items.forEach(item -> item.setCancel(cancelled));
    }

    @Override
    public String toString() {
        return "Group(" + name + "){" + groupRef.getId() + "}";
    }

    void setWeight(long weight) {
        groupRef.update("weight", weight);
    }

    public void moveItem(Item movedItem, Item itemAbove) {
        movedItem.setWeight(calculateWeight(itemAbove));
    }

    private long calculateWeight(Item itemAbove) {
        long weight;
        if (itemAbove == null) {
            long lowest = items.size() == 0 ?
                    0 :
                    items.stream().reduce((group1, group2) -> group1.getWeight() < group2.getWeight() ? group1 : group2)
                            .get().getWeight();

            weight = lowest - (1000000000) + (long) (Math.random() * 2000 - 1000);
        } else {
            int indexOfAbove = items.indexOf(itemAbove);
            if (indexOfAbove == items.size() - 1)
                weight = calculateNewWeight();
            else {
                long weightAbove = itemAbove.getWeight();
                long weightBelow = items.get(indexOfAbove + 1).getWeight();
                if (weightAbove == weightBelow - 1) {
                    //Todo big reorder no place in between1
                }
                weight = weightAbove + (weightBelow - weightAbove) / 2;
                weight += (long) Math.random() * ((weightBelow - weightAbove) / 10) - ((weightBelow - weightAbove) / 20);
            }
        }
        return weight;
    }

    void clearItems() {
        items.forEach(Item::delete);
    }

    void deleteCompleted() {
        items.stream().filter(Item::isCancelled).forEach(Item::delete);
    }
}
