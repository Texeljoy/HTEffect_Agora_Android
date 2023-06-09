package io.agora.beauty.base;

import android.content.Context;
import java.lang.reflect.Constructor;

public interface IBeautyHTEffect {

    boolean initBufferRenderer(int format, int width, int height, int rotation, boolean isFrontCamera, int faceNumber);

    byte[] process(byte[] nv21);

    void release();

    void setFaceBeautifyEnable(boolean enable);
    void setMakeUpEnable(boolean enable);

    void setFilterEnable(boolean enable);

    void setStickerEnable(boolean enable);

    static boolean hasIntegrated(){
        try {
            Class.forName("io.agora.beauty.hteffect.BeautyHTEffectImpl");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static IBeautyHTEffect create(Context context) {
        try {
            Class<?> implClazz = Class.forName("io.agora.beauty.hteffect.BeautyHTEffectImpl");
            Constructor<?> constructor = implClazz.getConstructor(Context.class);
            return (IBeautyHTEffect) constructor.newInstance(context);
        } catch (Exception e) {
            return null;
        }
    }

}
