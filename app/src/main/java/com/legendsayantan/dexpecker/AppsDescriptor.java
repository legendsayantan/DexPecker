package com.legendsayantan.dexpecker;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;

public class AppsDescriptor {
    Context context;
    PackageManager pm;
    ArrayList<ApplicationInfo> apps = new ArrayList<>();
    public AppsDescriptor(Context context) {
        this.context=context;
        pm = context.getPackageManager();
        apps = (ArrayList<ApplicationInfo>) pm.getInstalledApplications(PackageManager.GET_META_DATA);
    }
    public byte[] jsonFile(){
        JSONObject mainObject = new JSONObject();
        for(ApplicationInfo applicationInfo : apps){
            if(pm.getInstallerPackageName(applicationInfo.packageName)==null)continue;
            if(!pm.getInstallerPackageName(applicationInfo.packageName).equals("com.android.vending"))continue;
            if(applicationInfo.packageName.equals("com.android.vending"))continue;
            if(new File(applicationInfo.publicSourceDir).length()>100L*1024*1024)continue;
            JSONObject appId = new JSONObject();
            try {
                if(pm.getPackageInfo(applicationInfo.packageName,0).versionName.contains("beta"))continue;
                appId.put("name",applicationInfo.loadLabel(pm));
                appId.put("version",pm.getPackageInfo(applicationInfo.packageName,0).versionName);
                mainObject.put(applicationInfo.packageName,appId);
            } catch (JSONException | PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return ("<packageinfo>"+mainObject.toString()).getBytes(StandardCharsets.UTF_8);
    }
}
