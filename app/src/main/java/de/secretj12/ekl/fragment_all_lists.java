package de.secretj12.ekl;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.secretj12.ekl.Database.Database;
import de.secretj12.ekl.Database.EKList;
import de.secretj12.ekl.Listener.ListSettingsListener;
import de.secretj12.ekl.Listener.Receiver;


public class fragment_all_lists extends Fragment {
    private final int listenerID = 100;
    private final Database database;
    private ListView list;

    public fragment_all_lists() {
        database = Database.getInstance();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_lists, container, false);

        list = view.findViewById(R.id.listview_all_lists);
        List<String> messageList = Arrays.asList(getString(R.string.receiving_lists));
        ArrayAdapter<String> messageAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, android.R.id.text1, messageList);
        list.setAdapter(messageAdapter);


        Button create_list = view.findViewById(R.id.create_list);
        create_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View dialogview = LayoutInflater.from(getContext()).inflate(R.layout.dialog_name_input, null);
                EditText input = dialogview.findViewById(R.id.input);
                input.setHint(getString(R.string.name));
                AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                        .setTitle(getString(R.string.create_list))
                        .setView(dialogview)
                        .setPositiveButton(getString(R.string.create), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                database.createList(new Receiver<EKList>() {
                                    @Override
                                    public void onSuccess(EKList data) {
                                        swapTo(data);
                                        dialogInterface.dismiss();
                                    }

                                    @Override
                                    public void onFailure() {
                                        Snackbar.make(getView(), getString(R.string.creating_failed), Snackbar.LENGTH_LONG).show();
                                        dialogInterface.dismiss();
                                    }
                                }, input.getText().toString());
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .show();
            }
        });

        Button add_list = view.findViewById(R.id.add_list);
        add_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View dialogview = LayoutInflater.from(getContext()).inflate(R.layout.dialog_name_input, null);
                EditText input = dialogview.findViewById(R.id.input);
                input.setHint(getString(R.string.token));
                AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                        .setTitle(getString(R.string.add_list))
                        .setView(dialogview)
                        .setPositiveButton(getString(R.string.add), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                database.addList(new Receiver<EKList>() {
                                    @Override
                                    public void onSuccess(EKList data) {
                                        swapTo(data);
                                        dialogInterface.dismiss();
                                    }

                                    @Override
                                    public void onFailure() {
                                        Snackbar.make(getView(), getString(R.string.adding_failed), Snackbar.LENGTH_LONG).show();
                                        dialogInterface.dismiss();
                                    }
                                }, input.getText().toString());
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .show();
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateList();
    }

    private List<EKList> lists;

    @Override
    public void onPause() {
        super.onPause();
        lists.forEach(list -> list.delete(listenerID));
    }

    private void updateList() {
        if (lists == null)
            lists = new ArrayList<>();

        database.getAllLists(new Receiver<List<EKList>>() {
            @Override
            public void onSuccess(List<EKList> data) {
                if (data.size() == 0) {
                    List<String> messageList = Arrays.asList(getString(R.string.no_lists));
                    ArrayAdapter<String> messageAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, android.R.id.text1, messageList);
                    list.setAdapter(messageAdapter);
                    messageAdapter.notifyDataSetChanged();
                } else {
                    ArrayAdapter<EKList> adapter = new ArrayAdapter<EKList>(getContext(), android.R.layout.simple_list_item_1, android.R.id.text1, data) {
                        @NonNull
                        @Override
                        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);

                            TextView text = view.findViewById(android.R.id.text1);
                            text.setText(getString(R.string.receiving_list_name));
                            data.get(position).setListSettingsListener(new ListSettingsListener() {
                                @Override
                                public void onNameUpdate(String name) {
                                    text.setText(name);
                                }

                                @Override
                                public void onTokenUpdate(String token) {
                                    //hier nicht bebraucht
                                }

                                @Override
                                public void onKeepGroups(boolean keepGroups) {
                                    //hier nicht bebraucht
                                }
                            }, listenerID);

                            return view;
                        }
                    };
                    list.setAdapter(adapter);
                    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                            swapTo(data.get(position));
                        }
                    });
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure() {
                List<String> messageList = Arrays.asList(getString(R.string.receiving_lists_failed));
                ArrayAdapter<String> messageAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, android.R.id.text1, messageList);
                list.setAdapter(messageAdapter);
                messageAdapter.notifyDataSetChanged();
            }
        });
    }

    private void swapTo(EKList liste) {
        database.swapList(liste, getContext());
        Navigation.findNavController(fragment_all_lists.this.getView())
                .navigate(R.id.action_fragment_all_lists_to_fragment_list);
    }
}