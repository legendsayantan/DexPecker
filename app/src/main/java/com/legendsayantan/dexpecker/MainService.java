package com.legendsayantan.dexpecker;

import static com.google.android.gms.common.util.IOUtils.copyStream;
import static com.google.android.gms.nearby.connection.PayloadTransferUpdate.Status.SUCCESS;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class MainService extends Service {
    private static final String SERVICE_ID = "dexpecker";
    private static final String TAG = "Scanner";
    String connectedEnd = "";
    SharedPreferences sharedPreferences;
    private ConnectionLifecycleCallback connectionLifecycleCallback;
    private EndpointDiscoveryCallback endpointDiscoveryCallback;
    private PayloadCallback payloadCallback;
    static ArrayList<String> appsToGet = new ArrayList<>();
    Payload filePayload;
    boolean requestedFromHere;
    int count;
    String filename;
    ArrayList<String> files = new ArrayList<>();


    public MainService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        endpointDiscoveryCallback =
                new EndpointDiscoveryCallback() {
                    @Override
                    public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                        if (info.getEndpointName() != macAddress()&&connectedEnd.isEmpty()) {
                            if(sharedPreferences.getString(String.valueOf(System.currentTimeMillis()/86400000),"").contains(info.getEndpointName()))return;
                            System.out.println(endpointId + " found.\n" + info.getEndpointName());
                            Nearby.getConnectionsClient(getApplicationContext()).stopDiscovery();
                            Nearby.getConnectionsClient(getApplicationContext())
                                    .requestConnection(macAddress(), endpointId, connectionLifecycleCallback)
                                    .addOnSuccessListener(
                                            (Void unused) -> {
                                                System.out.println("requested connection");

                                                Nearby.getConnectionsClient(getApplicationContext()).stopAdvertising();
                                                requestedFromHere=true;
                                                // We successfully requested a connection. Now both sides
                                                // must accept before the connection is established.
                                            })
                                    .addOnFailureListener(
                                            (Exception e) -> {
                                                requestedFromHere=false;
                                                startDiscovery();
                                                System.out.println("request connection failed \n"+e.getMessage()+"\n"+e.getCause());
                                                // Nearby Connections failed to request the connection.
                                            });
                        }
                    }
                    @Override
                    public void onEndpointLost(String endpointId) {
                        if(connectedEnd.isEmpty())startDiscovery();
                        // A previously discovered endpoint has gone away.
                    }
                };
        connectionLifecycleCallback =
                new ConnectionLifecycleCallback() {
                    @Override
                    public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                        // Automatically accept the connection on both sides.
                        System.out.println("Called connectionInitiate");
                        files.clear();
                        Nearby.getConnectionsClient(getApplicationContext()).acceptConnection(endpointId, payloadCallback);
                        File folder = new File(Environment.getExternalStorageDirectory()+"/"+Environment.DIRECTORY_DOWNLOADS+"/.nearby/");
                        System.out.println("xxxx "+folder.getAbsolutePath());
                        ArrayList<String> strings = new ArrayList<>();
                        listfiles(folder.getAbsolutePath(),strings);
                        for(String s :strings){
                            new File(s).delete();
                        }
                    }
                    @Override
                    public void onConnectionResult(String endpointId, ConnectionResolution result) {
                        switch (result.getStatus().getStatusCode()) {
                            case ConnectionsStatusCodes.STATUS_OK:
                                Nearby.getConnectionsClient(getApplicationContext()).stopAdvertising();
                                Nearby.getConnectionsClient(getApplicationContext()).stopDiscovery();
                                appsToGet = new ArrayList<>();
                                System.out.println("Connected to "+endpointId);
                                System.out.println("My mac "+macAddress());
                                connectedEnd=endpointId;
                                // We're connected! Can now start sending and receiving data.
                                String message = "<start>";
                                Payload bytesPayload = Payload.fromBytes(message.getBytes(StandardCharsets.UTF_8));
                                Nearby.getConnectionsClient(getApplicationContext()).sendPayload(endpointId, bytesPayload);
                                break;
                            case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                                System.out.println("rejected "+endpointId);
                                requestedFromHere=false;
                                // The connection was rejected by one or both sides.
                                break;
                            case ConnectionsStatusCodes.STATUS_ERROR:
                                startAdvertising();
                                startDiscovery();
                                System.out.println("error "+endpointId);
                                requestedFromHere=false;
                                // The connection broke before it was able to be accepted.
                                break;
                            default:
                                // Unknown status code
                        }
                    }
                    @Override
                    public void onDisconnected(String endpointId) {
                        connectedEnd="";
                        requestedFromHere=false;
                        System.out.println("DISCONNECTED FROM "+endpointId);
                        startAdvertising();
                        startDiscovery();
                    }
                };
        payloadCallback=new PayloadCallback() {
            @Override
            public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {
                if (payload.getType() == Payload.Type.BYTES) {
                    byte[] receivedBytes = payload.asBytes();
                    String string = new String(receivedBytes);
                    System.out.println("Payload text "+string);
                    if(string.equals("<start>")){
                        Payload bytesPayload = Payload.fromBytes(new AppsDescriptor(getApplicationContext()).jsonFile());
                        Nearby.getConnectionsClient(getApplicationContext()).sendPayload(s, bytesPayload);
                    }
                    if(string.startsWith("<packageinfo>")){
                        string = string.replace("<packageinfo>","");
                        try {
                            JSONObject jsonObject = new JSONObject(string);
                            for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                                String key = it.next();
                                String version;
                                System.out.println("Checking app "+jsonObject.getString(key));
                                if(new File(Environment.getExternalStorageDirectory()+"/Android/media/"+getPackageName(),key+".apk").exists())continue;
                                if(new File(Environment.getExternalStorageDirectory()+"/Android/media/"+getPackageName(),key).isDirectory())continue;
                                try{
                                    version = getPackageManager().getPackageInfo(key,0).versionName;
                                    if(compareVersionNames(version,jsonObject.getJSONObject(key).getString("version"))==1){
                                        appsToGet.add(key);
                                    }
                                }catch (PackageManager.NameNotFoundException ignored){
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Updates needed for "+appsToGet.toString());
                        String text = "<update>"+appsToGet.size();
                        Payload bytesPayload = Payload.fromBytes(text.getBytes(StandardCharsets.UTF_8));
                        Nearby.getConnectionsClient(getApplicationContext()).sendPayload(s, bytesPayload);
                    }
                    if(string.startsWith("<update>")){
                        string = string.replace("<update>","");
                        int updCount = Integer.parseInt(string);
                        if(updCount==0&&appsToGet.size()==0){
                            String text = "<end>";
                            Payload bytesPayload = Payload.fromBytes(text.getBytes(StandardCharsets.UTF_8));
                            Nearby.getConnectionsClient(getApplicationContext()).sendPayload(s, bytesPayload);
                            sharedPreferences.edit().putString(String.valueOf(System.currentTimeMillis()/86400000),sharedPreferences.getString(String.valueOf(System.currentTimeMillis()/86400000),"")+","+connectedEnd).apply();
                            Nearby.getConnectionsClient(getApplicationContext()).disconnectFromEndpoint(s);
                            return;
                        }
                        if(updCount>appsToGet.size()){
                            String text = "<query>";
                            Payload bytesPayload = Payload.fromBytes(text.getBytes(StandardCharsets.UTF_8));
                            Nearby.getConnectionsClient(getApplicationContext()).sendPayload(s, bytesPayload);
                        }else if(updCount==appsToGet.size()){
                            if(requestedFromHere){
                                String text;
                                System.out.println("appstoget "+appsToGet.size());
                                if(!appsToGet.isEmpty()){
                                    text = "<sendpackage>"+appsToGet.get(0);
                                }else{
                                    text = "<query>";
                                }
                                Payload bytesPayload = Payload.fromBytes(text.getBytes(StandardCharsets.UTF_8));
                                Nearby.getConnectionsClient(getApplicationContext()).sendPayload(s, bytesPayload);
                            }
                        }
                    }
                    if(string.equals("<query>")){
                        String text;
                        if(!appsToGet.isEmpty()){
                            text = "<sendpackage>"+appsToGet.get(0);
                            System.out.println("requested app "+appsToGet.get(0));
                        }else{
                            text = "<query>";
                        }
                        Payload bytesPayload = Payload.fromBytes(text.getBytes(StandardCharsets.UTF_8));
                        Nearby.getConnectionsClient(getApplicationContext()).sendPayload(s, bytesPayload);
                    }
                    if(string.startsWith("<sendpackage>")){
                        string = string.replace("<sendpackage>","");
                        //File fileToSend = null;
                        try {
                            System.out.println("app directory "+getPackageManager().getApplicationInfo(string,0).publicSourceDir);
                            ArrayList<String> apks = new ArrayList<>();
                            listfiles(new File(getPackageManager().getApplicationInfo(string,0).publicSourceDir).getParent(),apks);
                            JSONObject jsonObject = new JSONObject();
                            for(String f : apks){
                                if(f.endsWith(".apk"))jsonObject.put(f,"");
                            }
                            String text = "<packages>"+jsonObject.toString();
                            Payload bytesPayload = Payload.fromBytes(text.getBytes(StandardCharsets.UTF_8));
                            Nearby.getConnectionsClient(getApplicationContext()).sendPayload(s, bytesPayload);
                            //fileToSend = new File(getPackageManager().getApplicationInfo(string,0).publicSourceDir);
                            //Payload filePayload = Payload.fromFile(fileToSend);
                            //Nearby.getConnectionsClient(getApplicationContext()).sendPayload(s, filePayload);
                        } catch (PackageManager.NameNotFoundException | JSONException e) { }
                    }
                    if(string.startsWith("<packages>")){
                        files.clear();
                        string = string.replace("<packages>","");
                        try {
                            JSONObject jsonObject = new JSONObject(string);
                            for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                                String s2 = it.next();
                                files.add(s2);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if(files.size()==0) {
                            return;
                        }
                        count=files.size();
                        System.out.println("count value "+count);
                        String text = "<sendfile>"+files.get(0);
                        filename=files.get(0);
                        System.out.println("requested file "+files.get(0));
                        Payload bytesPayload = Payload.fromBytes(text.getBytes(StandardCharsets.UTF_8));
                        Nearby.getConnectionsClient(getApplicationContext()).sendPayload(s, bytesPayload);
                    }
                    if(string.startsWith("<sendfile>")){
                        string = string.replace("<sendfile>","");
                        File fileToSend = new File(string);
                        try {
                            Payload filePayload = Payload.fromFile(fileToSend);
                            Nearby.getConnectionsClient(getApplicationContext()).sendPayload(s, filePayload);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    if(string.equals("<end>")){
                        sharedPreferences.edit().putString(String.valueOf(System.currentTimeMillis()/86400000),sharedPreferences.getString(String.valueOf(System.currentTimeMillis()/86400000),"")+","+connectedEnd).apply();
                    }
                }
                else if(payload.getType()==Payload.Type.FILE){
                    filePayload=payload;
                    System.out.println("set payloadfile");
                }
            }
            @Override
            public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
                if(payloadTransferUpdate.getStatus()==SUCCESS){
                    System.out.println("DETECTED PAYLOAD SUCCESS");
                    if(filePayload!=null){
                        File payloadFile = filePayload.asFile().asJavaFile();
                        if(!payloadFile.exists())return;
                        String finalname;
                        if(count<2) {
                            finalname=appsToGet.get(0);
                            payloadFile.renameTo(new File(Environment.getExternalStorageDirectory()+"/Android/media/"+getPackageName(), finalname+"apk"));
                        } else {
                            if(files.size()!=0){
                                finalname=new File(files.get(0)).getName();
                                new File(Environment.getExternalStorageDirectory()+"/Android/media/"+getPackageName()+"/"+appsToGet.get(0)).mkdirs();
                                payloadFile.renameTo(new File(Environment.getExternalStorageDirectory()+"/Android/media/"+getPackageName()+"/"+appsToGet.get(0), finalname));
                            }
                        }
                        if(files.size()!=0)files.remove(0);
                        System.out.println("files size "+files.size());
                        if(files.size()>0){
                            String text = "<sendfile>"+files.get(0);
                            filename=files.get(0);
                            System.out.println("requested file "+files.get(0));
                            Payload bytesPayload = Payload.fromBytes(text.getBytes(StandardCharsets.UTF_8));
                            Nearby.getConnectionsClient(getApplicationContext()).sendPayload(s, bytesPayload);
                        } else {
                            appsToGet.remove(0);
                            System.out.println(appsToGet);
                            System.out.println(files);
                            String text = "<update>"+appsToGet.size();
                            Payload bytesPayload = Payload.fromBytes(text.getBytes(StandardCharsets.UTF_8));
                            Nearby.getConnectionsClient(getApplicationContext()).sendPayload(s, bytesPayload);
                            filePayload=null;
                        }try {
                            MainActivity.refresh();
                        }catch (Exception ignored){}
                    }
                }
            }
        };
        startAdvertising();
        startDiscovery();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground()
    {
        String NOTIFICATION_CHANNEL_ID = getPackageName()+".background";
        String channelName = "Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(1, notification);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    private void startAdvertising() {
        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build();
        Nearby.getConnectionsClient(getApplicationContext())
                .startAdvertising(macAddress(), SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            System.out.println("ADVERTISING");
                            // We're advertising!
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            System.out.println("NOT ADVERTISING");
                            // We were unable to start advertising.
                        });
    }
    private void startDiscovery() {
        DiscoveryOptions discoveryOptions =
                new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build();
        Nearby.getConnectionsClient(getApplicationContext())
                .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            System.out.println("DISCOVERING");
                            // We're discovering!
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            System.out.println("NOT DISCOVERING");
                            // We're unable to start discovering.
                        });
    }
    public static String macAddress() {
        try {
            String interfaceName = "wlan0";
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (!intf.getName().equalsIgnoreCase(interfaceName)){
                    continue;
                }

                byte[] mac = intf.getHardwareAddress();
                if (mac==null){
                    return "";
                }

                StringBuilder buf = new StringBuilder();
                for (byte aMac : mac) {
                    buf.append(String.format("%02X:", aMac));
                }
                if (buf.length()>0) {
                    buf.deleteCharAt(buf.length() - 1);
                }
                return buf.toString();
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }
    public static int compareVersionNames(String oldVersionName, String newVersionName) {
        int res = 0;
        oldVersionName=oldVersionName.replace("_","");
        newVersionName=newVersionName.replace("_","");

        oldVersionName=oldVersionName.replaceAll("[^\\d.]","");
        newVersionName=newVersionName.replaceAll("[^\\d.]","");

        String[] oldNumbers = oldVersionName.split("\\.");
        String[] newNumbers = newVersionName.split("\\.");

        // To avoid IndexOutOfBounds
        int maxIndex = Math.min(oldNumbers.length, newNumbers.length);

        for (int i = 0; i < maxIndex; i ++) {
            if(oldNumbers[i].isEmpty())continue;
            if(newNumbers[i].isEmpty())continue;

            long oldVersionPart = Long.parseLong(oldNumbers[i]);
            long newVersionPart = Long.parseLong(newNumbers[i]);

            if (oldVersionPart < newVersionPart) {
                res = 1;
                break;
            } else if (oldVersionPart > newVersionPart) {
                res = -1;
                break;
            }
        }
        // If versions are the same so far, but they have different length...
        if (res == 0 && oldNumbers.length != newNumbers.length) {
            res = (oldNumbers.length > newNumbers.length)?-1:1;
        }
        return res;
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
}