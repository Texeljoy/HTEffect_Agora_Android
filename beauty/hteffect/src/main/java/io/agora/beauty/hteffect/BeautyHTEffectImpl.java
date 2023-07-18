package io.agora.beauty.hteffect;

import android.content.Context;
import com.texeljoy.hteffect.HTEffect;
import com.texeljoy.hteffect.HTEffect.InitCallback;
import com.texeljoy.hteffect.model.HTBeautyEnum;
import com.texeljoy.hteffect.model.HTFilterEnum;
import com.texeljoy.hteffect.model.HTFormatEnum;
import com.texeljoy.hteffect.model.HTItemEnum;
import com.texeljoy.hteffect.model.HTReshapeEnum;
import com.texeljoy.hteffect.model.HTRotationEnum;
import io.agora.beauty.base.IBeautyHTEffect;

public class BeautyHTEffectImpl implements IBeautyHTEffect {


    private HTEffect mHteffect = HTEffect.shareInstance();

    private volatile boolean isReleased = false;


    public BeautyHTEffectImpl(Context context) {
        initSdk(context);
    }

    private void initSdk(Context context) {
        mHteffect.initHTEffect(context.getApplicationContext(), "YOUR_APP_ID", new InitCallback() {
            @Override public void onInitSuccess() {

            }

            @Override public void onInitFailure() {

            }
        });
    }

    @Override public boolean initBufferRenderer(int format, int width, int height, int rotation, boolean isFrontCamera, int faceNumber) {
        return HTEffect.shareInstance().initBufferRenderer(HTFormatEnum.fromValue(format), width, height, HTRotationEnum.fromValue(rotation), isFrontCamera, faceNumber);
    }

    @Override public byte[] process(byte[] nv21) {
        mHteffect.processBuffer(nv21);
        return nv21;


    }

    @Override public void release() {
        isReleased = true;
        mHteffect.releaseBufferRenderer();

    }

    @Override public void setFaceBeautifyEnable(boolean enable) {
        if (isReleased) {
            return;
        }
        if (enable) {
            mHteffect.setBeauty(HTBeautyEnum.HTBeautyClearSmoothing.getValue(), 70);// 磨皮 100
            mHteffect.setBeauty(HTBeautyEnum.HTBeautySkinWhitening.getValue(), 70);// 美白 100
            mHteffect.setBeauty(HTBeautyEnum.HTBeautyImageSharpness.getValue(), 70);// 清晰 100
        } else {
            mHteffect.setBeauty(HTBeautyEnum.HTBeautyClearSmoothing.getValue(), 0);// 磨皮 0
            mHteffect.setBeauty(HTBeautyEnum.HTBeautySkinWhitening.getValue(), 0);// 美白 0
            mHteffect.setBeauty(HTBeautyEnum.HTBeautyImageSharpness.getValue(), 0);// 清晰 0
        }

    }

    @Override public void setMakeUpEnable(boolean enable) {
        if (isReleased) {
            return;
        }
        if (enable) {
            mHteffect.setReshape(HTReshapeEnum.HTReshapeCheekThinning.getValue(),70);//瘦脸 70
            mHteffect.setReshape(HTReshapeEnum.HTReshapeEyeEnlarging.getValue(),70);//大眼 70

        } else {
            mHteffect.setReshape(HTReshapeEnum.HTReshapeCheekThinning.getValue(),0);//瘦脸 0
            mHteffect.setReshape(HTReshapeEnum.HTReshapeEyeEnlarging.getValue(),0);//大眼 0
        }

    }

    @Override public void setFilterEnable(boolean enable) {
        if (isReleased) {
            return;
        }
        if (enable) {
            mHteffect.setFilter(HTFilterEnum.HTFilterBeauty.getValue(), "ziran3");//风格滤镜 自然3

        } else {
            mHteffect.setFilter(HTFilterEnum.HTFilterBeauty.getValue(), "");

        }

    }

    @Override public void setStickerEnable(boolean enable) {
        if (isReleased) {
            return;
        }
        if (enable) {
            mHteffect.setARItem(HTItemEnum.HTItemSticker.getValue(), "ht_sticker_effect_rabbit_bowknot");//贴纸 兔子蝴蝶结

        } else {
            mHteffect.setARItem(HTItemEnum.HTItemSticker.getValue(), "");

        }

    }
}
