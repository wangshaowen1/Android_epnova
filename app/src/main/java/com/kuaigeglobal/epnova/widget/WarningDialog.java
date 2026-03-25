package com.kuaigeglobal.epnova.widget;

import static com.blankj.utilcode.util.StringUtils.getString;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDialog;

import com.kuaigeglobal.epnova.R;


public class WarningDialog extends AppCompatDialog {

    Button btnConfirm;
    TextView tvContent;

    private Handler handler = new Handler(Looper.getMainLooper());
    private int maxWaitSeconds = 30;

    public WarningDialog(Context context, int resId) {
        this(context, context.getString(resId));
    }

    public WarningDialog(Context context) {
        this(context, null);
    }

    public WarningDialog(Context context, CharSequence content) {
        super(context, R.style.Dialog_Common);
        setContentView(R.layout.dialog_warning);
        btnConfirm = findViewById(R.id.btnConfirm);
        tvContent = findViewById(R.id.content);
        tvContent.setText(content);
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onClickListener != null) {
                    onClickListener.onConfirm();
                }
                dismiss();
            }
        });
        startCountDown();
    }

    private void startCountDown() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (maxWaitSeconds == 0) {
                    btnConfirm.setText(R.string.got_it);
                    btnConfirm.setEnabled(true);
                    maxWaitSeconds = 30;
                } else {
                    btnConfirm.setText(getString(R.string.got_it_s, maxWaitSeconds));
                    if (isShowing()) {
                        startCountDown();
                    }
                }
                maxWaitSeconds--;
            }

        }, 1000);
    }

    @Override
    public void onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null);
        super.onDetachedFromWindow();
    }

    public interface OnClickListener {
        void onConfirm();
    }

    OnClickListener onClickListener;

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }
}
