import kr.personal.historyjobs.core.*;
import static kr.personal.historyjobs.core.Model.*;
import java.time.*;
import java.util.*;

public final class PaginationTests {
    static void ok(boolean value,String message){CoreTests.check(value,message);}
    static String row(int id,String date,boolean pinned){return "<tr><td>"+(pinned?"<span class='flag_notice'>공지</span>":id)+"</td><td><a href='/view.do?nttSn="+id+"'>2026학년도 역사 채용</a></td><td>"+date+"</td></tr>";}
    static String page(String rows){return "<table><tr><th>번호</th><th>제목</th><th>작성일</th></tr>"+rows+"</table><a href='?page=1'>1</a><a href='?page=2'>2</a><a href='?page=3'>3</a><a href='?page=20'>20</a>";}
    public static void run()throws Exception{
        ok(Html.resolve("https://school.test/list?bbs=1","?page=2").equals("https://school.test/list?page=2"),"query-only page links preserve current endpoint path");
        Source source=Model.source("school","학교","https://school.test/list","GENERIC","서울","");
        LocalDate today=LocalDate.of(2026,11,27);long now=today.atStartOfDay(Classifier.KST).toInstant().toEpochMilli();
        List<String> calls=new ArrayList<>();Collector c=new Collector((url,max,ref)->{calls.add(url);return CoreTests.response(url,page(row(9,"2026-11-27",true)+row(Selection.pageNumber(source,url)==1?1:2,Selection.pageNumber(source,url)==1?"2026-11-27":"2024-12-31",false)));});
        var result=c.collect(source,Map.of(),now);
        ok(calls.equals(List.of(source.url,source.url+"?page=2"))&&!result.partial()&&result.errors().isEmpty(),"all boards stop normally at old regular rows despite recent pinned notice");
        ok(result.notices().size()==2&&result.checked()==2,"pinned and recent notices retained while archive rows excluded");
        var parsed=BoardParser.listing(source,source.url,page(row(9,"2026-11-27",true)));
        ok(parsed.items().get(0).pinned(),"notice marker in sibling number column recognized");
        ok(!Selection.oldPage(parsed.items(),today),"only pinned notices do not imply old archive frontier");
        var duplicate=BoardParser.listing(source,source.url,page(row(9,"2026-11-27",true)+row(9,"2026-11-27",false)));
        ok(duplicate.items().size()==1&&!duplicate.items().get(0).pinned(),"ordinary duplicate of pinned row remains a chronological record");
        ok(!Selection.oldPage(List.of(new Link("old","https://x/1","","2024-12-31",false),new Link("unknown","https://x/2")),today),"unknown date prevents early stop");
        ok(!Selection.oldPage(List.of(new Link("old","https://x/1","","2024-12-31",false),new Link("current","https://x/2","","2025-01-01",false)),today),"mixed date page continues through existing publication window");
        for(Source s:List.of(Model.defaults().get(0),Model.defaults().get(1),SchoolBoardTests.gsh(),SchoolBoardTests.sshs())){
            String key=SchoolBoards.pageKey(s),base=BoardParser.query(s.url,key,"3"),next=BoardParser.query(s.url,key,"4");
            // Seoul numeric menu shell is a bootstrap rather than a paginated AJAX response.
            if(BoardParser.kind(s).equals("SCHOOL_SEN")){base="https://sshs.sen.hs.kr"+SchoolBoards.SEN_LIST+"?bbsId=BBSMSTR_000000010949&pageIndex=3";next=BoardParser.query(base,key,"4");}
            ok(Selection.nextPages(s,base,List.of(BoardParser.query(base,key,"1"),BoardParser.query(base,key,"20"),next)).equals(List.of(next)),"sequential paging for "+BoardParser.kind(s));
        }
        calls.clear();Collector firstOld=new Collector((url,max,ref)->{calls.add(url);return CoreTests.response(url,page(row(1,"2024-12-31",false)));});
        ok(!firstOld.collect(source,Map.of(),now).partial()&&calls.size()==1,"old first page ends normally without extra archive requests");
        calls.clear();Collector deep=new Collector((url,max,ref)->{
            calls.add(url);int p=Selection.pageNumber(source,url);
            if(p>85)throw new AssertionError("requested page beyond old date frontier");
            String title=p==83?"2027학년도 역사 채용":"학교 행사 안내";
            String rows=row(999,"2026-11-27",true)+row(p,p==85?"2024-12-31":"2025-12-20",false).replace("2026학년도 역사 채용",title);
            return CoreTests.response(url,"<table><tr><th>번호</th><th>제목</th><th>작성일</th></tr>"+rows+"</table><a href='?page="+(p+1)+"'>다음</a>");
        });
        result=deep.collect(source,Map.of(),now);
        ok(calls.size()==85&&!result.partial()&&result.errors().isEmpty(),"new generic schools scan beyond former quotas to the old date frontier normally");
        ok(result.notices().stream().anyMatch(n->n.title.equals("2027학년도 역사 채용")),"matching post beyond eighty pages is collected after unrelated pages");
        Source act=InternationalCatalog.defaults().stream().filter(s->s.id.equals("daewon-international")).findFirst().orElseThrow();calls.clear();
        Collector legacy=new Collector((url,max,ref)->{
            calls.add(url);int p=Selection.pageNumber(act,url);if(p>24)throw new AssertionError("past legacy frontier");
            String rows=row(p,p==24?"2024-12-31":"2026-11-01",false).replace("/view.do?nttSn="+p,"/?act=board.read&code=1310&id="+p).replace("역사 채용","교사 공고");
            return CoreTests.response(url,"<table><tr><th>번호</th><th>제목</th><th>작성일</th></tr>"+rows+"</table><a href='?act=board.list&code=1310&page="+(p+1)+"'>다음</a>");
        });
        result=legacy.collect(act,Map.of(),now);
        ok(calls.size()==24&&result.checked()==23&&!result.partial(),"Daewon-style boards pass twenty pages and stop at old dates without range warning");
        calls.clear();Collector repeats=new Collector((url,max,ref)->{
            calls.add(url);if(calls.size()>2)throw new AssertionError("repeated logical list was not stopped");
            int p=Selection.pageNumber(source,url);
            return CoreTests.response(url,page(row(1,"2026-11-27",false))+"<span>조회수 "+p+"</span><a href='?page="+(p+1)+"'>다음</a>");
        });
        result=repeats.collect(source,Map.of(),now);
        ok(calls.size()==2&&!result.partial()&&result.notices().size()==1,"repeated lists with changing HTML counters terminate without endless paging");
        Collector failedLater=new Collector((url,max,ref)->{if(Selection.pageNumber(source,url)>1)throw new java.io.IOException("연결 실패");return CoreTests.response(url,page(row(1,"2026-11-27",false)));});
        result=failedLater.collect(source,Map.of(),now);
        ok(result.partial()&&result.errors().stream().anyMatch(e->e.contains("연결 실패")),"real later-page network failure remains visible");
        try{
            Thread.currentThread().interrupt();result=new Collector((url,max,ref)->{throw new AssertionError("cancelled collector requested network");}).collect(source,Map.of(),now);
            ok(result.partial()&&result.errors().stream().anyMatch(e->e.contains("중단")),"cancellation is not reported as a completed archive scan");
        }finally{Thread.interrupted();}
    }
}
