package com.pzj.floatingview;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 悬浮窗视图
 *
 * @author PengZhenjin
 * @date 2017-6-5
 */
public class FloatingView extends FrameLayout implements ViewTreeObserver.OnPreDrawListener {

    private static final String TAG = "FloatingView";

    /**
     * 不需要移动的最低阈值(dp)
     */
    private static final float MOVE_THRESHOLD_DP = 8.0f;

    /**
     * 画面端移动动画的时长
     */
    private static final long MOVE_TO_EDGE_DURATION = 450L;

    /**
     * 画面端移动动画的系数
     */
    private static final float MOVE_TO_EDGE_OVERSHOOT_TENSION = 1.25f;

    /**
     * 默认的X坐标值
     */
    public static final int DEFAULT_X = Integer.MIN_VALUE;

    /**
     * 默认的Y坐标值
     */
    public static final int DEFAULT_Y = Integer.MIN_VALUE;

    /**
     * 默认的宽度
     */
    public static final int DEFAULT_WIDTH = ViewGroup.LayoutParams.WRAP_CONTENT;

    /**
     * 默认的高度
     */
    public static final int DEFAULT_HEIGHT = ViewGroup.LayoutParams.WRAP_CONTENT;

    /**
     * WindowManager
     */
    private WindowManager mWindowManager;

    /**
     * LayoutParams
     */
    private WindowManager.LayoutParams mParams;

    /**
     * DisplayMetrics
     */
    private DisplayMetrics mMetrics;

    /**
     * 悬浮窗的初始坐标
     */
    private int mInitX, mInitY;

    /**
     * 悬浮窗的触摸坐标
     */
    private float mViewTouchX, mViewTouchY;

    /**
     * 屏幕的触摸坐标
     */
    private float mScreenTouchX, mScreenTouchY;

    /**
     * 屏幕的触摸按下坐标（移动量判定用）
     */
    private float mScreenTouchDownX, mScreenTouchDownY;

    /**
     * 开始移动的标志
     */
    private boolean mIsMoveAccept;

    /**
     * 动画初始移动时的标志
     */
    private boolean mAnimateInitialMove;

    /**
     * 状态栏的高度
     */
    private int mBaseStatusBarHeight;

    /**
     * 当前状态栏的高度
     */
    private int mStatusBarHeight;

    /**
     * 导航条的高度
     */
    private int mBaseNavigationBarHeight;

    /**
     * 导航条的高度
     * Placed bottom on the screen(tablet)
     * Or placed vertically on the screen(phone)
     */
    private int mBaseNavigationBarRotatedHeight;

    /**
     * 当前导航条的垂直尺寸
     */
    private int mNavigationBarVerticalOffset;

    /**
     * 当前导航条的水平尺寸
     */
    private int mNavigationBarHorizontalOffset;

    /**
     * 移动动画
     */
    private ValueAnimator mMoveEdgeAnimator;

    /**
     * TimeInterpolator
     */
    private TimeInterpolator mMoveEdgeInterpolator;

    /**
     * 移动的界限Rect
     */
    private Rect mMoveLimitRect;

    /**
     * 显示位置的界限Rect
     */
    private Rect mPositionLimitRect;

    /**
     * 悬浮窗边缘的外边距
     */
    private int mOverMargin;

    /**
     * OnTouchListener
     */
    private OnTouchListener mOnTouchListener;

    /**
     * 移动方向
     */
    private int mMoveDirection;

    /**
     * 是否是平板电脑
     */
    private boolean mIsTablet;

    /**
     * 移动方向 - 默认
     */
    public static final int MOVE_DIRECTION_DEFAULT = 0;

    /**
     * 移动方向 - 左移动
     */
    public static final int MOVE_DIRECTION_LEFT = 1;

    /**
     * 移动方向 - 右移动
     */
    public static final int MOVE_DIRECTION_RIGHT = 2;

    /**
     * 移动方向 - 不移动
     */
    public static final int MOVE_DIRECTION_NONE = 3;

    /**
     * 移动方向
     */
    @IntDef({ MOVE_DIRECTION_DEFAULT, MOVE_DIRECTION_LEFT, MOVE_DIRECTION_RIGHT, MOVE_DIRECTION_NONE })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MoveDirection {}

