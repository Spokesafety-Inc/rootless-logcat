package com.luxoft.spokelogcat;

import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.net.Uri;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class LogSaver {
    private final AppCompatActivity mActivity;
    private String mLastTag = null;

    public LogSaver(AppCompatActivity activity) {
        mActivity = activity;
    }

    public void saveLogs(String tag, List<Line> lines) {
        if (tag == null) {
            tag = "all";
        } else {
            String curTag = tag;
            tag = mLastTag == null ? "all" : mLastTag;
            mLastTag = curTag;
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        String fileName = String.format("%s_%s.log", timeStamp, tag);
        File file = new File(getLogFilePath(fileName));
        for (int fileUniqueSuffix = 1; file.exists(); ++fileUniqueSuffix) {
            fileName =  String.format("%s_%s_%d.log", timeStamp, tag, fileUniqueSuffix);
            file = new File(getLogFilePath(fileName));
        }
        try {
            file.createNewFile();
            Log.w(TAG, "Log file created: " + file.getCanonicalPath());
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            for (Line line : lines) {
                writer.write(line.content);
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            Log.w(TAG, e);
        }
    }

    public void shareLogs() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        String fileName = String.format("%s.log.zip", timeStamp);

        File zippedFile = zip(getLogFileList(), getLogFilePath(fileName));
        if (zippedFile == null) {
            Log.e(TAG, "Failed to create a zip file");
            return;
        }

        Uri uri = FileProvider.getUriForFile(mActivity, mActivity.getPackageName() , zippedFile);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent.createChooser(intent, "Share");
        mActivity.startActivity(intent);
    }

    private List<File> getLogFileList() {
        List<File> files = new ArrayList<>();
        File folder = new File(logFilesLocation());
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    files.add(file);
                }
            }
        }
        return files;
    }

    private static File zip(List<File> files, String filename) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        try {
            ZipOutputStream out = new ZipOutputStream(baos);
            for (int i=0; i<files.size(); i++) {
                FileInputStream in = new FileInputStream(files.get(i).getCanonicalFile());
                out.putNextEntry(new ZipEntry(files.get(i).getName()));
                int len;
                while((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.closeEntry();
                in.close();
            }
            out.close();

            File zipFile = new File(filename);
            FileOutputStream oFile = new FileOutputStream(zipFile);
            oFile.write(baos.toByteArray());
            return zipFile;
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage());
        }
        return null;
    }

    private String logFilesLocation() {
        return mActivity.getExternalCacheDir().toString();
    }

    private String getLogFilePath(String fileName) {
        return logFilesLocation() + "/" + fileName;
    }

    private static final String TAG = LogSaver.class.getSimpleName();
}
