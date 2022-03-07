package com.legendsayantan.dexpecker;

import static android.os.Build.VERSION.SDK_INT;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplitInstaller extends MainActivity{
    static Activity context;
    public static PackageInstaller packageInstaller;
    static PackageManager pm;
    static ContentResolver contentResolver;


    public SplitInstaller(Activity activity) {
        context=activity;
        packageInstaller =  activity.getPackageManager().getPackageInstaller();
        pm = activity.getPackageManager();
        contentResolver=new ContentResolver(activity) {
            @NonNull
            @Override
            public List<UriPermission> getPersistedUriPermissions() {
                return super.getPersistedUriPermissions();
            }
        };
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static int installApk(String[] files)
    {
        for(String s : files){
            System.out.println(s);
        }
        HashMap<String, Long> nameSizeMap = new HashMap<>();
        HashMap<String, String> filenameToPathMap = new HashMap<>();
        long totalSize = 0;
        int sessionId = 0;
        try {
            for (String file : files) {
                //Toast.makeText(activity.getApplicationContext(), file,Toast.LENGTH_LONG).show();
                File listOfFile = new File(file);
                if (listOfFile.isFile()) {
                    nameSizeMap.put(listOfFile.getName(), listOfFile.length());
                    filenameToPathMap.put(listOfFile.getName(),file);
                    totalSize += listOfFile.length();
                }else Toast.makeText(activity,"directory ",Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(activity,"ERROR "+e.getMessage(),Toast.LENGTH_LONG).show();            return -1;
        }

        final InstallParams installParams = makeInstallParams(totalSize);

        try {
            sessionId = runInstallCreate(installParams);

            for(Map.Entry<String,Long> entry : nameSizeMap.entrySet())
            {
                runInstallWrite(entry.getValue(),sessionId, entry.getKey(), filenameToPathMap.get(entry.getKey()));
            }

            if (doCommitSession(sessionId, false )
                    != PackageInstaller.STATUS_SUCCESS) {
            }
            System.out.println("Success");

        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return sessionId;
    }
    private static class InstallParams {
        PackageInstaller.SessionParams sessionParams;
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static InstallParams makeInstallParams(long totalSize) {
        final PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        final InstallParams params = new InstallParams();
        params.sessionParams = sessionParams;
        sessionParams.setSize(totalSize);
        return params;
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static int runInstallWrite(long size, int sessionId, String splitName, String path) throws RemoteException {
        long sizeBytes = -1;
        sizeBytes = size;
        return doWriteSession(sessionId, path, sizeBytes, splitName, true /*logSuccess*/);
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static int runInstallCreate(InstallParams installParams) throws RemoteException {
        final int sessionId = doCreateSession(installParams.sessionParams);
        System.out.println("Success: created install session [" + sessionId + "]");
        return sessionId;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static int doCreateSession(PackageInstaller.SessionParams params)
            throws RemoteException {

        int sessionId = 0 ;
        try {
            if(params == null)
            {
                System.out.println( "doCreateSession: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!param is null");
            }
            sessionId = packageInstaller.createSession(params);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sessionId;
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static int doWriteSession(int sessionId, String inPath, long sizeBytes, String splitName,
                                      boolean logSuccess) throws RemoteException {
        if ("-".equals(inPath)) {
            inPath = null;
        } else if (inPath != null) {
            final File file = new File(inPath);
            if (file.isFile()) {
                sizeBytes = file.length();
            }
        }

        PackageInstaller.Session session = null;
        InputStream in = null;
        OutputStream out = null;
        try {
            session = packageInstaller.openSession(sessionId);

            if (inPath != null) {
                in = new FileInputStream(inPath);
            }

            out = session.openWrite(splitName, 0, sizeBytes);

            int total = 0;
            byte[] buffer = new byte[65536];
            int c;
            while ((c = in.read(buffer)) != -1) {
                total += c;
                out.write(buffer, 0, c);
            }
            session.fsync(out);

            if (logSuccess) {
                System.out.println("Success: streamed " + total + " bytes");
            }
            return PackageInstaller.STATUS_SUCCESS;
        } catch (IOException e) {
            System.err.println("Error: failed to write; " + e.getMessage());
            return PackageInstaller.STATUS_FAILURE;
        } finally {
            try {
                out.close();
                in.close();
                session.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static int doCommitSession(int sessionId, boolean logSuccess) throws RemoteException {
        PackageInstaller.Session session = null;
        try {
            try {
                session = packageInstaller.openSession(sessionId);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Intent callbackIntent = new Intent(context, APKInstallService.class);
            PendingIntent pendingIntent = PendingIntent.getService(context, 0, callbackIntent, 0);
            session.commit(pendingIntent.getIntentSender());
            session.close();

            System.out.println("install request sent");

            System.out.println( "doCommitSession: " + packageInstaller.getMySessions());
            System.out.println(
                    "doCommitSession: after session commit ");
            return 1;
        } finally {
            session.close();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private StorageVolume getPrimaryVolume() {
        StorageManager sm = (StorageManager) getSystemService(STORAGE_SERVICE);
        return sm.getPrimaryStorageVolume();
    }
}
