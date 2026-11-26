package kr.personal.historyjobs;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.content.pm.PackageManager;
import kr.personal.historyjobs.core.*;
import static kr.personal.historyjobs.core.Model.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public final class SyncEngine {
    private static final ReentrantLock LOCK=new ReentrantLock();
    private SyncEngine(){}
    public interface Progress {void update(String message);}
    public static boolean busy(){return LOCK.isLocked();}
    public static boolean run(Context c,Progress progress){
        if(!LOCK.tryLock())return true;
        Context app=c.getApplicationContext();Store store=Store.get(app);boolean ok=true;
        try{
            Collector collector=new Collector(Net::get);
            for(Source source:store.sources()){
                if(Thread.currentThread().isInterrupted())return false;
                if(!source.enabled)continue;
                progress.update(source.name+" 확인 중…");long now=System.currentTimeMillis();
                try{
                    Collector.Result result=collector.collect(source,store.notices(),now);
                    store.merge(source,result.notices());
                    String summary=(result.partial()?"일부 확인":"확인 완료")+" · 목록 "+result.listed()+"건 / 조건 일치 "+result.checked()+"건";
                    if(!result.errors().isEmpty())summary+="\n"+String.join("\n",result.errors());
                    store.sourceResult(source,summary,true,now);
                }catch(Exception e){ok=false;store.sourceResult(source,"확인 실패 · "+Collector.shortError(e),false,now);}
            }
            app.getSharedPreferences("settings",0).edit().putLong("lastRun",System.currentTimeMillis()).apply();
            postPending(app,store);progress.update(ok?"확인을 마쳤어요. 게시판별 상태도 확인해 주세요.":"일부 게시판에 연결하지 못했어요. 게시판 탭에서 확인해 주세요.");
            return ok;
        }finally{LOCK.unlock();}
    }
    public static void postPending(Context c,Store store){
        if(!c.getSharedPreferences("settings",0).getBoolean("alerts",true))return;
        NotificationManager manager=c.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("jobs","채용공고",NotificationManager.IMPORTANCE_DEFAULT));
        if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        if(!manager.areNotificationsEnabled())return;
        android.content.SharedPreferences sent=c.getSharedPreferences("notified",0);
        Set<String> active=new HashSet<>();for(Source s:store.sources())if(s.enabled)active.add(s.id);
        List<Notice> matches=new ArrayList<>();long now=System.currentTimeMillis();
        for(Notice n:store.notices().values())if(active.contains(n.sourceId)&&n.status==Status.MATCH&&Classifier.matches(n)&&!n.read&&(n.deadline==0||n.deadline>=now)&&!n.fingerprint.equals(sent.getString(n.id,"")))matches.add(n);
        if(matches.isEmpty())return;
        matches.sort(Comparator.comparingLong((Notice n)->n.firstSeen).reversed());
        StringBuilder detail=new StringBuilder();for(Notice n:matches.subList(0,Math.min(8,matches.size())))detail.append("• ").append(n.title).append('\n');
        Intent intent=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent open=PendingIntent.getActivity(c,0,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(c,"jobs").setSmallIcon(R.drawable.ic_notification_school).setColor(Ui.BROWN).setLargeIcon(largeIcon(c)).setContentTitle("새 채용공고 "+matches.size()+"건").setContentText(matches.get(0).title).setStyle(new Notification.BigTextStyle().bigText(detail.toString())).setContentIntent(open).setAutoCancel(true).build();
        try{manager.notify(100,n);android.content.SharedPreferences.Editor edit=sent.edit();for(Notice x:matches)edit.putString(x.id,x.fingerprint);edit.apply();}catch(SecurityException ignored){}
    }
    private static android.graphics.Bitmap largeIcon(Context c){android.graphics.BitmapFactory.Options o=new android.graphics.BitmapFactory.Options();o.inSampleSize=8;return android.graphics.BitmapFactory.decodeResource(c.getResources(),R.drawable.ic_launcher,o);}
    public static void testNotification(Context c){
        NotificationManager nm=c.getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("jobs","채용공고",NotificationManager.IMPORTANCE_DEFAULT));
        if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        nm.notify(101,new Notification.Builder(c,"jobs").setSmallIcon(R.drawable.ic_notification_school).setColor(Ui.BROWN).setLargeIcon(largeIcon(c)).setContentTitle("교원 채용공고 알리미 · 테스트 알림").setContentText("앱 알림이 켜져 있어요.").setAutoCancel(true).build());
    }
}
