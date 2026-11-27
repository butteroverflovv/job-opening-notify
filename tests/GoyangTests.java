import kr.personal.historyjobs.core.*;
import static kr.personal.historyjobs.core.Model.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.time.*;

public final class GoyangTests {
    static Source source(){Source s=Model.source("goyang-fl","고양외국어고등학교","https://www.gyfl.hs.kr/main/main.php?categoryid=12&menuid=03&groupid=00","AUTO","경기","고양외국어고등학교");s.keywords=List.of("교사");return s;}
    static final String LIST="<script>for(var i=0;i<a.length;i++){}</script><form name='boardForm' method='post'><input name='boardtable' value='alim'></form><table class='noticelist'><tr><th>번호</th><th>제목</th><th>작성일</th><th>첨부파일</th><th>조회수</th></tr><tr><td>[공지]</td><td><a href='./' onclick=\"boardView('6792'); return false;\">생활관 방역 작업</a></td><td>2026-10-15</td><td><a onclick=\"boardDown('1','6792')\">파일</a></td><td>129</td></tr><tr><td>131</td><td><a href='./' onclick=\"boardView('6807'); return false;\">2027학년도 학교법인 순효학원 신규교사 임용후보자…</a></td><td>2026-10-30</td><td><a href='#' onclick=\"boardDown('1','6807'); return false;\">파일</a></td><td>112</td></tr></table><div class='pagination'><a href='./' onclick=\"boardPage('2'); return false;\">2</a><a href='./' onclick=\"boardPage('9'); return false;\">9</a></div>";
    static final String DETAIL="<table class='noticeview marB20'><tr><th colspan='4' class='title'>2027학년도 학교법인 순효학원 신규교사 임용후보자 선정경쟁시험 시행계획 공고문</th></tr><tr><td colspan='4' class='content'><p>2027학년도 학교법인 순효학원 신규교사 채용 공고 본문입니다.</p><p>자세한 사항은 붙임을 참조하세요.</p></td></tr><tr><th>첨부파일</th><td><a onclick=\"boardDown('1','6807')\">공고문.hwp</a></td></tr></table>";
    static void ok(boolean value,String label){CoreTests.check(value,label);}
    public static void run()throws Exception{
        Source s=source();ok(BoardParser.kind(s).equals("SCHOOL_GYFL"),"Goyang notice form adapter selected automatically");
        var list=BoardParser.listing(s,s.url,LIST);ok(list.items().size()==2&&list.warning().isEmpty(),"typed boardView records parsed without invoking JavaScript");
        var post=list.items().get(1);ok(post.published().equals("2026-10-30")&&post.url().contains("board=view")&&post.url().contains("no=6807"),"Goyang record number and publication date retained");
        ok(list.items().get(0).pinned(),"bracketed notice marker recognized for archive stopping");
        ok(list.pages().size()==2&&Selection.nextPages(s,s.url,list.pages()).size()==1&&GoyangBoard.fields(list.pages().get(0)).get("page").equals("2"),"typed boardPage callback generates sequential list form request");
        var fields=GoyangBoard.fields(post.url());ok(fields.get("board").equals("view")&&fields.get("no").equals("6807")&&fields.get("boardtable").equals("alim")&&fields.get("mode").isEmpty(),"only public view form state reconstructed");
        ok(!GoyangBoard.fields(s.url+"&fname1=secret.hwp&password=private").containsKey("fname1")&&!GoyangBoard.fields(s.url+"&fname1=x&password=private").containsKey("password"),"file and password fields never forwarded");
        for(String action:List.of("exec","write","modify","delete","check"))try{GoyangBoard.body(s.url+"&board="+action);throw new AssertionError("write form allowed");}catch(IllegalArgumentException expected){ok(true,"non-read action rejected: "+action);}
        try{GoyangBoard.body(s.url+"&board=view&no=alert(1)");throw new AssertionError("malformed id");}catch(IllegalArgumentException expected){ok(true,"non-numeric Goyang record ID rejected");}
        ok(!GoyangBoard.supports("https://www.gyfl.hs.kr.evil.test/main/main.php")&&!GoyangBoard.supports("https://www.gyfl.hs.kr/main/main.php?categoryid=07&menuid=01"),"form support limited to actual Goyang notice board");
        String malicious=LIST.replace("boardView('6807'); return false;","boardView('6807'); deletePost(1); return false;");ok(BoardParser.listing(s,s.url,malicious).items().size()==1,"extra code in record callback rejected");
        List<String> calls=new ArrayList<>();Collector c=new Collector((url,max,ref)->{calls.add(url);return CoreTests.response(url,LIST);});
        var result=c.collect(s,Map.of(),Instant.parse("2026-11-27T03:00:00Z").toEpochMilli());
        ok(result.checked()==1&&result.notices().size()==1&&calls.stream().noneMatch(u->u.contains("board=view")),"scheduled Goyang collection uses lists only and selects teacher title");
        var body=new ArticleReader((url,max,ref)->CoreTests.response(url,DETAIL)).read(post.url()).body();ok(body.contains("채용 공고 본문")&&!body.contains("공고문.hwp"),"Goyang body cell read without attached file row");
        String stable=GoyangBoard.article(GoyangBoard.page(s.url,8),"6807");ok(stable.equals(post.url()),"Goyang article identity independent of current list page");
        Process child=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-cp",System.getProperty("java.class.path"),"GoyangTransportTests").inheritIO().start();ok(child.waitFor()==0,"Goyang POST transport and normal GET regression checked in isolated JVM");
    }
    public static void main(String[] args)throws Exception{
        Source s=source();var list=BoardParser.listing(s,s.url,Files.readString(Path.of(args[0])));var matches=list.items().stream().filter(l->Classifier.titleMatches(s,l.title())&&Selection.inWindow(l.published(),LocalDate.of(2026,11,27))&&Selection.academicYearMatches(l.title(),s.url,LocalDate.of(2026,11,27))).toList();
        if(list.items().isEmpty()||list.items().stream().anyMatch(l->l.published().isEmpty())||matches.stream().noneMatch(l->l.url().contains("no=6807")))throw new AssertionError("Uploaded list metadata failed");
        String body=ArticleReader.parse(GoyangBoard.article(s.url,"6807"),Files.readString(Path.of(args[1]))).body();if(!body.contains("순효학원")||!body.contains("채용")||body.contains("첨부파일 다운로드")||body.contains("20261030081623.hwp"))throw new AssertionError("Uploaded article extraction failed");
        System.out.println("Uploaded Goyang fixtures: listed="+list.items().size()+" dated="+list.items().stream().filter(l->!l.published().isEmpty()).count()+" matched="+matches.size()+" bodyCharacters="+body.length());
    }
}
