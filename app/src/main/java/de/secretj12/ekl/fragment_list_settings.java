package de.secretj12.ekl;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.util.Objects;

import de.secretj12.ekl.Database.Database;
import de.secretj12.ekl.Database.EKList;
import de.secretj12.ekl.Listener.ListSettingsListener;
import de.secretj12.ekl.Listener.ListenerManager;
import de.secretj12.ekl.Listener.Receiver;

public class fragment_list_settings extends Fragment {
    private final int listenerID = 102;
    private final ListenerManager listenerManager;

    private final Database database;
    private EKList liste;

    private Toolbar toolbar;
    private TextView token;
    private SwitchCompat keepGroups;

    private boolean token_exits;

    public fragment_list_settings() {
        database = Database.getInstance();
        token_exits = false;
        listenerManager = ListenerManager.getInstance();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_settings, container, false);

        toolbar = view.findViewById(R.id.toolbar_list_settings);
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Navigation.findNavController(getView())
                        .navigate(R.id.action_fragment_list_settings_to_fragment_list);
            }
        });

        token = view.findViewById(R.id.token);

        ImageButton copy_token = view.findViewById(R.id.copy_token);
        copy_token.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (token_exits) {
                    ClipboardManager clipboard = (ClipboardManager)
                            getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("EKL-token", token.getText()));
                    Toast.makeText(getActivity(), R.string.token_to_clipboard, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), R.string.no_token_exists, Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button create_token = view.findViewById(R.id.create_token);
        create_token.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                liste.createToken(new Receiver<String>() {
                    @Override
                    public void onSuccess(String data) {
                        token.setText(data);
                        token_exits = true;
                    }

                    @Override
                    public void onFailure() {
                        Toast.makeText(getActivity(), R.string.token_creation_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        Button delete_token = view.findViewById(R.id.delete_token);
        delete_token.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                liste.resetToken(new Receiver() {
                    @Override
                    public void onSuccess(Object data) {
                        token.setText(R.string.no_token_exists);
                        token_exits = false;
                    }

                    @Override
                    public void onFailure() {
                        Toast.makeText(getActivity(), R.string.delete_token_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        keepGroups = view.findViewById(R.id.groups_deletable);
        keepGroups.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                liste.setKeepGroups(!keepGroups.isChecked());
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        EKList newList = database.getCurrentList(getContext());
        if (!Objects.equals(newList, liste)) {
            liste = newList;

            toolbar.setTitle(getString(R.string.default_title_list_settings));

            liste.setListSettingsListener(new ListSettingsListener() {
                @Override
                public void onNameUpdate(String name) {
                    toolbar.setTitle(getString(R.string.default_title_list_settings) + " " + name);
                }

                @Override
                public void onTokenUpdate(String token) {
                    if (token == null)
                        fragment_list_settings.this.token.setText(getString(R.string.no_token_exists));
                    else
                        fragment_list_settings.this.token.setText(token);

                    token_exits = token != null;
                }

                @Override
                public void onKeepGroups(boolean keepGroups) {
                    fragment_list_settings.this.keepGroups.setChecked(!keepGroups);
                }
            }, listenerID);
        } else
            liste.connect(listenerID);
    }

    @Override
    public void onPause() {
        super.onPause();

        liste.disconnect(listenerID);
    }
}