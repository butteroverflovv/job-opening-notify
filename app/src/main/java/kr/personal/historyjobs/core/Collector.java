package kr.personal.historyjobs.core;

import java.time.LocalDate;
import java.util.*;
import static kr.personal.historyjobs.core.Model.*;

/** Title-only collection: no detail requests and no attachment downloads. */
public final class Collector {
    public interface Transport { Net.Response get(String url,int limit,String referer)throws Exception; }
    public record Result(List<Notice> notices,int listed,int checked,boolean partial,List<String> errors) {}
    private final Transport net;
    public Collector(Transport net){this.net=net;}
    public Result collect(Source source,Map<String,Notice> known,long now)throws Exception{
        ArrayDeque<String> queue=new ArrayDeque<>();queue.add(source.url);
        Set<String> visited=new HashSet<>(),bodies=new HashSet<>();LinkedHashMap<String,Link> listings=new LinkedHashMap<>();
        List<String> errors=new ArrayList<>();int pages=0;boolean partial=false;long stopAt=System.nanoTime()+35_000_000_000L;
        while(!queue.isEmpty()&&pages<20&&System.nanoTime()<stopAt&&!Thread.currentThread().isInterrupted()){
            String page=queue.remove();if(!visited.add(page))continue;
            try{
                Net.Response response=net.get(page,4_000_000,source.url);String html=response.text();
                if(!bodies.add(Model.hash(html)))continue;
                BoardParser.Listing list=BoardParser.listing(source,response.url(),html);
                if(!list.warning().trim().isEmpty())throw new IllegalStateException(list.warning());
                pages++;
                for(Link l:list.items())listings.putIfAbsent(l.url(),l);
                for(String p:list.pages())if(!visited.contains(p))queue.add(p);
            }catch(Exception e){if(pages==0)throw e;errors.add("목록 일부: "+shortError(e));partial=true;}
        }
        if(!queue.isEmpty()){partial=true;errors.add("목록 확인 범위 제한으로 일부 페이지를 확인하지 못했어요. 게시판 원문도 확인해 주세요.");}
        List<Notice> out=new ArrayList<>();int matches=0;
        for(Link link:listings.values()){
            String key=id(source,link.url());Notice old=known.get(key);
            if(!Classifier.titleMatches(link.title())&&old==null)continue;
            Notice n=new Notice();n.id=key;n.sourceId=source.id;n.sourceName=source.name;n.region=source.region;
            n.url=link.url();n.title=link.title();n.school=source.schoolHint;n.firstSeen=now;n.lastSeen=now;
            if(old!=null){n.deadline=old.deadline;n.deadlineLabel=old.deadlineLabel;}
            Classifier.Deadline deadline=Classifier.deadline(link.summary(),LocalDate.now(Classifier.KST));
            if(deadline.epoch()!=0){n.deadline=deadline.epoch();n.deadlineLabel=deadline.label();}
            Classifier.reclassify(n);if(n.status==Status.MATCH)matches++;
            out.add(n);
        }
        return new Result(out,listings.size(),matches,partial,errors.stream().distinct().limit(15).collect(java.util.stream.Collectors.toList()));
    }
    public static String id(Source s,String url){return Model.hash(s.id+"|"+url);}
    public static String shortError(Exception e){String s=e.getMessage();return s==null?e.getClass().getSimpleName():s.substring(0,Math.min(s.length(),160));}
}
