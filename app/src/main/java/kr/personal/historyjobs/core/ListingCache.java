package kr.personal.historyjobs.core;

import java.time.Instant;
import java.util.*;
import static kr.personal.historyjobs.core.Model.*;

/** Disposable, bounded list fingerprints. Never contains bodies or attachments. */
public final class ListingCache {
    private ListingCache(){}
    public static final int MAX_RECORDS=100, UNCHANGED_PAGES=2;
    public static final long FULL_INTERVAL=7L*24*60*60*1000;
    public record State(String config,long lastFull,List<String> records){
        public State{Objects.requireNonNull(config);LinkedHashSet<String> unique=new LinkedHashSet<>();for(String record:records)if(record!=null&&record.matches("[0-9a-f]{64}")){unique.add(record);if(unique.size()==MAX_RECORDS)break;}records=List.copyOf(unique);}
    }
    public static String config(Source source,long now){
        int year=Instant.ofEpochMilli(now).atZone(Classifier.KST).getYear();
        return Model.hash("list-cache-v1|"+(BoardParser.kind(source).equals("SCHOOL_GNU")?"publication-v2|":"")+year+"|"+source.url+"|"+source.kind+"|"+source.region+"|"+source.schoolHint+"|"+String.join("\n",source.keywords));
    }
    public static boolean usable(Source source,State state,long now){return state!=null&&!state.records().isEmpty()&&state.config().equals(config(source,now))&&state.lastFull()>0&&now>=state.lastFull()&&now-state.lastFull()<FULL_INTERVAL;}
    public static String fingerprint(Link link){return Model.hash(link.url()+"\n"+link.title()+"\n"+link.published()+"\n"+link.duty());}
    public static State updated(Source source,State previous,Collection<Link> seen,long now,boolean full){
        LinkedHashSet<String> records=new LinkedHashSet<>();
        for(Link link:seen)if(!link.pinned()){records.add(fingerprint(link));if(records.size()==MAX_RECORDS)break;}
        if(!full&&previous!=null&&previous.config().equals(config(source,now)))for(String old:previous.records()){if(records.size()==MAX_RECORDS)break;records.add(old);}
        return new State(config(source,now),full?now:previous.lastFull(),new ArrayList<>(records));
    }
}
