package com.luxoft.spokelogcat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import androidx.fragment.app.DialogFragment;

public class WarningFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setMessage(R.string.warning_text)
                .setPositiveButton(R.string.warning_more, (DialogInterface dialog, int which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://github.com/tananaev/rootless-logcat/blob/master/README.md"));
                    startActivity(intent);
            })
            .setNegativeButton(R.string.warning_close, null).create();
    }

}
