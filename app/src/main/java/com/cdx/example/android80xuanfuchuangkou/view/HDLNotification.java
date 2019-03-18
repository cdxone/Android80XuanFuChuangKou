package com.cdx.example.android80xuanfuchuangkou.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.IntEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.cdx.example.android80xuanfuchuangkou.MyApplication;
import com.cdx.example.android80xuanfuchuangkou.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

/**
 * Created by： lgz
 * Time： 2018/12/24
 * Desc：通知悬浮窗
 */

public class HDLNotification implements View.OnTouchListener {

    private static final int DIRECTION_LEFT = -1;//向左的方向
    private static final int DIRECTION_NONE = 0;//没有方向
    private static final int DIRECTION_RIGHT = 1;//向右的方向
    private static final int DIRECTION_UP = 2;//向上的方向

    private static final int DISMISS_INTERVAL = 5000;

    private WindowManager mWindowManager;//窗口管理器
    private WindowManager.LayoutParams mWindowParams;//窗口管理参数
    private View mContentView;//内容View
    private Context mContext;//上下文
    private int mScreenWidth;//屏幕的宽度
    private int mStatusBarHeight;//状态栏的高度，也就是导航栏的高度

    private boolean isShowing = false;
    private ValueAnimator restoreAnimator = null;
    private ValueAnimator dismissAnimator = null;
    private ImageView mIvIcon;//图标
    private TextView mTvTitle;//标题
    private TextView mTvContent;//内容
    private TextView mTvTime;//时间

    private int mDownX = 0;//手指按下的位置
    private int mDownY = 0;//手指按下的位置
    private int direction = DIRECTION_NONE;//手指移动的方向


    public HDLNotification(Builder builder) {
        mContext = MyApplication.getContext();
        mStatusBarHeight = getStatusBarHeight();
        mScreenWidth = mContext.getResources().getDisplayMetrics().widthPixels;

        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mWindowParams = new WindowManager.LayoutParams();
        //为了兼容各个版本 Android6.0,Android7.0,Android8.0关于弹出Window有不同的一个机制。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mWindowParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            mWindowParams.type = WindowManager.LayoutParams.TYPE_TOAST;// 系统提示window
        } else {
            mWindowParams.type = TYPE_SYSTEM_ALERT;
        }
        mWindowParams.gravity = Gravity.LEFT | Gravity.TOP;
        mWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.flags =
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        //设置进入和退出动画
        mWindowParams.windowAnimations = R.style.NotificationAnim;
        //Android的坐标系不包括状态栏，但是包括ActionBar。
        mWindowParams.x = 0;
        mWindowParams.y = -mStatusBarHeight;