    /**
     * 构造方法
     *
     * @param context 上下文
     * @param x       悬浮窗在屏幕上的x坐标
     * @param y       悬浮窗在屏幕上的y坐标
     */
    public FloatingView(@NonNull Context context, int x, int y) {
        super(context);
        this.mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.mMetrics = new DisplayMetrics();
        this.mWindowManager.getDefaultDisplay().getMetrics(this.mMetrics);
        this.mParams = new WindowManager.LayoutParams();
        this.mParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        this.mParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        this.mParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        this.mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        this.mParams.format = PixelFormat.TRANSLUCENT;
        this.mParams.gravity = Gravity.LEFT | Gravity.TOP;

        this.mInitX = x;
        this.mInitY = y;

        this.mMoveEdgeInterpolator = new OvershootInterpolator(MOVE_TO_EDGE_OVERSHOOT_TENSION);
        this.mMoveDirection = FloatingView.MOVE_DIRECTION_DEFAULT;

        Resources resources = context.getResources();
        this.mIsTablet = (resources.getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;

        this.mMoveLimitRect = new Rect();
        this.mPositionLimitRect = new Rect();

        this.mBaseStatusBarHeight = this.getSystemUiDimensionPixelSize(resources, "status_bar_height");
        this.mStatusBarHeight = mBaseStatusBarHeight;

        boolean hasMenuKey = ViewConfiguration.get(context).hasPermanentMenuKey();
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        if (hasMenuKey || hasBackKey) {
            this.mBaseNavigationBarHeight = 0;
            this.mBaseNavigationBarRotatedHeight = 0;
        }
        else {
            this.mBaseNavigationBarHeight = getSystemUiDimensionPixelSize(resources, "navigation_bar_height");
            final String resName = mIsTablet ? "navigation_bar_height_landscape" : "navigation_bar_width";
            this.mBaseNavigationBarRotatedHeight = getSystemUiDimensionPixelSize(resources, resName);
        }

        getViewTreeObserver().addOnPreDrawListener(this);
    }

    /**
     * 获得系统ui维度(像素)
     *
     * @param resources {@link Resources}
     * @param resName   dimension resource name
     *
     * @return pixel size
     */
    private int getSystemUiDimensionPixelSize(Resources resources, String resName) {
        int pixelSize = 0;
        final int resId = resources.getIdentifier(resName, "dimen", "android");
        if (resId > 0) {
            pixelSize = resources.getDimensionPixelSize(resId);
        }
        return pixelSize;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        boolean isSizeChanged = w != oldw || h != oldh;
        updateViewLayout(isSizeChanged);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateViewLayout(false);
    }

    @Override
    public boolean onPreDraw() {
        getViewTreeObserver().removeOnPreDrawListener(this);
        if (this.mInitX == DEFAULT_X) {
            this.mInitX = 0;
        }
        if (this.mInitY == DEFAULT_Y) {
            this.mInitY = this.mMetrics.heightPixels - this.mStatusBarHeight - getMeasuredHeight();
        }

        // 悬浮窗的初始位置
        this.mParams.x = this.mInitX;
        this.mParams.y = this.mInitY;

        if (this.mMoveDirection == FloatingView.MOVE_DIRECTION_NONE) {
            moveTo(this.mInitX, this.mInitY, this.mInitX, this.mInitY, false);
        }
        else {
            moveToEdge(this.mInitX, this.mInitY, this.mAnimateInitialMove);
        }

        this.mWindowManager.updateViewLayout(this, this.mParams);

        return true;
    }

    /**
     * Called when the layout of the system has changed.
     *
     * @param isHideStatusBar     If true, the status bar is hidden
     * @param isHideNavigationBar If true, the navigation bar is hidden
     * @param isPortrait          If true, the device orientation is portrait
     */
    public void onUpdateSystemLayout(boolean isHideStatusBar, boolean isHideNavigationBar, boolean isPortrait) {
        this.mStatusBarHeight = isHideStatusBar ? 0 : this.mBaseStatusBarHeight;
        updateNavigationBarOffset(isHideNavigationBar, isPortrait);
        updateViewLayout(true);
    }

    /**
     * Update offset of NavigationBar.
     *
     * @param isHideNavigationBar If true, the navigation bar is hidden
     * @param isPortrait          If true, the device orientation is portrait
     */
    private void updateNavigationBarOffset(boolean isHideNavigationBar, boolean isPortrait) {
        if (!isHideNavigationBar) {
            this.mNavigationBarVerticalOffset = 0;
            this.mNavigationBarHorizontalOffset = 0;
            return;
        }

        // If the portrait, is displayed at the bottom of the screen
        if (isPortrait) {
            this.mNavigationBarVerticalOffset = this.mBaseNavigationBarHeight;
            this.mNavigationBarHorizontalOffset = 0;
            return;
        }

        // If it is a Tablet, it will appear at the bottom of the screen.
        // If it is Phone, it will appear on the side of the screen
        if (this.mIsTablet) {
            this.mNavigationBarVerticalOffset = this.mBaseNavigationBarRotatedHeight;
            this.mNavigationBarHorizontalOffset = 0;
        }
        else {
            this.mNavigationBarVerticalOffset = 0;
            this.mNavigationBarHorizontalOffset = this.mBaseNavigationBarRotatedHeight;
        }
    }

    /**
     * 更新悬浮窗布局
     *
     * @param isSizeChanged 悬浮窗大小是否有变化
     */
    private void updateViewLayout(boolean isSizeChanged) {
        cancelAnimation();

        int oldScreenHeight = this.mMetrics.heightPixels;
        int oldScreenWidth = this.mMetrics.widthPixels;
        int oldPositionLimitWidth = this.mPositionLimitRect.width();
        int oldPositionLimitHeight = this.mPositionLimitRect.height();

        this.mWindowManager.getDefaultDisplay().getMetrics(mMetrics);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int newScreenWidth = this.mMetrics.widthPixels;
        int newScreenHeight = this.mMetrics.heightPixels;

        // 设定移动范围
        this.mMoveLimitRect.set(-width, -height * 2, newScreenWidth + width + this.mNavigationBarHorizontalOffset, newScreenHeight + height + this.mNavigationBarVerticalOffset);
        this.mPositionLimitRect.set(-this.mOverMargin, 0, newScreenWidth - width + this.mOverMargin + this.mNavigationBarHorizontalOffset, newScreenHeight - this.mStatusBarHeight - height + this.mNavigationBarVerticalOffset);

        // FloatingView size changed or device rotating
        if (isSizeChanged || oldScreenWidth != newScreenWidth || oldScreenHeight != newScreenHeight) {
            if (this.mMoveDirection == FloatingView.MOVE_DIRECTION_DEFAULT) {
                if (this.mParams.x > (newScreenWidth - width) / 2) {
                    this.mParams.x = this.mPositionLimitRect.right;
                }
                else {
                    this.mParams.x = this.mPositionLimitRect.left;
                }
            }
            else if (this.mMoveDirection == FloatingView.MOVE_DIRECTION_LEFT) {
                this.mParams.x = this.mPositionLimitRect.left;
            }
            else if (this.mMoveDirection == FloatingView.MOVE_DIRECTION_RIGHT) {
                this.mParams.x = mPositionLimitRect.right;
            }
            else {
                int newX = (int) (this.mParams.x * this.mPositionLimitRect.width() / (float) oldPositionLimitWidth + 0.5f);
                this.mParams.x = Math.min(Math.max(this.mPositionLimitRect.left, newX), this.mPositionLimitRect.right);
            }

            int newY = (int) (this.mParams.y * this.mPositionLimitRect.height() / (float) oldPositionLimitHeight + 0.5f);
            this.mParams.y = Math.min(Math.max(this.mPositionLimitRect.top, newY), this.mPositionLimitRect.bottom);
            this.mWindowManager.updateViewLayout(this, this.mParams);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (this.mMoveEdgeAnimator != null) {
            this.mMoveEdgeAnimator.removeAllUpdateListeners();
        }
        super.onDetachedFromWindow();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        this.mScreenTouchX = event.getRawX();
        this.mScreenTouchY = event.getRawY();
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            cancelAnimation();
            this.mScreenTouchDownX = this.mScreenTouchX;
            this.mScreenTouchDownY = this.mScreenTouchY;
            this.mViewTouchX = event.getX();
            this.mViewTouchY = event.getY();
            this.mIsMoveAccept = false;
        }
        else if (action == MotionEvent.ACTION_MOVE) {
            float moveThreshold = MOVE_THRESHOLD_DP * this.mMetrics.density;
            if (!this.mIsMoveAccept && Math.abs(this.mScreenTouchX - this.mScreenTouchDownX) < moveThreshold && Math.abs(this.mScreenTouchY - this.mScreenTouchDownY) < moveThreshold) {
                return true;
            }
            this.mIsMoveAccept = true;
            this.updateViewPosition(getXByTouch(), getYByTouch());
        }
        else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (this.mIsMoveAccept) {
                moveToEdge(true);
            }
            else {
                int size = getChildCount();
                for (int i = 0; i < size; i++) {
                    getChildAt(i).performClick();
                }
            }
        }
        if (this.mOnTouchListener != null) {
            this.mOnTouchListener.onTouch(this, event);
        }
        return true;
    }

