package com.pzj.floatingview;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.Toast;

/**
 * 悬浮窗Service
 *
 * @author PengZhenjin
 * @date 2017-6-5
 */
public class FloatingViewService extends Service implements FloatingViewListener {

    private static final String TAG = "FloatingViewService";

    /**
     * FloatingViewManager
     */
    private FloatingViewManager mFloatingViewManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (this.mFloatingViewManager != null) {
            return START_STICKY;
        }

        Log.d(TAG, "悬浮窗Service已启动");

        View floatView = LayoutInflater.from(this).inflate(R.layout.call_float_view, null, false);
        Chronometer ch = (Chronometer) floatView.findViewById(R.id.call_time_ch);
        ch.start();
        floatView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(FloatingViewService.this, "点击了悬浮窗", Toast.LENGTH_SHORT).show();
            }
        });

        DisplayMetrics dm = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(dm);

        this.mFloatingViewManager = new FloatingViewManager(this, this);

        FloatingViewManager.Configs configs = new FloatingViewManager.Configs();
        configs.floatingViewX = dm.widthPixels / 2;
        configs.floatingViewY = dm.heightPixels / 4;
        configs.overMargin = -(int) (8 * dm.density);

        this.mFloatingViewManager.addFloatingView(floatView, configs);

        return START_REDELIVER_INTENT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        destroyFloatingView();
        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFinishFloatingView() {
        stopSelf();
    }

    /**
     * 销毁悬浮窗
     */
    private void destroyFloatingView() {
        if (this.mFloatingViewManager != null) {
            this.mFloatingViewManager.removeAllFloatingView();
            this.mFloatingViewManager = null;
        }
        Log.d(TAG, "悬浮窗已销毁");
    }
}
