package com.zeno.incrementupdate.ndk;

/**
 * Created by Zeno on 2016/9/12.
 */

public class BspatchJNI {

    /**
     * 合并增量文件
     * @param oldFilePath 当前APK路径
     * @param newFilePath 合成后的新的APK路径
     * @param patchFilePath 增量文件路径
     */
    public static native void bspatchJNI(String oldFilePath,String newFilePath,String patchFilePath) ;

    static {
        System.loadLibrary("bspatch");
    }
}
