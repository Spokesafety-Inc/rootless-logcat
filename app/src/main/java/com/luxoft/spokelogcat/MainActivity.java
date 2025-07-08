package com.luxoft.spokelogcat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.widget.Toast;

import com.luxoft.spokelogcat.view.FilterOptionsController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private KeyPair keyPair;
    private LineAdapter adapter;
    private RecyclerView recyclerView;
    private FloatingActionButton scrollDownBtn;
    private FloatingActionButton scrollUpBtn;
    private EditText tagName;
    private ReaderTask readerTask = null;
    private MenuItem statusItem = null;
    private MenuItem filterItem = null;
    private boolean autoscroll = true;
    private Context mContext;
    private LogSaver mLogSaver = null;

    private static class StatusUpdate {
        public final int statusMessage;
        public final List<String> lines;

        public StatusUpdate(int statusMessage, List<String> lines) {
            this.statusMessage = statusMessage;
            this.lines = lines;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mLogSaver = new LogSaver(this);

        tagName = findViewById(R.id.tagName);

        scrollDownBtn = findViewById(R.id.scrollDownBtn);
        scrollDownBtn.setVisibility(RecyclerView.INVISIBLE);
        scrollDownBtn.setOnClickListener(view -> { updateScrollState(true); });

        scrollUpBtn = findViewById(R.id.scrollUpBtn);
        scrollUpBtn.setVisibility(RecyclerView.INVISIBLE);
        scrollUpBtn.setOnClickListener(view -> {
            updateScrollState(false);
            recyclerView.scrollToPosition(0);
            scrollUpBtn.setVisibility(RecyclerView.INVISIBLE);
        });

        recyclerView = findViewById(R.id.recyclerView);
        adapter = new LineAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState != RecyclerView.SCROLL_STATE_DRAGGING) {
                    updateScrollState(false);
                }
            }
        });

        Button tagButton = findViewById(R.id.tagButton);
        tagButton.setOnClickListener(v -> {
            injectTag();
            updateScrollState(true);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        try {
            keyPair = getKeyPair(); // crashes on non-main thread
        } catch (GeneralSecurityException | IOException e) {
            Log.w(TAG, e);
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.getBoolean(KEY_WARNING_SHOWN, false)) {
            new WarningFragment(mContext, getString(R.string.warning_text), (DialogInterface dialog, int which) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(getString(R.string.warning_more_link)));
                startActivity(intent);
            }).show(getSupportFragmentManager(), null);
            preferences.edit().putBoolean(KEY_WARNING_SHOWN, true).apply();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        statusItem = menu.findItem(R.id.miStatus);
        filterItem = menu.findItem(R.id.miFilter);
        restartReader();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.miSave) {
            saveLogs();
        } else if (itemId == R.id.miShare) {
            mLogSaver.shareLogs();
        } else if (itemId == R.id.miReconnect) {
            restartReader();
        } else if (itemId == R.id.miSettings) {
            showSettingsDialog();
        } else if (itemId == R.id.miFilter) {
            showFilterDialog();
        } else if (itemId == R.id.miClearLogs) {
            clearLogs();
        } else if (itemId == R.id.miBltParedDevices) {
            checkBltPermissions();
        }
        else {
            return false;
        }
        return true;
    }

    private void checkBltPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Toast.makeText(this, R.string.sdk_version_warn + Build.VERSION_CODES.S, Toast.LENGTH_SHORT).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_BLUETOOTH_CONNECT);
        } else {
            showBluetoothPairedDevices();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showBluetoothPairedDevices();
            } else {
                Toast.makeText(this, R.string.blt_permission_text, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showBluetoothPairedDevices() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Toast.makeText(this, R.string.sdk_version_warn + Build.VERSION_CODES.S, Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<String> devicesList = getBluetoothPairedDevices();
        if (devicesList == null) {
            Toast.makeText(this, R.string.blt_no_pared_devices, Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayAdapter<String> lineAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, devicesList);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.blt_pared_devices_title);
        builder.setAdapter(lineAdapter, null);
        builder.setPositiveButton(android.R.string.ok, null);
        AlertDialog alert = builder.create();
        alert.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private ArrayList<String> getBluetoothPairedDevices() {
        assert ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;

        BluetoothManager btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        Set<BluetoothDevice> pairedDevices = btManager.getAdapter().getBondedDevices();
        if (!pairedDevices.isEmpty()) {
            ArrayList<String> devicesList = new ArrayList<>();
            for (BluetoothDevice device : pairedDevices) {
                devicesList.add(String.format("%s at %s", device.getName(), device.getAddress()));
            }
            return devicesList;
        }
        return null;
    }

//    private boolean isConnected(BluetoothDevice device) {
//        try {
//            Method m = device.getClass().getDeclaredMethod("isConnected", (Class[]) null);
//            m.setAccessible(true);
//            boolean isConnected = (boolean) m.invoke(device, (Object[]) null);
//            Log.w("tag", isConnected ? "connected" : "NOT connected");
//            return isConnected;
//        } catch (Exception e) {
//            throw new IllegalStateException(e);
//        }
//    }

    private void showSettingsDialog() {
        // TODO
    }

    private void clearLogs() {
        adapter.clear();
        if (readerTask != null) {
            readerTask.clearLogs();
        }
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        FilterOptionsController filterOptionsViewController = new FilterOptionsController(this);
        filterOptionsViewController.setLevel(adapter.level());
        filterOptionsViewController.setKeyword(adapter.keyword());
        builder.setView(filterOptionsViewController.baseView);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String level = filterOptionsViewController.getLevel();
            String keyword = filterOptionsViewController.getKeyword();
            adapter.filter(level, keyword);
            filterOptionsViewController.saveLevelKeyword(level, keyword);
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        Dialog dialog = builder.create();
        dialog.getWindow().setGravity(Gravity.TOP);
        dialog.show();
    }

    private void updateScrollState(boolean autoscroll) {
        this.autoscroll = autoscroll;
        if (autoscroll) {
            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
        }
        scrollDownBtn.setVisibility(autoscroll ? RecyclerView.INVISIBLE : RecyclerView.VISIBLE);
        scrollUpBtn.setVisibility(autoscroll ? RecyclerView.INVISIBLE : RecyclerView.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopReader();
    }

    private void stopReader() {
        adapter.clear();
        if (readerTask != null) {
            readerTask.cancel(true);
        }
    }

    private void restartReader() {
        stopReader();
        readerTask = new ReaderTask(mContext);
        readerTask.execute();
    }

    private void saveLogs() {
        mLogSaver.saveLogs(null, adapter.lines());
    }

    private void injectTag() {
        clearTagFocus();
        String tag = tagName.getText().toString();
        if (tag.isEmpty()) {
            return;
        }
        mLogSaver.saveLogs(tag, adapter.lines());
        clearLogs();

        String timeStamp = new SimpleDateFormat("MM-dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
        adapter.addItems(new ArrayList<>(List.of(String.format("%s D %s %s", timeStamp, Line.DEBUG_TAG, tag))));
    }

    private void clearTagFocus() {
        View v = getCurrentFocus();
        if (v != null) {
            v.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    private KeyPair getKeyPair() throws GeneralSecurityException, IOException {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        KeyPair keyPair;
        if (preferences.contains(KEY_PUBLIC) && preferences.contains(KEY_PRIVATE)) {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            java.security.PublicKey publicKey = keyFactory.generatePublic(
                    new X509EncodedKeySpec(
                            Base64.decode(preferences.getString(KEY_PUBLIC, null), Base64.DEFAULT)
                    )
            );
            java.security.PrivateKey privateKey = keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(
                            Base64.decode(preferences.getString(KEY_PRIVATE, null), Base64.DEFAULT)
                    )
            );
            keyPair = new KeyPair(publicKey, privateKey);
        } else {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            keyPair = generator.generateKeyPair();
            preferences
                    .edit()
                    .putString(
                            KEY_PUBLIC,
                            Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.DEFAULT)
                )
                .putString(
                    KEY_PRIVATE,
                    Base64.encodeToString(keyPair.getPrivate().getEncoded(), Base64.DEFAULT)
                )
                .apply();
        }
        return keyPair;
    }

    @SuppressLint("StaticFieldLeak")
    private class ReaderTask extends AsyncTask<Void, StatusUpdate, Integer> {
        private final Context context;
        private String errMessage;
        private Reader reader = null;

        public ReaderTask(Context mContext) {
            this.context = mContext;
        }

        public void clearLogs() {
            reader.clearLogs();
        }

        @Override
        public Integer doInBackground(Void... params) {
            errMessage = null;
            reader = new RemoteReader(keyPair, APP_NAME);
            int retCode = reader.read(new Reader.UpdateHandler() {
                @Override
                public boolean isCancelled() {
                    return ReaderTask.this.isCancelled();
                }

                @Override
                public void update(int status, List<String> lines) {
                    publishProgress(new StatusUpdate(status, lines));
                }
            });
            if (retCode != 0) {
                errMessage = reader.getErrorMessage();
            }
            reader = null;
            return retCode;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (result != 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(errMessage != null ? errMessage : getString(R.string.adb_failed))
                       .setCancelable(false)
                       .setNegativeButton(R.string.warning_close, (dialog, id) -> dialog.cancel());
                builder.create().show();
//                WarningFragment dialog = new WarningFragment(context, errMessage, null);
//                dialog.show(getSupportFragmentManager(), null);
            }
        }

        @Override
        public void onProgressUpdate(StatusUpdate... values) {
            for (StatusUpdate statusUpdate : values) {
                if (statusUpdate.statusMessage != 0) {
                    statusItem.setTitle(statusUpdate.statusMessage);
                    filterItem.setVisible(statusUpdate.statusMessage == R.string.status_active);
//                    searchItem?.isVisible = statusUpdate.statusMessage == R.string.status_active
//                    moreMenuItem?.isVisible = statusUpdate.statusMessage == R.string.status_active
                }
                if (statusUpdate.lines != null) {
                    adapter.addItems(statusUpdate.lines);
                    if (autoscroll) {
                        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                    }
                }
            }
        }

    } // ReaderTask class

    private static final String APP_NAME = "com.spoke.safety";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String KEY_PUBLIC = "publicKey";
    private static final String KEY_PRIVATE = "privateKey";
    private static final String KEY_WARNING_SHOWN = "warningShown";
    private static final int REQUEST_BLUETOOTH_CONNECT = 1;
}