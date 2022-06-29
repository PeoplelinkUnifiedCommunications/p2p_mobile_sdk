package com.peoplellink.p2psdk;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

public class SimpleSdpObserver implements SdpObserver {
    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
    }

    @Override
    public void onSetSuccess() {
    }
    @Override
    public void onCreateFailure(String msg) {
    }

    @Override
    public void onSetFailure(String msg) {
    }
}
