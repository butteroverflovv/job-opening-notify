package kr.personal.historyjobs;

import android.content.Context;
import androidx.work.*;
import java.time.*;
import java.util.concurrent.TimeUnit;
import kr.personal.historyjobs.core.Classifier;

public class CheckWorker extends Worker {
    private static final String TAG="history-check";
    private volatile Thread checkingThread;
    public CheckWorker(Context c,WorkerParameters p){super(c,p);}
    @Override public Result doWork(){
        Context c=getApplicationContext();
        if(!c.getSharedPreferences("settings",0).getBoolean("automatic",true))return Result.success();
        checkingThread=Thread.currentThread();
        try{
            if(isStopped())return Result.success();
            // Reserve the next day before networking so errors cannot break future checks.
            schedule(c);
            boolean ok=SyncEngine.run(c,message->{});
            if(!ok&&getRunAttemptCount()<2&&!isStopped())return Result.retry();
            return Result.success();
        }finally{checkingThread=null;}
    }
    @Override public void onStopped(){super.onStopped();Thread running=checkingThread;if(running!=null)running.interrupt();}
    public static void schedule(Context c){
        WorkManager wm=WorkManager.getInstance(c);
        android.content.SharedPreferences prefs=c.getSharedPreferences("settings",0);
        if(!prefs.getBoolean("automatic",true)){wm.cancelAllWorkByTag(TAG);return;}
        ZonedDateTime now=ZonedDateTime.now(Classifier.KST);
        for(int i=0;i<2;i++){
            int minutes=prefs.getInt("slot"+i,i==0?720:1080);
            ZonedDateTime next=kr.personal.historyjobs.core.Schedule.next(now.toInstant(),minutes);
            long delay=Duration.between(now,next).toMillis();
            Data input=new Data.Builder().putLong("target",next.toInstant().toEpochMilli()).build();
            OneTimeWorkRequest req=new OneTimeWorkRequest.Builder(CheckWorker.class).setInitialDelay(delay,TimeUnit.MILLISECONDS).setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,10,TimeUnit.MINUTES).setInputData(input).addTag(TAG).build();
            String name="history-check-"+next.toLocalDate()+"-"+i;
            wm.enqueueUniqueWork(name,ExistingWorkPolicy.KEEP,req);
        }
    }
    public static void reschedule(Context c){WorkManager.getInstance(c).cancelAllWorkByTag(TAG).getResult().addListener(()->schedule(c),Runnable::run);}
}
