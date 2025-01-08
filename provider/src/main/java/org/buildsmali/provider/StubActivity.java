package org.buildsmali.provider;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;


public class StubActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //启动后跳转viewer
        startViewerActivity();
        finish();
    }

    private void startViewerActivity() {
        Intent intent = getPackageManager().getLaunchIntentForPackage("org.buildsmali.viewer");
        if (intent != null)
            startActivity(intent);

    }

    public static String getApkPath(Context context, String packageName) {
        try {
            //需要在manifest指定对应包名的query,才能获取到info
            return context.getPackageManager().getApplicationInfo(packageName, 0).sourceDir;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}