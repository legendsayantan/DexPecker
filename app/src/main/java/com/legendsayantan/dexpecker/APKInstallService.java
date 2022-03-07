package com.legendsayantan.dexpecker;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class APKInstallService extends Service {
    private static final String TAG = "APKInstallService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999);
        String pkg = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME);
        switch (status) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                Log.d(TAG, "Requesting user confirmation for installation");
                Intent confirmationIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(confirmationIntent);
                } catch (Exception e) {
                }
                break;
            case PackageInstaller.STATUS_SUCCESS:
                Log.d(TAG, "Installation succeed");
                Toast.makeText(getApplicationContext(),"INSTALLED.",Toast.LENGTH_LONG).show();
                for(File file : new File(Environment.getExternalStorageDirectory()+"/Android/media/"+getPackageName()+"/"+pkg).listFiles()) file.delete();
                new File(Environment.getExternalStorageDirectory()+"/Android/media/"+getPackageName()+"/"+pkg).delete();
                try {
                    MainActivity.refresh();
                }catch (Exception e){}
                break;
            default:
                Toast.makeText(getApplicationContext(),"FAILED TO INSTALL",Toast.LENGTH_LONG).show();
                Log.d(TAG, "Installation failed");
                break;
        }
        stopSelf();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
