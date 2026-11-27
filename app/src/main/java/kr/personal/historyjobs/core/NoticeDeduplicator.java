package kr.personal.historyjobs.core;

import java.util.*;
import static kr.personal.historyjobs.core.Model.*;

/** Merge only the same source's article identity, never title-only matches. */
public final class NoticeDeduplicator {
    private NoticeDeduplicator(){}
    public record Result(LinkedHashMap<String,Notice> notices,Map<String,String> aliases){}
    public static Result consolidate(Collection<Notice> input){
        Map<String,List<Notice>> groups=new LinkedHashMap<>();Map<Notice,String> oldIds=new IdentityHashMap<>();
        for(Notice n:input){oldIds.put(n,n.id);String key=PostIdentity.key(n.sourceUrl,n.url);String group=n.sourceId+"|"+(key.isEmpty()||key.startsWith("raw:")?"missing:"+n.id:key);groups.computeIfAbsent(group,k->new ArrayList<>()).add(n);}
        LinkedHashMap<String,Notice> output=new LinkedHashMap<>();Map<String,String> aliases=new LinkedHashMap<>();
        for(List<Notice> group:groups.values()){
            Notice winner=group.stream().max(Comparator.comparingLong((Notice n)->n.lastSeen).thenComparingLong(n->n.bodyFetchedAt)).orElseThrow();String signature=PostIdentity.content(winner);
            boolean read=false,favorite=false;long first=0,bodyTime=0;String body="";Notice deadline=winner;
            for(Notice n:group){favorite|=n.favorite;if(n.firstSeen>0&&(first==0||n.firstSeen<first))first=n.firstSeen;
                if(signature.equals(PostIdentity.content(n))){read|=n.read;if(n.bodyFetchedAt>bodyTime){body=n.body;bodyTime=n.bodyFetchedAt;}if(deadline.deadline==0&&n.deadline>0)deadline=n;}
            }
            String post=PostIdentity.key(winner.sourceUrl,winner.url);String target=!post.isEmpty()&&!post.startsWith("raw:")&&group.stream().anyMatch(n->oldIds.get(n).matches("[0-9a-f]{64}"))?PostIdentity.id(winner.sourceId,winner.sourceUrl,winner.url):oldIds.get(group.get(0));
            winner.id=target;winner.favorite=favorite;winner.read=read;winner.firstSeen=first;winner.body=body;winner.bodyFetchedAt=bodyTime;if(winner.deadline==0&&deadline.deadline>0){winner.deadline=deadline.deadline;winner.deadlineLabel=deadline.deadlineLabel;}
            for(Notice n:group)aliases.put(oldIds.get(n),target);output.put(target,winner);
        }return new Result(output,aliases);
    }
}
