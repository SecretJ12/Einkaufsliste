package de.secretj12.ekl.Database;

import de.secretj12.ekl.Listener.ItemChangeListener;

public abstract class ListItem {
    public abstract int setItemListener(ItemChangeListener listener);

    public abstract void resetItemListener(int id);

    public abstract void delete();

    public abstract boolean isCancelled();

    public abstract boolean isEnableAble();

    public abstract boolean isCancelAble();

    public abstract void setName(String name);

    public abstract void setCancel(boolean cancelled);
}
