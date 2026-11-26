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
    private static final int BG=Ui.WHITE,INK=Ui.INK,ACCENT=Ui.BROWN,MUTED=Ui.MUTED,LINE=Ui.LINE,AMBER=Ui.NEGATIVE;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final ExecutorService articleExecutor=Executors.newFixedThreadPool(2);private Dialog articleDialog;
    private Store store;private LinearLayout root,screen,noticeList;private TextView progress;private LinearLayout bottomNavigation;
    private int tab=0,filter=0;private String query="";private boolean closed=false;private volatile String running="";
    private android.content.SharedPreferences prefs;
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener settingsListener=(p,key)->{if("lastRun".equals(key)&&root!=null&&!isDestroyed())render();};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);store=Store.get(this);prefs=getSharedPreferences("settings",0);
        if(state!=null){tab=state.getInt("tab");filter=state.getInt("filter");query=state.getString("query","");closed=state.getBoolean("closed");}
        if(filter==1)filter=0;CheckWorker.schedule(this);render();
        if(Build.VERSION.SDK_INT>=33&&!prefs.getBoolean("permissionAsked",false)){
            new AlertDialog.Builder(this).setTitle("새 채용공고를 알려드릴게요").setMessage("공고 목록은 권한 없이도 볼 수 있어요. 새 공고 알림을 받으려면 다음 화면에서 알림을 허용해 주세요.").setPositiveButton("알림 설정",(d,w)->requestAlerts()).setNegativeButton("나중에",(d,w)->prefs.edit().putBoolean("permissionAsked",true).apply()).show();
        }
    }
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putInt("tab",tab);b.putInt("filter",filter);b.putString("query",query);b.putBoolean("closed",closed);}
    @Override protected void onResume(){super.onResume();if(root!=null){prefs.registerOnSharedPreferenceChangeListener(settingsListener);SyncEngine.postPending(this,store);render();}}
    @Override protected void onPause(){if(prefs!=null)prefs.unregisterOnSharedPreferenceChangeListener(settingsListener);super.onPause();}
    @Override protected void onDestroy(){if(articleDialog!=null)articleDialog.dismiss();articleExecutor.shutdownNow();executor.shutdown();super.onDestroy();}
    private int dp(float n){return Ui.dp(this,n);}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView text(String s,int size,int color){return Ui.text(this,s,size,color,false);}
    private TextView bold(String s,int size,int color){return Ui.text(this,s,size,color,true);}
    private GradientDrawable shape(int color,int stroke){return Ui.shape(this,color,stroke,12);}
    private void space(LinearLayout l,int h){View v=new View(this);v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    private LinearLayout card(LinearLayout into){
        LinearLayout l=col();l.setPadding(dp(20),dp(20),dp(20),dp(20));l.setBackground(Ui.shape(this,BG,LINE,16));l.setElevation(dp(.5f));
        into.setClipChildren(false);into.setClipToPadding(false);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(2);lp.bottomMargin=dp(14);lp.leftMargin=dp(1);lp.rightMargin=dp(1);into.addView(l,lp);return l;
    }
    private Button button(String label,boolean primary,Runnable action){return Ui.button(this,label,primary,action);}
    private TextView badge(String label,boolean emphasis){TextView b=bold(label,11,emphasis?ACCENT:Ui.SECONDARY);b.setPadding(dp(8),dp(5),dp(8),dp(5));b.setBackground(Ui.shape(this,emphasis?Ui.TINT:Ui.FILL,0,6));return b;}
    private void heading(String title,String subtitle){TextView heading=bold(title,24,INK);heading.setTag("page_heading");screen.addView(heading);if(!subtitle.isEmpty()){space(screen,8);TextView intro=text(subtitle,14,Ui.SECONDARY);intro.setTag("page_intro");screen.addView(intro);}space(screen,24);}
    private void sectionTitle(LinearLayout into,String title,String icon){LinearLayout r=row();r.addView(Ui.icon(this,icon,ACCENT),new LinearLayout.LayoutParams(dp(21),dp(21)));TextView t=bold(title,17,INK);t.setPadding(dp(9),0,0,0);r.addView(t,new LinearLayout.LayoutParams(0,-2,1));into.addView(r);space(into,16);}
    private void render(){
        progress=null;noticeList=null;root=col();root.setBackgroundColor(BG);
        if(Build.VERSION.SDK_INT>=30){getWindow().setDecorFitsSystemWindows(false);root.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets x=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());android.graphics.Insets keyboard=i.getInsets(WindowInsets.Type.ime());v.setPadding(x.left,x.top,x.right,Math.max(x.bottom,keyboard.bottom));if(bottomNavigation!=null)bottomNavigation.setVisibility(keyboard.bottom>x.bottom?View.GONE:View.VISIBLE);return WindowInsets.CONSUMED;});}
        setContentView(root);
        LinearLayout header=row();header.setPadding(dp(20),dp(16),dp(20),dp(16));header.addView(bold("교원 채용공고 알리미",19,INK),new LinearLayout.LayoutParams(0,-2,1));root.addView(header);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);scroll.setTag("page_scroll");
        screen=col();screen.setPadding(dp(20),dp(14),dp(20),dp(24));screen.setFocusableInTouchMode(true);scroll.addView(screen);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(!store.warning().trim().isEmpty()){TextView warning=text(store.warning(),13,AMBER);warning.setPadding(dp(14),dp(14),dp(14),dp(14));warning.setBackground(shape(0xfffff4f2,0));screen.addView(warning);space(screen,12);}
        if(tab==0)noticesScreen();else if(tab==1)sourcesScreen();else settingsScreen();
        bottomNavigation=col();Ui.divider(this,bottomNavigation);LinearLayout nav=row();nav.setPadding(dp(8),dp(4),dp(8),dp(4));
        String[] labels={"공고","게시판","설정"},icons={"list","boards","settings"},tags={"nav_notices","nav_sources","nav_settings"};
        for(int i=0;i<3;i++){final int target=i;int color=tab==i?ACCENT:MUTED;LinearLayout item=col();item.setGravity(Gravity.CENTER);item.setPadding(dp(4),dp(6),dp(4),dp(6));item.setMinimumHeight(dp(60));item.setBackground(Ui.touch(this,BG,0,12));item.setContentDescription(labels[i]);item.setTag(tags[i]);item.setSelected(tab==i);item.addView(Ui.icon(this,icons[i],color),new LinearLayout.LayoutParams(dp(24),dp(24)));space(item,5);TextView navLabel=bold(labels[i],11,color);navLabel.setGravity(Gravity.CENTER);item.addView(navLabel);item.setOnClickListener(v->{tab=target;render();});nav.addView(item,new LinearLayout.LayoutParams(0,-2,1));}
        bottomNavigation.addView(nav);root.addView(bottomNavigation);if(Build.VERSION.SDK_INT>=30)root.requestApplyInsets();
    }
    private void noticesScreen(){
        heading("채용공고","");
        LinearLayout sync=row();sync.setPadding(dp(14),dp(14),dp(12),dp(14));sync.setBackground(shape(Ui.FILL,0));
        LinearLayout status=col();TextView statusTitle=bold(SyncEngine.busy()?"새 공고를 확인하고 있어요":"게시판의 새 소식",13,INK);status.addView(statusTitle);space(status,5);
        progress=text(running.trim().isEmpty()?lastRun():running,11,MUTED);progress.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);status.addView(progress);LinearLayout.LayoutParams statusLp=new LinearLayout.LayoutParams(0,-2,1);statusLp.rightMargin=dp(12);sync.addView(status,statusLp);
        Button refresh=button(SyncEngine.busy()?"확인 중…":"지금 확인",true,this::sync);refresh.setTag("sync_button");refresh.setEnabled(!SyncEngine.busy());refresh.setAlpha(SyncEngine.busy()?.55f:1);sync.addView(refresh);screen.addView(sync);space(screen,24);
        LinearLayout segments=row();segments.setPadding(dp(4),dp(4),dp(4),dp(4));segments.setBackground(shape(Ui.FILL,0));
        String[] filters={"전체 공고","관심 공고"};
        for(int i=0;i<filters.length;i++){final int f=i==1?2:0;boolean selected=filter==f;TextView chip=bold(filters[i],14,selected?ACCENT:Ui.SECONDARY);chip.setGravity(Gravity.CENTER);chip.setPadding(dp(8),dp(13),dp(8),dp(13));chip.setMinHeight(dp(44));chip.setBackground(Ui.touch(this,selected?BG:Ui.FILL,0,9));chip.setElevation(selected?dp(1):0);chip.setSelected(selected);chip.setTag(i==0?"filter_all":"filter_favorite");segments.addView(chip,new LinearLayout.LayoutParams(0,-2,1));chip.setOnClickListener(v->{filter=f;render();});}
        screen.addView(segments);space(screen,16);
        LinearLayout field=row();field.setPadding(dp(14),0,dp(2),0);field.setBackground(shape(Ui.FILL,0));field.addView(Ui.icon(this,"search",MUTED),new LinearLayout.LayoutParams(dp(20),dp(20)));
        EditText search=new EditText(this);search.setTag("search_field");search.setContentDescription("학교·지역·공고명 검색");search.setSingleLine(true);search.setTextSize(14);search.setTextColor(INK);search.setHintTextColor(MUTED);search.setHint("학교·지역·공고명 검색");search.setText(query);search.setPadding(dp(10),dp(14),0,dp(14));search.setBackgroundColor(Color.TRANSPARENT);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);field.addView(search,new LinearLayout.LayoutParams(0,-2,1));
        ImageButton clear=Ui.iconButton(this,"close","검색어 지우기",MUTED,()->search.setText(""));clear.setTag("search_clear");clear.setVisibility(query.isEmpty()?View.INVISIBLE:View.VISIBLE);field.addView(clear);screen.addView(field);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString();clear.setVisibility(query.isEmpty()?View.INVISIBLE:View.VISIBLE);drawNotices();}public void afterTextChanged(Editable e){}});
        search.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);search.clearFocus();return true;}return false;});
        CheckBox include=new CheckBox(this);include.setTag("closed_toggle");include.setText("마감된 공고 포함");include.setTextSize(12);include.setTextColor(Ui.SECONDARY);include.setMinimumHeight(dp(48));Ui.tint(include);include.setChecked(closed);include.setOnCheckedChangeListener((b,on)->{closed=on;drawNotices();});screen.addView(include);space(screen,4);
        noticeList=col();screen.addView(noticeList);drawNotices();
    }
    private String lastRun(){long t=prefs.getLong("lastRun",0);return t==0?"아직 확인 전이에요":"최근 확인 시도 · "+date(t);}
    private void drawNotices(){
        if(noticeList==null)return;noticeList.removeAllViews();Set<String> active=new HashSet<>();for(Source s:store.sources())if(s.enabled)active.add(s.id);
        List<Notice> found=new ArrayList<>();long now=System.currentTimeMillis();
        for(Notice n:store.notices().values()){
            if(!active.contains(n.sourceId)&&!n.favorite)continue;
            boolean show=Classifier.matches(n)&&(filter==2?n.favorite:n.status==Status.MATCH);
            if(!show||(!closed&&n.deadline>0&&n.deadline<now))continue;
            if(!(n.title+" "+n.school+" "+n.region+" "+n.sourceName).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))continue;
            found.add(n);
        }
        found.sort(Comparator.comparingLong((Notice n)->n.deadline>now?n.deadline:Long.MAX_VALUE).thenComparing(Comparator.comparingLong((Notice n)->n.firstSeen).reversed()));
        LinearLayout counts=row();counts.addView(bold("공고 "+found.size()+"개",14,INK),new LinearLayout.LayoutParams(0,-2,1));counts.addView(text("마감 가까운 순",11,MUTED));noticeList.addView(counts);space(noticeList,16);
        if(found.isEmpty()){
            LinearLayout empty=col();empty.setGravity(Gravity.CENTER);empty.setPadding(dp(16),dp(32),dp(16),dp(32));
            ImageView illustration=Ui.icon(this,filter==2?"bookmark":"search",MUTED);illustration.setPadding(dp(16),dp(16),dp(16),dp(16));illustration.setBackground(Ui.shape(this,Ui.FILL,0,32));empty.addView(illustration,new LinearLayout.LayoutParams(dp(64),dp(64)));space(empty,20);
            String title=!query.trim().isEmpty()?"검색 결과가 없어요":filter==2?"관심 공고를 모아보세요":"아직 표시할 공고가 없어요";
            TextView h=bold(title,17,INK);h.setGravity(Gravity.CENTER);empty.addView(h);space(empty,10);
            String hint=!query.trim().isEmpty()?"학교 이름이나 검색어를 바꿔보세요.":filter==2?"공고의 북마크를 누르면\n여기서 다시 확인할 수 있어요.":"‘지금 확인’을 눌러 새 공고를 찾아보세요.\n게시판 탭에서 연결 상태도 확인할 수 있어요.";
            TextView message=text(hint,13,Ui.SECONDARY);message.setGravity(Gravity.CENTER);empty.addView(message);noticeList.addView(empty);return;
        }
        for(Notice n:found){
            LinearLayout c=card(noticeList);c.setTag("notice_"+n.id);c.setBackground(Ui.touch(this,BG,LINE,16));c.setPadding(dp(18),dp(12),dp(12),dp(18));
            LinearLayout top=row();top.addView(badge(n.region.trim().isEmpty()?n.sourceName:n.region,false));
            if(!n.read){TextView fresh=bold("NEW",10,ACCENT);fresh.setPadding(dp(8),0,0,0);top.addView(fresh);}
            top.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));
            ImageButton favorite=Ui.iconButton(this,n.favorite?"bookmark-fill":"bookmark",n.favorite?"관심 공고 해제":"관심 공고에 저장",n.favorite?ACCENT:MUTED,()->{store.favorite(n.id);drawNotices();});favorite.setTag("favorite_"+n.id);favorite.setSelected(n.favorite);top.addView(favorite);c.addView(top);space(c,4);
            TextView title=bold(n.title,17,INK);title.setMaxLines(3);title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setPadding(0,0,dp(6),0);c.addView(title);space(c,10);
            TextView school=text(n.school.trim().isEmpty()?n.sourceName:n.school,13,Ui.SECONDARY);school.setMaxLines(1);school.setEllipsize(android.text.TextUtils.TruncateAt.END);c.addView(school);space(c,16);
            LinearLayout bottom=row();boolean expired=n.deadline>0&&n.deadline<now;String deadline=n.deadlineLabel.trim().isEmpty()?"마감일은 원문 확인":n.deadlineLabel;
            TextView date=text(deadline,12,MUTED);bottom.addView(date,new LinearLayout.LayoutParams(0,-2,1));
            if(n.deadline>0){long days=java.time.temporal.ChronoUnit.DAYS.between(Instant.ofEpochMilli(now).atZone(Classifier.KST).toLocalDate(),Instant.ofEpochMilli(n.deadline).atZone(Classifier.KST).toLocalDate());bottom.addView(badge(expired?"마감":days==0?"오늘 마감":"D-"+days,!expired&&days<=3));}
            c.addView(bottom);c.setOnClickListener(v->detail(n));
        }
    }
    private void detail(Notice n){
        store.markRead(n.id);Dialog dialog=new Dialog(this);articleDialog=dialog;dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);LinearLayout sheet=col();sheet.setPadding(dp(24),dp(10),dp(24),dp(24));sheet.setBackground(Ui.shape(this,BG,0,24));
        View handle=new View(this);handle.setBackground(Ui.shape(this,0xffd9d9de,0,2));LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(dp(32),dp(4));hp.gravity=Gravity.CENTER;sheet.addView(handle,hp);space(sheet,6);
        LinearLayout top=row();top.addView(badge(n.region.trim().isEmpty()?n.sourceName:n.region,false));top.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));top.addView(Ui.iconButton(this,"close","닫기",MUTED,dialog::dismiss));sheet.addView(top);
        ScrollView scroll=new ScrollView(this);LinearLayout body=col();body.setPadding(0,dp(8),0,dp(20));body.addView(bold(n.title,23,INK));space(body,12);body.addView(text(n.sourceName+" · "+n.region,13,Ui.SECONDARY));space(body,24);
        LinearLayout deadline=card(body);deadline.setElevation(0);deadline.setBackground(shape(Ui.FILL,0));deadline.addView(text("접수 마감",12,MUTED));space(deadline,8);deadline.addView(bold(n.deadlineLabel.trim().isEmpty()?"원문에서 확인해 주세요":n.deadlineLabel,16,INK));
        body.addView(bold("공고 본문",17,INK));space(body,10);
        TextView bodyStatus=text(n.bodyFetchedAt>0?"저장된 본문 · 새 내용 확인 중…":"본문을 불러오고 있어요…",12,MUTED);bodyStatus.setTag("article_status");bodyStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);body.addView(bodyStatus);space(body,12);
        TextView article=text(n.bodyFetchedAt>0?n.body:"",14,INK);article.setTag("article_body");article.setTextIsSelectable(true);body.addView(article);
        space(body,14);Button retry=button("다시 불러오기",false,()->{});retry.setTag("article_retry");retry.setVisibility(View.GONE);body.addView(retry);Future<?>[] loading=new Future<?>[1];
        Runnable load=()->{
            retry.setVisibility(View.GONE);bodyStatus.setText(n.bodyFetchedAt>0?"저장된 본문 · 새 내용 확인 중…":"본문을 불러오고 있어요…");
            loading[0]=articleExecutor.submit(()->{try{
                ArticleReader.Article result=new ArticleReader(Net::getHtml).read(n.url);long fetchedAt=System.currentTimeMillis();store.articleBody(n.id,n.url,result.body(),fetchedAt);n.body=result.body();n.bodyFetchedAt=fetchedAt;
                runOnUiThread(()->{if(isDestroyed()||!dialog.isShowing())return;article.setText(result.body());bodyStatus.setText("원문에서 불러온 본문");});
            }catch(Exception e){runOnUiThread(()->{if(isDestroyed()||!dialog.isShowing())return;bodyStatus.setText(n.bodyFetchedAt>0?"새 본문을 가져오지 못해 저장된 내용을 보여드려요.":"본문을 가져오지 못했어요.");if(n.bodyFetchedAt<=0)article.setText(Collector.shortError(e)+"\n\n아래 버튼으로 공고 원문을 열 수 있어요.");retry.setVisibility(View.VISIBLE);});}});
        };
        retry.setOnClickListener(v->load.run());
        scroll.addView(body);sheet.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout actions=row();ImageButton bookmark=Ui.iconButton(this,n.favorite?"bookmark-fill":"bookmark",n.favorite?"관심 공고 해제":"관심 공고에 저장",ACCENT,()->{store.favorite(n.id);dialog.dismiss();Notice updated=store.notices().get(n.id);detail(updated==null?n:updated);});bookmark.setBackground(Ui.touch(this,BG,LINE,12));actions.addView(bookmark);Button original=button("공고 원문 열기",true,()->open(n.url));LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(0,-2,1);op.leftMargin=dp(10);actions.addView(original,op);sheet.addView(actions);
        dialog.setContentView(sheet);Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams a=w.getAttributes();a.gravity=Gravity.BOTTOM;a.width=-1;a.height=(int)(getResources().getDisplayMetrics().heightPixels*.82);a.dimAmount=.35f;w.setAttributes(a);}
        dialog.setOnDismissListener(d->{if(loading[0]!=null)loading[0].cancel(true);if(articleDialog==dialog)articleDialog=null;if(tab==0)drawNotices();});dialog.show();load.run();
    }
    private void sourcesScreen(){
        heading("게시판 관리",store.sources().size()+"개 게시판을 한곳에서 관리해요.");
        Button add=button("＋ 게시판 추가",true,()->editSource(null));add.setTag("add_source");screen.addView(add);space(screen,22);
        for(Source s:store.sources()){
            LinearLayout c=card(screen);LinearLayout head=row();LinearLayout title=col();title.addView(bold(s.name,17,INK));space(title,7);title.addView(text(s.region+" · "+(s.schoolHint.trim().isEmpty()?"채용 게시판":s.schoolHint),12,MUTED));head.addView(title,new LinearLayout.LayoutParams(0,-2,1));
            Switch on=new Switch(this);on.setTag("source_enabled_"+s.id);on.setContentDescription(s.name+" 자동 확인");on.setMinimumHeight(dp(48));on.setPadding(dp(10),0,0,0);Ui.tint(on);on.setChecked(s.enabled);head.addView(on);c.addView(head);on.setOnCheckedChangeListener((b,v)->{s.enabled=v;store.putSource(s);});space(c,18);
            boolean failed=s.message.startsWith("확인 실패")||s.message.startsWith("일부");LinearLayout state=col();state.setPadding(dp(12),dp(12),dp(12),dp(12));state.setBackground(shape(Ui.FILL,0));state.addView(text(s.message,12,failed?AMBER:Ui.SECONDARY));space(state,6);state.addView(text("마지막 성공 · "+(s.lastSuccess==0?"아직 없음":date(s.lastSuccess)),11,MUTED));c.addView(state);space(c,12);
            LinearLayout actions=row();String[] labels={"원문 보기","수정","삭제"};Runnable[] callbacks={()->open(s.url),()->editSource(s),()->new AlertDialog.Builder(this).setTitle("게시판을 삭제할까요?").setMessage("이 게시판의 자동 확인을 중지해요. 저장한 관심 공고는 남아요.").setPositiveButton("삭제",(d,w)->{store.removeSource(s.id);render();}).setNegativeButton("취소",null).show()};
            for(int i=0;i<3;i++){Button b=button(labels[i],false,callbacks[i]);b.setTextColor(i==2?MUTED:Ui.SECONDARY);b.setBackground(Ui.touch(this,BG,0,8));b.setPadding(dp(4),dp(10),dp(4),dp(10));actions.addView(b,new LinearLayout.LayoutParams(0,-2,1));}c.addView(actions);Ui.divider(this,c);
            actionRow(c,"게시판 연결 진단","", "info",()->startActivity(new Intent(this,InspectActivity.class).putExtra("url",s.url)));
        }
        if(store.sources().isEmpty())screen.addView(text("확인하고 싶은 학교나 교육청의 채용 게시판을 추가해 주세요.",14,Ui.SECONDARY));
    }
    private EditText input(LinearLayout into,String label,String value){into.addView(bold(label,12,Ui.SECONDARY));space(into,8);EditText e=new EditText(this);e.setHint(label);e.setText(value);e.setTextSize(14);e.setTextColor(INK);e.setHintTextColor(MUTED);e.setSingleLine(true);e.setPadding(dp(14),dp(15),dp(14),dp(15));e.setBackground(shape(Ui.FILL,0));into.addView(e,new LinearLayout.LayoutParams(-1,-2));space(into,18);return e;}
    private void editSource(Source previous){
        Source s=previous==null?source(UUID.randomUUID().toString(),"","","AUTO","서울/경기",""):previous.copy();
        LinearLayout form=col();form.setPadding(dp(24),dp(12),dp(24),dp(24));EditText name=input(form,"게시판 이름",s.name),url=input(form,"게시판 주소 (https://…)",s.url),hint=input(form,"학교 이름 (선택)",s.schoolHint);
        url.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
        form.addView(bold("지역",12,Ui.SECONDARY));space(form,8);Spinner region=new Spinner(this);String[] regions={"서울/경기","서울","경기","동경"};region.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,regions));region.setSelection(Math.max(0,Arrays.asList(regions).indexOf(s.region)));region.setMinimumHeight(dp(48));form.addView(region);
        space(form,18);form.addView(bold("읽기 방식",12,Ui.SECONDARY));space(form,8);Spinner kind=new Spinner(this);String[] kinds={"AUTO","SEOUL","GYEONGGI","TOKYO","GENERIC"};kind.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"주소로 자동 감지","서울교육일자리포털","경기도교육청","동경한국학교","일반 게시판 (시험 기능)"}));kind.setSelection(Math.max(0,Arrays.asList(kinds).indexOf(s.kind)));kind.setMinimumHeight(dp(48));form.addView(kind);
        space(form,16);form.addView(text("학교 메인 화면이 아닌, 공고 목록이 보이는 게시판 주소를 넣어 주세요.",12,MUTED));
        ScrollView scroll=new ScrollView(this);scroll.addView(form);AlertDialog dialog=new AlertDialog.Builder(this).setTitle(previous==null?"게시판 추가":"게시판 수정").setView(scroll).setPositiveButton("저장",null).setNegativeButton("취소",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String u=url.getText().toString().trim().replaceAll(",+$","");if(Html.resolve(u,u).trim().isEmpty()||name.getText().toString().trim().isEmpty()){toast("이름과 http(s) 게시판 주소를 확인해 주세요");return;}if(previous==null&&store.sources().size()>=50){toast("게시판은 최대 50개까지 등록할 수 있어요");return;}s.name=name.getText().toString().trim();s.url=u;s.schoolHint=hint.getText().toString().trim();s.region=regions[region.getSelectedItemPosition()];s.kind=kinds[kind.getSelectedItemPosition()];s.message="설정 변경 · 다시 확인해 주세요";s.lastSuccess=0;store.putSource(s);dialog.dismiss();render();}));dialog.show();if(dialog.getWindow()!=null)dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    private Switch toggleRow(LinearLayout into,String title,String subtitle,boolean checked){
        LinearLayout line=row();line.setPadding(0,dp(8),0,dp(8));LinearLayout copy=col();copy.addView(bold(title,15,INK));if(!subtitle.isEmpty()){space(copy,6);copy.addView(text(subtitle,12,MUTED));}LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.rightMargin=dp(12);line.addView(copy,p);Switch toggle=new Switch(this);toggle.setContentDescription(title);toggle.setMinimumHeight(dp(48));toggle.setChecked(checked);Ui.tint(toggle);line.addView(toggle);into.addView(line);return toggle;
    }
    private void actionRow(LinearLayout into,String title,String value,String icon,Runnable action){
        LinearLayout line=row();line.setMinimumHeight(dp(56));line.setPadding(0,dp(12),0,dp(12));line.setBackground(Ui.touch(this,BG,0,8));
        if(!icon.isEmpty()){line.addView(Ui.icon(this,icon,Ui.SECONDARY),new LinearLayout.LayoutParams(dp(19),dp(19)));}
        TextView label=text(title,14,INK);label.setPadding(icon.isEmpty()?0:dp(10),0,dp(8),0);line.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        if(!value.isEmpty()){TextView v=bold(value,15,ACCENT);v.setPadding(0,0,dp(8),0);line.addView(v);}line.addView(Ui.icon(this,"arrow",0xffb4b4bb),new LinearLayout.LayoutParams(dp(16),dp(16)));line.setOnClickListener(v->action.run());into.addView(line);
    }
    private void settingsScreen(){
        heading("설정","알림과 확인 시간을 관리해요.");
        LinearLayout c=card(screen);sectionTitle(c,"자동 확인","clock");Switch automatic=toggleRow(c,"하루 두 번 확인","한국 시간 기준",prefs.getBoolean("automatic",true));automatic.setTag("automatic_toggle");automatic.setOnCheckedChangeListener((b,on)->{prefs.edit().putBoolean("automatic",on).apply();CheckWorker.reschedule(this);});space(c,8);Ui.divider(this,c);
        for(int i=0;i<2;i++){final int slot=i;int minute=prefs.getInt("slot"+i,i==0?720:1080);actionRow(c,i==0?"첫 번째 확인":"두 번째 확인",String.format(Locale.KOREA,"%02d:%02d",minute/60,minute%60),"",()->new TimePickerDialog(this,(view,h,m)->{prefs.edit().putInt("slot"+slot,h*60+m).apply();CheckWorker.reschedule(this);render();},minute/60,minute%60,true).show());if(i==0)Ui.divider(this,c);}
        space(c,8);c.addView(text("절전이나 네트워크 상황에 따라 늦어질 수 있어요. 앱을 강제 종료했다면 다시 열어 주세요.",12,MUTED));
        LinearLayout a=card(screen);sectionTitle(a,"새 공고 알림","bell");Switch alerts=toggleRow(a,"새 공고 알려주기","조건에 맞는 공고가 추가되거나 바뀔 때",prefs.getBoolean("alerts",true));alerts.setOnCheckedChangeListener((b,on)->{prefs.edit().putBoolean("alerts",on).apply();if(on)requestAlerts();});space(a,8);Ui.divider(this,a);
        actionRow(a,"알림 허용·설정","","",this::requestAlerts);Ui.divider(this,a);actionRow(a,"테스트 알림 보내기","","",()->{if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestAlerts();return;}SyncEngine.testNotification(this);toast("휴대폰 알림창을 확인해 주세요");});Ui.divider(this,a);actionRow(a,"배터리 설정","","",()->{try{startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));}catch(Exception e){openAppSettings();}});
        LinearLayout data=card(screen);sectionTitle(data,"데이터 관리","download");actionRow(data,"백업 파일 저장","","download",()->documentIntent(true));Ui.divider(this,data);actionRow(data,"백업 가져오기","","upload",()->documentIntent(false));Ui.divider(this,data);actionRow(data,"진단 내용 복사","","info",()->{android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("교원 채용공고 알리미 진단",diagnostics()));toast("진단 내용을 복사했어요");});
        if(!prefs.getString("storageError","").trim().isEmpty()){space(data,10);data.addView(text(prefs.getString("storageError",""),12,AMBER));}
        space(screen,10);screen.addView(text("공고와 읽어 온 본문은 이 휴대폰에 저장돼요.\n교육청 게시판은 ‘역사’, 학교 게시판은 ‘역사’·‘교원 채용’·‘교직원 채용’ 제목을 모아요.\n첨부파일과 지원 자격은 원문에서 확인해 주세요.",12,MUTED));space(screen,18);screen.addView(text("교원 채용공고 알리미 · 0.1.5",11,MUTED));
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
    private String diagnostics(){StringBuilder b=new StringBuilder("교원 채용공고 알리미 0.1.5\nAndroid "+Build.VERSION.RELEASE+" / "+Build.MANUFACTURER+" "+Build.MODEL+"\n");b.append("자동 확인: ").append(prefs.getBoolean("automatic",true)).append("\n");for(Source s:store.sources())b.append("\n").append(s.name).append(" / ").append(s.enabled?"켜짐":"꺼짐").append("\n").append(s.url).append("\n").append(s.message).append("\n마지막 성공 ").append(s.lastSuccess==0?"없음":date(s.lastSuccess)).append('\n');return b.toString();}
}
