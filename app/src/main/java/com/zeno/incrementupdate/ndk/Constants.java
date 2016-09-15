package com.zeno.incrementupdate.ndk;

import android.os.Environment;

import java.io.File;

public class Constants {

	public static final String PATCH_FILE = "App_patch.patch";
	public static final String URL_PATCH_DOWNLOAD = "http://192.168.0.8:8080/AppUpdateServer/"+PATCH_FILE;
	
	public static final String SD_CARD = Environment.getExternalStorageDirectory() + File.separator;
	
	//新版本apk的目录
	public static final String NEW_APK_PATH = SD_CARD+"incrementupdate.apk";

	
}
