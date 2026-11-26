package kr.personal.historyjobs;
import android.content.*;
public class RescheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){CheckWorker.reschedule(c.getApplicationContext());}
}
