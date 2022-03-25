package de.secretj12.ekl.Listener;

import java.util.List;

public interface ListChangeListener<T> {
    void onReorder(List<T> items);
}
