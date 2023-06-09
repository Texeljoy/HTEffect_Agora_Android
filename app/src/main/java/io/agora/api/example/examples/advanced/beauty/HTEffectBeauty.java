package io.agora.api.example.examples.advanced.beauty;

import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.agora.api.example.R;
import io.agora.api.example.common.BaseFragment;
import io.agora.api.example.common.widget.VideoReportLayout;
import io.agora.api.example.databinding.FragmentBeautyHteffectBinding;
import io.agora.api.example.utils.TokenUtils;
import io.agora.base.NV21Buffer;
import io.agora.base.TextureBufferHelper;
import io.agora.base.VideoFrame;
import io.agora.base.internal.video.YuvHelper;
import io.agora.beauty.base.IBeautyHTEffect;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.ColorEnhanceOptions;
import io.agora.rtc2.video.IVideoFrameObserver;
import io.agora.rtc2.video.VideoCanvas;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Random;

public class HTEffectBeauty extends BaseFragment {
    private static final String TAG = "SceneTimeBeauty";
    private static final Matrix IDENTITY_MATRIX = new Matrix();
    private IBeautyHTEffect iBeautyHTEffect;
    private FragmentBeautyHteffectBinding mBinding;
    private RtcEngine rtcEngine;
    private String channelId;
    private boolean isFrontCamera = true;

    private TextureBufferHelper mTextureBufferHelper;

    private VideoReportLayout mLocalVideoLayout;
    private VideoReportLayout mRemoteVideoLayout;
    private boolean isLocalFull = true;
    private IVideoFrameObserver mVideoFrameObserver;
    private IRtcEngineEventHandler mRtcEngineEventHandler;

