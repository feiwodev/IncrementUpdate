# IncrementUpdate
IncrementUpdate

接续上篇[NDK开发基础③增量更新之服务器端生成差分包](http://www.jianshu.com/p/47d6c115f7ca)

### 前情提要
增量更新原理就是在服务器端使用bsdiff进行文件内容比较，再使用了bzip2进行文件压缩 ， 在下载APP时可以减少用户流量 。在客户端 ， 则是将下载好的拆分包与现有的APK进行文件合并 ， 得出新的APK， 再进行安装 。

### 生产资源及工具
bsdiff --- [bsdiff ](http://www.daemonology.net/bsdiff/bsdiff-4.3.tar.gz)  生成差分包及合并差分包库 ， 使用`bspatch.c`文件
bzip2  --- [bzip2](http://www.bzip.org/downloads.html) bsdiff 依赖
服务器 --- [Tomcat 7.0 ](http://tomcat.apache.org/tomcat-7.0-doc/index.html) （模拟网络环境）放置差分包 ， 供APP下载
开发工具 --- Android Studio 2.2RC2 NDK开发 

### 一 ， 合并差分包

> Ⅰ 提取bzip2中的源文件

![bzip2](http://upload-images.jianshu.io/upload_images/643851-35bd3d402c5d3e60.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

> Ⅱ 将`bzip2`加入到Android Studio项目中

首先将工程切换到Project模式 ， 将`bzip2`文件夹复制到cpp目录下 。因为最新的Android Studio采用的是CMake构建工具 ， 所有需要在`bzip2`目录下，创建一个`CMakeLists.txt`文件：

![bzip2 cmake](http://upload-images.jianshu.io/upload_images/643851-1212805081f9ac72.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

> Ⅲ 将`bspatch.c`复制到cpp目录下 ， 并将自动生成的`CMakeList.txt`文件拖拽到cpp目录下 ， 并添加子目录参与编译 。

![bspatch cmake](http://upload-images.jianshu.io/upload_images/643851-a9f2aa27fb2fabac.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

修改了`CMakeLists.txt`文件的路径之后 ， 需要在`build.gradle`中修改一下配置了：

![build.gradle](http://upload-images.jianshu.io/upload_images/643851-bb27f1ff4abf50ec.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

并且配置一下build环境

![build](http://upload-images.jianshu.io/upload_images/643851-ec8788547963af16.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)



> Ⅳ 编写JNI

```java
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
```

> Ⅴ 编写C函数 ， 怎样找执行函数 ， 上一篇已经说了 ， 套路都是一样的 。

```c
/*合并APK*/
JNIEXPORT void JNICALL
Java_com_zeno_incrementupdate_ndk_BspatchJNI_bspatchJNI(JNIEnv *env, jclass type,
														jstring oldFilePath_, jstring newFilePath_,
														jstring patchFilePath_) {
	const char *oldFilePath = (*env)->GetStringUTFChars(env, oldFilePath_, 0);
	const char *newFilePath = (*env)->GetStringUTFChars(env, newFilePath_, 0);
	const char *patchFilePath = (*env)->GetStringUTFChars(env, patchFilePath_, 0);


	// if(argc!=4) errx(1,"usage: %s oldfile newfile patchfile\n",argv[0]);

	int argc = 4 ;
	char* argv[4] ;
	argv[0] = "bspatch";
	argv[1] = oldFilePath;
	argv[2] = newFilePath;
	argv[3] = patchFilePath;

	bspatch_main(argc,argv);

	LOGE("MainActivity","%s","合并APK完成");

	(*env)->ReleaseStringUTFChars(env, oldFilePath_, oldFilePath);
	(*env)->ReleaseStringUTFChars(env, newFilePath_, newFilePath);
	(*env)->ReleaseStringUTFChars(env, patchFilePath_, patchFilePath);
}
```
需要注意的时 ， 在`bspatch.c`中是需要引入`bzip2`的 ， 所有需要在文件头部， 引入`bzip2` :

```c
// bzip2
#include "bzip2/bzlib.c"
#include "bzip2/crctable.c"
#include "bzip2/compress.c"
#include "bzip2/decompress.c"
#include "bzip2/randtable.c"
#include "bzip2/blocksort.c"
#include "bzip2/huffman.c"

#define LOGE(TAG,FORMAT,...) __android_log_print(ANDROID_LOG_ERROR,TAG,FORMAT,__VA_ARGS__)
```
> Ⅵ 使用

```java
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
```
使用起来都比较简单 ， 这里就不将代码贴全了，篇末会给出github地址。

> Ⅶ 打包

因为Android Studio使用了`instant run`技术 ， 所以使用Android Studio生成APK最好是打正式包 ， 并且包中内容要有差异性 ， 然后再生成差分包 ， 直接放置在WEB项目的`WebContent`根目录下即可 。

### 结语
增量更新 ， 从服务器端到客户端实现 ， 要写的代码其实不多 ， 关键在于使用第三方C/C++源码的套路 ， 使用JNI技术调用C/C++函数 ， 其关键点就是找执行函数，通常为`main`函数 。NDK开发基础 ， 这一篇算是结尾 ， 新版的Android Studio的NDK支持比较完善 ， 使用了CMake进行项目构建 ，语法高亮以及语法提示 ， 都做得相当的好了 。开始下一个系列 ， C++开发 。

###参考
[CMake Practice 百度网盘](http://pan.baidu.com/s/1i4IRcLv) 密码: 58a3