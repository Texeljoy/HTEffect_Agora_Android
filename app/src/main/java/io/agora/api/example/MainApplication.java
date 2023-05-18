package io.agora.api.example;

import android.app.Application;

import com.texeljoy.hteffect.HTEffect;
import com.texeljoy.hteffect.HTEffect.InitCallback;

import java.lang.annotation.Annotation;
import java.util.Set;

import io.agora.api.example.annotation.Example;
import io.agora.api.example.common.model.Examples;
import io.agora.api.example.common.model.GlobalSettings;
import io.agora.api.example.utils.ClassUtils;

public class MainApplication extends Application {

    private GlobalSettings globalSettings;

    @Override
    public void onCreate() {
        super.onCreate();
        initExamples();
        //todo --- hteffect start
        #请联系商务获取美颜授权码key
        HTEffect.shareInstance().initHTEffect(getApplicationContext(), "YOUR_APP_ID", new InitCallback() {
            @Override public void onInitSuccess() {

            }

            @Override public void onInitFailure() {

            }
        });
        //todo --- hteffect end

    }

    private void initExamples() {
        try {
            Set<String> packageName = ClassUtils.getFileNameByPackageName(this, "io.agora.api.example.examples");
            for (String name : packageName) {
                Class<?> aClass = Class.forName(name);
                Annotation[] declaredAnnotations = aClass.getAnnotations();
                for (Annotation annotation : declaredAnnotations) {
                    if (annotation instanceof Example) {
                        Example example = (Example) annotation;
                        Examples.addItem(example);
                    }
                }
            }
            Examples.sortItem();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public GlobalSettings getGlobalSettings() {
        if(globalSettings == null){
            globalSettings = new GlobalSettings();
        }
        return globalSettings;
    }
}
