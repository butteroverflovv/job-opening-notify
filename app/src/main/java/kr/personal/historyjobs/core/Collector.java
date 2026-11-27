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
        LocalDate today=java.time.Instant.ofEpochMilli(now).atZone(Classifier.KST).toLocalDate();
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
                if(pages>1&&Selection.oldTokyoPage(source,list.items(),today)){queue.clear();break;}
                List<String> next=Selection.nextPages(source,response.url(),list.pages());
                if(BoardParser.kind(source).equals("TOKYO")&&next.isEmpty()&&list.pages().stream().anyMatch(p->Selection.tokyoPage(p)>Selection.tokyoPage(response.url())+1)){partial=true;errors.add("다음 페이지 주소를 확인하지 못해 일부 목록만 확인했어요.");}
                for(String p:next)if(!visited.contains(p))queue.add(p);
            }catch(Exception e){if(pages==0)throw e;errors.add("목록 일부: "+shortError(e));partial=true;}
        }
        if(!queue.isEmpty()){partial=true;errors.add("목록 확인 범위 제한으로 일부 페이지를 확인하지 못했어요. 게시판 원문도 확인해 주세요.");}
        List<Notice> out=new ArrayList<>();int matches=0,undated=0;
        for(Link link:listings.values()){
            String key=id(source,link.url());Notice old=known.get(key);
            if((!Classifier.titleMatches(source,link.title())||!Selection.academicYearMatches(link.title(),source.url,today))&&old==null)continue;
            Notice n=new Notice();n.id=key;n.sourceId=source.id;n.sourceName=source.name;n.sourceUrl=source.url;n.region=source.region;
            n.keywords=new ArrayList<>(source.keywords);n.published=Selection.date(link.published());if(n.published.isEmpty()&&old!=null)n.published=old.published;
            n.url=link.url();n.title=link.title();n.school=source.schoolHint;n.firstSeen=now;n.lastSeen=now;
            if(old!=null){n.deadline=old.deadline;n.deadlineLabel=old.deadlineLabel;}
            Classifier.Deadline deadline=Classifier.deadline(link.summary(),today);
            if(deadline.epoch()!=0){n.deadline=deadline.epoch();n.deadlineLabel=deadline.label();}
            Classifier.reclassify(n,today);if(n.status==Status.MATCH)matches++;
            if(Classifier.matches(n)&&n.published.isEmpty())undated++;
            if(n.status==Status.MATCH||old!=null)out.add(n);
        }
        if(undated>0){partial=true;errors.add("게시일을 확인하지 못한 "+undated+"건은 기간을 판단할 수 없어 제외했어요.");}
        return new Result(out,listings.size(),matches,partial,errors.stream().distinct().limit(15).collect(java.util.stream.Collectors.toList()));
    }
    public static String id(Source s,String url){return Model.hash(s.id+"|"+url);}
    public static String shortError(Exception e){String s=e.getMessage();return s==null?e.getClass().getSimpleName():s.substring(0,Math.min(s.length(),160));}
}
