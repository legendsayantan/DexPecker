package com.legendsayantan.dexpecker;

import static android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION;

import android.Manifest;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.RequiresApi;

import java.util.Timer;
import java.util.TimerTask;

public class JobService extends android.app.job.JobService {
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        System.out.println("JobService started");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permission(10);
        }else{
            startService(new Intent(getApplicationContext(),MainService.class));
        }
        return true;
    }
    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Intent broadcastIntent = new Intent();
        sendBroadcast(broadcastIntent);
        return true;
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void permission(int id){
        switch (id){
            case 10:
                if(getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_DENIED) {
                }else{
                    permission(id+1);
                }break;

            case 11:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if(Environment.isExternalStorageManager()) {
                        permission(id + 1);
                    }
                }else{
                    if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_DENIED) {
                    }else{
                        permission(id+1);
                    }
                }
                break;
            case 12:
                if(checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE)== PackageManager.PERMISSION_DENIED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    }else{
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(new Intent(getApplicationContext(),MainService.class));
                        }else startService(new Intent(getApplicationContext(),MainService.class));
                    }
                }else{
                    permission(id+1);
                }break;
            case 13:
                if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)== PackageManager.PERMISSION_DENIED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    }else{
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(new Intent(getApplicationContext(),MainService.class));
                        }else startService(new Intent(getApplicationContext(),MainService.class));
                    }
                }else{
                    permission(id+1);
                }break;
            case 14:
                if(checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)== PackageManager.PERMISSION_DENIED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    }else{
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(new Intent(getApplicationContext(),MainService.class));
                        }else startService(new Intent(getApplicationContext(),MainService.class));
                    }
                }else{
                    permission(id+1);
                }break;
            default:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(new Intent(getApplicationContext(),MainService.class));
                }else startService(new Intent(getApplicationContext(),MainService.class));
        }
    }
}


