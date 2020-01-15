package com.sty.ne.andfix;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;

import dalvik.system.DexFile;

public class DexManager {
    private static final DexManager ourInstance = new DexManager();

    public static DexManager getInstance() {
        return ourInstance;
    }

    private Context mContext;
    private DexManager() {
    }

    /**
     * 首先要调用该方法
     * @param mContext
     */
    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    /**
     * 加载dex文件
     * @param file 修复好的补丁包（dex文件）
     */
    public void loadDex(File file) {
        try {
            DexFile dexFile = DexFile.loadDex(file.getAbsolutePath(),
                    new File(mContext.getCacheDir(), "opt").getAbsolutePath(),
                    Context.MODE_PRIVATE);
            Enumeration<String> entries = dexFile.entries();
            while(entries.hasMoreElements()) {
                //获取类名
                String className = entries.nextElement();
                //加载类
                //Class<?> aClass = Class.forName(className); //不适用！只能加载安装了的APP中的class
                Class fixedClass = dexFile.loadClass(className, mContext.getClassLoader());
                if(null != fixedClass) {
                    fixClass(fixedClass);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 修复class （里面的method）
     * 核心
     * @param fixedClass
     */
    private void fixClass(Class fixedClass) {
        //获取methods
        Method[] fixedMethods = fixedClass.getDeclaredMethods();
        for (Method fixedMethod : fixedMethods) {
            MethodReplace methodReplace = fixedMethod.getAnnotation(MethodReplace.class);
            if(null == methodReplace) {
                continue;
            }
            //找到注解了（要修复的方法）
            String className = methodReplace.className();
            String methodName = methodReplace.methodName();
            if(!TextUtils.isEmpty(className) && !TextUtils.isEmpty(methodName)) {
                try {
                    //获取带bug的方法所在的class
                    Class<?> bugClass = Class.forName(className);
                    //获取带bug的方法
                    Method bugMethod = bugClass.getDeclaredMethod(methodName, fixedMethod.getParameterTypes());
                    //fixMethod + bugMethod
                    //接下来替换对应的artMethod结构体成员
                    replace(bugMethod, fixedMethod);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private native void replace(Method bugMethod, Method fixedMethod);

    //不用忘记
    static {
        System.loadLibrary("native-lib");
    }
}
