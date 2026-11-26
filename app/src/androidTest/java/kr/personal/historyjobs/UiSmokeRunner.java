package kr.personal.historyjobs;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import kr.personal.historyjobs.core.*;
import static kr.personal.historyjobs.core.Model.*;

/** Emulator-only interaction checks and screenshots; fixtures never enter release. */
public final class UiSmokeRunner extends Instrumentation {
    private MainActivity activity;private File screenshots;
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        Context c=getTargetContext();screenshots=new File(c.getExternalFilesDir(null),"ui-screenshots");screenshots.mkdirs();
        c.getSharedPreferences("settings",0).edit().putBoolean("permissionAsked",true).putBoolean("automatic",false).putBoolean("alerts",false).apply();
        Store store=Store.get(c);store.restore("{\"schema\":1,\"selectionVersion\":2,\"sources\":[],\"notices\":[]}");long now=System.currentTimeMillis();
        for(Source s:store.sources())store.removeSource(s.id);
        for(Source s:defaults()){s.message=s.id.equals("tokyo")?"확인 실패 · 연결 상태를 확인해 주세요":"목록 확인 완료 · 역사 공고 수집";s.lastSuccess=s.id.equals("tokyo")?0:now;store.putSource(s);}
        Source src=store.source("sen");List<Notice> notices=new ArrayList<>();
        String[] titles={"가람중학교 역사 기간제교원 채용 공고","나래고등학교 2027학년도 역사과 기간제교원 채용 재공고 (2차)","다온중학교 역사 기간제교원 채용"};
        for(int i=0;i<3;i++){Notice n=new Notice();n.id="preview"+i;n.sourceId="sen";n.sourceName=src.name;n.school=i==0?"가람중학교":i==1?"나래고등학교":"다온중학교";n.region="서울";n.url="https://example.org/preview/"+i;n.title=titles[i];n.deadline=now+(i==2?-1L:i+1L)*86400000L;n.deadlineLabel=java.time.Instant.ofEpochMilli(n.deadline).atZone(Classifier.KST).format(java.time.format.DateTimeFormatter.ofPattern("MM.dd HH:mm"));n.firstSeen=now;n.status=Status.MATCH;n.complete=true;n.fingerprint=n.contentHash();notices.add(n);}
        store.merge(src,notices);
        activity=(MainActivity)startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));idle();
        visible("nav_notices");visible("nav_sources");visible("nav_settings");expect("notice_preview0",true);expect("notice_preview2",false);capture("01-notices");
        click("favorite_preview0");if(!store.notices().get("preview0").favorite)throw new AssertionError("Bookmark not saved");
        click("filter_favorite");expect("notice_preview0",true);expect("notice_preview1",false);capture("02-favorites");
        main(()->((EditText)find("search_field")).setText("없는학교"));idle();expect("notice_preview0",false);capture("03-search-empty");
        click("search_clear");expect("notice_preview0",true);click("filter_all");click("closed_toggle");expect("notice_preview2",true);
        click("notice_preview0");capture("04-detail");sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);idle();
        click("nav_sources");visible("nav_sources");capture("05-sources");
        click("source_enabled_sen");if(store.source("sen").enabled)throw new AssertionError("Source switch failed");click("source_enabled_sen");
        click("add_source");capture("06-source-form");sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);idle();
        click("nav_settings");visible("automatic_toggle");capture("07-settings");click("automatic_toggle");if(!c.getSharedPreferences("settings",0).getBoolean("automatic",false))throw new AssertionError("Schedule switch failed");click("automatic_toggle");
        main(()->((ScrollView)find("page_scroll")).fullScroll(View.FOCUS_DOWN));idle();capture("08-settings-data");
        result.putString("stream","OK (1 test)\nUI interactions, filtering, bookmarks, source/schedule toggles and screenshots passed.\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable t){result.putString("stream","FAIL: "+t+"\n"+android.util.Log.getStackTraceString(t));finish(Activity.RESULT_CANCELED,result);}}
    private void main(Runnable r){AtomicReference<Throwable> error=new AtomicReference<>();runOnMainSync(()->{try{r.run();}catch(Throwable t){error.set(t);}});if(error.get()!=null)throw new AssertionError(error.get());}
    private void idle(){waitForIdleSync();SystemClock.sleep(250);}
    private View find(String tag){return activity.getWindow().getDecorView().findViewWithTag(tag);}
    private void click(String tag){main(()->{View v=find(tag);if(v==null)throw new AssertionError("Missing "+tag);if(!v.isEnabled()||!v.isClickable())throw new AssertionError("Cannot click "+tag);v.performClick();});idle();}
    private void expect(String tag,boolean present){main(()->{if((find(tag)!=null)!=present)throw new AssertionError("Unexpected presence "+tag);});}
    private void visible(String tag){main(()->{View v=find(tag);android.graphics.Rect rect=new android.graphics.Rect();if(v==null||!v.getGlobalVisibleRect(rect)||rect.height()<v.getHeight())throw new AssertionError("Clipped control "+tag);});}
    private void capture(String name)throws Exception{idle();Bitmap image=getUiAutomation().takeScreenshot();if(image==null)throw new IOException("Screenshot unavailable");try(OutputStream out=new FileOutputStream(new File(screenshots,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}finally{image.recycle();}}
}
