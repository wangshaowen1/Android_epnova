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
     * @param countDownTimeCount 调用次数
     * @param countDownTime      事件间隔 单位:秒
     * @param isOrder            是否是正序  true表示正序列(例:1,2,3,4...) false表示反序(例:5,4,3,2...)
     */
    public void startCountDown(int countDownTimeCount, int countDownTime, boolean isOrder) {
        if (this.mCountDownTimer != null) {
            Logger.d("当前正在倒计时...");
            return;
        }
        if (!clickAble) {
            setEnabled(false);
        }
        int totalTimeCount = countDownTimeCount * 1000;
        int time = countDownTime * 1000;
        this.mCountDownTimer = new CountDownTimer(totalTimeCount + 500, time) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (mOnTimeListener == null) {
                    return;
                }
                int time = (int) (millisUntilFinished / 1000);
                mOnTimeListener.onTime(isOrder ? totalTimeCount - time : time);
            }

            @Override
            public void onFinish() {
                stopCountDown();
                if (mOnTimeListener == null) {
                    return;
                }
                if (!clickAble) {
                    setEnabled(true);
                }
            }
        };
        if (mOnTimeListener != null) {
            mOnTimeListener.onStartCountTime();
        }
        this.mCountDownTimer.start(); //开始倒计时
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
        if (this.mCountDownTimer != null) {
            Logger.d("停止倒计时...");
            this.mCountDownTimer.cancel();
            this.mCountDownTimer = null;
        }
        if (this.mOnTimeListener != null) {
            this.mOnTimeListener.onComplete();
        }
    }


    public void setOnTimeListener(OnTimeListener mOnTimeListener) {
        this.mOnTimeListener = mOnTimeListener;
    }
}
