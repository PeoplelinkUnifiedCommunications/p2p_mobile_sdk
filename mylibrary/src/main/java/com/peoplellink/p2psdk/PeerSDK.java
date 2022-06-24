package com.peoplellink.p2psdk;

import android.content.Context;

import org.webrtc.SurfaceViewRenderer;


public class PeerSDK {

    /* For connect to webSocket pass webSocket url and meeting id. */
    public void connectServer(String serverUrl, String selfId, ActionCallBack callBack) {
        PeerConnections.getInstance().connectServer(serverUrl, selfId, callBack);
    }

    /*For initialise InApiListener */
    public void inApiListener(SDKListener instaListener) {
        PeerConnections.getInstance().setListener(instaListener);
    }

    /*For initialise surfaceViews here*/
    public void initialise(Context context, SurfaceViewRenderer localView, SurfaceViewRenderer remoteView, boolean localMirror, boolean remoteMirror) {
        PeerConnections.getInstance().initialise(context, localView, remoteView, localMirror, remoteMirror);
    }

    /* For initialise call to remote user pass callType as audio or video along with remote id. */
    public void initiateCall(String remoteId, boolean isAudioCall) {
        PeerConnections.getInstance().initiateCall(remoteId, isAudioCall);
    }

    /*For answer call. */
    public void answerCall() {
        PeerConnections.getInstance().callAccepted();
    }

    /*For make call to remote user. */
    public void makeCall(String remoteId) {
        PeerConnections.getInstance().makeCall(remoteId);
    }

    /*For answer call from caller. */
    public void answerCallFromCaller() {
        PeerConnections.getInstance().answerCall();
    }

    /*For disconnect call. */
    public void disconnectCall() {
        PeerConnections.getInstance().disconnect();
    }

    /*For leave call. */
    public void leaveCall() {
        PeerConnections.getInstance().leave();
    }

    /*For decline call. */
    public void declineCall() {
        PeerConnections.getInstance().declineCall();
    }

    /*For call audio Mute */
    public void audioMute() {
        PeerConnections.getInstance().audioMute();
    }

    /*For device speaker turn on */
    public void speakerOn() {
        PeerConnections.getInstance().setSpeakerOn();
    }

    /*For device speaker turn off */
    public void speakerOff() {
        PeerConnections.getInstance().setHeadsetOn();
    }

    /*For call audio UnMute */
    public void audioUnMute() {
        PeerConnections.getInstance().audioUnMute();
    }

    /*For call video Mute */
    public void videoMute() {
        PeerConnections.getInstance().videoMute();
    }

    /*For call video UnMute */
    public void videoUnMute() {
        PeerConnections.getInstance().videoUnMute();
    }

    /*For switch camera to front or rear */
    public void switchCamera() {
        PeerConnections.getInstance().switchCamera();
    }

    public boolean isBluetoothConnected() {
        return PeerConnections.getInstance().isBluetoothHeadsetConnected();
    }

    /*For reconnect*/
    public void reConnect() {
        PeerConnections.getInstance().reConnect();
    }

    public void sendMessage(String remoteId, String message) {
        PeerConnections.getInstance().sendMessage(remoteId, message);
    }

    public void sendReply(String message) {
        PeerConnections.getInstance().sendReply(message);
    }

}
