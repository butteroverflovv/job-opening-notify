import kr.personal.historyjobs.core.*;
import static kr.personal.historyjobs.core.Model.*;
import java.time.*;
import java.util.*;

public final class NoticeAndDateTests {
    static void ok(boolean value,String text){CoreTests.check(value,text);}
    static final LocalDate TODAY=LocalDate.of(2026,11,27);
    static final long NOW=TODAY.atStartOfDay(Classifier.KST).toInstant().toEpochMilli();
    static String row(int id,String title,String date){return "<tr><td>"+id+"</td><td><a href='?bo_table=030601&wr_id="+id+"'>"+title+"</a></td><td class='td_datetime'>"+date+"</td></tr>";}
    static String list(String rows,int next){return "<table><tr><th>번호</th><th>제목</th><th>날짜</th></tr>"+rows+"</table>"+(next>0?"<a href='?bo_table=030601&page="+next+"'>다음</a>":"");}
    static String detail(String date){return "<section id='bo_v_info'><span class='if_date'><span class='sound_only'>작성일</span> "+date+" 11:29</span></section><div id='bo_v_con'>2027학년도 모집 본문<a href='/file.pdf'>첨부파일</a></div>";}
    public static void run()throws Exception{
        ok(NoticeOrder.elapsed(0).equals("소요 시간 · 0초")&&NoticeOrder.elapsed(59999).equals("소요 시간 · 59초"),"elapsed display uses total seconds below a minute");
        ok(NoticeOrder.elapsed(60000).equals("소요 시간 · 1분 0초")&&NoticeOrder.elapsed(125999).equals("소요 시간 · 2분 5초"),"elapsed display uses minutes and remaining seconds");
        Notice old=new Notice();old.id="old";old.firstSeen=10;old.lastSeen=100;old.deadline=NOW+100;
        Notice fresh=new Notice();fresh.id="new";fresh.firstSeen=20;fresh.lastSeen=20;fresh.deadline=NOW+200;
        List<Notice> notices=new ArrayList<>(List.of(fresh,old));notices.sort(NoticeOrder.comparator(false,NOW));ok(notices.get(0)==old,"deadline mode keeps the nearer future deadline first");
        old.firstSeen=30;old.published="2026-11-26";fresh.published="2026-11-27";
        notices.sort(NoticeOrder.comparator(true,NOW));ok(notices.get(0)==fresh,"latest publication wins even when the older post was discovered more recently");
        old.published=fresh.published;notices.sort(NoticeOrder.comparator(true,NOW));ok(notices.get(0)==old,"same-day publications retain deterministic discovery tie order");
        old.published="";notices.sort(NoticeOrder.comparator(true,NOW));ok(notices.get(0)==fresh,"unknown publication dates follow verified dates");
        old.deadline=0;notices.sort(NoticeOrder.comparator(false,NOW));ok(notices.get(0)==fresh,"unknown deadline follows known open deadline");
        ok(GnuDates.detailPublished(detail("2026-11-27"),TODAY).equals("2026-11-27"),"Dongtan four-digit detail publication date supported");
        ok(GnuDates.detailPublished(detail("26-11-27"),TODAY).equals("2026-11-27"),"Suwon two-digit detail publication year expanded");
        ok(GnuDates.detailPublished(detail("11-27"),TODAY).isEmpty()&&GnuDates.detailPublished(detail("11:29"),TODAY).isEmpty(),"month-day and clock-only detail dates never imply a year");
        ok(GnuDates.detailPublished("<div id='bo_v_con'>작성일 2027-01-01</div><p>작성일 2026-11-27</p>",TODAY).isEmpty(),"body dates and arbitrary prose cannot supply publication metadata");
        ok(GnuDates.detailPublished("<table><tr><th>작성일</th><td>26-11-27</td></tr></table>",TODAY).equals("2026-11-27"),"labeled metadata table supplies a full publication date");
        Source source=SchoolCatalog.defaults().stream().filter(s->s.id.equals("suwon-fl")).findFirst().orElseThrow();
        String hidden=list(row(1,"2027학년도 교사 공고","<time datetime='2026-11-27'>11-27</time>"),0);
        ok(BoardParser.listing(source,source.url,hidden).items().get(0).published().equals("2026-11-27"),"full date in abbreviated list time attribute used before detail request");
        String html=list(row(1,"2027학년도 교사 공고","11-27")+row(2,"학교 행사 안내","11:29"),0);List<String> calls=new ArrayList<>();
        Collector c=new Collector((url,max,ref)->{calls.add(url);if(url.contains("wr_id=1"))return CoreTests.response(url,detail("26-11-27"));if(url.contains("wr_id=2"))throw new AssertionError("unrelated non-anchor details requested");return CoreTests.response(url,html);});
        var result=c.collect(source,Map.of(),NOW);
        ok(result.notices().size()==1&&result.notices().get(0).published.equals("2026-11-27")&&!result.partial()&&calls.size()==2,"abbreviated GNU list includes matching notice using detail metadata only");
        ok(calls.stream().noneMatch(u->u.endsWith(".pdf")),"publication fallback never downloads attachment links");
        calls.clear();Collector archive=new Collector((url,max,ref)->{calls.add(url);if(url.contains("wr_id="))return CoreTests.response(url,detail(url.contains("wr_id=1")?"2024-12-31":"2024-12-30"));if(url.contains("page=2"))throw new AssertionError("archive stop failed");return CoreTests.response(url,list(row(1,"학교 행사","12-31")+row(2,"다른 학교 행사","12-30"),2));});
        result=archive.collect(source,Map.of(),NOW);ok(calls.size()==3&&!result.partial()&&result.notices().isEmpty(),"old GNU page stops only after every ordinary date has been verified");
        calls.clear();Collector mixed=new Collector((url,max,ref)->{calls.add(url);if(url.contains("wr_id="))return CoreTests.response(url,detail(url.contains("wr_id=1")?"2024-12-31":"2026-11-27"));return CoreTests.response(url,list(row(1,"학교 행사","12-31")+row(2,"2027학년도 교사 공고","11-27"),0));});
        result=mixed.collect(source,Map.of(),NOW);ok(result.notices().size()==1&&!result.partial(),"old anchor cannot exclude a current matching notice in the same page");
        Collector failed=new Collector((url,max,ref)->{if(url.contains("wr_id="))throw new java.io.IOException("detail unavailable");return CoreTests.response(url,html);});result=failed.collect(source,Map.of(),NOW);
        ok(result.partial()&&result.notices().isEmpty()&&result.cache()==null,"unverified year is excluded with explanation and cannot advance cache");
        // Fingerprints are made from raw list metadata so selective date lookups
        // cannot invalidate overlap on later checks when page boundaries move.
        calls.clear();final int[] run={0};Collector repeated=new Collector((url,max,ref)->{calls.add(url);if(url.contains("wr_id="))return CoreTests.response(url,detail("26-11-27"));int p=Selection.pageNumber(source,url);return CoreTests.response(url,list(row(p,"2027학년도 교사 공고","11-27"),p<3?p+1:0));});
        var full=repeated.collect(source,Map.of(),NOW);calls.clear();var quick=repeated.collect(source,Map.of(),NOW+1000,full.cache());
        ok(quick.incremental()&&quick.pages()==2&&!quick.partial(),"GNU raw-list cache remains compatible with exact detail dates");
        Source dtg=InternationalCatalog.defaults().stream().filter(s->s.id.equals("dongtan-international")).findFirst().orElseThrow();
        String dhtml="<table><tr><td><a href='/030101/1'>2027학년도 교사 채용</a></td><td class='td_datetime'>11:29</td></tr></table>";
        var d=new Collector((url,max,ref)->CoreTests.response(url,url.endsWith("/030101/1")?detail("2026-11-27"):dhtml)).collect(dtg,Map.of(),NOW);
        ok(d.notices().size()==1&&d.notices().get(0).published.equals("2026-11-27"),"Dongtan pretty links with time-only cells use verified four-digit detail dates");
    }
}
