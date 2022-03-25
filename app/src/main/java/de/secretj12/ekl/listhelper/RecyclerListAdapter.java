package de.secretj12.ekl.listhelper;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import de.secretj12.ekl.Database.EKList;
import de.secretj12.ekl.Database.Group;
import de.secretj12.ekl.Database.Item;
import de.secretj12.ekl.Database.ListItem;
import de.secretj12.ekl.Listener.ItemChangeListener;
import de.secretj12.ekl.Listener.ListChangeListener;
import de.secretj12.ekl.Listener.ListSettingsListener;
import de.secretj12.ekl.R;

public class RecyclerListAdapter
        extends RecyclerView.Adapter<ItemViewHolder>
        implements ListChangeListener<ListItem>, ItemTouchHelperAdapter, ListSettingsListener {
    private final OnStartDragListener onStartDragListener;
    private final EKList ekList;
    private final List<ListItem> list;
    private final List<Group> grouplist;
    private boolean groupMode;
    private boolean ignoreNextUpdate;
    private boolean keepGroups;

    public RecyclerListAdapter(OnStartDragListener onStartDragListener, EKList eklist, String standardMessage, int fragmentID) {
        this.onStartDragListener = onStartDragListener;
        this.ekList = eklist;
        groupMode = false;
        list = new ArrayList<>();
        grouplist = new ArrayList<>();
        ignoreNextUpdate = false;
        keepGroups = true;
        list.add(new ListItem() {
            @Override
            public int setItemListener(ItemChangeListener listener) {
                listener.onNameChange(standardMessage);
                listener.onStateChanged(false);
                return -1;
            }

            @Override
            public void resetItemListener(int id) {
                //not needed
            }

            @Override
            public void delete() {
                //not needed
            }

            @Override
            public boolean isCancelled() {
                //not needed
                return true;
            }

            @Override
            public boolean isEnableAble() {
                return false;
            }

            @Override
            public boolean isCancelAble() {
                return false;
            }

            @Override
            public void setName(String name) {
                //not needed
            }

            @Override
            public void setCancel(boolean cancelled) {
                //not needed
            }
        });
        notifyDataSetChanged();

        eklist.setListChangeListener(this, fragmentID);
        eklist.setGroupsChangeListener(items -> onGroupReorder(items), fragmentID);
        eklist.setListSettingsListener(this, fragmentID);
    }

    @Override
    public int getItemViewType(int position) {
        if (groupMode)
            return grouplist.get(position) instanceof Group ? 1 : 0;
        return list.get(position) instanceof Group ? 1 : 0;
    }

    public boolean isKeepGroups() {
        return keepGroups;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 1)
            return new ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_group, parent, false), parent);
        else
            return new ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_item, parent, false), parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        if (groupMode)
            holder.setItem(grouplist.get(position), position, onStartDragListener);
        else
            holder.setItem(list.get(position), position, onStartDragListener);
    }

    @Override
    public int getItemCount() {
        if (!groupMode)
            return list.size();
        else
            return grouplist.size();
    }

    //Änderungen vom Server
    @Override
    public void onReorder(List<ListItem> newList) {
        if (ignoreNextUpdate) {
            ignoreNextUpdate = false;
            return;
        }

        for (int i = 0; i < list.size(); ) {
            ListItem item = list.get(i);
            if (!newList.contains(item)) {
                list.remove(i);
                if (!groupMode)
                    notifyItemRemoved(i);
            } else {
                i++;
            }
        }

        for (int i = 0; i < newList.size(); i++) {
            ListItem item = newList.get(i);
            if (!list.contains(item)) {
                list.add(i, item);
                if (!groupMode) {
                    notifyItemInserted(i);
                }
            }
        }

        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).equals(newList.get(i))) {
                if (newList.get(i).equals(list.get(i + 1))) {   //Item wurde runtergerutscht
                    ListItem item = list.get(i);
                    int from = i;
                    int to = newList.indexOf(item);
                    list.remove(i);
                    list.add(to, item);
                    if (!groupMode)
                        notifyItemMoved(from, to);
                } else {    //item wurde hochgerutscht
                    ListItem item = newList.get(i);
                    int from = list.indexOf(item);
                    int to = i;
                    list.remove(from);
                    list.add(to, item);
                    if (!groupMode)
                        notifyItemMoved(from, to);
                }
            }
        }

        if (!list.equals(newList)) {
            list.clear();
            list.addAll(newList);
            if (!groupMode)
                notifyDataSetChanged();
        }

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) != newList.get(i))
                Log.v("RecyclerListAdapter-listItems", "seltsamer Fehler, Items nicht übereinstimmend: " + i);
        }
    }

    private void onGroupReorder(List<Group> newList) {
        for (int i = 0; i < grouplist.size(); ) {
            Group item = grouplist.get(i);
            if (!newList.contains(item)) {
                grouplist.remove(i);
                if (groupMode)
                    notifyItemRemoved(i);
            } else {
                i++;
            }
        }

        for (int i = 0; i < newList.size(); i++) {
            Group item = newList.get(i);
            if (!grouplist.contains(item)) {
                grouplist.add(item);
                if (groupMode)
                    notifyItemInserted(i);
            }
        }

        for (int i = 0; i < grouplist.size(); i++) {
            if (!grouplist.get(i).equals(newList.get(i))) {
                if (newList.get(i).equals(grouplist.get(i + 1))) {   //item wurde runtergerutscht
                    Group item = grouplist.get(i);
                    int from = i;
                    int to = newList.indexOf(item);
                    grouplist.remove(i);
                    grouplist.add(to, item);
                    if (groupMode)
                        notifyItemMoved(from, to);
                } else {    //item wurde hochgerutscht
                    Group item = newList.get(i);
                    int from = grouplist.indexOf(item);
                    int to = i;
                    grouplist.remove(from);
                    grouplist.add(to, item);
                    if (groupMode)
                        notifyItemMoved(from, to);
                }
            }
        }

        if (!grouplist.equals(newList)) {
            grouplist.clear();
            grouplist.addAll(newList);
            if (groupMode)
                notifyDataSetChanged();
        }

        for (int i = 0; i < grouplist.size(); i++) {
            if (grouplist.get(i) != newList.get(i))
                Log.e("RecyclerListAdapter-groups", "seltsamer Fehler, items nicht übereinstimmend: " + i);
        }
    }

    //Änderungen vom Benutzer
    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        if (groupMode) {
            if (from == null) {
                from = grouplist.get(fromPosition);
            }
            grouplist.add(toPosition, grouplist.remove(fromPosition));
            to = toPosition == 0 ? null : grouplist.get(toPosition - 1);
        } else {
            if (from == null) {
                from = list.get(fromPosition);
                groupFrom = findGroupOf(fromPosition);
            }
            list.add(toPosition, list.remove(fromPosition));
            to = toPosition == 0 ? null : list.get(toPosition - 1);
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    private ListItem from;
    private Group groupFrom;
    private ListItem to;

    @Override
    public void onItemDelete(int position, ItemViewHolder holder) {
        Log.v("RecyclerListAdapter-Benutzer", "Item delete");
        holder.deleteItem();
    }

    @Override
    public void onItemEnable(int position, ItemViewHolder holder) {
        Log.v("RecyclerListAdapter-Benutzer", "Item cancel");
        holder.setCancelled(false);
    }

    @Override
    public void onItemCancel(int position, ItemViewHolder holder) {
        Log.v("RecyclerListAdapter-Benutzer", "Item cancel");
        holder.setCancelled(true);
    }

    @Override
    public void onItemUpdated(int position, ItemViewHolder holder) {
        notifyItemChanged(position);
    }

    @Override
    public void onActivateGroupMode() {
        groupMode = true;
        for (int i = list.size() - 1; i >= 0; i--)
            if (list.get(i) instanceof Item)
                notifyItemRemoved(i);
    }

    @Override
    public void onActionEnded() {
        Log.v("RecyclerListAdapter-Benutzer", "Action done");
        if (from != null) {
            if (groupMode) {
                groupMode = false;
                if (from != null)
                    ekList.updateGroup((Group) from, (Group) to);
                notifyDataSetChanged();
            } else {
                Group groupTo = to instanceof Group ? (Group) to : findGroupOf(list.indexOf(to));

                if (groupFrom.equals(groupTo)) {
                    groupFrom.moveItem((Item) from, to instanceof Group ? null : (Item) to);
                } else {
                    ignoreNextUpdate = true;
                    groupTo.addExistingItem((Item) from, to instanceof Group ? null : (Item) to);
                    from.delete();
                }
            }
            from = null;
            to = null;
        }
        if (groupMode) {
            groupMode = false;
            notifyDataSetChanged();
        }
    }

    private Group findGroupOf(int index) {
        while (index >= 0) {
            if (list.get(index) instanceof Group)
                return (Group) list.get(index);
            index--;
        }
        return null;
    }

    @Override
    public void onNameUpdate(String name) {
        //irrelevant
    }

    @Override
    public void onTokenUpdate(String token) {
        //irrelevant
    }

    @Override
    public void onKeepGroups(boolean keepGroups) {
        this.keepGroups = keepGroups;
    }
}
