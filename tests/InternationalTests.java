import kr.personal.historyjobs.core.*;
import static kr.personal.historyjobs.core.Model.*;
import java.util.*;

public final class InternationalTests {
    static void ok(boolean value,String label){CoreTests.check(value,label);}
    public static void run()throws Exception{
        List<Source> added=InternationalCatalog.defaults();ok(added.size()==6&&added.stream().map(s->s.id).distinct().count()==6,"six requested international school subscriptions");
        var byId=new HashMap<String,Source>();for(Source s:added)byId.put(s.id,s);
        ok(byId.get("sejong-international").region.equals("기타 지역")&&byId.get("seoul-international").region.equals("서울"),"Sejong International belongs to other region while Seoul International is Seoul");
        ok(byId.get("seoul-international").keywords.equals(List.of("교사","기간제 교원")),"Seoul International OR phrases preserved");
        ok(byId.get("sejong-international").keywords.equals(List.of("교사 AND 공고")),"Sejong International AND condition preserved");
        Source cs=byId.get("cheongshim-international");ok(Classifier.titleMatches(cs,"2027학년도 교사 임용 공고")&&Classifier.titleMatches(cs,"2026학년도 기간제교원 모집")&&!Classifier.titleMatches(cs,"교사 연수 안내"),"Cheongshim AND group OR temporary teacher phrase");
        Source dw=byId.get("daewon-international");ok(Classifier.titleMatches(dw,"기간제교원 모집")&&Classifier.titleMatches(dw,"기간제 교원 모집")&&Classifier.titleMatches(dw,"교사 채용"),"Daewon equivalent spaced phrases and teacher alternative");
        List<Source> old=new ArrayList<>(SchoolCatalog.defaults());old.get(0).keywords=List.of("사용자 설정");InternationalCatalog.install(old);InternationalCatalog.install(old);ok(old.size()==20&&old.get(0).keywords.equals(List.of("사용자 설정")),"new catalog adds six once without resetting previous fourteen schools");
        Source custom=byId.get("seoul-international").copy();custom.id="my-seoul";custom.enabled=false;old=new ArrayList<>(List.of(custom));InternationalCatalog.install(old);ok(old.size()==6&&old.get(0)==custom&&!custom.enabled,"same international board reuses custom identity and enabled state");
        Source ggg=byId.get("goyang-international");ok(ggg.url.equals(InternationalBoards.GOYANG)&&SchoolCatalog.key("https://ggg.hs.kr/?menu_no=168").equals(SchoolCatalog.key(ggg.url)),"Goyang mobile notice URL recognized as requested desktop board");
        String html="<ul><li><a class='b_notice' href='sub04_1-1.asp?menu_no=168&bno=100&board_mode=view&page=2'><h2><span>공지</span>2027학년도 교사 공고</h2><p class='b_info'>작성자 | 조회 : 100 | 2026.11.01</p></a></li><li><a href='sub04_1-1.asp?menu_no=481&bno=1&board_mode=view'><h2>다른 게시판 교사 공고</h2><p class='b_info'>2026.11.01</p></a></li></ul><a href='sub04_1-1.asp?menu_no=168&page=2'>2</a>";
        var list=BoardParser.listing(ggg,ggg.url,html);ok(list.items().size()==1&&list.items().get(0).title().equals("2027학년도 교사 공고")&&list.items().get(0).published().equals("2026-11-01")&&list.items().get(0).pinned(),"Goyang International mobile titles, metadata and pinned flag isolated by menu");
        ok(list.pages().size()==1&&Selection.nextPages(ggg,ggg.url,list.pages()).size()==1&&!list.items().get(0).url().contains("page=2"),"Goyang International sequential pages and stable article identity");
        var body=ArticleReader.parse(InternationalBoards.article("100"),"<div class='b_view'><h2>제목</h2><ul><li class='b_file'>첨부 문서.pdf</li><li class='b_con'><p>공고 본문</p><div id='hwpEditorBoardContent' class='hwp_editor_board_content'>documentPr editor metadata</div></li></ul></div>").body();ok(body.equals("공고 본문"),"mobile body excludes file list and editor metadata");
        Source dtg=byId.get("dongtan-international");ok(BoardParser.kind(dtg).equals("SCHOOL_GNU")&&BoardParser.listing(dtg,dtg.url,"<div><a href='/030101/123'>2027학년도 교사 채용</a><time datetime='2026-11-01'></time></div>").items().size()==1,"Dongtan GNUboard pretty record route recognized");
        Source a=Model.source("a","나래고등학교","https://a.test","GENERIC","서울","");a.keywords=List.of("교사 AND 공고");a.enabled=false;
        Source b=Model.source("b","가람고등학교","https://b.test","GENERIC","경기","");b.keywords=List.of("역사");
        Source c=Model.source("c","다온중학교","https://c.test","GENERIC","기타 지역","");c.keywords=List.of("기간제 교원");
        List<Source> sources=new ArrayList<>(List.of(a,c,b));Set<String> all=Set.of("서울","경기","기타 지역");
        ok(SourceList.select(sources,"",all).stream().map(s->s.name).toList().equals(List.of("가람고등학교","나래고등학교","다온중학교")),"management list uses Korean alphabetical order");
        ok(SourceList.select(sources,"나 래",all).equals(List.of(a))&&SourceList.select(sources,"교사",all).equals(List.of(a)),"source search matches names and keyword text ignoring spaces");
        ok(SourceList.select(sources,"",Set.of("경기")).equals(List.of(b))&&SourceList.select(sources,"",Set.of()).isEmpty(),"source region filter applies only selected regions");
        ok(SourceList.select(sources,"기간제",Set.of("서울")).isEmpty()&&SourceList.select(sources,"기간제",all).equals(List.of(c)),"source search and region selection combine");
        ok(sources.equals(List.of(a,c,b))&&!a.enabled&&SourceList.select(sources,"",all).contains(a),"display sorting preserves storage order and keeps disabled boards manageable");
        ok(Model.defaults().size()==25,"new installation has previous nineteen and six new boards");
    }
    public static void main(String[] args)throws Exception{
        Source source=InternationalCatalog.defaults().stream().filter(s->s.id.equals("goyang-international")).findFirst().orElseThrow();
        var list=BoardParser.listing(source,source.url,java.nio.file.Files.readString(java.nio.file.Path.of(args[0])));
        if(list.items().isEmpty()||list.items().stream().anyMatch(l->l.published().isEmpty())||list.items().stream().noneMatch(l->l.title().contains("교사")))throw new AssertionError("Actual mobile list failed");
        var body=ArticleReader.parse(InternationalBoards.article("1685"),java.nio.file.Files.readString(java.nio.file.Path.of(args[1]))).body();
        if(!body.contains("원어민 보조교사")||body.contains("documentPr")||body.contains("test.hwp"))throw new AssertionError("Actual mobile body failed");
        System.out.println("Goyang International public HTML: listed="+list.items().size()+" dated="+list.items().stream().filter(l->!l.published().isEmpty()).count()+" bodyCharacters="+body.length());
    }

}
