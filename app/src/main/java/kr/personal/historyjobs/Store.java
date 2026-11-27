package kr.personal.historyjobs;

import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static kr.personal.historyjobs.core.Model.*;
import kr.personal.historyjobs.core.Selection;
import kr.personal.historyjobs.core.Classifier;

public final class Store {
    private static Store instance;
    public static synchronized Store get(Context c){if(instance==null)instance=new Store(c.getApplicationContext());return instance;}
    private final AtomicFile file;
    private final List<Source> sources=new ArrayList<>();
    private final LinkedHashMap<String,Notice> notices=new LinkedHashMap<>();
    private final Context context;
    private String loadWarning="";
    private Store(Context c){context=c;file=new AtomicFile(new File(c.getFilesDir(),"history-jobs.json"));
        if(file.getBaseFile().exists())try{load(new JSONObject(new String(file.readFully(),StandardCharsets.UTF_8)));save();}
        catch(Exception e){loadWarning="저장 데이터를 읽지 못했어요. 기존 파일은 유지했어요. 설정에서 진단 내보내기를 이용해 주세요.";}
        else{sources.addAll(defaults());save();}
    }
    public synchronized List<Source> sources(){List<Source> out=new ArrayList<>();sources.forEach(s->out.add(s.copy()));return out;}
    public synchronized Map<String,Notice> notices(){return new LinkedHashMap<>(notices);}
    public synchronized String warning(){return loadWarning;}
    public synchronized Source source(String id){return sources.stream().filter(s->s.id.equals(id)).map(Source::copy).findFirst().orElse(null);}
    public synchronized void putSource(Source s){if(!loadWarning.trim().isEmpty())return;sources.removeIf(x->x.id.equals(s.id));sources.add(s);for(Notice n:notices.values())if(n.sourceId.equals(s.id)){n.keywords=new ArrayList<>(s.keywords);n.region=s.region;Classifier.reclassify(n);}save();}
    public synchronized void removeSource(String id){sources.removeIf(s->s.id.equals(id));save();ListingCacheStore.get(context).remove(id);}
    public synchronized void sourceResult(Source attempted,String message,boolean success,long now){
        for(Source s:sources)if(s.id.equals(attempted.id)&&s.url.equals(attempted.url)&&s.keywords.equals(attempted.keywords)&&s.region.equals(attempted.region)){s.lastAttempt=now;if(success)s.lastSuccess=now;s.message=message;save();break;}
    }
    public synchronized List<Notice> merge(Source attempted,List<Notice> updates){
        Source current=sources.stream().filter(s->s.id.equals(attempted.id)).findFirst().orElse(null);
        if(current==null||!current.enabled||!current.url.equals(attempted.url)||!current.keywords.equals(attempted.keywords)||!current.region.equals(attempted.region))return List.of();
        List<Notice> notify=new ArrayList<>();
        for(Notice n:updates){if(n.sourceUrl.isEmpty())n.sourceUrl=attempted.url;n.keywords=new ArrayList<>(current.keywords);n.region=current.region;Classifier.reclassify(n);Notice old=notices.get(n.id);if(old!=null){n.favorite=old.favorite;n.read=old.read;n.firstSeen=old.firstSeen;if(n.title.equals(old.title)){n.body=old.body;n.bodyFetchedAt=old.bodyFetchedAt;}}
            boolean changed=old==null||!n.fingerprint.equals(old.fingerprint);
            if(changed&&n.status==Status.MATCH){n.read=false;if(n.deadline==0||n.deadline>=System.currentTimeMillis())notify.add(n);}
            notices.put(n.id,n);
        }
        save();return notify;
    }
    public synchronized void articleBody(String id,String url,String body,long fetchedAt){Notice n=notices.get(id);if(n==null||!n.url.equals(url))return;n.body=body;n.bodyFetchedAt=fetchedAt;n.attachments.clear();save();}
    public synchronized void markRead(String id){Notice n=notices.get(id);if(n!=null){n.read=true;save();}}
    public synchronized void favorite(String id){Notice n=notices.get(id);if(n!=null){n.favorite=!n.favorite;save();}}
    public synchronized JSONObject exportData(){return toJson();}
    public synchronized void restore(String text)throws Exception{
        JSONObject j=new JSONObject(text);if(j.optInt("schema")!=1)throw new IOException("지원하지 않는 백업 형식");
        JSONArray src=j.optJSONArray("sources"),items=j.optJSONArray("notices");
        if(src==null||src.length()>50||items==null||items.length()>10000)throw new IOException("백업 크기 또는 형식 오류");
        for(int i=0;i<src.length();i++){JSONObject s=src.getJSONObject(i);String url=s.optString("url");if(kr.personal.historyjobs.core.Html.resolve(url,url).trim().isEmpty())throw new IOException("게시판 주소 형식 오류");if(s.has("keywords")&&!Selection.validKeywords(words(s.optJSONArray("keywords"))))throw new IOException("게시판 키워드 형식 오류");}
        load(j);loadWarning="";save();ListingCacheStore.get(context).clear();
    }
    private void load(JSONObject j)throws JSONException{
        List<Source> ss=new ArrayList<>();LinkedHashMap<String,Notice> nn=new LinkedHashMap<>();
        JSONArray a=j.getJSONArray("sources");for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i);Source s=kr.personal.historyjobs.core.Model.source(x.getString("id"),x.getString("name"),x.getString("url"),x.optString("kind","AUTO"),x.optString("region","서울/경기"),x.optString("schoolHint"));if(x.has("keywords"))s.keywords=words(x.optJSONArray("keywords"));s.enabled=x.optBoolean("enabled",true);s.lastAttempt=x.optLong("lastAttempt");s.lastSuccess=x.optLong("lastSuccess");s.message=x.optString("message","아직 확인하지 않았어요");ss.add(s);}
        if(j.optInt("catalogVersion",0)<2)kr.personal.historyjobs.core.SchoolCatalog.install(ss);
        if(j.optInt("catalogVersion",0)<3)kr.personal.historyjobs.core.InternationalCatalog.install(ss);
        a=j.getJSONArray("notices");for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i);Notice n=new Notice();n.id=x.getString("id");n.sourceId=x.optString("sourceId");n.sourceName=x.optString("sourceName");n.sourceUrl=x.optString("sourceUrl");if(n.sourceUrl.isEmpty())for(Source src:ss)if(src.id.equals(n.sourceId)){n.sourceUrl=src.url;break;}n.url=x.getString("url");n.title=x.optString("title");n.duty=x.optString("duty");n.school=x.optString("school");n.region=Selection.region(x.optString("region"));n.published=Selection.date(x.optString("published"));if(x.has("keywords"))n.keywords=words(x.optJSONArray("keywords"));for(Source src:ss)if(src.id.equals(n.sourceId)){n.keywords=new ArrayList<>(src.keywords);n.region=src.region;break;}n.body=x.optString("body");n.bodyFetchedAt=x.optLong("bodyFetchedAt");n.reason=x.optString("reason");n.fingerprint=x.optString("fingerprint");n.deadlineLabel=x.optString("deadlineLabel");n.deadline=x.optLong("deadline");n.firstSeen=x.optLong("firstSeen");n.lastSeen=x.optLong("lastSeen");n.read=x.optBoolean("read");n.favorite=x.optBoolean("favorite");n.complete=x.optBoolean("complete");try{n.status=Status.valueOf(x.optString("status","REVIEW"));}catch(Exception e){n.status=Status.REVIEW;}JSONArray fs=x.optJSONArray("attachments");if(fs!=null)for(int k=0;k<fs.length();k++){JSONObject f=fs.getJSONObject(k);n.attachments.add(new Link(f.optString("title"),f.optString("url")));}nn.put(n.id,n);}
        if(j.optInt("catalogVersion",0)<1)kr.personal.historyjobs.core.Model.addRegularSources(ss);
        if(j.optInt("selectionVersion",0)<7)for(Source src:ss)src.message="새 학교 게시판·AND 키워드 기준을 적용했어요. ‘지금 확인’으로 갱신해 주세요.";
        android.content.SharedPreferences notified=context.getSharedPreferences("notified",0);
        android.content.SharedPreferences.Editor delivery=notified.edit();
        for(Notice n:nn.values()){
            String previous=n.fingerprint;boolean wasSent=!previous.isEmpty()&&previous.equals(notified.getString(n.id,""));
            kr.personal.historyjobs.core.Classifier.reclassify(n);
            if(wasSent)delivery.putString(n.id,n.fingerprint);
        }
        delivery.apply();
        sources.clear();sources.addAll(ss);notices.clear();notices.putAll(nn);
    }
    private static List<String> words(JSONArray values){List<String> out=new ArrayList<>();if(values!=null)for(int i=0;i<values.length();i++)out.add(values.optString(i));return Selection.keywords(String.join("\n",out));}
    private JSONObject toJson(){
        JSONObject j=new JSONObject();try{j.put("schema",1).put("selectionVersion",7).put("catalogVersion",3);JSONArray ss=new JSONArray(),ns=new JSONArray();
            for(Source s:sources){JSONObject x=new JSONObject();x.put("id",s.id).put("name",s.name).put("url",s.url).put("kind",s.kind).put("region",s.region).put("schoolHint",s.schoolHint).put("keywords",new JSONArray(s.keywords)).put("enabled",s.enabled).put("lastAttempt",s.lastAttempt).put("lastSuccess",s.lastSuccess).put("message",s.message);ss.put(x);}
            for(Notice n:notices.values()){JSONObject x=new JSONObject();x.put("id",n.id).put("sourceId",n.sourceId).put("sourceName",n.sourceName).put("sourceUrl",n.sourceUrl).put("url",n.url).put("title",n.title).put("duty",n.duty).put("school",n.school).put("region",n.region).put("published",n.published).put("keywords",new JSONArray(n.keywords==null?Selection.defaults(n.sourceUrl):n.keywords)).put("body",n.body).put("bodyFetchedAt",n.bodyFetchedAt).put("reason",n.reason).put("fingerprint",n.fingerprint).put("deadlineLabel",n.deadlineLabel).put("deadline",n.deadline).put("firstSeen",n.firstSeen).put("lastSeen",n.lastSeen).put("read",n.read).put("favorite",n.favorite).put("complete",n.complete).put("status",n.status.name());JSONArray fs=new JSONArray();for(Link f:n.attachments)fs.put(new JSONObject().put("title",f.title()).put("url",f.url()));x.put("attachments",fs);ns.put(x);}
            j.put("sources",ss).put("notices",ns);
        }catch(JSONException e){throw new IllegalStateException(e);}return j;
    }
    private void save(){if(!loadWarning.trim().isEmpty())return;FileOutputStream out=null;try{out=file.startWrite();out.write(toJson().toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}catch(Exception e){if(out!=null)file.failWrite(out);context.getSharedPreferences("settings",0).edit().putString("storageError","저장 실패: "+e.getClass().getSimpleName()).apply();}}
}
