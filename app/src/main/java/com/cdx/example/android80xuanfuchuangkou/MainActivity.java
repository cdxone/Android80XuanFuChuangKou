package com.cdx.example.android80xuanfuchuangkou;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.cdx.example.android80xuanfuchuangkou.utils.FloatWindowPermissionChecker;
import com.cdx.example.android80xuanfuchuangkou.view.HDLNotification;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * 显示悬浮窗口
     * @param view
     */
    public void startWindow(View view) {
        if (!FloatWindowPermissionChecker.checkFloatWindowPermission(MyApplication.getContext())) {
            FloatWindowPermissionChecker.askForFloatWindowPermission(this);
            return;
        } else {
            final HDLNotification notification =
                    new HDLNotification.Builder().setContext(this)
                            .setTime(System.currentTimeMillis())
                            .setImgRes(R.drawable.bg)
                            .setTitle("titletitlelkjlkj")
                            .setContent("contentcontentldksjflakds")
                            .build();
            notification.show();
        }
    }
}
