package com.zeno.incrementupdate;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.zeno.incrementupdate.ndk.APKUtils;
import com.zeno.incrementupdate.ndk.BspatchJNI;
import com.zeno.incrementupdate.ndk.Constants;
import com.zeno.incrementupdate.ndk.DownloadUtils;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new ApkUpdateTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    class ApkUpdateTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                //1.下载差分包
                Log.e(TAG, "doInBackground: 正在下载。。。。" );
                File patchFile = DownloadUtils.download(Constants.URL_PATCH_DOWNLOAD);

                //获取当前应用的apk文件/data/app/app
                String oldFile = APKUtils.getSourceApkPath(MainActivity.this, getPackageName());
                //2.合并得到最新版本的APK文件
                String newApkPath = Constants.NEW_APK_PATH;
                String patchFileAbsolutePath = patchFile.getAbsolutePath();
                BspatchJNI.bspatchJNI(oldFile, newApkPath, patchFileAbsolutePath);

                Log.d(TAG, "oldfile:"+oldFile);
                Log.d(TAG, "newfile:"+newApkPath);
                Log.d(TAG, "patchfile:"+patchFileAbsolutePath);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setTitle("正在下载...");
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            //3.安装
            if(result){
                Toast.makeText(MainActivity.this, "您正在进行无流量更新", Toast.LENGTH_SHORT).show();
                APKUtils.installApk(MainActivity.this, Constants.NEW_APK_PATH);
            }
        }

    }
}
