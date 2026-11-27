package kr.personal.historyjobs;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import kr.personal.historyjobs.core.*;
import static kr.personal.historyjobs.core.Model.*;

/** Emulator-only interaction checks and screenshots; fixtures never enter release. */
public final class UiSmokeRunner extends Instrumentation {
    private MainActivity activity;private File screenshots;private PageServer server;
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        server=new PageServer();Context c=getTargetContext();c.getSharedPreferences("notified",0).edit().clear().apply();screenshots=new File(c.getExternalFilesDir(null),"ui-screenshots");screenshots.mkdirs();
        c.getSharedPreferences("settings",0).edit().remove("noticeRegions").remove("noticeSources").remove("sourceRegions").remove("sourceDescending").putBoolean("permissionAsked",true).putBoolean("automatic",false).putBoolean("alerts",false).apply();
        Store store=Store.get(c);store.restore("{\"schema\":1,\"selectionVersion\":2,\"sources\":[],\"notices\":[]}");long now=System.currentTimeMillis();
        if(store.source("sen-regular")==null||store.source("goe-regular")==null)throw new AssertionError("Regular boards not added on migration");
        if(store.source("sejong-science")==null||store.source("anyang-fl")==null||!store.source("dongducheon-fl").keywords.equals(List.of("교사 AND 공고")))throw new AssertionError("School catalog migration or AND seed missing");
        if(store.source("seoul-international")==null||store.source("daewon-international")==null||!store.source("sejong-international").region.equals("기타 지역"))throw new AssertionError("International school migration missing");
        store.removeSource("seoul-international");store.restore(store.exportData().toString());if(store.source("seoul-international")!=null)throw new AssertionError("Deleted international board returned");
        store.removeSource("sejong-science");store.restore(store.exportData().toString());if(store.source("sejong-science")!=null)throw new AssertionError("Deleted school returned after restore");
        store.removeSource("goe-regular");store.restore(store.exportData().toString());if(store.source("goe-regular")!=null)throw new AssertionError("Deleted regular board returned after backup restore");
        for(Source s:store.sources())store.removeSource(s.id);
        for(Source s:defaults()){s.message=s.id.equals("tokyo")?"확인 실패 · 연결 상태를 확인해 주세요":"목록 확인 완료 · 역사 공고 수집";s.lastSuccess=s.id.equals("tokyo")?0:now;store.putSource(s);}
        Source src=store.source("sen");List<Notice> notices=new ArrayList<>();
        String[] titles={"가람중학교 역사 기간제교원 채용 공고","나래고등학교 2027학년도 역사과 기간제교원 채용 재공고 (2차)","다온중학교 역사 기간제교원 채용"};
        for(int i=0;i<3;i++){Notice n=recentNotice();n.id="preview"+i;n.sourceId="sen";n.sourceName=src.name;n.school=i==0?"가람중학교":i==1?"나래고등학교":"다온중학교";n.region="서울";n.url=server.base()+"/post"+i;n.title=titles[i];n.deadline=now+(i==2?-1L:i+1L)*86400000L;n.deadlineLabel=java.time.Instant.ofEpochMilli(n.deadline).atZone(Classifier.KST).format(java.time.format.DateTimeFormatter.ofPattern("MM.dd HH:mm"));n.firstSeen=now;n.status=Status.MATCH;n.complete=true;n.fingerprint=n.contentHash();notices.add(n);}
        store.merge(src,notices);
        Notice dutyOnly=recentNotice();dutyOnly.id="duty-only";dutyOnly.sourceId=src.id;dutyOnly.title="선덕중학교 신규교사 채용 공고";dutyOnly.duty="과목(담당업무) · 역사(1명), 체육(1명)";dutyOnly.url=server.base()+"/duty-only";dutyOnly.firstSeen=now;store.merge(src,List.of(dutyOnly));
        if(store.notices().get("duty-only").status!=Status.MATCH)throw new AssertionError("Duty-only history missing");store.restore(store.exportData().toString());if(!store.notices().get("duty-only").duty.contains("역사")||store.notices().get("duty-only").status!=Status.MATCH)throw new AssertionError("Duty metadata backup failed");
        Source school=Model.source("school","추가한 학교",server.base()+"/list","AUTO","동경","");store.putSource(school);
        Notice broad=recentNotice();broad.id="school-broad";broad.sourceId=school.id;broad.sourceName=school.name;broad.title=java.time.LocalDate.now(Classifier.KST).getYear()+"학년도 추가한 학교 교직원 채용 공고";broad.url=server.base()+"/school-post";broad.status=Status.EXCLUDED;broad.body="이전 버전 안내문";broad.firstSeen=now;broad.fingerprint=broad.contentHash();store.merge(school,List.of(broad));
        Notice central=recentNotice();central.id="central-excluded";central.sourceId=src.id;central.title="교육청 교원 채용 공고";central.url=server.base()+"/central";central.status=Status.EXCLUDED;central.fingerprint=central.contentHash();store.merge(src,List.of(central));
        Notice custom=recentNotice();custom.id="school-custom";custom.sourceId=school.id;custom.sourceName=school.name;custom.title=java.time.LocalDate.now(Classifier.KST).getYear()+"학년도 가람학교 생활지도 담당자 모집";custom.url=server.base()+"/custom";custom.firstSeen=now;store.merge(school,List.of(custom));
        Notice ancient=recentNotice();ancient.id="old-post";ancient.sourceId=src.id;ancient.title="2027학년도 역사 채용";ancient.published=java.time.LocalDate.now(Classifier.KST).minusYears(2).withMonth(12).withDayOfMonth(31).toString();ancient.url=server.base()+"/ancient";ancient.favorite=true;store.merge(src,List.of(ancient));
        Notice unknown=recentNotice();unknown.id="unknown-date";unknown.sourceId=src.id;unknown.title="2026학년도 역사 채용";unknown.published="";unknown.url=server.base()+"/unknown";store.merge(src,List.of(unknown));
        Notice oldSchool=recentNotice();oldSchool.id="old-school-year";oldSchool.sourceId=school.id;oldSchool.title=(java.time.LocalDate.now(Classifier.KST).getYear()-1)+"학년도 교직원 채용";oldSchool.url=server.base()+"/school-old";oldSchool.favorite=true;store.merge(school,List.of(oldSchool));
        org.json.JSONObject legacy=store.exportData().put("selectionVersion",3);for(int i=0;i<legacy.getJSONArray("sources").length();i++){org.json.JSONObject source=legacy.getJSONArray("sources").getJSONObject(i);source.remove("keywords");if(source.optString("id").equals("school"))source.put("region","동경");}for(int i=0;i<legacy.getJSONArray("notices").length();i++)legacy.getJSONArray("notices").getJSONObject(i).remove("keywords");
        for(int i=0;i<legacy.getJSONArray("notices").length();i++){org.json.JSONObject item=legacy.getJSONArray("notices").getJSONObject(i);if(item.optString("id").equals("old-school-year"))item.put("status","MATCH");}
        store.restore(legacy.toString());if(store.notices().get("old-school-year").status!=Status.EXCLUDED||!store.notices().get("old-school-year").favorite)throw new AssertionError("Saved old academic year was not excluded on upgrade");if(!store.source("school").region.equals("기타 지역"))throw new AssertionError("Legacy region migration failed");
        if(store.notices().get("school-broad").status!=Status.MATCH||store.notices().get("central-excluded").status!=Status.EXCLUDED||!store.notices().get("school-broad").body.isEmpty())throw new AssertionError("Source-aware migration failed");
        for(Source s:store.sources())store.sourceResult(s,s.id.equals("tokyo")?"확인 실패 · 연결 상태를 확인해 주세요":"확인 완료 · 목록 20건 / 조건 일치 0건",!s.id.equals("tokyo"),now);
        activity=(MainActivity)startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));idle();
        oneLineHeading();expect("notice_duty-only",true);expect("page_intro",false);expect("notice_school-broad",true);expect("notice_central-excluded",false);expect("notice_old-post",false);expect("notice_old-school-year",false);expect("notice_unknown-date",false);expect("notice_school-custom",false);visible("nav_notices");visible("nav_sources");visible("nav_settings");expect("notice_preview0",true);expect("notice_preview2",false);capture("01-notices");
        click("favorite_preview0");if(!store.notices().get("preview0").favorite)throw new AssertionError("Bookmark not saved");
        click("filter_favorite");expect("notice_preview0",true);expect("notice_preview1",false);capture("02-favorites");
        main(()->((EditText)find("search_field")).setText("없는학교"));idle();expect("notice_preview0",false);capture("03-search-empty");
        click("search_clear");expect("notice_preview0",true);click("filter_all");click("closed_toggle");expect("notice_preview2",true);
        click("notice_preview0");awaitText("article_status","원문에서 불러온 본문");awaitText("article_body","테스트용 공고 본문");capture("04-detail");
        String fingerprint=store.notices().get("preview0").fingerprint;
        if(store.notices().get("preview0").bodyFetchedAt<=0||!store.notices().get("preview0").attachments.isEmpty())throw new AssertionError("Body cache not saved");
        store.restore(store.exportData().toString());if(!store.notices().get("preview0").body.contains("테스트용 공고 본문")||!store.notices().get("preview0").fingerprint.equals(fingerprint)||!store.notices().get("preview0").favorite)throw new AssertionError("Body/flags backup round trip failed");
        sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);idle();
        click("notice_preview1");awaitText("article_status","본문을 가져오지 못했어요");if(store.notices().get("preview1").status!=Status.MATCH)throw new AssertionError("Body failure changed selection");click("article_retry");awaitText("article_status","원문에서 불러온 본문");sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);idle();
        if(server.requests.stream().anyMatch(x->x.contains("pdf")||x.contains("download")||x.contains("hwp")))throw new AssertionError("Attachment request detected");
        chooseRegions(Set.of(1));expect("notice_preview0",true);expect("notice_school-broad",false);if(!c.getSharedPreferences("settings",0).getStringSet("noticeRegions",Set.of()).equals(Set.of("서울")))throw new AssertionError("Region filter not saved");
        click("filter_favorite");expect("notice_old-post",false);expect("notice_old-school-year",false);expect("notice_preview0",true);click("filter_all");expect("notice_school-broad",false);
        chooseRegions(Set.of(2));expect("notice_preview0",false);expect("notice_school-broad",true);capture("11-other-region");chooseRegions(Set.of(0,1,2));
        chooseBoards(Set.of("school"));expect("notice_preview0",false);expect("notice_school-broad",true);if(!c.getSharedPreferences("settings",0).getStringSet("noticeSources",Set.of()).equals(Set.of("school")))throw new AssertionError("Board filter not saved");
        chooseRegions(Set.of(1));expect("notice_school-broad",false);expect("notice_preview0",false);
        click("region_filter");click("filter_reset");click("region_apply");expect("notice_preview0",true);expect("notice_school-broad",true);if(c.getSharedPreferences("settings",0).contains("noticeSources"))throw new AssertionError("All-board filter should include future boards");
        click("region_filter");click("filter_board_school");main(()->((AlertDialog)dialog("regionPicker")).getButton(AlertDialog.BUTTON_NEGATIVE).performClick());idle();expect("notice_school-broad",true);
        click("nav_sources");oneLineHeading();visible("nav_sources");capture("05-sources");
        visible("source_sort");visible("source_region_filter");
        main(()->{String meta=((TextView)find("source_meta_sen")).getText().toString();if(!meta.contains("서울 · 키워드 \"")||!meta.endsWith("확인 완료")||!((TextView)find("source_meta_tokyo")).getText().toString().endsWith("확인 실패"))throw new AssertionError("Source region/keyword/latest-state metadata missing");});
        List<String> ascending=sourceOrder(),stored=store.sources().stream().map(s->s.id).collect(java.util.stream.Collectors.toList());List<String> reverse=new ArrayList<>(ascending);Collections.reverse(reverse);
        click("source_sort");capture("16-source-sort-choice");chooseSourceOrder(1);
        if(!sourceOrder().equals(reverse)||!c.getSharedPreferences("settings",0).getBoolean("sourceDescending",false)||!store.sources().stream().map(s->s.id).collect(java.util.stream.Collectors.toList()).equals(stored))throw new AssertionError("Source reverse order/persistence or collection-order independence failed");capture("15-sources-reversed");
        main(()->((EditText)find("source_search")).setText("추가한"));idle();expect("source_item_school",true);expect("source_item_sen",false);click("source_search_clear");expect("source_item_sen",true);
        chooseSourceRegions(Set.of("서울"));expect("source_item_sen",true);expect("source_item_school",false);if(!c.getSharedPreferences("settings",0).getStringSet("sourceRegions",Set.of()).equals(Set.of("서울"))||!store.source("school").enabled)throw new AssertionError("Source display filter changed collection or failed to persist");capture("14-source-filter");chooseSourceRegions(Set.of("서울","경기","기타 지역"));
        if(!sourceOrder().equals(reverse))throw new AssertionError("Search/region changes reset descending order");click("source_sort");chooseSourceOrder(0);if(!sourceOrder().equals(ascending)||c.getSharedPreferences("settings",0).getBoolean("sourceDescending",true))throw new AssertionError("Ascending order failed to restore");
        Source originalSchool=store.source("school");expect("edit_source_school",false);click("source_item_school");visible("source_detail_close");capture("13-source-detail");click("edit_source_school");main(()->{((EditText)find("source_keywords")).setText("생활 지도 AND 모집 OR 면접 공고");((Spinner)find("source_region")).setSelection(0);});click("source_save");
        if(!store.source("school").keywords.equals(List.of("생활 지도 AND 모집","면접 공고"))||!store.source("school").region.equals("경기")||store.notices().get("school-custom").status!=Status.MATCH||store.notices().get("school-broad").status!=Status.EXCLUDED)throw new AssertionError("Keyword/region editing did not reclassify saved notices");
        Notice stale=recentNotice();stale.id="stale";stale.title="교직원 채용";stale.url=server.base()+"/stale";if(!store.merge(originalSchool,List.of(stale)).isEmpty()||store.notices().containsKey("stale"))throw new AssertionError("Stale source request overwrote edited keyword config");
        click("nav_notices");expect("notice_school-custom",true);expect("notice_school-broad",false);store.restore(store.exportData().toString());if(!store.source("school").keywords.equals(List.of("생활 지도 AND 모집","면접 공고"))||store.notices().get("school-custom").published.isEmpty())throw new AssertionError("Keyword/publication backup failed");
        store.putSource(originalSchool);click("nav_sources");
        click("source_item_sen");click("source_enabled_sen");if(store.source("sen").enabled)throw new AssertionError("Source switch failed");click("source_enabled_sen");click("source_detail_close");
        click("add_source");capture("06-source-form");sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);idle();
        click("nav_settings");oneLineHeading();visible("automatic_toggle");capture("07-settings");click("automatic_toggle");if(!c.getSharedPreferences("settings",0).getBoolean("automatic",false))throw new AssertionError("Schedule switch failed");click("automatic_toggle");
        main(()->((ScrollView)find("page_scroll")).fullScroll(View.FOCUS_DOWN));idle();capture("08-settings-data");
        c.getSharedPreferences("settings",0).edit().putBoolean("alerts",true).apply();SyncEngine.postPending(c,store);idle();
        NotificationManager nm=c.getSystemService(NotificationManager.class);boolean found=false;
        for(android.service.notification.StatusBarNotification posted:nm.getActiveNotifications())if(posted.getId()==100){Notification n=posted.getNotification();found=n.getSmallIcon().getResId()==R.drawable.ic_notification_school&&n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT,"").toString().contains("교직원 채용");}
        if(!found)throw new AssertionError("Broad school title notification or small icon incorrect");
        SyncEngine.testNotification(c);idle();for(android.service.notification.StatusBarNotification posted:nm.getActiveNotifications())if(posted.getId()==101&&posted.getNotification().getSmallIcon().getResId()!=R.drawable.ic_notification_school)throw new AssertionError("Test notification icon incorrect");
        getUiAutomation().executeShellCommand("cmd statusbar expand-notifications").close();SystemClock.sleep(600);capture("09-notification");getUiAutomation().executeShellCommand("cmd statusbar collapse").close();nm.cancelAll();
        c.getSharedPreferences("settings",0).edit().putBoolean("alerts",false).apply();
        Instrumentation.ActivityMonitor originalMonitor=addMonitor(InspectActivity.class.getName(),new Instrumentation.ActivityResult(Activity.RESULT_OK,null),true);
        main(()->{try{var open=MainActivity.class.getDeclaredMethod("open",String.class);open.setAccessible(true);open.invoke(activity,GoyangBoard.article("https://www.gyfl.hs.kr/main/main.php?categoryid=12&menuid=03&groupid=00","6807"));}catch(Exception e){throw new AssertionError(e);}});idle();
        if(originalMonitor.getHits()!=1)throw new AssertionError("Goyang original must open in POST-capable page viewer");removeMonitor(originalMonitor);
        result.putString("stream","OK (1 test)\nUI date exclusion, keyword/region editing and backup, stale config guard, region filter persistence, body and notification checks passed.\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable t){result.putString("stream","FAIL: "+t+"\n"+android.util.Log.getStackTraceString(t));finish(Activity.RESULT_CANCELED,result);}finally{if(server!=null)server.close();}}
    private static Notice recentNotice(){Notice n=new Notice();n.published=java.time.LocalDate.now(Classifier.KST).toString();return n;}
    private void main(Runnable r){AtomicReference<Throwable> error=new AtomicReference<>();runOnMainSync(()->{try{r.run();}catch(Throwable t){error.set(t);}});if(error.get()!=null)throw new AssertionError(error.get());}
    private void idle(){waitForIdleSync();SystemClock.sleep(250);}
    private Dialog dialog(String fieldName){try{var f=MainActivity.class.getDeclaredField(fieldName);f.setAccessible(true);return (Dialog)f.get(activity);}catch(Exception e){throw new AssertionError(e);}}
    private View find(String tag){View v=activity.getWindow().getDecorView().findViewWithTag(tag);if(v!=null)return v;for(String field:new String[]{"articleDialog","regionPicker","sourceEditor","sourceDetails","sourceRegionPicker"}){Dialog d=dialog(field);if(d!=null){v=d.getWindow().getDecorView().findViewWithTag(tag);if(v!=null)return v;}}return null;}
    private void chooseRegions(Set<Integer> selected)throws Exception{click("region_filter");main(()->{String[] names={"경기","서울","기타 지역"};for(int i=0;i<names.length;i++)((CheckBox)find("filter_region_"+names[i])).setChecked(selected.contains(i));});if(selected.equals(Set.of(1)))capture("10-region-filter");click("region_apply");}
    private void chooseSourceRegions(Set<String> selected)throws Exception{click("source_region_filter");main(()->{for(String name:List.of("서울","경기","기타 지역"))((CheckBox)find("source_filter_"+name)).setChecked(selected.contains(name));});click("source_region_apply");}
    private void chooseSourceOrder(int which){main(()->{ListView list=((AlertDialog)dialog("sourceSortPicker")).getListView();list.performItemClick(list.getChildAt(which),which,which);});idle();}
    private List<String> sourceOrder(){List<String> result=new ArrayList<>();main(()->{ViewGroup list=(ViewGroup)find("source_list");for(int i=0;i<list.getChildCount();i++){Object tag=list.getChildAt(i).getTag();if(tag instanceof String&&((String)tag).startsWith("source_item_"))result.add((String)tag);}});return result;}
    private void chooseBoards(Set<String> selected)throws Exception{click("region_filter");main(()->{for(Source s:Store.get(getTargetContext()).sources())((CheckBox)find("filter_board_"+s.id)).setChecked(selected.contains(s.id));});capture("12-board-filter");click("region_apply");}

    private void click(String tag){main(()->{View v=find(tag);if(v==null)throw new AssertionError("Missing "+tag);if(!v.isEnabled()||!v.isClickable())throw new AssertionError("Cannot click "+tag);v.performClick();});idle();}
    private void expect(String tag,boolean present){main(()->{if((find(tag)!=null)!=present)throw new AssertionError("Unexpected presence "+tag);});}
    private void visible(String tag){main(()->{View v=find(tag);android.graphics.Rect rect=new android.graphics.Rect();if(v==null||!v.getGlobalVisibleRect(rect)||rect.height()<v.getHeight())throw new AssertionError("Clipped control "+tag);});}
    private void oneLineHeading(){main(()->{for(String tag:new String[]{"page_heading","page_intro"}){TextView t=(TextView)find(tag);if(t!=null&&t.getLineCount()!=1)throw new AssertionError("Heading wraps: "+t.getText());}});}
    private void awaitText(String tag,String text){long until=SystemClock.uptimeMillis()+12_000;while(SystemClock.uptimeMillis()<until){final boolean[] found={false};main(()->{View v=find(tag);found[0]=v instanceof TextView&&((TextView)v).getText().toString().contains(text);});if(found[0])return;SystemClock.sleep(100);}throw new AssertionError("Timed out waiting for "+tag+": "+text);}
    private static final class PageServer {
        private final ServerSocket socket;final List<String> requests=Collections.synchronizedList(new ArrayList<>());private final AtomicInteger retries=new AtomicInteger();
        PageServer()throws IOException{socket=new ServerSocket(0,16,InetAddress.getByName("127.0.0.1"));Thread t=new Thread(()->{while(!socket.isClosed())try(Socket client=socket.accept()){
            BufferedReader in=new BufferedReader(new InputStreamReader(client.getInputStream(),StandardCharsets.UTF_8));String request=in.readLine();if(request==null)continue;String path=request.split(" ")[1];requests.add(path);String line;while((line=in.readLine())!=null&&!line.isEmpty()){}
            boolean fail=path.equals("/post1")&&retries.getAndIncrement()==0;
            String html="<article><p>테스트용 공고 본문입니다. 모집 분야와 접수 방법을 앱 안에서 확인할 수 있어요. 학교급이나 자격 조건으로 공고를 다시 걸러내지 않습니다.</p><a href='/do-not-fetch.pdf'>첨부파일.pdf</a><iframe src='/download?file=a.hwp'></iframe></article>";
            byte[] bytes=html.getBytes(StandardCharsets.UTF_8);OutputStream out=client.getOutputStream();out.write(("HTTP/1.1 "+(fail?"503 Unavailable":"200 OK")+"\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: "+bytes.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));out.write(bytes);out.flush();
        }catch(IOException e){if(!socket.isClosed())throw new RuntimeException(e);}},"body-fixture-server");t.setDaemon(true);t.start();}
        String base(){return "http://127.0.0.1:"+socket.getLocalPort();}void close(){try{socket.close();}catch(IOException ignored){}}
    }
    private void capture(String name)throws Exception{idle();Bitmap image=getUiAutomation().takeScreenshot();if(image==null)throw new IOException("Screenshot unavailable");try(OutputStream out=new FileOutputStream(new File(screenshots,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}finally{image.recycle();}}
}
