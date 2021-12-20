package com.imooc.butterknife;

import android.app.Activity;

import java.lang.reflect.Constructor;

public class ButterKnife {
    public static Unbinder bind(Activity activity){
        try {
            Class<? extends Unbinder> bindClassName = (Class<? extends Unbinder>)
                    Class.forName(activity.getClass().getCanonicalName() + "_ViewBinding");
            // 构造函数
            Constructor<? extends Unbinder> bindConstructor =
                    bindClassName.getDeclaredConstructor(activity.getClass());
            Unbinder unbinder = bindConstructor.newInstance(activity);
            return unbinder;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Unbinder.EMPTY;
    }
}
