package de.secretj12.ekl.Database;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.secretj12.ekl.Listener.Receiver;

public class Database {
    private static Database database;

    private final FirebaseAuth mAuth;
    private FirebaseUser user;
    private final FirebaseFirestore firestore;
    private EKList currentList;

    private Database() {
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        if (user == null)
            mAuth.signInAnonymously().addOnSuccessListener(new OnSuccessListener() {
                @Override
                public void onSuccess(Object o) {
                    user = mAuth.getCurrentUser();
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("lists", new ArrayList<>());
                    map.put("newlisttoken", null);
                    firestore.collection("users")
                            .document(user.getUid())
                            .set(map);
                }
            });
    }

    public static Database getInstance() {
        if (database == null)
            database = new Database();
        return database;
    }

    public EKList getCurrentList(Context context) {
        if (currentList != null)
            return currentList;

        SharedPreferences prefs = context.getSharedPreferences("current_list", Context.MODE_PRIVATE);
        String list = prefs.getString("currentlist", null);
        if (list == null)
            return null;
        else {
            currentList = new EKList(firestore.collection("lists").document(list));
            return currentList;
        }
    }

    private void getUserData(final Receiver<DocumentSnapshot> receiver) {
        if (user == null) {
            receiver.onFailure();
            return;
        }

        firestore.collection("users")
                .document(user.getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful())
                    receiver.onSuccess(task.getResult());
                else
                    receiver.onFailure();
            }
        });
    }

    private void getListData(final Receiver<DocumentSnapshot> receiver, String listID) {
        firestore.collection("lists")
                .document(listID)
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if (task.isSuccessful())
                    receiver.onSuccess(task.getResult());
                else
                    receiver.onFailure();
            }
        });
    }

    public void createList(final Receiver<EKList> receiver, String listName) {
        Map<String, Object> listData = new HashMap<>();
        listData.put("name", listName);
        listData.put("token", null);
        listData.put("keepGroups", true);
        listData.put("user", Arrays.asList(user.getUid()));
        listData.put("groups", new ArrayList<String>());

        final DocumentReference listRef = firestore.collection("lists")
                .document();
        listRef.set(listData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            receiver.onFailure();
                            return;
                        }
                        getUserData(new Receiver<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot data) {
                                List<DocumentReference> lists = (List<DocumentReference>) data.get("lists");
                                if (lists == null) {
                                    lists = new ArrayList<>();
                                    lists.add(listRef);
                                } else
                                    lists.add(listRef);
                                firestore.collection("users")
                                        .document(user.getUid())
                                        .update("lists", lists)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (!task.isSuccessful()) {
                                                    receiver.onFailure();
                                                    return;
                                                }
                                                receiver.onSuccess(new EKList(listRef));
                                            }
                                        });
                            }

                            @Override
                            public void onFailure() {
                                receiver.onFailure();
                            }
                        });
                    }
                });
    }

    public void addList(final Receiver<EKList> receiver, String token) {
        final String id = token.split("-")[0];
        final DocumentReference listref = firestore.collection("lists").document(id);
        final DocumentReference userref = firestore.collection("users").document(user.getUid());

        userref
                .update("newlisttoken", token)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            Log.e("add_list", "fail1");
                            receiver.onFailure();
                            return;
                        }

                        getListData(new Receiver<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot data) {
                                List<String> userlist = (List<String>) data.get("user");
                                userlist.add(user.getUid());
                                listref
                                        .update("user", userlist)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (!task.isSuccessful()) {
                                                    Log.e("add_list", "fail2 " + task.getException().getMessage());
                                                    receiver.onFailure();
                                                    return;
                                                }

                                                getUserData(new Receiver<DocumentSnapshot>() {
                                                    @Override
                                                    public void onSuccess(DocumentSnapshot data) {
                                                        List<DocumentReference> lists = (List<DocumentReference>) data.get("lists");
                                                        lists.add(listref);
                                                        userref
                                                                .update("lists", lists)
                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        if (!task.isSuccessful()) {
                                                                            Log.e("add_list", "fail3");
                                                                            receiver.onFailure();
                                                                            return;
                                                                        }

                                                                        receiver.onSuccess(getList(id));
                                                                    }
                                                                });
                                                    }

                                                    @Override
                                                    public void onFailure() {
                                                        Log.e("add_list", "fail4");
                                                        receiver.onFailure();
                                                    }
                                                });
                                            }
                                        });
                            }

                            @Override
                            public void onFailure() {
                                Log.e("add_list", "fail5");
                                receiver.onFailure();
                            }
                        }, id);
                    }
                });
    }

    private EKList getList(String listID) {
        return new EKList(firestore.collection("lists")
                .document(listID));
    }

    public void swapList(EKList liste, Context context) {
        SharedPreferences prefs = context.getSharedPreferences("current_list", Context.MODE_PRIVATE);
        currentList = liste;
        prefs.edit()
                .putString("currentlist", liste.getID())
                .apply();
    }

    public void getAllLists(final Receiver<List<EKList>> receiver) {
        if (user == null) {
            mAuth.signInAnonymously().addOnSuccessListener(new OnSuccessListener() {
                @Override
                public void onSuccess(Object o) {
                    getAllLists(receiver);
                }
            })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            receiver.onFailure();
                        }
                    });
            return;
        }

        user = mAuth.getCurrentUser();
        HashMap<String, Object> map = new HashMap<>();
        map.put("lists", new ArrayList<>());
        map.put("newlisttoken", null);
        getUserData(new Receiver<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot data) {
                if (!data.exists())
                    firestore.collection("users")
                            .document(user.getUid())
                            .set(map);
            }
        });

        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (!task.isSuccessful()) {
                            receiver.onFailure();
                            return;
                        }

                        List<DocumentReference> list = (List<DocumentReference>) task.getResult().get("lists");
                        if (list == null)
                            receiver.onSuccess(new ArrayList<EKList>());
                        else
                            receiver.onSuccess(list.stream().map(dr -> new EKList(dr)).collect(Collectors.toList()));
                    }
                });
    }
}
