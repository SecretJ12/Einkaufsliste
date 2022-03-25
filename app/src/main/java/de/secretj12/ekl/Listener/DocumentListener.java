package de.secretj12.ekl.Listener;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;

class DocumentListener extends Listener {
    private final Receiver<DocumentSnapshot> receiver;
    private final DocumentReference docRef;

    DocumentListener(Receiver<DocumentSnapshot> receiver, DocumentReference docRef) {
        super();
        this.receiver = receiver;
        this.docRef = docRef;
    }

    public void connect() {
        if (deleted)
            throw new IllegalStateException();

        if (connected)
            return;

        connected = true;
        listReg = docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
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