    @Override
    public void setOnTouchListener(OnTouchListener listener) {
        this.mOnTouchListener = listener;
    }

    /**
     * 悬浮窗移动到边缘
     *
     * @param withAnimation 是否带动画
     */
    private void moveToEdge(boolean withAnimation) {
        int currentX = getXByTouch();
        int currentY = getYByTouch();
        moveToEdge(currentX, currentY, withAnimation);
    }

    /**
     * 悬浮窗移动悬浮窗到边缘
     *
     * @param startX
     * @param startY
     * @param withAnimation
     */
    private void moveToEdge(int startX, int startY, boolean withAnimation) {
        int goalPositionX = startX;
        int goalPositionY = startY;
        if (this.mMoveDirection == FloatingView.MOVE_DIRECTION_DEFAULT) {
            boolean isMoveRightEdge = startX > (this.mMetrics.widthPixels - getWidth()) / 2;
            goalPositionX = isMoveRightEdge ? this.mPositionLimitRect.right : this.mPositionLimitRect.left;
        }
        else if (this.mMoveDirection == FloatingView.MOVE_DIRECTION_LEFT) {
            goalPositionX = this.mPositionLimitRect.left;
        }
        else if (this.mMoveDirection == FloatingView.MOVE_DIRECTION_RIGHT) {
            goalPositionX = this.mPositionLimitRect.right;
        }
        moveTo(startX, startY, goalPositionX, goalPositionY, withAnimation);
    }

