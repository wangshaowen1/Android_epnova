package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.nhsd.basic.utils.Logger;

/**
 * ProjectName:    MakeMoneyCat
 * <p>
 * Package:        com.ibaiwandd.makemoneycat.ui.widget
 * <p>
 * ClassName:      CountDownTextView
 * <p>
 * Description:    倒计时View
 * <p>
 * Author:         Xie
 * <p>
 * CreateDate:     2020/4/3 14:09
 * <p>
 * UpdateUser:
 * <p>
 * UpdateDate:     2020/4/3 14:09
 * <p>
 * UpdateRemark:
 * <p>
 * Version:
 */
public class CountDownTextView extends AppCompatTextView {

    private CountDownTimer mCountDownTimer;

    private OnTimeListener mOnTimeListener;

    private boolean clickAble = false;
    private boolean enabledBeforeCountDown;
    private boolean disabledByCountDown;

    public CountDownTextView(Context context) {
        this(context, null);
    }

    public CountDownTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CountDownTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 时间监听
     */
    public interface OnTimeListener {

        /**
         * 开始倒计时
         */
        void onStartCountTime();

        /**
         * 获取时间
         *
         * @param time 单位:秒
         */
        void onTime(int time);

        /**
         * 倒计时完成
         */
        void onComplete();
    }

    /**
     * 开始倒计时
     *
     * @param countDownTimeCount 总时长，单位：秒
     * @param countDownTime      事件间隔 单位:秒
     * @param isOrder            true 返回已过去的秒数，false 返回剩余秒数
     */
    public void startCountDown(int countDownTimeCount, int countDownTime, boolean isOrder) {
        if (this.mCountDownTimer != null) {
            Logger.d("当前正在倒计时...");
            return;
        }
        enabledBeforeCountDown = isEnabled();
        disabledByCountDown = !clickAble;
        if (disabledByCountDown) {
            setEnabled(false);
        }
        int totalSeconds = Math.max(0, countDownTimeCount);
        long totalTimeCount = totalSeconds * 1000L;
        long intervalMillis = Math.max(1, countDownTime) * 1000L;
        CountDownTimer timer = new CountDownTimer(totalTimeCount == 0 ? 0 : totalTimeCount + 500, intervalMillis) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (mCountDownTimer != this || mOnTimeListener == null) {
                    return;
                }
                int remainingSeconds = (int) Math.min(totalSeconds, millisUntilFinished / 1000);
                mOnTimeListener.onTime(isOrder ? totalSeconds - remainingSeconds : remainingSeconds);
            }

            @Override
            public void onFinish() {
                if (mCountDownTimer == this) {
                    stopCountDown();
                }
            }
        };
        this.mCountDownTimer = timer;
        if (mOnTimeListener != null) {
            mOnTimeListener.onStartCountTime();
        }
        // 监听器可能同步停止或重启倒计时，只启动仍归本次调用所有的 Timer。
        if (this.mCountDownTimer == timer) {
            timer.start();
        }
    }

    public boolean isCountDowning() {
        return mCountDownTimer != null;
    }

    public void setClickAble(boolean clickAble) {
        this.clickAble = clickAble;
    }

    /**
     * 停止倒计时
     */
    public void stopCountDown() {
        if (this.mCountDownTimer == null) {
            return;
        }
        cancelCountDown();
        if (this.mOnTimeListener != null) {
            this.mOnTimeListener.onComplete();
        }
    }

    private void cancelCountDown() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
        if (disabledByCountDown) {
            disabledByCountDown = false;
            setEnabled(enabledBeforeCountDown);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        // 离开窗口只取消，不把生命周期清理误报为业务完成。
        cancelCountDown();
        super.onDetachedFromWindow();
    }

    public void setOnTimeListener(OnTimeListener mOnTimeListener) {
        this.mOnTimeListener = mOnTimeListener;
    }
}
