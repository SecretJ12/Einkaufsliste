package de.secretj12.ekl.listhelper;

interface ItemTouchHelperAdapter {
    void onItemMove(int fromPosition, int toPosition);

    void onItemDelete(int position, ItemViewHolder holder);

    void onItemEnable(int position, ItemViewHolder holder);

    void onItemCancel(int position, ItemViewHolder holder);

    void onItemUpdated(int position, ItemViewHolder holder);

    void onActivateGroupMode();

    void onActionEnded();

    boolean isKeepGroups();
}