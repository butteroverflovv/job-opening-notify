package kr.personal.historyjobs;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import kr.personal.historyjobs.core.*;
import static kr.personal.historyjobs.core.Model.*;

public class MainActivity extends Activity {
    private static final int BG=0xfff6f5f0,INK=0xff1e3030,GREEN=0xff246854,MUTED=0xff65736d,LINE=0xffe1e5dc,AMBER=0xff916321;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private Store store;private LinearLayout root,screen,noticeList;private TextView progress;
    private int tab=0,filter=0;private String query="";private boolean closed=false;private volatile String running="";
    private android.content.SharedPreferences prefs;
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener settingsListener=(p,key)->{if("lastRun".equals(key)&&root!=null&&!isDestroyed())render();};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);store=Store.get(this);prefs=getSharedPreferences("settings",0);
        if(state!=null){tab=state.getInt("tab");filter=state.getInt("filter");query=state.getString("query","");}
        CheckWorker.schedule(this);render();
        if(Build.VERSION.SDK_INT>=33&&!prefs.getBoolean("permissionAsked",false)){
            new AlertDialog.Builder(this).setTitle("새 역사 공고를 알려드릴게요").setMessage("공고 목록은 권한 없이도 볼 수 있어요. 새 공고 알림을 받으려면 다음 화면에서 알림을 허용해 주세요.").setPositiveButton("알림 설정",(d,w)->requestAlerts()).setNegativeButton("나중에",(d,w)->prefs.edit().putBoolean("permissionAsked",true).apply()).show();
        }
    }
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putInt("tab",tab);b.putInt("filter",filter);b.putString("query",query);}
    @Override protected void onResume(){super.onResume();if(root!=null){prefs.registerOnSharedPreferenceChangeListener(settingsListener);SyncEngine.postPending(this,store);render();}}
    @Override protected void onPause(){if(prefs!=null)prefs.unregisterOnSharedPreferenceChangeListener(settingsListener);super.onPause();}
    @Override protected void onDestroy(){executor.shutdown();super.onDestroy();}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(dp(3),1);return v;}
    private TextView bold(String s,int size,int color){TextView v=text(s,size,color);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private GradientDrawable shape(int color,int stroke){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(18));if(stroke!=0)g.setStroke(dp(1),stroke);return g;}
    private void space(LinearLayout l,int h){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    private LinearLayout card(LinearLayout into){LinearLayout l=col();l.setPadding(dp(18),dp(18),dp(18),dp(18));l.setBackground(shape(Color.WHITE,LINE));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);into.addView(l,lp);return l;}
    private Button button(String label,boolean primary,Runnable action){Button b=new Button(this);b.setText(label);b.setTextSize(14);b.setAllCaps(false);b.setTextColor(primary?Color.WHITE:GREEN);b.setBackground(shape(primary?GREEN:0xffe9efe8,0));b.setMinHeight(dp(44));b.setPadding(dp(14),dp(8),dp(14),dp(8));b.setOnClickListener(v->action.run());return b;}
    private void render(){
        root=col();root.setBackgroundColor(BG);root.setPadding(dp(20),0,dp(20),0);
        if(Build.VERSION.SDK_INT>=30)root.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets x=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(dp(20)+x.left,x.top,dp(20)+x.right,x.bottom);return i;});
        setContentView(root);if(Build.VERSION.SDK_INT>=30)root.requestApplyInsets();
        space(root,18);TextView eyebrow=bold("HISTORY / TEACHING",11,GREEN);eyebrow.setLetterSpacing(.12f);root.addView(eyebrow);
        space(root,6);root.addView(bold("역사 채용노트",28,INK));space(root,4);root.addView(text("서울 · 경기, 그리고 동경한국학교",13,MUTED));space(root,16);
        LinearLayout nav=row();String[] titles={"공고 모아보기","게시판","설정"};
        for(int i=0;i<3;i++){final int target=i;TextView n=bold(titles[i],14,tab==i?Color.WHITE:MUTED);n.setGravity(Gravity.CENTER);n.setPadding(dp(6),dp(13),dp(6),dp(13));n.setBackground(shape(tab==i?INK:BG,0));nav.addView(n,new LinearLayout.LayoutParams(0,-2,1));n.setOnClickListener(v->{tab=target;render();});}
        root.addView(nav);space(root,14);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);screen=col();scroll.addView(screen);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(!store.warning().trim().isEmpty())screen.addView(text(store.warning(),14,AMBER));
        if(tab==0)noticesScreen();else if(tab==1)sourcesScreen();else settingsScreen();
        TextView footer=text("개인용 · 0.1.0",11,MUTED);footer.setGravity(Gravity.CENTER);footer.setPadding(0,dp(8),0,dp(8));root.addView(footer);
    }
    private void noticesScreen(){
        LinearLayout intro=card(screen);intro.addView(bold("중·고등학교 역사 기간제",17,INK));space(intro,5);
        intro.addView(text("맞는 공고를 모으고, 애매한 공고는 따로 남겨요.",13,MUTED));space(intro,12);
        progress=text(running.trim().isEmpty()?lastRun():running,12,MUTED);intro.addView(progress);space(intro,10);
        intro.addView(button(SyncEngine.busy()?"확인 중…":"지금 확인",true,this::sync));
        LinearLayout chips=row();String[] filters={"역사 기간제","확인 필요","관심 공고"};
        for(int i=0;i<3;i++){final int f=i;TextView chip=bold(filters[i],12,filter==i?GREEN:MUTED);chip.setPadding(dp(10),dp(12),dp(10),dp(12));chip.setBackground(shape(filter==i?0xffe2ebdf:BG,0));chips.addView(chip,new LinearLayout.LayoutParams(0,-2,1));chip.setGravity(Gravity.CENTER);chip.setOnClickListener(v->{filter=f;render();});}
        screen.addView(chips);space(screen,10);
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(14);search.setHint("학교·지역·공고명 검색");search.setText(query);search.setPadding(dp(13),dp(8),dp(13),dp(8));search.setBackground(shape(Color.WHITE,LINE));screen.addView(search,new LinearLayout.LayoutParams(-1,dp(46)));
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString();drawNotices();}public void afterTextChanged(Editable e){}});
        CheckBox include=new CheckBox(this);include.setText("마감된 공고도 보기");include.setTextSize(12);include.setTextColor(MUTED);include.setChecked(closed);include.setOnCheckedChangeListener((b,on)->{closed=on;drawNotices();});screen.addView(include);
        noticeList=col();screen.addView(noticeList);drawNotices();
    }
    private String lastRun(){long t=prefs.getLong("lastRun",0);return t==0?"아직 공고를 확인하지 않았어요":"최근 확인 시도  "+date(t)+" · 게시판별 성공 여부는 게시판 탭";}
    private void drawNotices(){
        if(noticeList==null)return;noticeList.removeAllViews();Set<String> active=new HashSet<>();for(Source s:store.sources())if(s.enabled)active.add(s.id);
        List<Notice> found=new ArrayList<>();long now=System.currentTimeMillis();
        for(Notice n:store.notices().values()){
            if(!active.contains(n.sourceId)&&!n.favorite)continue;
            boolean show=filter==2?n.favorite:filter==1?n.status==Status.REVIEW:n.status==Status.MATCH;
            if(!show||(!closed&&n.deadline>0&&n.deadline<now))continue;
            if(!(n.title+" "+n.school+" "+n.region+" "+n.sourceName).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))continue;
            found.add(n);
        }
        found.sort(Comparator.comparingLong((Notice n)->n.deadline>now?n.deadline:Long.MAX_VALUE).thenComparing(Comparator.comparingLong((Notice n)->n.firstSeen).reversed()));
        TextView count=text(found.size()+"개의 공고 · 마감 가까운 순",12,MUTED);count.setPadding(0,0,0,dp(10));noticeList.addView(count);
        if(found.isEmpty()){
            LinearLayout empty=card(noticeList);empty.addView(bold(filter==1?"확인이 필요한 공고가 없어요":"아직 표시할 공고가 없어요",18,INK));space(empty,8);
            empty.addView(text("‘지금 확인’을 눌러 공고를 가져와 주세요.\n표시되는 공고가 없다면 게시판 탭의 연결 상태와 ‘확인 필요’ 목록도 살펴보세요.",14,MUTED));return;
        }
        for(Notice n:found){LinearLayout c=card(noticeList);LinearLayout line=row();TextView badge=bold(n.status==Status.MATCH?"역사 · 기간제":"확인 필요",11,n.status==Status.MATCH?GREEN:AMBER);line.addView(badge,new LinearLayout.LayoutParams(0,-2,1));if(!n.read)line.addView(bold("NEW",10,GREEN));TextView star=bold(n.favorite?"  ★":"  ☆",22,GREEN);line.addView(star);star.setOnClickListener(v->{store.favorite(n.id);drawNotices();});c.addView(line);space(c,9);c.addView(bold(n.title,17,INK));space(c,8);c.addView(text((n.school.trim().isEmpty()?n.sourceName:n.school)+"  ·  "+n.region,13,MUTED));
            space(c,6);c.addView(text(n.deadlineLabel.trim().isEmpty()?"접수 마감은 원문에서 확인":"접수 마감  "+n.deadlineLabel,12,n.deadline>0&&n.deadline-now<3L*86400000?AMBER:MUTED));
            if(n.status==Status.REVIEW){space(c,6);c.addView(text(n.reason,12,AMBER));}
            c.setOnClickListener(v->detail(n));
        }
    }
    private void detail(Notice n){
        store.markRead(n.id);LinearLayout body=col();body.setPadding(dp(20),dp(12),dp(20),dp(12));body.addView(bold(n.title,20,INK));space(body,10);body.addView(text(n.sourceName+" · "+n.region+"\n"+n.reason,13,MUTED));
        if(!n.complete){space(body,10);body.addView(text("일부 내용을 확인하지 못했어요. 원문과 첨부를 함께 확인해 주세요.",13,AMBER));}
        if(!n.deadlineLabel.trim().isEmpty()){space(body,10);body.addView(bold("접수 마감  "+n.deadlineLabel,14,GREEN));}
        space(body,16);TextView content=text(n.body.trim().isEmpty()?"본문 확인 전입니다.":n.body,14,INK);content.setTextIsSelectable(true);body.addView(content);
        for(Link file:n.attachments){space(body,8);body.addView(button("첨부 · "+file.title(),false,()->open(file.url())));}
        space(body,12);body.addView(button(n.favorite?"관심 공고 해제":"관심 공고에 저장",false,()->{store.favorite(n.id);toast("관심 공고 설정을 바꿨어요");}));
        ScrollView scroll=new ScrollView(this);scroll.addView(body);new AlertDialog.Builder(this).setView(scroll).setPositiveButton("원문 열기",(d,w)->open(n.url)).setNegativeButton("닫기",(d,w)->{}).setOnDismissListener(d->{if(tab==0)drawNotices();}).show();
    }
    private void sourcesScreen(){
        screen.addView(bold("내가 확인할 게시판",20,INK));space(screen,5);screen.addView(text("추가한 게시판은 휴대폰이 직접 확인해요. 새로운 구조는 수집 기능 수정이 필요할 수 있어요.",13,MUTED));space(screen,12);
        screen.addView(button("＋ 게시판 추가",true,()->editSource(null)));space(screen,16);
        for(Source s:store.sources()){
            LinearLayout c=card(screen);LinearLayout head=row();head.addView(bold(s.name,17,INK),new LinearLayout.LayoutParams(0,-2,1));Switch on=new Switch(this);on.setContentDescription(s.name+" 수집");on.setChecked(s.enabled);head.addView(on);c.addView(head);on.setOnCheckedChangeListener((b,v)->{s.enabled=v;store.putSource(s);});space(c,5);c.addView(text(s.region+" · "+(s.schoolHint.trim().isEmpty()?"교육청·게시판":"학교: "+s.schoolHint),12,MUTED));space(c,9);
            c.addView(text(s.message,13,s.message.startsWith("확인 실패")||s.message.startsWith("일부")?AMBER:MUTED));space(c,6);c.addView(text("마지막 성공  "+(s.lastSuccess==0?"아직 없음":date(s.lastSuccess)),11,MUTED));
            space(c,10);LinearLayout actions=row();actions.addView(button("원문",false,()->open(s.url)),new LinearLayout.LayoutParams(0,-2,1));actions.addView(button("수정",false,()->editSource(s)),new LinearLayout.LayoutParams(0,-2,1));actions.addView(button("삭제",false,()->new AlertDialog.Builder(this).setTitle("게시판을 삭제할까요?").setMessage("이 게시판의 자동 확인을 중지해요. 저장한 관심 공고는 남아요.").setPositiveButton("삭제",(d,w)->{store.removeSource(s.id);render();}).setNegativeButton("취소",null).show()),new LinearLayout.LayoutParams(0,-2,1));c.addView(actions);space(c,8);c.addView(button("게시판 진단 · 페이지 열기",false,()->startActivity(new Intent(this,InspectActivity.class).putExtra("url",s.url))));
        }
    }
    private EditText input(LinearLayout into,String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextSize(14);e.setSingleLine(true);into.addView(e,new LinearLayout.LayoutParams(-1,dp(54)));return e;}
    private void editSource(Source previous){
        Source s=previous==null?source(UUID.randomUUID().toString(),"","","AUTO","서울/경기",""):previous.copy();
        LinearLayout form=col();form.setPadding(dp(22),dp(8),dp(22),dp(8));EditText name=input(form,"게시판 이름",s.name),url=input(form,"https://게시판 주소",s.url),hint=input(form,"학교 이름 (학교별 게시판이면 입력)",s.schoolHint);
        url.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
        form.addView(text("지역",12,MUTED));Spinner region=new Spinner(this);String[] regions={"서울/경기","서울","경기","동경"};region.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,regions));region.setSelection(Math.max(0,Arrays.asList(regions).indexOf(s.region)));form.addView(region);
        space(form,8);form.addView(text("읽기 방식",12,MUTED));Spinner kind=new Spinner(this);String[] kinds={"AUTO","SEOUL","GYEONGGI","TOKYO","GENERIC"};kind.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"주소로 자동 감지","서울교육일자리포털","경기도교육청","동경한국학교","일반 게시판 (시험 기능)"}));kind.setSelection(Math.max(0,Arrays.asList(kinds).indexOf(s.kind)));form.addView(kind);
        space(form,8);form.addView(text("학교 메인 화면보다 공고 목록이 보이는 게시판 주소를 넣어 주세요.",12,MUTED));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(previous==null?"게시판 추가":"게시판 수정").setView(form).setPositiveButton("저장",null).setNegativeButton("취소",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String u=url.getText().toString().trim().replaceAll(",+$","");if(Html.resolve(u,u).trim().isEmpty()||name.getText().toString().trim().isEmpty()){toast("이름과 http(s) 게시판 주소를 확인해 주세요");return;}if(previous==null&&store.sources().size()>=50){toast("게시판은 최대 50개까지 등록할 수 있어요");return;}s.name=name.getText().toString().trim();s.url=u;s.schoolHint=hint.getText().toString().trim();s.region=regions[region.getSelectedItemPosition()];s.kind=kinds[kind.getSelectedItemPosition()];s.message="설정 변경 · 다시 확인해 주세요";s.lastSuccess=0;store.putSource(s);dialog.dismiss();render();}));dialog.show();
    }
    private void settingsScreen(){
        LinearLayout c=card(screen);c.addView(bold("하루 두 번, 공고 확인",19,INK));space(c,10);Switch automatic=new Switch(this);automatic.setText("자동 확인");automatic.setChecked(prefs.getBoolean("automatic",true));c.addView(automatic);automatic.setOnCheckedChangeListener((b,on)->{prefs.edit().putBoolean("automatic",on).apply();CheckWorker.reschedule(this);});
        space(c,10);for(int i=0;i<2;i++){final int slot=i;int minute=prefs.getInt("slot"+i,i==0?720:1080);c.addView(button((i==0?"첫 번째":"두 번째")+" 확인  "+String.format(Locale.KOREA,"%02d:%02d",minute/60,minute%60),false,()->new TimePickerDialog(this,(view,h,m)->{prefs.edit().putInt("slot"+slot,h*60+m).apply();CheckWorker.reschedule(this);render();},minute/60,minute%60,true).show()));space(c,7);}
        c.addView(text("한국 시간 기준이에요. 절전 상태나 네트워크 상황에 따라 늦어질 수 있어요. 앱을 강제 종료하면 다시 열 때까지 자동 확인이 멈출 수 있어요.",12,MUTED));
        LinearLayout a=card(screen);a.addView(bold("새 공고 알림",18,INK));Switch alerts=new Switch(this);alerts.setText("역사 기간제 신규·수정 알림");alerts.setChecked(prefs.getBoolean("alerts",true));a.addView(alerts);alerts.setOnCheckedChangeListener((b,on)->{prefs.edit().putBoolean("alerts",on).apply();if(on)requestAlerts();});space(a,8);a.addView(button("알림 허용·설정",false,this::requestAlerts));space(a,8);a.addView(button("테스트 알림 보내기",false,()->{if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestAlerts();return;}SyncEngine.testNotification(this);toast("휴대폰 알림창을 확인해 주세요");}));
        space(a,8);a.addView(button("배터리 설정 열기",false,()->{try{startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));}catch(Exception e){openAppSettings();}}));
        LinearLayout data=card(screen);data.addView(bold("기록과 문제 확인",18,INK));space(data,8);data.addView(button("백업 파일 저장",false,()->documentIntent(true)));space(data,8);data.addView(button("백업 가져오기",false,()->documentIntent(false)));space(data,8);data.addView(button("진단 내용 복사",false,()->{android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("역사 채용노트 진단",diagnostics()));toast("진단 내용을 복사했어요");}));
        space(data,12);data.addView(text("공고와 설정은 이 휴대폰에 저장돼요. AI 계정·유료 API·별도 서버는 사용하지 않아요.\n\n‘역사 기간제’는 모집 조건 분류이며 지원 자격을 보장하지 않아요. 제출 전 원문에서 자격과 마감 시각을 확인해 주세요.",12,MUTED));
        if(!prefs.getString("storageError","").trim().isEmpty())data.addView(text(prefs.getString("storageError",""),12,AMBER));
    }
    private void documentIntent(boolean export){Intent i=new Intent(export?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");if(export)i.putExtra(Intent.EXTRA_TITLE,"history-jobs-backup.json");try{startActivityForResult(i,export?201:202);}catch(Exception e){toast("파일 선택 앱을 열지 못했어요");}}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();
        if(request==201){try(OutputStream o=getContentResolver().openOutputStream(uri)){if(o==null)throw new IOException();o.write(store.exportData().toString(2).getBytes(StandardCharsets.UTF_8));toast("백업을 저장했어요");}catch(Exception e){toast("백업 저장 실패");}}
        if(request==202)new AlertDialog.Builder(this).setTitle("백업으로 바꿀까요?").setMessage("현재 공고와 게시판 설정이 백업 내용으로 교체돼요.").setPositiveButton("가져오기",(d,w)->{if(SyncEngine.busy()){toast("공고 확인이 끝난 후 다시 시도해 주세요");return;}try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException();store.restore(new String(Net.read(in,32*1024*1024),StandardCharsets.UTF_8));render();toast("백업을 가져왔어요");}catch(Exception e){toast("가져오기 실패: "+e.getMessage());}}).setNegativeButton("취소",null).show();
    }
    private void sync(){if(SyncEngine.busy()){toast("이미 확인하고 있어요");return;}running="게시판 확인을 시작해요…";render();executor.submit(()->{try{SyncEngine.run(getApplicationContext(),m->{running=m;runOnUiThread(()->{if(progress!=null&&!isDestroyed())progress.setText(m);});});}catch(Exception e){runOnUiThread(()->toast("확인 중 오류: "+e.getClass().getSimpleName()));}finally{running="";runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())render();});}});}
    private void requestAlerts(){prefs.edit().putBoolean("permissionAsked",true).apply();if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},5);else{Intent i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName());startActivity(i);}}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==5&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)SyncEngine.postPending(this,store);}
    private void openAppSettings(){startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}
    private void open(String raw){String safe=Html.resolve(raw,raw);if(safe.trim().isEmpty()){toast("열 수 없는 주소예요");return;}try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(safe)));}catch(Exception e){toast("주소를 열 앱이 없어요");}}
    private String date(long t){return Instant.ofEpochMilli(t).atZone(Classifier.KST).format(DateTimeFormatter.ofPattern("MM.dd HH:mm",Locale.KOREA));}
    private void toast(String msg){Toast.makeText(this,msg,Toast.LENGTH_LONG).show();}
    private String diagnostics(){StringBuilder b=new StringBuilder("역사 채용노트 0.1.0\nAndroid "+Build.VERSION.RELEASE+" / "+Build.MANUFACTURER+" "+Build.MODEL+"\n");b.append("자동 확인: ").append(prefs.getBoolean("automatic",true)).append("\n");for(Source s:store.sources())b.append("\n").append(s.name).append(" / ").append(s.enabled?"켜짐":"꺼짐").append("\n").append(s.url).append("\n").append(s.message).append("\n마지막 성공 ").append(s.lastSuccess==0?"없음":date(s.lastSuccess)).append('\n');return b.toString();}
}