        setContentView(mContext, builder);
    }


    private static final int HIDE_WINDOW = 0;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case HIDE_WINDOW:
                    dismiss();
                    break;
            }
            return false;
        }
    });

    /***
     * 设置内容视图
     *
     * @param context
     */
    private void setContentView(Context context, Builder builder) {
        mContentView = LayoutInflater.from(context).inflate(R.layout.layout_notification, null);
        //设置空View的高度为状态栏的高度
        View v_state_bar = mContentView.findViewById(R.id.v_state_bar);
        ViewGroup.LayoutParams layoutParameter = v_state_bar.getLayoutParams();
        layoutParameter.height = mStatusBarHeight;
        v_state_bar.setLayoutParams(layoutParameter);

        mIvIcon = mContentView.findViewById(R.id.iv_icon);
        mTvTitle = mContentView.findViewById(R.id.tv_title);
        mTvContent = mContentView.findViewById(R.id.tv_content);
        mTvTime = mContentView.findViewById(R.id.tv_time);

        setIcon(builder.imgRes);
        setTitle(builder.title);
        setContent(builder.content);
        setTime(builder.time);

        mContentView.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (isAnimatorRunning()) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = (int) event.getRawX();
                mDownY = (int) event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                //处于滑动状态就取消自动消失
                mHandler.removeMessages(HIDE_WINDOW);
                //记录移动的长度
                int moveX = (int) event.getRawX() - mDownX;
                int moveY = (int) event.getRawY() - mDownY;
                //水平方向和竖直方向
                if (Math.abs(moveY) >= Math.abs(moveX)){
                    //向上滑动:更新Window的位置。
                    if (moveY < 0){
                        direction = DIRECTION_UP;
                        updateWindowLocation(mWindowParams.x,moveY-mStatusBarHeight);
                    }
                } else {
                    //如果是向右
                    if (moveX > 0) {
                        direction = DIRECTION_RIGHT;
                    } else {
                        //如果是向左
                        direction = DIRECTION_LEFT;
                    }
                    updateWindowLocation(moveX, mWindowParams.y);
                }
                break;
            case MotionEvent.ACTION_UP:
                //如果是向上，那么开启消失的动画
                if (direction == DIRECTION_UP) {
                    startDismissAnimator(direction);
                } else {
                    //如果window向左移动了超过屏幕的一半或者向右移动了屏幕的一半，那么就让这个window消失。
                    if (Math.abs(mWindowParams.x) > mScreenWidth / 2) {
                        startDismissAnimator(direction);
                    } else {
                        //如果window处在屏幕中间的左边，那么让这个window消失。
                        startRestoreAnimator();
                    }
                }
                break;
        }
        return true;
    }

    private void startRestoreAnimator() {
        restoreAnimator = new ValueAnimator().ofInt(mWindowParams.x, 0);
        restoreAnimator.setDuration(300);
        restoreAnimator.setEvaluator(new IntEvaluator());

        restoreAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                System.out.println("onAnimationUpdate:" + animation.getAnimatedValue());
                updateWindowLocation((Integer) animation.getAnimatedValue(), -mStatusBarHeight);
            }
        });
        restoreAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                restoreAnimator = null;
                autoDismiss();
            }
        });
        restoreAnimator.start();
    }

    /**
     * 开启消失动画
     * @param direction
     */
    private void startDismissAnimator(int direction) {
        if (direction == DIRECTION_UP){//向上滑动
            //移除屏幕动画
            dismissAnimator = new ValueAnimator().ofInt(mWindowParams.y,-mStatusBarHeight - mContentView.getHeight());
            dismissAnimator.setDuration(300);
            dismissAnimator.setEvaluator(new IntEvaluator());
            dismissAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    //水平位置不变，竖直位置变化。
                    updateWindowLocation(0, (Integer) animation.getAnimatedValue());
                }
            });
            dismissAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    //动画结束的时候，从windowManager中移除View.
                    restoreAnimator = null;
                    dismiss();
                }
            });
            dismissAnimator.start();
        } else {
            if (direction == DIRECTION_LEFT)
                dismissAnimator = new ValueAnimator().ofInt(mWindowParams.x, -mScreenWidth);
            else {
                dismissAnimator = new ValueAnimator().ofInt(mWindowParams.x, mScreenWidth);
            }
            dismissAnimator.setDuration(300);
            dismissAnimator.setEvaluator(new IntEvaluator());

            dismissAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    updateWindowLocation((Integer) animation.getAnimatedValue(), -mStatusBarHeight);
                }
            });
            dismissAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    restoreAnimator = null;
                    dismiss();
                }
            });
            dismissAnimator.start();
        }
    }

    /**
     * 判断动画是否进行
     * @return
     */
    private boolean isAnimatorRunning() {
        return (restoreAnimator != null && restoreAnimator.isRunning()) || (dismissAnimator != null && dismissAnimator.isRunning());
    }

    /**
     * 更新窗口位置
     * @param x
     * @param y
     */
    public void updateWindowLocation(int x, int y) {
        if (isShowing) {
            mWindowParams.x = x;
            mWindowParams.y = y;
            mWindowManager.updateViewLayout(mContentView, mWindowParams);
        }
    }

    /**
     * 展示悬浮窗
     */
    public void show() {
        if (!isShowing) {
            isShowing = true;
            //将View添加进入WindowManager。
            mWindowManager.addView(mContentView, mWindowParams);
            //5秒以后从WindowManager中移除View。
            autoDismiss();
        }
    }

    /**
     * 移除炫富穿
     */
    public void dismiss() {
        if (isShowing) {
            resetState();
            mWindowManager.removeView(mContentView);
        }
    }

    /**
     * 重置状态
     */
    private void resetState() {
        isShowing = false;
        mWindowParams.x = 0;
        mWindowParams.y = -mStatusBarHeight;
    }

    /**
     * 自动隐藏通知
     */
    private void autoDismiss() {
        mHandler.removeMessages(HIDE_WINDOW);
        mHandler.sendEmptyMessageDelayed(HIDE_WINDOW, DISMISS_INTERVAL);
    }

    /**
     * 获取状态栏的高度
     */
    public int getStatusBarHeight() {
        int height = 0;
        int resId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            height = mContext.getResources().getDimensionPixelSize(resId);
        }
        return height;
    }


    public void setIcon(int imgRes) {
        if (-1 != imgRes) {
            mIvIcon.setVisibility(View.VISIBLE);
            mIvIcon.setImageResource(imgRes);
        }
    }

    public void setTitle(String title) {
        if (!TextUtils.isEmpty(title)) {
            mTvTitle.setVisibility(View.VISIBLE);
            mTvTitle.setText(title);
        }
    }

    public void setContent(String content) {
        mTvContent.setText(content);
    }

    public void setTime(long time) {
        SimpleDateFormat formatDateTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
        mTvTime.setText(formatDateTime.format(new Date(time)));
    }

    public static class Builder {

        private Context context;
        private int imgRes = -1;
        private String title;
        private String content = "none";
        private long time = System.currentTimeMillis();

        public Context getContext() {
            return context;
        }

        public Builder setContext(Context context) {
            this.context = context;
            return this;
        }

        public Builder setImgRes(int imgRes) {
            this.imgRes = imgRes;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setContent(String content) {
            this.content = content;
            return this;
        }

        public Builder setTime(long time) {
            this.time = time;
            return this;
        }

        public HDLNotification build() {
            if (null == context)
                throw new IllegalArgumentException("the context is required.");

            return new HDLNotification(this);
        }
    }
}
