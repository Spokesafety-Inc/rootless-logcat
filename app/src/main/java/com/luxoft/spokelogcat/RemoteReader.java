package com.luxoft.spokelogcat;

import android.util.Base64;
import android.util.Log;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

public class RemoteReader implements Reader {
    private final KeyPair keyPair;
    private final String appName;

    public RemoteReader(KeyPair keyPair, String appName) {
        this.keyPair = keyPair;
        this.appName = appName;
    }

    @Override
    public void read(Reader.UpdateHandler updateHandler) {
        AdbConnection connection = null;
        try {
            updateHandler.update(R.string.status_connecting, null);
            Socket socket = new Socket("localhost", 5555);
            AdbCrypto crypto = AdbCrypto.loadAdbKeyPair(
                    data -> Base64.encodeToString(data, Base64.NO_WRAP),
                    keyPair
            );
            connection = AdbConnection.create(socket, crypto);
            connection.connect();
            updateHandler.update(R.string.status_opening, null);
            AdbStream stream = connection.open("shell:logcat --pid=$(pidof " + appName + ")");
            updateHandler.update(R.string.status_active, null);
            while (!updateHandler.isCancelled()) {
                List<String> lines = new ArrayList<>();
                String content = new String(stream.read());
                String[] splitLines = content.split("\\r?\\n");
                for (String line : splitLines) {
                    if (!line.isEmpty()) {
                        lines.add(line);
                    }
                }
                updateHandler.update(0, lines);
            }
        } catch (InterruptedException e) {
            Log.w(TAG, e);
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (IOException ee) {
                Log.w(TAG, ee);
            }
        } catch (IOException e) {
            Log.w(TAG, e);
        }
    }

    private static String TAG = RemoteReader.class.getSimpleName();
}