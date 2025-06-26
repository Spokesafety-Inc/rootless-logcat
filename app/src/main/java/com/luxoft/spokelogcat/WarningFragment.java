package com.luxoft.spokelogcat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class WarningFragment extends DialogFragment {
    private final Context context;
    private final String message;
    private final DialogInterface.OnClickListener moreBtnCallback;

    public WarningFragment(Context context, String message, DialogInterface.OnClickListener moreCallback) {
        this.context = context;
        this.message = message;
        this.moreBtnCallback = moreCallback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message)
               .setNegativeButton(R.string.warning_close,  (dialog, id) -> dialog.cancel());
        if (moreBtnCallback != null) {
            builder.setPositiveButton(R.string.warning_more, moreBtnCallback);
        }
        return builder.create();
    }

}