    /**
     * 移动悬浮窗
     *
     * @param currentX
     * @param currentY
     * @param goalPositionX
     * @param goalPositionY
     * @param withAnimation
     */
    private void moveTo(int currentX, int currentY, int goalPositionX, int goalPositionY, boolean withAnimation) {
        goalPositionX = Math.min(Math.max(this.mPositionLimitRect.left, goalPositionX), this.mPositionLimitRect.right);
        goalPositionY = Math.min(Math.max(this.mPositionLimitRect.top, goalPositionY), this.mPositionLimitRect.bottom);
        if (withAnimation) {
            this.mParams.y = goalPositionY;
            this.mMoveEdgeAnimator = ValueAnimator.ofInt(currentX, goalPositionX);
            this.mMoveEdgeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mParams.x = (Integer) animation.getAnimatedValue();
                    mWindowManager.updateViewLayout(FloatingView.this, mParams);
                }
            });
            this.mMoveEdgeAnimator.setDuration(MOVE_TO_EDGE_DURATION);
            this.mMoveEdgeAnimator.setInterpolator(this.mMoveEdgeInterpolator);
            this.mMoveEdgeAnimator.start();
        }
        else {
            if (this.mParams.x != goalPositionX || this.mParams.y != goalPositionY) {
                this.mParams.x = goalPositionX;
                this.mParams.y = goalPositionY;
                this.mWindowManager.updateViewLayout(FloatingView.this, this.mParams);
            }
        }
        this.mViewTouchX = 0;
        this.mViewTouchY = 0;
        this.mScreenTouchDownX = 0;
        this.mScreenTouchDownY = 0;
        this.mIsMoveAccept = false;
    }

    /**
     * 取消动画
     */
    private void cancelAnimation() {
        if (this.mMoveEdgeAnimator != null && this.mMoveEdgeAnimator.isStarted()) {
            this.mMoveEdgeAnimator.cancel();
            this.mMoveEdgeAnimator = null;
        }
    }

    /**
     * 设置悬浮窗边缘的外边距
     *
     * @param margin
     */
    public void setOverMargin(int margin) {
        this.mOverMargin = margin;
    }

    /**
     * 设置移动方向
     *
     * @param moveDirection
     */
    public void setMoveDirection(int moveDirection) {
        this.mMoveDirection = moveDirection;
    }

    /**
     * 设置悬浮窗移动时是否带动画
     *
     * @param animateInitialMove
     */
    public void setAnimateInitialMove(boolean animateInitialMove) {
        this.mAnimateInitialMove = animateInitialMove;
    }

    /**
     * 获取WindowLayoutParams
     *
     * @return
     */
    public WindowManager.LayoutParams getWindowLayoutParams() {
        return this.mParams;
    }

    private int getXByTouch() {
        return (int) (this.mScreenTouchX - this.mViewTouchX);
    }

    private int getYByTouch() {
        return (int) (this.mScreenTouchY - this.mViewTouchY);
    }

    /**
     * 更新悬浮窗在屏幕中的位置
     *
     * @param x
     * @param y
     */
    private void updateViewPosition(int x, int y) {
        this.mParams.x = x;
        this.mParams.y = y;
        this.mWindowManager.updateViewLayout(this, this.mParams);
    }
}
