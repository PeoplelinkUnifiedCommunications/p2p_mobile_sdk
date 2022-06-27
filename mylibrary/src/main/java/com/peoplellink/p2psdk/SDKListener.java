package com.peoplellink.p2psdk;

public interface SDKListener {

    void onCallInitiated(String remoteId);

    void offerReceived(String remoteId, boolean isAudioCall);

    void onFinished();

    void remoteUserDisconnected();

    void onCallDeclined();

    void onCallAccept();

    void onStatus(String message);

    void onVideoMuted(boolean isVideoMute);

    void onMessageReceived(String message);
}
