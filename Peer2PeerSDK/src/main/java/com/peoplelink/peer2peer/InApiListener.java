package com.peoplelink.peer2peer;

public interface InApiListener {

    void onCallInitiated();

    void offerReceived(String remoteId, boolean isAudioCall);

    void onFinished();

    void remoteUserDisconnected();

    void onCallDeclined();

    void onCallAccept();

    void onStatus(String message);

    void onVideoMuted(boolean isVideoMute);

    void onMessageReceived(String message);
}
