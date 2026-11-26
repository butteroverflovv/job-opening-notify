import kr.personal.historyjobs.core.*;
import static kr.personal.historyjobs.core.Model.*;
import java.util.*;
import java.time.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public final class SchoolBoardTests {
    static void ok(boolean value,String message){CoreTests.check(value,message);}
    static Source gsh(){Source s=Model.source("gsh","경기과학고","https://gs-h.goesw.kr/gs-h/na/ntt/selectNttList.do?mi=1275&bbsId=1088","AUTO","경기","");s.keywords=List.of("교사");return s;}
    static Source sshs(){Source s=Model.source("sshs","서울과학고","https://sshs.sen.hs.kr/75052/subMenu.do","AUTO","서울","");s.keywords=List.of("교원");return s;}
    static final String GSH="<script>for(var i=0;i<check_list.length;i++){var fake='<a href=\"/view.do?nttSn=999\">교사 가짜</a>';}</script><table><thead><tr><th>번호</th><th>제목</th><th>등록일</th><th>파일</th></tr></thead><tbody><tr><td>41</td><td><a class='nttInfoBtn' href='javascript:' data-id='599447'>2027학년도 경기과학고등학교 교사 공모 최종 합격자 발표</a></td><td><em>등록일</em>2026.11.05</td><td><a href='javascript:openAttachment(123)'>파일</a></td></tr></tbody></table><a href='javascript:' onclick='goPaging(2)'>2</a>";
    static final String SHELL="<script>var endpoint='/dggb/module/board/selectBoardListAjax.do';for(var i=0;i<items.length;i++){}</script><form id='boardFrm'><input name='bbsId' value='BBSMSTR_000000010949'><input name='bbsTyCode' value='notice'><input name='pageIndex' value='1'><input name='customRecordCountPerPage' value='10'></form><div id='board_area'></div>";
    static String senList(String id,String date){return "<script>for(var i=0;i<check_list.length;i++){} function fnView(bbsId,nttId){}</script><table class='board_type01_tb_list'><tr><th>번호</th><th>제목</th><th>작성일</th></tr><tr><td><span class='flag_notice'>공지</span></td><td class='subject'><a href='javascript:void(0)' onclick=\"fnView('BBSMSTR_000000010949', '"+id+"');\">2027학년도 교원 임용후보자 공개선발 안내</a></td><td>"+date+"</td></tr><tr><td>1</td><td class='subject'><a href='javascript:void(0)' onclick=\"fnView('BBSMSTR_000000010949', '"+id+"');\">2027학년도 교원 임용후보자 공개선발 안내</a></td><td>"+date+"</td></tr></table><a href='javascript:' onclick='fnPage(2)'>2</a>";}
    static Net.Response response(String url,String html){return new Net.Response(html.getBytes(StandardCharsets.UTF_8),"text/html; charset=utf-8",url);}
    public static void run()throws Exception{
        Source g=gsh(),s=sshs();
        ok(BoardParser.kind(g).equals("SCHOOL_NTT")&&BoardParser.kind(s).equals("SCHOOL_SEN"),"school CMS auto-detected from registered URLs");
        Source override=g.copy();override.kind="GYEONGGI";ok(BoardParser.kind(override).equals("SCHOOL_NTT"),"school path takes precedence over mistaken education-portal adapter");
        var list=BoardParser.listing(g,g.url,GSH);
        ok(list.items().size()==1&&list.warning().isEmpty(),"data-id school title found without executing scripts");
        Link entry=list.items().get(0);ok(entry.url().contains("selectNttInfo.do?mi=1275&bbsId=1088&nttSn=599447"),"school detail URL retains school board identifiers");
        ok(entry.published().equals("2026-11-05"),"publication read from row containing separate attachment action");
        ok(list.pages().stream().anyMatch(p->p.endsWith("currPage=2")),"school pagination uses currPage instead of generic page");
        ok(!entry.title().contains("가짜"),"JavaScript less-than comparisons do not corrupt following HTML");
        ok(BoardParser.listing(g,g.url,GSH.replace("data-id='599447'","data-id='alert(1)'" )).items().isEmpty(),"non-numeric school IDs rejected");
        var root=BoardParser.listing(s,s.url,SHELL);ok(root.items().isEmpty()&&root.pages().size()==1&&root.warning().isEmpty(),"Seoul empty menu shell discovers public list request");
        String ajax=root.pages().get(0);ok(ajax.contains(SchoolBoards.SEN_LIST)&&ajax.contains("bbsId=BBSMSTR_000000010949")&&ajax.contains("pageIndex=1"),"Seoul list route derives board identifier from form");
        var posts=BoardParser.listing(s,ajax,senList("27641073","2026-11-23"));
        ok(posts.items().size()==1&&posts.items().get(0).published().equals("2026-11-23"),"Seoul pinned and ordinary rows deduplicated with publication date");
        ok(posts.items().get(0).url().equals(s.url+"?nttId=27641073"),"Seoul original link opens full public school detail page");
        ok(posts.pages().stream().anyMatch(p->p.contains("pageIndex=2")&&p.contains("bbsId=BBSMSTR_000000010949")),"Seoul AJAX pagination retains board context");
        Source phrase=s.copy();phrase.keywords=List.of("교원 선발");ok(!Classifier.titleMatches(phrase,posts.items().get(0).title())&&Classifier.titleMatches(s,posts.items().get(0).title()),"keyword phrase is not silently broadened across intervening words");
        ok(!SchoolBoards.seoulRequest(s.url,SHELL,Html.parse(SHELL.replace("BBSMSTR_000000010949","javascript:alert(1)")),false).contains("selectBoard"),"untrusted form identifiers cannot generate a school API request");
        ok(BoardParser.listing(s,ajax,senList("27641073","2026-11-23").replace("BBSMSTR_000000010949","BBSMSTR_999")).items().isEmpty(),"unrelated board callback IDs rejected");
        ok(BoardParser.listing(s,s.url,"<h1>안전한 서비스 이용 안내</h1><p>웹 보안 필터링 시스템이 동작하였습니다. 차단 정보</p>").warning().contains("보안"),"security response reported distinctly instead of interpreted as empty list");
        long now=Instant.parse("2026-11-27T03:00:00Z").toEpochMilli();List<String> calls=new ArrayList<>();
        Collector c=new Collector((url,max,ref)->{calls.add(url);if(url.equals(s.url))return response(url,SHELL);return response(url,SchoolBoards.query(url).getOrDefault("pageIndex","1").equals("2")?senList("200","2024-12-31"):senList("27641073","2026-11-23"));});
        var collected=c.collect(s,Map.of(),now);ok(collected.notices().size()==1&&collected.checked()==1&&!collected.partial(),"Seoul shell and list pages collected with keyword and year rules");
        ok(calls.size()==3&&calls.stream().noneMatch(u->u.contains("nttId=")),"scheduled school collection never opens details or attachments");
        Collector gc=new Collector((url,max,ref)->response(url,GSH));ok(gc.collect(g,Map.of(),now).checked()==1,"Gyeonggi teacher keyword survives actual school link construction");
        String publicDetail=s.url+"?nttId=27641073",detail="<table class='board_type01_tb_readform'><tr><th>제목</th><td>교원 선발 안내</td></tr><tr><th>내용</th><td><div class='content'><p>서울 학교의 실제 본문입니다. 이 부분만 읽어서 앱에서 보여줍니다.</p></div></td></tr><tr><th>파일첨부</th><td><a href='/file.hwp'>첨부 문서</a></td></tr></table>";
        calls.clear();ArticleReader reader=new ArticleReader((url,max,ref)->{calls.add(url);return response(url,url.equals(publicDetail)?SHELL:detail);});
        String body=reader.read(publicDetail).body();ok(body.startsWith("서울 학교의 실제 본문")&&!body.contains("첨부 문서")&&calls.size()==2,"Seoul article reader follows public body request and excludes attachment row");
        ok(calls.get(1).contains(SchoolBoards.SEN_DETAIL)&&calls.get(1).contains("nttId=27641073"),"Seoul detail request carries public article identifier");
        body=ArticleReader.parse(g.url,"<div class='bbs_ViewA'><div class='bbsV_cont'><p>경기 학교 공고 본문</p></div><div class='bbsV_atchmnfl'>파일첨부 정보</div></div>").body();
        ok(body.equals("경기 학교 공고 본문"),"Gyeonggi school body container extracted without file section");
        cookieSession();
    }
    static void cookieSession()throws Exception{
        try(ServerSocket server=new ServerSocket(0,10,InetAddress.getByName("127.0.0.1"))){
            server.setSoTimeout(8000);String base="http://127.0.0.1:"+server.getLocalPort();List<String> requests=Collections.synchronizedList(new ArrayList<>());AtomicReference<Throwable> fault=new AtomicReference<>();
            Thread worker=new Thread(()->{try{for(int i=0;i<5;i++)try(Socket socket=server.accept()){
                socket.setSoTimeout(5000);BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII));String first=in.readLine(),path=first.split(" ")[1],line;StringBuilder headers=new StringBuilder();while((line=in.readLine())!=null&&!line.isEmpty())headers.append(line).append('\n');requests.add(path+" "+headers);
                boolean start=path.equals("/school/start"),binary=path.equals("/school/binary");String status=start?"302 Found":"200 OK",extra=start?"Set-Cookie: schoolSession=fixture; Path=/school; HttpOnly\r\nSet-Cookie: secureOnly=fixture; Path=/; Secure\r\nLocation: /school/list\r\n":"";byte[] bytes="<article>학교 본문</article>".getBytes(StandardCharsets.UTF_8);
                var out=socket.getOutputStream();out.write(("HTTP/1.1 "+status+"\r\n"+extra+"Content-Type: "+(binary?"application/pdf":"text/html; charset=utf-8")+"\r\nContent-Length: "+bytes.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));out.write(bytes);out.flush();
            }}catch(Throwable e){fault.set(e);}},"school-cookie-test");worker.setDaemon(true);worker.start();
            Net.get(base+"/school/start",4000,"");Net.get(base+"/outside",4000,"");Net.get("http://localhost:"+server.getLocalPort()+"/school/public",4000,"");
            try{Net.getHtml(base+"/school/binary",4000,"");throw new AssertionError("binary accepted");}catch(IOException expected){}
            worker.join(2000);if(fault.get()!=null)throw new AssertionError(fault.get());
            ok(requests.size()==5&&requests.get(1).contains("schoolSession=fixture"),"anonymous session cookie carried through redirect to school list");
            ok(!requests.get(2).contains("schoolSession=fixture")&&!requests.get(3).contains("schoolSession=fixture"),"school cookies respect path and host boundaries");
            ok(requests.stream().noneMatch(x->x.contains("secureOnly=fixture")),"secure cookies never sent over HTTP");
        }
    }
}
