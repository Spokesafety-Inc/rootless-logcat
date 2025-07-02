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
    private String errMsg;
    private boolean clearLogSet = false;

    public RemoteReader(KeyPair keyPair, String appName) {
        this.keyPair = keyPair;
        this.appName = appName;
    }

    @Override
    public int read(Reader.UpdateHandler updateHandler) {
        AdbConnection connection = null;
        errMsg = null;
        String readLogsCmd  = "logcat -v time --pid=$(pidof " + appName + ")";
        String clearLogsCmd = "logcat -c";
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
            AdbStream stream = connection.open("shell:");
            stream.write(readLogsCmd);
            updateHandler.update(R.string.status_active, null);
            while (!updateHandler.isCancelled()) {
                boolean needClearing;
                synchronized (this) {
                    needClearing = clearLogSet;
                    if (clearLogSet) {
                        clearLogSet = false;
                    }
                }
                List<String> lines = new ArrayList<>();
                if (needClearing) {
                    byte[] ctrlC = new byte[]{3}; // CTRL-C (ASCII 3)
                    stream.write(ctrlC);
                    stream.write(clearLogsCmd);
                    stream.readAll();
                    stream.write(System.lineSeparator() + readLogsCmd);
                }
                else {
                    String content = new String(stream.read());
                    String[] splitLines = content.split("\\r?\\n");
                    for (String line : splitLines) {
                        if (!line.isEmpty()) {
                            lines.add(line);
                        }
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
            errMsg = e.getMessage();
        } catch (IOException e) {
            Log.w(TAG, e);
            errMsg = e.getMessage();
        }
        if (errMsg != null) {
            updateHandler.update(R.string.status_failed, null);
            return -1;
        }
        return 0;
    }

    @Override
    public void clearLogs() {
        synchronized (this) {
            clearLogSet = true;
        }
    }

    @Override
    public String getErrorMessage() {
        return errMsg;
    }

    private static final String TAG = RemoteReader.class.getSimpleName();
}