package de.secretj12.ekl.Listener;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

class CollectionListener extends Listener {
    private final Receiver<QuerySnapshot> receiver;
    private final CollectionReference colRef;

    CollectionListener(Receiver<QuerySnapshot> receiver, CollectionReference colRef) {
        super();
        this.receiver = receiver;
        this.colRef = colRef;
    }

    public void connect() {
        if (deleted)
            throw new IllegalStateException();

        if (connected)
            return;

        connected = true;
        listReg = colRef.orderBy("weight")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null)
                            receiver.onFailure();
                        else
                            receiver.onSuccess(value);
                    }
                });
    }

    public void disconnect() {
        if (deleted)
            throw new IllegalStateException();

        if (!connected || listReg == null)
            return;

        connected = false;
        listReg.remove();
    }
}
