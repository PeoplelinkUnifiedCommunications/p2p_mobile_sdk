package com.peoplellink.p2psdk;

import android.content.Context;

import org.webrtc.SurfaceViewRenderer;


public class InApiSDK {

    /* For connect to webSocket pass webSocket url and meeting id. */
    public void connectServer(String serverUrl, String selfId, ActionCallBack callBack) {
        InApiPeer.getInstance().connectServer(serverUrl, selfId, callBack);
    }

    /*For initialise InApiListener */
    public void inApiListener(InApiListener instaListener) {
        InApiPeer.getInstance().setListener(instaListener);
    }

    /*For initialise surfaceViews here*/
    public void initialise(Context context, SurfaceViewRenderer localView, SurfaceViewRenderer remoteView, boolean localMirror, boolean remoteMirror) {
        InApiPeer.getInstance().initialise(context, localView, remoteView, localMirror, remoteMirror);
    }

    /* For initialise call to remote user pass callType as audio or video along with remote id. */
    public void initiateCall(String remoteId, boolean isAudioCall) {
        InApiPeer.getInstance().initiateCall(remoteId, isAudioCall);
    }

    /*For answer call. */
    public void answerCall() {
        InApiPeer.getInstance().callAccepted();
    }

    /*For make call to remote user. */
    public void makeCall(String remoteId) {
        InApiPeer.getInstance().makeCall(remoteId);
    }

    /*For answer call from caller. */
    public void answerCallFromCaller() {
        InApiPeer.getInstance().answerCall();
    }

    /*For disconnect call. */
    public void disconnectCall() {
        InApiPeer.getInstance().disconnect();
    }

    /*For leave call. */
    public void leaveCall() {
        InApiPeer.getInstance().leave();
    }

    /*For decline call. */
    public void declineCall() {
        InApiPeer.getInstance().declineCall();
    }

    /*For call audio Mute */
    public void audioMute() {
        InApiPeer.getInstance().audioMute();
    }

    /*For device speaker turn on */
    public void speakerOn() {
        InApiPeer.getInstance().setSpeakerOn();
    }

    /*For device speaker turn off */
    public void speakerOff() {
        InApiPeer.getInstance().setHeadsetOn();
    }

    /*For call audio UnMute */
    public void audioUnMute() {
        InApiPeer.getInstance().audioUnMute();
    }

    /*For call video Mute */
    public void videoMute() {
        InApiPeer.getInstance().videoMute();
    }

    /*For call video UnMute */
    public void videoUnMute() {
        InApiPeer.getInstance().videoUnMute();
    }

    /*For switch camera to front or rear */
    public void switchCamera() {
        InApiPeer.getInstance().switchCamera();
    }

    public boolean isBluetoothConnected() {
        return InApiPeer.getInstance().isBluetoothHeadsetConnected();
    }

    /*For reconnect*/
    public void reConnect() {
        InApiPeer.getInstance().reConnect();
    }

    public void sendMessage(String remoteId, String message) {
        InApiPeer.getInstance().sendMessage(remoteId, message);
    }

    public void sendReply(String message) {
        InApiPeer.getInstance().sendReply(message);
    }

}
