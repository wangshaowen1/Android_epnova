package com.kuaigeglobal.epnova.widget;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.kuaigeglobal.epnova.R;
import com.kuaigeglobal.epnova.databinding.ProgressBarBinding;
import com.nhsd.basic.BaseDialogFragment;

public class LoadingDialog2 extends BaseDialogFragment<ProgressBarBinding> {

    protected static final String TAG = "LoadingDialog2";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireActivity(), R.style.LoadingDialog);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

}
