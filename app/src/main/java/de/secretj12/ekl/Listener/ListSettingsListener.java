package de.secretj12.ekl.Listener;


public interface ListSettingsListener {
    void onNameUpdate(String name);

    void onTokenUpdate(String token);

    void onKeepGroups(boolean keepGroups);
}
