package com.luxoft.spokelogcat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class WarningFragment extends DialogFragment {
    private final String message;
    private final DialogInterface.OnClickListener moreBtnCallback;

    public WarningFragment(String msg, DialogInterface.OnClickListener moreCallback) {
        message = msg;
        moreBtnCallback = moreCallback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message)
               .setNegativeButton(R.string.warning_close, null);
        if (moreBtnCallback != null) {
            builder.setPositiveButton(R.string.warning_more, moreBtnCallback);
        }
        return builder.create();
    }

}
