package kr.personal.historyjobs.core;

import java.time.LocalDate;
import java.util.*;
import static kr.personal.historyjobs.core.Model.*;

/** Title-based collection. GNU boards with abbreviated dates may read detail date metadata; no attachments. */
public final class Collector {
    public interface Transport { Net.Response get(String url,int limit,String referer)throws Exception; }
    public record Result(List<Notice> notices,int listed,int checked,boolean partial,List<String> errors,ListingCache.State cache,boolean incremental,int pages) {}
    private final Transport net;
    public Collector(Transport net){this.net=net;}
    public Result collect(Source source,Map<String,Notice> known,long now)throws Exception{
        return collect(source,known,now,null);
    }
    public Result collect(Source source,Map<String,Notice> known,long now,ListingCache.State previous)throws Exception{
        boolean incremental=ListingCache.usable(source,previous,now);Set<String> remembered=incremental?new HashSet<>(previous.records()):Set.of();int unchanged=0;
        LocalDate today=java.time.Instant.ofEpochMilli(now).atZone(Classifier.KST).toLocalDate();
        ArrayDeque<String> queue=new ArrayDeque<>();queue.add(source.url);
        Set<String> visited=new HashSet<>(),bodies=new HashSet<>(),lists=new HashSet<>();LinkedHashMap<String,Link> listings=new LinkedHashMap<>();
        LinkedHashMap<String,Link> cacheListings=new LinkedHashMap<>();Map<String,String> dates=new HashMap<>();
        List<String> errors=new ArrayList<>();int pages=0;boolean partial=false;
        // Follow consecutive pages to the date frontier or the board's end. A fixed
        // page/time quota can hide valid posts before reaching the old section.
        // Each network request still has connect/read timeouts and cancellation.
        while(!queue.isEmpty()&&!Thread.currentThread().isInterrupted()){
            String page=queue.remove();if(!visited.add(page))continue;
            try{
                Net.Response response=net.get(page,4_000_000,source.url);String html=response.text();
                if(!bodies.add(Model.hash(html)))continue;
                BoardParser.Listing list=BoardParser.listing(source,response.url(),html);
                if(!list.warning().trim().isEmpty())throw new IllegalStateException(list.warning());
                pages++;
                // Some boards ignore page parameters but change counters/tokens in
                // their HTML. Detect a repeated logical list to prevent such loops.
                if(!list.items().isEmpty()&&!lists.add(Model.hash(list.items().stream().map(l->PostIdentity.key(source.url,l.url())+"\n"+l.title()+"\n"+l.published()+"\n"+l.duty()).collect(java.util.stream.Collectors.joining("\n\n")))))break;
                for(Link l:list.items())PostIdentity.add(source,cacheListings,l);
                List<Link> dated=GnuDates.resolve(source,list.items(),net,dates,today);
                for(Link l:dated)PostIdentity.add(source,listings,l);
                if(Selection.oldPage(dated,today)){queue.clear();break;}
                List<Link> regular=list.items().stream().filter(l->!l.pinned()).collect(java.util.stream.Collectors.toList());
                unchanged=incremental&&!regular.isEmpty()&&regular.stream().allMatch(l->remembered.contains(ListingCache.fingerprint(l)))?unchanged+1:0;
                if(unchanged>=ListingCache.UNCHANGED_PAGES){queue.clear();break;}
                List<String> next=Selection.nextPages(source,response.url(),list.pages());
                if(next.isEmpty()&&list.pages().stream().anyMatch(p->Selection.pageNumber(source,p)>Selection.pageNumber(source,response.url())+1)){partial=true;errors.add("다음 페이지 주소를 확인하지 못해 일부 목록만 확인했어요.");}
                for(String p:next)if(!visited.contains(p))queue.add(p);
            }catch(Exception e){if(pages==0)throw e;errors.add("목록 일부: "+shortError(e));partial=true;}
        }
        if(Thread.currentThread().isInterrupted()){partial=true;errors.add("확인이 중단됐어요. 다음 확인 때 다시 시도해 주세요.");}
        List<Notice> out=new ArrayList<>();int matches=0,undated=0;
        for(Link link:listings.values()){
            String key=id(source,link.url());Notice old=known.get(key);
            if((!Classifier.matches(source,link.title(),link.duty())||!Selection.academicYearMatches(link.title(),source.url,today))&&old==null)continue;
            Notice n=new Notice();n.id=key;n.sourceId=source.id;n.sourceName=source.name;n.sourceUrl=source.url;n.region=source.region;
            n.keywords=new ArrayList<>(source.keywords);n.published=Selection.date(link.published());if(n.published.isEmpty()&&old!=null)n.published=old.published;
            n.url=link.url();n.title=link.title();n.duty=link.duty();n.school=source.schoolHint;n.firstSeen=now;n.lastSeen=now;
            if(old!=null){n.deadline=old.deadline;n.deadlineLabel=old.deadlineLabel;}
            Classifier.Deadline deadline=Classifier.deadline(link.summary(),today);
            if(deadline.epoch()!=0){n.deadline=deadline.epoch();n.deadlineLabel=deadline.label();}
            Classifier.reclassify(n,today);if(n.status==Status.MATCH)matches++;
            if(Classifier.matches(n)&&n.published.isEmpty())undated++;
            if(n.status==Status.MATCH||old!=null)out.add(n);
        }
        if(undated>0){partial=true;errors.add("게시일을 확인하지 못한 "+undated+"건은 기간을 판단할 수 없어 제외했어요.");}
        ListingCache.State cache=!partial&&pages>0?ListingCache.updated(source,incremental?previous:null,cacheListings.values(),now,!incremental):null;
        return new Result(out,listings.size(),matches,partial,errors.stream().distinct().limit(15).collect(java.util.stream.Collectors.toList()),cache,incremental,pages);
    }
    public static String id(Source s,String url){return PostIdentity.id(s.id,s.url,url);}
    public static String shortError(Exception e){String s=e.getMessage();return s==null?e.getClass().getSimpleName():s.substring(0,Math.min(s.length(),160));}
}
