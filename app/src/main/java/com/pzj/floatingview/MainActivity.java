package com.pzj.floatingview;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = "MainActivity";

    private Button mCreateBtn;
    private Button mDestroyBtn;

    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.mCreateBtn = (Button) findViewById(R.id.create_btn);
        this.mDestroyBtn = (Button) findViewById(R.id.destroy_btn);
        this.mCreateBtn.setOnClickListener(this);
        this.mDestroyBtn.setOnClickListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.create_btn: {
                showFloatingView(this);
                break;
            }
            case R.id.destroy_btn: {
                stopService(new Intent(this, FloatingViewService.class));
                break;
            }
        }
    }

    /**
     * 显示悬浮窗
     *
     * @param context
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void showFloatingView(Context context) {
        Log.i(TAG, "手机系统版本：" + Build.VERSION.SDK_INT);
        // API22以下直接启动
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            context.startService(new Intent(context, FloatingViewService.class));
        }
        else {
            if (!Settings.canDrawOverlays(MainActivity.this)) {
                showDialog();
            }
            else {
                context.startService(new Intent(context, FloatingViewService.class));
            }
        }
    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("请开启悬浮窗权限");
        builder.setPositiveButton("开启", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + MainActivity.this.getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            showFloatingView(this);
        }
    }
}
