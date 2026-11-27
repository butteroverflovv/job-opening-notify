import kr.personal.historyjobs.core.*;
import static kr.personal.historyjobs.core.Model.*;
import java.nio.charset.*;
import java.util.*;

public final class CatalogTests {
    static void ok(boolean value,String message){CoreTests.check(value,message);}
    public static void run()throws Exception{
        var and=Selection.keywords("교사 AND 공고");
        ok(Selection.titleMatches("2027학년도 교사 채용 공고",and),"AND accepts both terms");
        ok(!Selection.titleMatches("2027학년도 교사 모집",and)&&!Selection.titleMatches("2027학년도 공고 안내",and),"AND requires every term");
        ok(Selection.titleMatches("2026학년도 공고: 교 사 채용",and),"AND terms ignore spaces and order");
        var expression=Selection.keywords("교사 and 채용 OR 교원 선발");
        ok(Selection.titleMatches("교원선발 안내",expression)&&Selection.titleMatches("교사 신규 채용",expression)&&!Selection.titleMatches("교사 안내",expression),"AND precedes OR and preserves phrase terms");
        ok(!Selection.validKeywords(Selection.keywords("AND 교사"))&&!Selection.validKeywords(Selection.keywords("교사 AND"))&&!Selection.validKeywords(Selection.keywords("교사 AND AND 공고")),"incomplete AND clauses rejected");
        ok(Selection.keywords("교사 AND 공고,교사and공고").size()==2,"operator clauses not deduplicated against literal words");
        ok(Selection.titleMatches("coordinator 교사",Selection.keywords("coordinator"))&&!Selection.titleMatches("교사 채용",Selection.keywords("교사AND공고")),"embedded and letters stay literal");
        List<Source> schools=SchoolCatalog.defaults();ok(schools.size()==14&&schools.stream().map(s->s.id).distinct().count()==14,"all fourteen requested schools registered once");
        for(String id:List.of("dongducheon-fl","suwon-fl","anyang-fl"))ok(schools.stream().filter(s->s.id.equals(id)).findFirst().orElseThrow().keywords.get(0).contains(" AND "),"AND school seed: "+id);
        ok(schools.stream().filter(s->s.region.equals("서울")).count()==6&&schools.stream().filter(s->s.region.equals("경기")).count()==8,"school region assignments are Seoul six Gyeonggi eight");
        List<Source> current=new ArrayList<>();SchoolCatalog.install(current);SchoolCatalog.install(current);ok(current.size()==14,"school catalog installation is idempotent");
        Source old=schools.get(0).copy();old.id="custom-sejong";old.name="내 세종과고";old.enabled=false;old.keywords=List.of("이전 키워드");current=new ArrayList<>(List.of(old));SchoolCatalog.install(current);
        ok(current.size()==14&&current.get(0)==old&&!old.enabled&&old.name.equals("내 세종과고")&&old.keywords.equals(List.of("교사")),"same board keeps custom identity and enabled state while receiving requested keywords once");
        Source ewha=schools.stream().filter(s->s.id.equals("ewha-fl")).findFirst().orElseThrow();
        String pretty="<div class='clearfix bo_notice'><div><a href='/0201/1334'>2027학년도 신규교사 채용 공고</a></div><div class='e_date'>2026-10-29</div></div><a href='/0308/222'>다른 게시판 교사 공고</a><a href='/0201?page=2'>2</a>";
        var list=BoardParser.listing(ewha,ewha.url,pretty);
        ok(list.items().size()==1&&list.items().get(0).published().equals("2026-10-29")&&list.items().get(0).pinned(),"Gnuboard pretty URL, date and pinned row parsed without other board leakage");
        ok(ArticleReader.parse(ewha.url,"<article><h1>제목</h1><div id='bo_v_con'>채용 공고 본문입니다.</div><section id='bo_v_file'>파일 영역</section></article>").body().equals("채용 공고 본문입니다."),"Gnuboard selects body rather than whole article and file section");
        Source md=schools.stream().filter(s->s.id.equals("myungduk-fl")).findFirst().orElseThrow();
        String act="<table><tr><td><a href='/?act=news.main&comm=com&mcode=1310&sid=1262'>2027학년도 신규교사 채용 공고</a></td><td>2026-11-20</td></tr></table><a href='/?act=news.main&comm=other&mcode=1310&sid=12'>다른 글</a>";
        var ml=BoardParser.listing(md,md.url,act);ok(ml.items().size()==1&&ml.items().get(0).published().equals("2026-11-20"),"legacy news board typed identifier and publication date parsed");
        Source gimpo=schools.stream().filter(s->s.id.equals("gimpo-fl")).findFirst().orElseThrow();
        var gl=BoardParser.listing(gimpo,gimpo.url,"<a href='/?act=board.read&code=2312&id=1000' title='2027학년도 신규 교사 채용'><span class='title'>줄임말...</span><span class='date'>2026-11-20</span></a><a href='/?act=board.read&code=other&id=1000'>다른 게시판 교사</a>");
        ok(gl.items().size()==1&&gl.items().get(0).title().equals("2027학년도 신규 교사 채용"),"legacy act board keeps full title and correct board code");
        ok(ArticleReader.parse(md.url,"<div class='boardRead'><div id='tmpContentsArea'>공고 본문</div><div class='boradReadFooter'>첨부 파일 목록</div></div>").body().equals("공고 본문"),"legacy read body excludes attached file footer");
        byte[] encoded="<meta charset='euc-kr'>2027학년도 교사 채용 공고".getBytes(Charset.forName("EUC-KR"));
        ok(new Net.Response(encoded,"text/html; charset=utf-8",gimpo.url).text().contains("2027학년도 교사 채용 공고"),"incorrect UTF-8 server header falls back to declared Korean encoding");
        Source hansung=schools.stream().filter(s->s.id.equals("hansung-science")).findFirst().orElseThrow();
        String shell=SchoolBoardTests.SHELL.replace("BBSMSTR_000000010949","BBS_0000000000001352");
        var shellList=BoardParser.listing(hansung,hansung.url,shell);ok(shellList.pages().size()==1&&shellList.pages().get(0).contains("BBS_0000000000001352"),"imported Seoul BBS identifiers accepted alongside BBSMSTR");
        ok(SchoolBoards.articleRequest(hansung.url+"?nttId=123",shell).contains("selectBoardDetailAjax.do"),"imported Seoul board uses its public detail endpoint");
        String duplicated="<a href='/?act=news.main&comm=com&mcode=1310&Page=1&sid=1262'>2027학년도 신규교사 공고</a><table><tr><td><a href='/?act=news.main&comm=com&mcode=1310&Page=2&sid=1262'>2027학년도 신규교사 공고</a></td><td>2026-11-20</td></tr></table>";
        var stable=BoardParser.listing(md,md.url,duplicated);ok(stable.items().size()==1&&stable.items().get(0).published().equals("2026-11-20"),"same legacy record deduplicated across pages, with dated row preferred over navigation");
        Source gc=schools.stream().filter(s->s.id.equals("gwacheon-fl")).findFirst().orElseThrow();List<String> calls=new ArrayList<>();
        Collector asp=new Collector((url,max,ref)->{calls.add(url);int page=Selection.pageNumber(gc,url);String date=page<=22?"2026.11.20":"2024.12.31";String body="<table><tr><td><img src='/images/notice.jpg'></td><td><a href='notice_read.asp?idx=999&lv1=999&lv3=0&page="+page+"'>2026학년도 안내</a></td><td>2026.11.20</td></tr><tr><td>"+page+"</td><td><a href='notice_read.asp?idx="+page+"&lv1="+page+"&lv3=0&page="+page+"'>2026학년도 채용 공고</a></td><td>"+date+"</td></tr></table><a href='?page="+(page+1)+"'>다음</a>";return CoreTests.response(url,body);});
        var archive=asp.collect(gc,Map.of(),java.time.Instant.parse("2026-11-27T03:00:00Z").toEpochMilli());
        ok(calls.size()==23&&!archive.partial()&&archive.checked()==22,"sparse ASP board continues past twenty pages and stops at dated archive boundary despite pinned notice");
        ok(ArticleReader.parse(gc.url,"<iframe src='/comment.asp?idx=1'></iframe>").frames().isEmpty(),"article reader does not follow comment frames");
        ok(SchoolCatalog.key("http://www.gcfl.or.kr/").equals(SchoolCatalog.key(gc.url)),"Gwacheon landing page recognized as same requested notice board");
        String aspNav="<a href='notice_read.asp?idx=42&lv1=42&lv3=0&page=1'>2026학년도 채용 공고</a><table><tr><td><a href='notice_read.asp?page=1&idx=43&lv1=43&lv3=0'>2026학년도 채용 공고</a></td><td>2026.11.20</td></tr></table>";
        ok(BoardParser.listing(gc,gc.url,aspNav).items().size()==1,"ASP summary navigation not mistaken for undated list records");
        ok(ArticleReader.parse("https://www.gcfl.or.kr/notification/notice_read.asp?idx=43","<table width='675' cellspacing='0'><tr><td>첨부파일 목록</td></tr></table><table width='675' cellspacing='10'><tr><td height='16'>채용 공고 본문</td></tr></table><iframe src='/include/comment.asp'></iframe>").body().equals("채용 공고 본문"),"legacy ASP body cell selected without attachments or comments");
        ok(Model.defaults().size()==25&&Model.defaults().get(2).id.equals("tokyo"),"catalog appended without replacing existing source identifiers");
    }
}
