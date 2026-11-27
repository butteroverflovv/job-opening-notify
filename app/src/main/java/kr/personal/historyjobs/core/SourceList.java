package kr.personal.historyjobs.core;

import java.text.Collator;
import java.util.*;
import static kr.personal.historyjobs.core.Model.*;

/** Display-only source sorting and filters, independent of collection settings. */
public final class SourceList {
    private SourceList(){}
    public static List<Source> select(List<Source> sources,String query,Set<String> regions){
        return select(sources,query,regions,false);
    }
    public static List<Source> select(List<Source> sources,String query,Set<String> regions,boolean descending){
        String needle=Selection.compact(query);List<Source> out=new ArrayList<>();
        for(Source source:sources)if(Selection.regionMatches(source.region,regions)&&Selection.compact(source.name+" "+source.schoolHint+" "+String.join(" OR ",source.keywords)).contains(needle))out.add(source);
        Collator collator=Collator.getInstance(Locale.KOREAN);out.sort((a,b)->{int compared=collator.compare(a.name,b.name);return compared==0?a.id.compareTo(b.id):compared;});if(descending)Collections.reverse(out);return out;
    }
    public static String status(Source source){
        String message=Objects.toString(source.message,"");
        if(message.startsWith("확인 실패"))return "확인 실패";
        if(message.startsWith("일부"))return "일부 확인";
        if(message.startsWith("확인 완료")||message.startsWith("목록 확인 완료"))return "확인 완료";
        return source.lastAttempt==0&&source.lastSuccess==0?"미확인":"확인 필요";
    }
}
