package com.peoplellink.p2psdk;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.media.AudioManager;
import android.text.TextUtils;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.RtpReceiver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PeerConnections {

    private static final String VIDEO_TRACK_ID = "ARDAMSv0";
    private static final String AUDIO_TRACK_ID = "ARDAMSa0";
    private static final int VIDEO_RESOLUTION_WIDTH = 320;
    private static final int VIDEO_RESOLUTION_HEIGHT = 240;
    private static final int VIDEO_FPS = 60;

    private String remoteUserId;
    private boolean isAudioCall;
    private WebSocketClient webSocketClient;
    private VideoTrack mVideoTrack;
    private AudioTrack mAudioTrack;
    private AudioManager audioManager;

    //FOR DISPOSE
    private PeerConnectionFactory mPeerConnectionFactory;
    private SurfaceTextureHelper mSurfaceTextureHelper;
    private VideoCapturer mVideoCapture;
    private PeerConnection mPeerConnection;
    private SurfaceViewRenderer mLocalSurfaceView, mRemoteSurfaceView;

    private String stunUrl;
    private String udpUrl;
    private String tcpUrl;
    private String userName;
    private String credential;
    private boolean isVideoMuted = false;

    private static PeerConnections instance = null;

    private SDKListener instaListener;
    private boolean isInitialised = false;

    private PeerConnections() {
    }

    protected static PeerConnections getInstance() {
        if (instance == null) {
            instance = new PeerConnections();
        }
        return instance;
    }

    public void setListener(SDKListener listener) {
        instaListener = listener;
    }

    protected void connectServer(String serverUrl, String selfId, ActionCallBack callBack) {
        if (webSocketClient == null || webSocketClient.isClosed()) {
            webSocketClient = new WebSocketClient(URI.create(serverUrl)) {
                @Override
                public void onOpen(ServerHandshake handShakeData) {
                    if (!isInitialised) {
                        setUserID(selfId);
                        callBack.onSuccess("Connected to server");
                        isInitialised = true;
                    }
                }

                @Override
                public void onMessage(String message) {
                    if (!TextUtils.isEmpty(message)) {
                        try {
                            JSONObject mainJson = new JSONObject(message);
                            String type = mainJson.getString("type");
                            instaListener.onStatus(mainJson.toString());

                            switch (type) {
                                case "hello":
                                    break;

                                case "iceServers":
                                    JSONArray iceArrayMain = mainJson.getJSONArray("iceServers");
                                    try {
                                        JSONObject jsonObjectOne = iceArrayMain.getJSONObject(0);
                                        stunUrl = jsonObjectOne.getString("urls");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    try {
                                        JSONObject jsonObjectTwo = iceArrayMain.getJSONObject(1);
                                        JSONArray turnArray = jsonObjectTwo.getJSONArray("urls");
                                        udpUrl = turnArray.getString(0);
                                        tcpUrl = turnArray.getString(1);
                                        userName = jsonObjectTwo.getString("username");
                                        credential = jsonObjectTwo.getString("credential");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    break;

                                case "offer": {
                                    remoteUserId = mainJson.getString("id");
                                    onRemoteOfferReceived(mainJson, remoteUserId);
                                    instaListener.offerReceived(remoteUserId, isAudioCall);
                                    answerCall();
                                    break;
                                }

                                case "call-initiated": {
                                    remoteUserId = mainJson.getString("id");
                                    isAudioCall = mainJson.getBoolean("isAudioCall");
                                    instaListener.onCallInitiated();
                                    break;
                                }

                                case "call-accepted": {
                                    onRemoteAnswerReceived(mainJson);
                                    remoteUserId = mainJson.getString("id");
                                    makeCall(remoteUserId);
                                    break;
                                }

                                case "answer":
                                    instaListener.onCallAccept();
                                    onRemoteAnswerReceived(mainJson);
                                    break;

                                case "candidate":
                                    onRemoteCandidateReceived(mainJson);
                                    break;

                                case "bye":
                                    instaListener.remoteUserDisconnected();
                                    break;

                                case "call-declined":
                                    instaListener.onCallDeclined();
                                    break;
                                case "video-muted":
                                    isVideoMuted = mainJson.getBoolean("isVideoMute");
                                    instaListener.onVideoMuted(isVideoMuted);
                                    break;

                                case "message":
                                    remoteUserId = mainJson.getString("id");
                                    String text = mainJson.getString("message");
                                    instaListener.onMessageReceived(text);
                                    break;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (code != 1000) {
//                        abcCaller();
                        instaListener.onStatus(reason);

                        reconnectIfNecessary();
                    } else {
                        isInitialised = false;
                    }
                }

                @Override
                public void onError(Exception ex) {
                    callBack.onFailure(ex.getMessage());
                }
            };
            try {
                webSocketClient.connectBlocking();
            } catch (InterruptedException e) {
                callBack.onFailure(e.getMessage() + " Trying to reconnect");
                reconnectIfNecessary();
            }
        } else {
            callBack.onSuccess("already connected to server");
        }

    }

    private void setUserID(String userid) {
        JSONObject jsonMain = new JSONObject();
        try {
            jsonMain.put("type", "userid");
            jsonMain.put("value", userid);
            send(jsonMain.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void initialise(Context context, SurfaceViewRenderer localView, SurfaceViewRenderer remoteView, boolean localMirror, boolean remoteMirror) {
        mLocalSurfaceView = localView;
        mRemoteSurfaceView = remoteView;

        EglBase mRootEglBase = EglBase.create();

        mLocalSurfaceView.init(mRootEglBase.getEglBaseContext(), null);
        mLocalSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        mLocalSurfaceView.setMirror(localMirror);
        mLocalSurfaceView.setEnableHardwareScaler(false);

        mRemoteSurfaceView.init(mRootEglBase.getEglBaseContext(), null);
        mRemoteSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        mRemoteSurfaceView.setMirror(remoteMirror);
        mRemoteSurfaceView.setEnableHardwareScaler(true);
        mRemoteSurfaceView.setZOrderMediaOverlay(false);

        //CAN INITIALIZE SEPARATE
        mPeerConnectionFactory = createPeerConnectionFactory(context, mRootEglBase);
        Logging.enableLogToDebugOutput(Logging.Severity.LS_VERBOSE);
        mSurfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", mRootEglBase.getEglBaseContext());
        VideoSource videoSource = mPeerConnectionFactory.createVideoSource(false);
        mVideoCapture = createVideoCapture(context);
        mVideoCapture.initialize(mSurfaceTextureHelper, context, videoSource.getCapturerObserver());
        mVideoTrack = mPeerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        mVideoTrack.setEnabled(true);
        mVideoTrack.addSink(mLocalSurfaceView);
        AudioSource audioSource = mPeerConnectionFactory.createAudioSource(new MediaConstraints());
        mAudioTrack = mPeerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        mAudioTrack.setEnabled(true);

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setSpeakerphoneOn(true);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        //TO START LOCAL CAPTURE
        mVideoCapture.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, VIDEO_FPS);


    }


    private PeerConnectionFactory createPeerConnectionFactory(Context context, EglBase mRootEglBase) {
        VideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(mRootEglBase.getEglBaseContext(),
                true,
                true);
        VideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(mRootEglBase.getEglBaseContext());
        PeerConnectionFactory.InitializationOptions initializationOptions = PeerConnectionFactory
                .InitializationOptions
                .builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);
        PeerConnectionFactory.Builder builder = PeerConnectionFactory.builder()
                .setVideoDecoderFactory(decoderFactory)
                .setVideoEncoderFactory(encoderFactory);
        builder.setOptions(null);

        return builder.createPeerConnectionFactory();
    }

    private VideoCapturer createVideoCapture(Context context) {
        if (Camera2Enumerator.isSupported(context)) {
            return createCameraCapturer(new Camera2Enumerator(context));
        } else {
            return createCameraCapturer(new Camera1Enumerator(true));
        }
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        String[] deviceNames = enumerator.getDeviceNames();
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) return videoCapturer;
            }
        }

        for (String deviceName : deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) return videoCapturer;
            }
        }

        return null;
    }

    private PeerConnection createPeerConnection() {

        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        if (stunUrl != null) {
            PeerConnection.IceServer stun = PeerConnection.IceServer.builder(stunUrl).createIceServer();
            iceServers.add(stun);
        }
        if (udpUrl != null) {
            PeerConnection.IceServer udp = PeerConnection.IceServer.builder(udpUrl).setUsername(userName).setPassword(credential).createIceServer();
            iceServers.add(udp);
        }
        if (tcpUrl != null) {
            PeerConnection.IceServer tcp = PeerConnection.IceServer.builder(tcpUrl).setUsername(userName).setPassword(credential).createIceServer();
            iceServers.add(tcp);
        }

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        rtcConfig.enableDtlsSrtp = true;
        //rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        PeerConnection connection = mPeerConnectionFactory.createPeerConnection(rtcConfig, mPeerConnectionObserver);
        if (connection == null) {
            return null;
        }
        List<String> mediaStreamLabels = Collections.singletonList("ARDAMS");
        connection.addTrack(mVideoTrack, mediaStreamLabels);
        connection.addTrack(mAudioTrack, mediaStreamLabels);

        return connection;
    }

    PeerConnection.Observer mPeerConnectionObserver = new PeerConnection.Observer() {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {

        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {

            try {
                JSONObject childObj = new JSONObject();
                childObj.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                childObj.put("sdpMid", iceCandidate.sdpMid);
                childObj.put("candidate", iceCandidate.sdp);

                JSONObject message = new JSONObject();
                message.put("type", "candidate");
                message.put("id", getRemoteUserId());
                message.put("candidate", childObj);

                try {
                    send(message.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            mPeerConnection.removeIceCandidates(iceCandidates);
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {

        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {

        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {

        }

        @Override
        public void onRenegotiationNeeded() {

        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
            MediaStreamTrack track = rtpReceiver.track();
            if (track instanceof VideoTrack) {

                VideoTrack remoteVideoTrack = (VideoTrack) track;
                remoteVideoTrack.setEnabled(true);
                remoteVideoTrack.addSink(mRemoteSurfaceView);
            }
        }
    };

    private String getRemoteUserId() {
        return remoteUserId;
    }

    private void onRemoteOfferReceived(JSONObject message, String remoteId) {
        if (mPeerConnection == null) {
            mPeerConnection = createPeerConnection();
        }

        try {
            String description = message.getString("sdp");
            mPeerConnection.setRemoteDescription(
                    new SimpleSdpObserver(),
                    new SessionDescription(
                            SessionDescription.Type.OFFER,
                            description));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void onRemoteAnswerReceived(JSONObject message) {
        try {
            String description = message.getString("sdp");
            mPeerConnection.setRemoteDescription(
                    new SimpleSdpObserver(),
                    new SessionDescription(
                            SessionDescription.Type.ANSWER,
                            description));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void onRemoteCandidateReceived(JSONObject message) {
        try {
            JSONObject childJson = message.getJSONObject("candidate");
            IceCandidate remoteIceCandidate =
                    new IceCandidate(childJson.getString("sdpMid"), childJson.getInt("sdpMLineIndex"), childJson.getString("candidate"));

            mPeerConnection.addIceCandidate(remoteIceCandidate);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void initiateCall(String remoteId, boolean isAudioCall) {
        remoteUserId = remoteId;
        JSONObject message = new JSONObject();
        try {
            message.put("id", getRemoteUserId());
            message.put("type", "call-initiated");
            message.put("isAudioCall", isAudioCall);
            send(message.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    protected void makeCall(String remoteId) {
        this.remoteUserId = remoteId;
        if (mPeerConnection == null) {
            mPeerConnection = createPeerConnection();
        }

        MediaConstraints mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("IceRestart", "true"));
        mPeerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                mPeerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                JSONObject message = new JSONObject();
                try {
                    message.put("id", getRemoteUserId());
                    message.put("type", "offer");
                    message.put("sdp", sessionDescription.description);
                    send(message.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, mediaConstraints);
    }

    protected void callAccepted() {
        JSONObject message = new JSONObject();
        try {
            message.put("id", getRemoteUserId());
            message.put("type", "call-accepted");
            send(message.toString());
        } catch (JSONException e) {
            e.printStackTrace();

        }
    }

    protected void sendMessage(String remoteId, String message) {
        remoteUserId = remoteId;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", getRemoteUserId());
            jsonObject.put("type", "message");
            jsonObject.put("message", message);
            send(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();

        }
    }

    protected void sendReply(String message) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", getRemoteUserId());
            jsonObject.put("type", "message");
            jsonObject.put("message", message);
            send(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();

        }
    }

    protected void answerCall() {
        if (mPeerConnection == null) {
            mPeerConnection = createPeerConnection();
        }
        MediaConstraints sdpMediaConstraints = new MediaConstraints();

        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        sdpMediaConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        sdpMediaConstraints.optional.add(new MediaConstraints.KeyValuePair("IceRestart", "true"));
        mPeerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                mPeerConnection.setLocalDescription(new SimpleSdpObserver(),
                        sessionDescription);

                JSONObject message = new JSONObject();

                try {
                    message.put("id", getRemoteUserId());
                    message.put("type", "answer");
                    message.put("sdp", sessionDescription.description);
                    send(message.toString());
                } catch (JSONException e) {
                    e.printStackTrace();

                }
            }
        }, sdpMediaConstraints);
    }

    protected void declineCall() {
        JSONObject message = new JSONObject();
        try {
            message.put("id", getRemoteUserId());
            message.put("type", "call-declined");
            send(message.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void disconnect() {
        JSONObject message = new JSONObject();
        try {
            message.put("id", getRemoteUserId());
            message.put("type", "bye");
            send(message.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        leave();
    }


    protected void leave() {
        try {
            mVideoCapture.stopCapture();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (mPeerConnection != null) {
            mPeerConnection.close();
            mPeerConnection = null;
        }

        mLocalSurfaceView.release();
        mRemoteSurfaceView.release();
        mVideoCapture.dispose();
        mSurfaceTextureHelper.dispose();
        PeerConnectionFactory.stopInternalTracingCapture();
        PeerConnectionFactory.shutdownInternalTracer();
        mPeerConnectionFactory.dispose();

        webSocketClient.close();

        instaListener.onFinished();
    }

    protected void audioMute() {
        mAudioTrack.setEnabled(false);
    }

    protected void audioUnMute() {
        mAudioTrack.setEnabled(true);
    }

    protected void videoMute() {
        mVideoTrack.setEnabled(false);
        JSONObject jsonMain = new JSONObject();
        try {
            jsonMain.put("isVideoMute", true);
            jsonMain.put("id", getRemoteUserId());
            jsonMain.put("type", "video-muted");
            send(jsonMain.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void videoUnMute() {
        mVideoTrack.setEnabled(true);
        JSONObject jsonMain = new JSONObject();
        try {
            jsonMain.put("isVideoMute", false);
            jsonMain.put("id", getRemoteUserId());
            jsonMain.put("type", "video-muted");
            send(jsonMain.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void reConnect() {
        reconnectIfNecessary();
    }

    protected void switchCamera() {
        if (mVideoCapture != null) {
            if (mVideoCapture instanceof CameraVideoCapturer) {
                CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) mVideoCapture;
                cameraVideoCapturer.switchCamera(null);
            }
        }
    }

    public synchronized void reconnectIfNecessary() {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            if (webSocketClient.isClosed() || webSocketClient.isClosing()) {
                try {
                    webSocketClient.reconnectBlocking();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 500, TimeUnit.MICROSECONDS);
    }


    public boolean isOpen() {
        return webSocketClient != null && webSocketClient.isOpen();
    }

    public boolean isClosed() {
        return webSocketClient != null && webSocketClient.isClosed();
    }

    public void send(String msg) {
        if (isOpen()) {
            webSocketClient.send(msg);
        }
    }

    protected void setSpeakerOn() {
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.stopBluetoothSco();
        audioManager.setBluetoothScoOn(false);
        audioManager.setSpeakerphoneOn(true);
    }

    protected void setHeadsetOn() {
        if (isBluetoothHeadsetConnected()) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
        } else {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.stopBluetoothSco();
            audioManager.setBluetoothScoOn(false);
            audioManager.setSpeakerphoneOn(false);
        }

    }

    public boolean isBluetoothHeadsetConnected() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()
                && mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED;
    }

}
