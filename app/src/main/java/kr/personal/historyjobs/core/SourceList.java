package kr.personal.historyjobs.core;

import java.text.Collator;
import java.util.*;
import static kr.personal.historyjobs.core.Model.*;

/** Display-only source sorting and filters, independent of collection settings. */
public final class SourceList {
    private SourceList(){}
    public static List<Source> select(List<Source> sources,String query,Set<String> regions){
        String needle=Selection.compact(query);List<Source> out=new ArrayList<>();
        for(Source source:sources)if(Selection.regionMatches(source.region,regions)&&Selection.compact(source.name+" "+source.schoolHint+" "+String.join(" OR ",source.keywords)).contains(needle))out.add(source);
        Collator collator=Collator.getInstance(Locale.KOREAN);out.sort((a,b)->{int compared=collator.compare(a.name,b.name);return compared==0?a.id.compareTo(b.id):compared;});return out;
    }
}
