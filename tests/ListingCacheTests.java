import kr.personal.historyjobs.core.*;
import static kr.personal.historyjobs.core.Model.*;
import java.util.*;
import java.time.*;

public final class ListingCacheTests {
    static void ok(boolean value,String message){CoreTests.check(value,message);}
    static final long NOW=Instant.parse("2026-11-27T03:00:00Z").toEpochMilli();
    static Source school(){return Model.source("cache-school","캐시 학교","https://cache.test/list","GENERIC","서울","");}
    static String page(int p,int total,int edited){
        StringBuilder html=new StringBuilder("<table><tr><th>번호</th><th>제목</th><th>작성일</th></tr>");
        html.append(row(9999,"2027학년도 역사 고정 공지","2026-01-01",true));
        int top=total-(p-1)*10;
        if(top>0)for(int id=top;id>Math.max(0,top-10);id--)html.append(row(id,(id==edited?"수정 ":"")+"2027학년도 "+(id%3==0?"역사 채용":"학교 행사 안내"),"2026-11-01",false));
        else html.append(row(1,"2024학년도 역사 채용","2024-12-31",false));
        html.append("</table><a href='?page=").append(p+1).append("'>다음</a>");return html.toString();
    }
    static String row(int id,String title,String date,boolean pinned){return "<tr><td>"+(pinned?"공지":id)+"</td><td><a href='/view.do?nttSn="+id+"'>"+title+"</a></td><td>"+date+"</td></tr>";}
    static Collector collector(Source source,List<String> calls,int total,int edited){return new Collector((url,max,ref)->{calls.add(url);if(calls.size()>100)throw new AssertionError("unexpected endless fixture");return CoreTests.response(url,page(Selection.pageNumber(source,url),total,edited));});}
    public static void run()throws Exception{
        Source source=school();List<String> calls=new ArrayList<>();var first=collector(source,calls,120,0).collect(source,Map.of(),NOW,null);var saved=first.cache();
        ok(calls.size()==13&&!first.incremental()&&!first.partial(),"first cached check scans full date window to old frontier");
        ok(saved!=null&&saved.records().size()==100&&saved.lastFull()==NOW&&saved.records().stream().allMatch(s->s.length()==64),"cache is capped at one hundred fingerprints, not post text");
        ok(!saved.records().contains(ListingCache.fingerprint(new Link("2027학년도 역사 고정 공지","https://cache.test/view.do?nttSn=9999","","2026-01-01",true))),"pinned posts do not consume regular-list cache capacity");
        calls.clear();var quick=collector(source,calls,120,0).collect(source,Map.of(),NOW+1000,saved);
        ok(quick.incremental()&&calls.size()==2&&!quick.partial()&&quick.errors().isEmpty(),"two unchanged regular pages finish a fast check without range warning");
        ok(quick.cache().lastFull()==NOW&&quick.cache().records().size()==100,"fast checks keep weekly full-check date and bounded cache tail");
        calls.clear();var news=collector(source,calls,135,0).collect(source,Map.of(),NOW+2000,saved);
        ok(calls.size()==4&&news.notices().stream().anyMatch(n->n.title.contains("역사")&&n.url.endsWith("=135")),"new and shifted posts are scanned before two unchanged pages");
        calls.clear();var burst=collector(source,calls,250,0).collect(source,Map.of(),NOW+2000,saved);
        ok(calls.size()==15&&burst.notices().stream().anyMatch(n->n.url.endsWith("=249")),"cache capacity never limits the number of new posts to collect");
        calls.clear();var edited=collector(source,calls,120,105).collect(source,Map.of(),NOW+3000,saved);
        ok(calls.size()==4&&edited.notices().stream().anyMatch(n->n.title.startsWith("수정")),"changed title within checked pages resets unchanged-page streak");
        calls.clear();var weekly=collector(source,calls,120,30).collect(source,Map.of(),NOW+ListingCache.FULL_INTERVAL,saved);
        ok(!weekly.incremental()&&calls.size()==13&&weekly.notices().stream().anyMatch(n->n.title.startsWith("수정")),"weekly full scan discovers deeper edits and renews full timestamp");
        ok(weekly.cache().lastFull()==NOW+ListingCache.FULL_INTERVAL,"successful full scan renews weekly schedule");
        Source changed=source.copy();changed.keywords=List.of("행사");calls.clear();var keywords=collector(changed,calls,120,0).collect(changed,Map.of(),NOW+1000,saved);
        ok(!keywords.incremental()&&calls.size()==13&&!keywords.notices().isEmpty(),"editing keywords forces full scan including previously unrelated posts");
        changed=source.copy();changed.url+="?category=new";ok(!ListingCache.usable(changed,saved,NOW+1000),"changed board URL invalidates cache");
        changed=source.copy();changed.kind="AUTO";ok(!ListingCache.usable(changed,saved,NOW+1000),"changed parser type invalidates cache");
        ok(!ListingCache.usable(source,saved,Instant.parse("2027-01-01T00:00:00Z").toEpochMilli())&&!ListingCache.usable(source,saved,NOW-1),"year rollover and backwards clock trigger full scan");
        Link a=new Link("제목","https://cache.test/1","조회수 1","2026-11-01",false,"역사"),b=new Link("제목","https://cache.test/1","조회수 2","2026-11-01",false,"역사");
        ok(ListingCache.fingerprint(a).equals(ListingCache.fingerprint(b))&&!ListingCache.fingerprint(a).equals(ListingCache.fingerprint(new Link("제목",a.url(),"",a.published(),false,"영어"))),"volatile view counters ignored while duty changes invalidate fingerprints");
        calls.clear();Collector failed=new Collector((url,max,ref)->{calls.add(url);if(calls.size()==2)throw new java.io.IOException("연결 실패");return CoreTests.response(url,page(1,120,0));});
        var failure=failed.collect(source,Map.of(),NOW+1000,saved);ok(failure.partial()&&failure.cache()==null&&saved.lastFull()==NOW,"partial network failure cannot replace a successful cache");
        Collector undated=new Collector((url,max,ref)->CoreTests.response(url,"<a href='/view.do?nttSn=9'>2027학년도 역사 채용</a>"));var missing=undated.collect(source,Map.of(),NOW+1000,saved);
        ok(missing.partial()&&missing.cache()==null,"missing publication dates do not create successful checkpoints");
        calls.clear();Collector pins=new Collector((url,max,ref)->{calls.add(url);int p=Selection.pageNumber(source,url);String html=p<=3?"<table><tr><th>번호</th><th>제목</th><th>작성일</th></tr>"+row(9999,"2027학년도 역사 고정 공지","2026-01-01",true)+"</table>":page(p,120,0);return CoreTests.response(url,html+"<span>counter "+p+"</span><a href='?page="+(p+1)+"'>다음</a>");});
        // Identical logical pinned-only lists end via the existing repeat guard,
        // and cannot falsely create the two-page regular-post overlap.
        var onlyPins=pins.collect(source,Map.of(),NOW+1000,saved);ok(calls.size()==2&&onlyPins.incremental(),"pinned-only lists are handled by repeat guard rather than regular overlap");
        ok(!ListingCache.usable(source,new ListingCache.State(saved.config(),NOW,List.of()),NOW+1000),"empty cache falls back to a full scan");
        ok(new ListingCache.State(saved.config(),NOW,Collections.nCopies(200,saved.records().get(0))).records().size()==1,"cache records are immutable bounded unique hashes");
    }
}
