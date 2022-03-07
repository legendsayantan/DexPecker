package com.legendsayantan.dexpecker;

import static android.content.Context.JOB_SCHEDULER_SERVICE;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;

public class ServiceStarter extends BroadcastReceiver {
    static JobScheduler jobScheduler;
    @Override
    public void onReceive(Context context, Intent intent) {
            scheduleJob(context);
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void scheduleJob(Context context) {
        if (jobScheduler == null) {
            jobScheduler = (JobScheduler) context
                    .getSystemService(JOB_SCHEDULER_SERVICE);
        }
        ComponentName componentName = new ComponentName(context,
                JobService.class);
        JobInfo jobInfo = new JobInfo.Builder(1, componentName)
                // setOverrideDeadline runs it immediately - you must have at least one constraint
                // https://stackoverflow.com/questions/51064731/firing-jobservice-without-constraints
                .setOverrideDeadline(0)
                .setPersisted(true).build();
        jobScheduler.schedule(jobInfo);
    }
}