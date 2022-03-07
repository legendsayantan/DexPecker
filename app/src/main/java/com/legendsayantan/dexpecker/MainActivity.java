package com.legendsayantan.dexpecker;

import static android.os.Build.VERSION.SDK_INT;
import static android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    static Activity activity;
    static ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.theme));
        activity=this;
        listView = findViewById(R.id.list);
        connect();
        refresh();
    }
    public void connect(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permission(10);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(Arrays.equals(grantResults, new int[]{PackageManager.PERMISSION_GRANTED})){
            System.out.println("granted "+ Arrays.toString(permissions));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permission(requestCode+1);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void permission(int id){
        switch (id){
            case 10:
                if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_DENIED) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},id);
                }else{
                    permission(id+1);
                }break;

            case 11:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if(Environment.isExternalStorageManager()) {
                        permission(id+1);
                    }else {
                        startActivity(new Intent(ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        new Timer().scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                if(Environment.isExternalStorageManager()) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            permission(id+1);
                                        }
                                    });
                                    this.cancel();
                                }
                            }
                        },2000,1000);}
                }else{
                    if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_DENIED) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},id);
                    }else{
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},id);
                        permission(id+1);
                    }
                }
                break;
            case 12:
                if(checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE)== PackageManager.PERMISSION_DENIED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADVERTISE},id);
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
                        requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},id);
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
                        requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN},id);
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
    public static void refresh(){
        File[] data = new File(Environment.getExternalStorageDirectory()+"/Android/media/"+activity.getPackageName()).listFiles();
        ArrayList<String> apps = new ArrayList<>();
        if(data==null)return;
        for(File d : data){
            if(MainService.appsToGet!=null && MainService.appsToGet.contains(d.getName()))continue;
            System.out.println(d);
            if(d.isDirectory()){
                try {
                    apps.add((String) activity.getPackageManager().getPackageInfo(d.getName(),0).applicationInfo.loadLabel(activity.getPackageManager()));
                } catch (PackageManager.NameNotFoundException e) {
                    System.out.println(e.getMessage());
                }
            }else if(d.isFile()){
                System.out.println(d);
                apps.add((String) activity.getPackageManager().getPackageArchiveInfo(d.getAbsolutePath(),0).applicationInfo.loadLabel(activity.getPackageManager()));
            }
        }
        listView.setAdapter(new ArrayAdapter<String>(activity.getApplicationContext(), android.R.layout.simple_list_item_1,apps));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    if(data[i].isFile())
                        installapk(data[i], activity.getApplicationContext());
                    if(data[i].isDirectory()){
                        String[] strings = new String[Objects.requireNonNull(data[i].listFiles()).length];
                        for (int j = 0; j< Objects.requireNonNull(data[i].listFiles()).length; j++){
                            strings[j]= Objects.requireNonNull(data[i].listFiles())[j].getAbsolutePath();
                        }
                        new SplitInstaller(activity);
                        SplitInstaller.installApk(strings);
                    }
                }catch (Exception e){
                    Toast.makeText(activity.getApplicationContext(),"ERROR "+e.getMessage(),Toast.LENGTH_LONG).show();
                }
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(data[i].isFile()){
                    data[i].delete();
                }else{
                    for(File file : data[i].listFiles()){
                        file.delete();
                    }
                    data[i].delete();
                }
                Toast.makeText(activity.getApplicationContext(),"Deleted update for "+apps.get(i),Toast.LENGTH_LONG).show();
                refresh();
                return true;
            }
        });
    }
    public static void listfiles(String directoryName, ArrayList<String> files) {
        File directory = new File(directoryName);

        // Get all files from a directory.
        File[] fList = directory.listFiles();
        if(fList != null)
            for (File file : fList) {
                if (file.isFile()) {
                    files.add(file.getAbsolutePath());
                } else if (file.isDirectory()) {
                    listfiles(file.getAbsolutePath(), files);
                }
            }
    }
    public static void installapk(File apkFile, Context context) {
        if (SDK_INT >= Build.VERSION_CODES.N) {
            if (apkFile.exists()) {
                Intent install = new Intent(Intent.ACTION_VIEW);
                install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (SDK_INT >= Build.VERSION_CODES.N) {
                    install.setDataAndType(Uri.fromFile(apkFile),
                            "application/vnd.android.package-archive");
                    Uri apkUri =
                            FileProvider.getUriForFile(context, context.getPackageName()+".fileProvider", apkFile);
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    install.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    install.setDataAndType(apkUri, "application/vnd.android.package-archive");
                    context.startActivity(install);

                } else {
                    install.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    install.setDataAndType(Uri.fromFile(apkFile),
                            "application/vnd.android.package-archive");
                    context.startActivity(install);
                }
            } else {
                System.out.println("apk doesnot exist.");
            }
        }
    }
}