    private volatile boolean isDestroyed = false;
    private int mFrameRotation;
    private ByteBuffer nv21ByteBuffer;
    private byte[] nv21ByteArray;
    private boolean isRenderInit = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentBeautyHteffectBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!IBeautyHTEffect.hasIntegrated()) {
            mBinding.tvIntegrateTip.setVisibility(View.VISIBLE);
            return;
        }

        channelId = getArguments().getString(getString(R.string.key_channel_name));
        initVideoView();
        initRtcEngine();
        joinChannel();
        mBinding.switchVideoEffect.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            ColorEnhanceOptions options = new ColorEnhanceOptions();
            options.strengthLevel = (float) 0.5f;
            options.skinProtectLevel = (float) 0.5f;
            rtcEngine.setColorEnhanceOptions(isChecked, options);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (rtcEngine != null) {
            rtcEngine.leaveChannel();
        }
        if (mTextureBufferHelper != null) {
            mTextureBufferHelper.invoke(() -> {
                iBeautyHTEffect.release();
                iBeautyHTEffect = null;
                return null;
            });
            boolean disposeSuccess = false;
            while (!disposeSuccess) {
                try {
                    mTextureBufferHelper.dispose();
                    disposeSuccess = true;
                } catch (Exception e) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {
                        // do nothing
                    }
                }
            }
            mTextureBufferHelper = null;
        }
        RtcEngine.destroy();
    }

    @Override
    protected void onBackPressed() {
        isDestroyed = true;
        mBinding.fullVideoContainer.removeAllViews();
        mBinding.smallVideoContainer.removeAllViews();
        super.onBackPressed();

    }

    private void initVideoView() {
        mBinding.cbFaceBeautifyy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (iBeautyHTEffect == null) {
                return;
            }
            iBeautyHTEffect.setFaceBeautifyEnable(isChecked);
        });
        mBinding.cbFaceMakeup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (iBeautyHTEffect == null) {
                return;
            }
            iBeautyHTEffect.setMakeUpEnable(isChecked);
        });
        mBinding.cbFilter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (iBeautyHTEffect == null) {
                return;
            }
            iBeautyHTEffect.setFilterEnable(isChecked);
        });
        mBinding.cbSticker.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (iBeautyHTEffect == null) {
                return;
            }
            iBeautyHTEffect.setStickerEnable(isChecked);
        });
        mBinding.ivCamera.setOnClickListener(v -> {
            rtcEngine.switchCamera();
            isFrontCamera = !isFrontCamera;
        });
        mBinding.smallVideoContainer.setOnClickListener(v -> updateVideoLayouts(!HTEffectBeauty.this.isLocalFull));
    }

    private void initRtcEngine() {
        try {
            mRtcEngineEventHandler = new IRtcEngineEventHandler() {
                @Override
                public void onError(int err) {
                    super.onError(err);
                    showLongToast(String.format(Locale.US, "msg:%s, code:%d", RtcEngine.getErrorDescription(err), err));
                }

                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    super.onJoinChannelSuccess(channel, uid, elapsed);
                    mLocalVideoLayout.setReportUid(uid);
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    super.onUserJoined(uid, elapsed);
                    runOnUIThread(() -> {
                        if (mRemoteVideoLayout == null) {
                            mRemoteVideoLayout = new VideoReportLayout(requireContext());
                            mRemoteVideoLayout.setReportUid(uid);
                            TextureView videoView = new TextureView(requireContext());
                            rtcEngine.setupRemoteVideo(new VideoCanvas(videoView, Constants.RENDER_MODE_HIDDEN, uid));
                            mRemoteVideoLayout.addView(videoView);
                            updateVideoLayouts(isLocalFull);
                        }
                    });
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    super.onUserOffline(uid, reason);
                    runOnUIThread(() -> {
                        if (mRemoteVideoLayout != null && mRemoteVideoLayout.getReportUid() == uid) {
                            mRemoteVideoLayout.removeAllViews();
                            mRemoteVideoLayout = null;
                            updateVideoLayouts(isLocalFull);
                        }
                    });
                }

                @Override
                public void onLocalAudioStats(LocalAudioStats stats) {
                    super.onLocalAudioStats(stats);
                    runOnUIThread(() -> mLocalVideoLayout.setLocalAudioStats(stats));
                }

                @Override
                public void onLocalVideoStats(Constants.VideoSourceType source, LocalVideoStats stats) {
                    super.onLocalVideoStats(source, stats);
                    runOnUIThread(() -> mLocalVideoLayout.setLocalVideoStats(stats));
                }

                @Override
                public void onRemoteAudioStats(RemoteAudioStats stats) {
                    super.onRemoteAudioStats(stats);
                    if (mRemoteVideoLayout != null) {
                        runOnUIThread(() -> mRemoteVideoLayout.setRemoteAudioStats(stats));
                    }
                }

                @Override
                public void onRemoteVideoStats(RemoteVideoStats stats) {
                    super.onRemoteVideoStats(stats);
                    if (mRemoteVideoLayout != null) {
                        runOnUIThread(() -> mRemoteVideoLayout.setRemoteVideoStats(stats));
                    }
                }
            };
            rtcEngine = RtcEngine.create(getContext(), getString(R.string.agora_app_id), mRtcEngineEventHandler);

            if (rtcEngine == null) {
                return;
            }
            rtcEngine.enableExtension("agora_video_filters_clear_vision", "clear_vision", true);


            mVideoFrameObserver = new IVideoFrameObserver() {
                @Override
                public boolean onCaptureVideoFrame(int sourceType, VideoFrame videoFrame) {
                    return processBeauty(videoFrame);
                }

                @Override
                public boolean onPreEncodeVideoFrame(int sourceType, VideoFrame videoFrame) {
                    return false;
                }

                @Override
                public boolean onMediaPlayerVideoFrame(VideoFrame videoFrame, int mediaPlayerId) {
                    return false;
                }

                @Override
                public boolean onRenderVideoFrame(String channelId, int uid, VideoFrame videoFrame) {
                    return false;
                }

                @Override
                public int getVideoFrameProcessMode() {
                    return IVideoFrameObserver.PROCESS_MODE_READ_WRITE;
                }

                @Override
                public int getVideoFormatPreference() {
                    return IVideoFrameObserver.VIDEO_PIXEL_DEFAULT;
                }

                @Override
                public boolean getRotationApplied() {
                    return false;
                }

                @Override
                public boolean getMirrorApplied() {
                    return false;
                }

                @Override
                public int getObservedFramePosition() {
                    return IVideoFrameObserver.POSITION_POST_CAPTURER;
                }
            };
            rtcEngine.registerVideoFrameObserver(mVideoFrameObserver);
            rtcEngine.enableVideo();
            rtcEngine.disableAudio();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean processBeauty(VideoFrame videoFrame) {
        if (isDestroyed) {
            return false;
        }
        VideoFrame.Buffer buffer = videoFrame.getBuffer();
        if (!(buffer instanceof VideoFrame.TextureBuffer)) {
            return false;
        }

        VideoFrame.TextureBuffer texBuffer = (VideoFrame.TextureBuffer) buffer;

        if (mTextureBufferHelper == null) {
            doOnBeautyCreatingBegin();
            mTextureBufferHelper = TextureBufferHelper.create("STRender", texBuffer.getEglBaseContext());
            mTextureBufferHelper.invoke(() -> {
                iBeautyHTEffect = IBeautyHTEffect.create(getContext());
                return null;
            });
            doOnBeautyCreatingEnd();
        }

        // VideoFrame.I420Buffer i420Buffer = buffer.toI420();
        // int width = i420Buffer.getWidth();
        // int height = i420Buffer.getHeight();
        //
        // int nv21MinSize = (int) ((width * height * 3 + 1) / 2.0f);
        // if(nv21ByteBuffer == null || nv21ByteBuffer.capacity() < nv21MinSize){
        //     nv21ByteBuffer = ByteBuffer.allocateDirect(nv21MinSize);
        //     nv21ByteArray = new byte[nv21MinSize];
        // }
        // YuvHelper.I420ToNV12(i420Buffer.getDataY(), i420Buffer.getStrideY(),
        //     i420Buffer.getDataV(), i420Buffer.getStrideV(),
        //     i420Buffer.getDataU(), i420Buffer.getStrideU(),
        //     nv21ByteBuffer, width, height);
        // nv21ByteBuffer.position(0);
        // nv21ByteBuffer.get(nv21ByteArray);
        // i420Buffer.release();


        int width = buffer.getWidth();
        int height = buffer.getHeight();

        int nv21Size = (int) (width * height * 3.0f / 2.0f + 0.5f);
        if (nv21ByteBuffer == null || nv21ByteBuffer.capacity() != nv21Size) {
            if (nv21ByteBuffer != null) {
                nv21ByteBuffer.clear();
            }
            nv21ByteBuffer = ByteBuffer.allocateDirect(nv21Size);
            nv21ByteArray = new byte[nv21Size];

        }

        VideoFrame.I420Buffer i420Buffer = buffer.toI420();
        YuvHelper.I420ToNV12(i420Buffer.getDataY(), i420Buffer.getStrideY(),
            i420Buffer.getDataV(), i420Buffer.getStrideV(),
            i420Buffer.getDataU(), i420Buffer.getStrideU(),
            nv21ByteBuffer, width, height);
        nv21ByteBuffer.position(0);
        nv21ByteBuffer.get(nv21ByteArray);
        i420Buffer.release();


        if (!isRenderInit) {
            //参数format的值可参考HTFormatEnum类中的枚举值
            isRenderInit = mTextureBufferHelper.invoke(() ->iBeautyHTEffect.initBufferRenderer(
                5,
                width, height,
                mFrameRotation, isFrontCamera, 5
            ));
        }

        byte[] dstnv21 = mTextureBufferHelper.invoke(() -> iBeautyHTEffect.process(nv21ByteArray));


        // drag one frame to avoid reframe when switching camera.
        if(mFrameRotation != videoFrame.getRotation()){
            mFrameRotation = videoFrame.getRotation();
            return false;
        }


        videoFrame.replaceBuffer(new NV21Buffer(dstnv21, width, height, null), mFrameRotation, videoFrame.getTimestampNs());
        return true;
    }

    private void joinChannel() {
        int uid = new Random(System.currentTimeMillis()).nextInt(1000) + 10000;
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        TokenUtils.gen(requireActivity(), channelId, uid, token -> {
            int ret = rtcEngine.joinChannel(token, channelId, uid, options);
            if (ret != Constants.ERR_OK) {
                showAlert(String.format(Locale.US, "%s\ncode:%d", RtcEngine.getErrorDescription(ret), ret));
            }
        });

        mLocalVideoLayout = new VideoReportLayout(requireContext());
        TextureView videoView = new TextureView(requireContext());
        VideoCanvas local = new VideoCanvas(videoView, Constants.RENDER_MODE_HIDDEN, 0);
        local.mirrorMode = Constants.VIDEO_MIRROR_MODE_DISABLED;
        rtcEngine.setupLocalVideo(local);
        mLocalVideoLayout.addView(videoView);
        rtcEngine.startPreview();

        updateVideoLayouts(isLocalFull);
    }

    private void updateVideoLayouts(boolean isLocalFull) {
        this.isLocalFull = isLocalFull;
        if (isLocalFull) {
            if (mLocalVideoLayout != null) {
                ViewParent parent = mLocalVideoLayout.getParent();
                if (parent instanceof ViewGroup && parent != mBinding.fullVideoContainer) {
                    ((ViewGroup) parent).removeView(mLocalVideoLayout);
                    mBinding.fullVideoContainer.addView(mLocalVideoLayout);
                } else if (parent == null) {
                    mBinding.fullVideoContainer.addView(mLocalVideoLayout);
                }
            }

            if (mRemoteVideoLayout != null) {
                mRemoteVideoLayout.getChildAt(0).setOnClickListener(v -> updateVideoLayouts(!HTEffectBeauty.this.isLocalFull));
                ViewParent parent = mRemoteVideoLayout.getParent();
                if (parent instanceof ViewGroup && parent != mBinding.smallVideoContainer) {
                    ((ViewGroup) parent).removeView(mRemoteVideoLayout);
                    mBinding.smallVideoContainer.addView(mRemoteVideoLayout);
                } else if(parent == null){
                    mBinding.smallVideoContainer.addView(mRemoteVideoLayout);
                }
            }
        } else {
            if (mLocalVideoLayout != null) {
                mLocalVideoLayout.getChildAt(0).setOnClickListener(v -> updateVideoLayouts(!HTEffectBeauty.this.isLocalFull));
                ViewParent parent = mLocalVideoLayout.getParent();
                if (parent instanceof ViewGroup && parent != mBinding.smallVideoContainer) {
                    ((ViewGroup) parent).removeView(mLocalVideoLayout);
                    mBinding.smallVideoContainer.addView(mLocalVideoLayout);
                } else if(parent == null){
                    mBinding.smallVideoContainer.addView(mLocalVideoLayout);
                }
            }

            if (mRemoteVideoLayout != null) {
                ViewParent parent = mRemoteVideoLayout.getParent();
                if (parent instanceof ViewGroup && parent != mBinding.fullVideoContainer) {
                    ((ViewGroup) parent).removeView(mRemoteVideoLayout);
                    mBinding.fullVideoContainer.addView(mRemoteVideoLayout);
                } else if(parent == null) {
                    mBinding.fullVideoContainer.addView(mRemoteVideoLayout);
                }
            }
        }
    }

    private void doOnBeautyCreatingBegin() {
        Log.d(TAG, "doOnBeautyCreatingBegin...");
    }

    private void doOnBeautyCreatingEnd() {
        Log.d(TAG, "doOnBeautyCreatingEnd.");
        runOnUIThread(() -> {
            mBinding.cbFaceBeautifyy.setChecked(false);
            mBinding.cbFaceMakeup.setChecked(false);
            mBinding.cbFilter.setChecked(false);
            mBinding.cbSticker.setChecked(false);
        });
    }

    private void doOnBeautyReleasingBegin() {
        Log.d(TAG, "doOnBeautyReleasingBegin...");
    }

    private void doOnBeautyReleasingEnd() {
        Log.d(TAG, "doOnBeautyReleasingEnd.");
    }
}
