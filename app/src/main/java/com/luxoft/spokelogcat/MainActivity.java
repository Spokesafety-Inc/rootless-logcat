package com.luxoft.spokelogcat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.preference.PreferenceManager;
import android.util.Base64;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

import kotlin.jvm.Throws;

public class MainActivity extends AppCompatActivity {
    private KeyPair keyPair;
    private LineAdapter adapter;
    private final String appName = "com.spoke.safety";
    private RecyclerView recyclerView;
    private EditText logFileName;
    private ReaderTask readerTask = null;

    private class StatusUpdate {
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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        logFileName = findViewById(R.id.logFileName);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(Calendar.getInstance().getTime());
        timeStamp += ".log";
        logFileName.setText(timeStamp);

        recyclerView = findViewById(R.id.recyclerView);
        adapter = new LineAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState != RecyclerView.SCROLL_STATE_DRAGGING) {
                    recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                }
            }
        });

        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveLogs();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        try {
            keyPair = getKeyPair(); // crashes on non-main thread
        } catch (GeneralSecurityException e) {
            Log.w(TAG, e);
        } catch (IOException e) {
            Log.w(TAG, e);
        }

        restartReader();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        restartReader();
        return true;
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
        readerTask = new ReaderTask();
        if (readerTask != null) {
            readerTask.execute();
        }
    }

    private void saveLogs() {
        String fileName = logFileName.getText().toString();
        File file = new File(getExternalCacheDir().toString() + "/" + fileName);
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
            Log.w(TAG, "Log file created: " + file.getCanonicalPath());
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            for (Line line : adapter.lines()) {
                writer.write(line.content);
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        Uri uri = FileProvider.getUriForFile(this, getPackageName() , file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent.createChooser(intent, "Share");
        startActivity(intent);
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
    private class ReaderTask extends AsyncTask<Void, StatusUpdate, Void> {
        @Override
        public Void doInBackground(Void... params) {
            Reader reader = new RemoteReader(keyPair, appName);
            reader.read(new Reader.UpdateHandler() {
                @Override
                public boolean isCancelled() {
                    return ReaderTask.this.isCancelled();
                }

                @Override
                public void update(int status, List<String> lines) {
                    publishProgress(new StatusUpdate(status, lines));
                }
            });

            return null;
        }

        @Override
        public void onProgressUpdate(StatusUpdate... values) {
            for (StatusUpdate statusUpdate : values) {
//                if (statusUpdate.statusMessage != 0) {
//                    statusItem?.setTitle(statusUpdate.statusMessage)
//                    reconnectItem?.isVisible = statusUpdate.statusMessage != R.string.status_active
//                    scrollItem?.isVisible = statusUpdate.statusMessage == R.string.status_active
//                    filterItem?.isVisible = statusUpdate.statusMessage == R.string.status_active
//                    searchItem?.isVisible = statusUpdate.statusMessage == R.string.status_active
//                    moreMenuItem?.isVisible = statusUpdate.statusMessage == R.string.status_active
//                }
                if (statusUpdate.lines != null) {
                    adapter.addItems(statusUpdate.lines);
                    recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                }
            }
        }

    } // ReaderTask class

    private static String TAG = MainActivity.class.getSimpleName();
    private static String KEY_PUBLIC = "publicKey";
    private static String KEY_PRIVATE = "privateKey";
}