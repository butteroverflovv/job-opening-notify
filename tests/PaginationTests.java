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
    }
}
