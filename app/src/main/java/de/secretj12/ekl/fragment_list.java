package de.secretj12.ekl;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import de.secretj12.ekl.Database.Database;
import de.secretj12.ekl.Database.EKList;
import de.secretj12.ekl.Database.Group;
import de.secretj12.ekl.Listener.ListSettingsListener;
import de.secretj12.ekl.Listener.Receiver;
import de.secretj12.ekl.listhelper.RecyclerListAdapter;
import de.secretj12.ekl.listhelper.SimpleItemTouchHelperCallback;

public class fragment_list extends Fragment {
    private final static int listenerID = 101;
    private final Database database;
    private EKList liste;
    private Toolbar toolbar;
    private RecyclerListAdapter adapter;
    private ItemTouchHelper touchHelper;
    private RecyclerView list;

    public fragment_list() {
        database = Database.getInstance();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        toolbar = view.findViewById(R.id.toolbar_list);
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.add_group:
                    addGroup();
                    return true;
                case R.id.settings_list:
                    Navigation.findNavController(getView())
                            .navigate(R.id.action_fragment_list_to_fragment_list_settings);
                    return true;
                case R.id.change_list:
                    Navigation.findNavController(getView())
                            .navigate(R.id.action_fragment_list_to_fragment_all_lists);
                    return true;
                case R.id.clear_list:
                    liste.clearList();
                    return true;
            }
            return false;
        });

        list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        return view;
    }

    private void addGroup() {
        View dialogview = LayoutInflater.from(getContext()).inflate(R.layout.dialog_name_input, null);
        EditText input = dialogview.findViewById(R.id.input);
        input.setHint(getString(R.string.name));
        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.add_group))
                .setView(dialogview)
                .setPositiveButton(getString(R.string.add), (dialogInterface, i) -> {
                    liste.addGroup(input.getText().toString(), new Receiver<Group>() {
                        @Override
                        public void onFailure() {

                        }
                    });
                })
                .setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        EKList newListe = database.getCurrentList(getContext());

        if (newListe == null) {
            Navigation.findNavController(getView())
                    .navigate(R.id.action_fragment_list_to_fragment_all_lists);
            return;
        } else {
            liste = newListe;

            adapter = new RecyclerListAdapter((viewHolder) -> {
                if (viewHolder.getItemViewType() == 1)
                    adapter.onActivateGroupMode();
                touchHelper.startDrag(viewHolder);
            }, liste, getString(R.string.loading_data), listenerID);
            list.setAdapter(adapter);

            ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
            touchHelper = new ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(list);

            toolbar.setTitle(getString(R.string.default_title_list));
            liste.setListSettingsListener(new ListSettingsListener() {
                @Override
                public void onNameUpdate(String name) {
                    toolbar.setTitle(getString(R.string.default_title_list) + " " + name);
                }

                @Override
                public void onTokenUpdate(String token) {
                    //hier irrelevant
                }

                @Override
                public void onKeepGroups(boolean keepGroups) {
                    //irrelevant (wird durch Adapter erledigt)
                }
            }, listenerID);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (liste != null)
            liste.delete(listenerID);
    }
}