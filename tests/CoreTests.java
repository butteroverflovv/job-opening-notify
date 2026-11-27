import kr.personal.historyjobs.core.*;
import static kr.personal.historyjobs.core.Model.*;
import java.util.*;
import java.time.*;
import java.nio.*;
import java.nio.charset.*;
import java.io.*;
import java.util.zip.*;

public class CoreTests {
    static int passed=0;
    static void check(boolean value,String label){if(!value)throw new AssertionError(label);passed++;System.out.println("PASS "+label);}
    static Notice recentNotice(){Notice n=new Notice();n.published=LocalDate.now(Classifier.KST).toString();return n;}
    static void verdict(Status want,String title,String body,String hint,boolean complete){check(Classifier.classify(title,body,hint,complete).status()==want,"classify: "+title+" => "+want);}
    static Source sen=Model.defaults().get(0),goe=Model.defaults().get(1),tokyo=Model.defaults().get(2);
    public static void main(String[] args)throws Exception{
        verdict(Status.MATCH,"가람중학교 역사 기간제교사 채용","","",true);
        verdict(Status.MATCH,"가람고등학교 기간제교원(역사) 채용 공고","","",false);
        verdict(Status.MATCH,"가람중학교 역사 시간강사 채용","","",false);
        verdict(Status.MATCH,"역사 교원 채용","","",true);
        verdict(Status.MATCH,"동경한국학교 역사 교원 채용","","동경한국학교",false);
        verdict(Status.MATCH,"가람초등학교 역사 관련 강사 채용","","",true);
        verdict(Status.EXCLUDED,"가람고등학교 한국사 기간제교사 채용","","",true);
        verdict(Status.EXCLUDED,"가람고등학교 세계사 교사 채용","","",true);
        verdict(Status.EXCLUDED,"가람중학교 영어 기간제교사 채용","모집 과목: 역사","",true);
        verdict(Status.EXCLUDED,"기간제교사 채용","가람중학교 모집과목: 역사","",false);
        verdict(Status.EXCLUDED,"가람고등학교 기간제교사 채용","","",false);
        verdict(Status.EXCLUDED,"역 사 교원 채용","","",true);
        check(!Classifier.titleMatches(null),"null title excluded");
        Notice migrated=recentNotice();migrated.title="가람중학교 영어 채용";migrated.status=Status.REVIEW;migrated.favorite=true;migrated.read=true;migrated.body="본문에 역사 포함";migrated.attachments.add(new Link("파일","https://a.test/file.hwp"));
        Classifier.reclassify(migrated);
        check(migrated.status==Status.EXCLUDED&&migrated.favorite&&migrated.read,"old unrelated review hidden without losing saved flags");
        migrated.title=LocalDate.now(Classifier.KST).getYear()+"학년도 가람중학교 역사 채용";migrated.complete=false;Classifier.reclassify(migrated);
        check(migrated.status==Status.MATCH&&migrated.complete&&migrated.attachments.isEmpty(),"old failed history item becomes normal title match");
        Notice hashItem=recentNotice();hashItem.title="가람중학교 역사 기간제";hashItem.url="https://a.test/job";hashItem.body="본문 1";String beforeHash=hashItem.contentHash();hashItem.body="본문 오류";
        check(beforeHash.equals(hashItem.contentHash()),"body errors do not produce revised alerts");
        Classifier.Deadline d=Classifier.deadline("접수기간: 2026. 11. 26. ~ 2026. 11. 30. 15:00",LocalDate.of(2026,11,26));
        check(Instant.ofEpochMilli(d.epoch()).equals(Instant.parse("2026-11-30T06:00:00Z")),"deadline KST conversion");
        d=Classifier.deadline("접수기간 2026/12/28 ~ 1/3",LocalDate.of(2026,12,26));
        check(Instant.ofEpochMilli(d.epoch()).atZone(Classifier.KST).toLocalDate().equals(LocalDate.of(2027,1,3)),"deadline year rollover");
        check(Classifier.deadline("계약기간 2026/12/1 ~ 2027/2/28",LocalDate.now()).epoch()==0,"do not use employment end as application deadline");
        check(Classifier.deadline("접수기간 2026.13.40",LocalDate.now()).epoch()==0,"invalid date ignored");
        check(Classifier.deadline("접수기간 2026/11/30",LocalDate.now()).label().contains("원문"),"unknown closing time disclosed");
        String sh="<html><body><table><tr><td><a href=\"javascript:fnView('123456');\">가람중학교 역사 기간제교사 채용</a></td></tr></table><a href='?q_currPage=2'>2</a></body></html>";
        sh=sh.replace("</tr>","<td class='date'>2026-11-26</td></tr>");
        BoardParser.Listing sl=BoardParser.listing(sen,sen.url,sh);
        check(sl.items().size()==1&&sl.items().get(0).url().endsWith("q_rcrtSn=123456"),"Seoul javascript detail links");
        check(sl.pages().size()==1,"pagination discovery");
        String gh="<main><a href=\"javascript:fn_pbancInfoView('56789')\">가람고등학교 역사 기간제교사 채용</a></main>";
        BoardParser.Listing gl=BoardParser.listing(goe,goe.url,gh);
        check(gl.items().size()==1&&gl.items().get(0).url().contains("pbancSn=56789"),"Gyeonggi javascript detail links");
        check(BoardParser.listing(goe,goe.url,"<a href='#wrap' onclick='goPaging(2)'>2</a>").pages().get(0).contains("currPage=2"),"real Gyeonggi pagination parameter");
        check(BoardParser.listing(sen,sen.url,"<a href='?q_CurrPage=2' onclick='opMovePage(2);return false;'>2<span>페이지 이동</span></a>").pages().stream().anyMatch(x->x.contains("q_currPage=2")&&x.contains("q_srchJob=")),"Seoul pagination retains job filters");
        check(BoardParser.listing(goe,goe.url,"<a href=\"javascript:goView('67178')\"><div class='cont_top'>가람중학교 조회수 1</div><p class='cont_tit'>마감임박 역사 기간제 채용</p></a>").items().get(0).title().equals("가람중학교 역사 기간제 채용"),"stable title from real card structure");
        check(BoardParser.listing(tokyo,tokyo.url,"<h1>사용제약에 따른 안내</h1>").warning().contains("사용제약"),"Tokyo restriction page reported explicitly");
        check(ArrayLiteral.posts("var Posts=[alert(1)];").isEmpty(),"legacy data never executes JavaScript");
        String legacy="<script>var Posts=[[],[[[405,\"656\",0,0],\":\",\"/front_notice.brd/_656.405.hash/?shell=/index.shell:225\",\"\",\"body\"],[\"\",\"\",0],[\"2026/11/19\",\"11:49:30\",4],[],6252,\"중고등부 역사 기간제 채용 공고\",\"\"]];</script>";
        check(BoardParser.listing(tokyo,tokyo.url,legacy).items().size()==1,"Tokyo legacy array recruitment records");
        check(BoardParser.listing(tokyo,tokyo.url,"<a href='/front_notice.brd/02..abc?shell=/index.shell:225'>02</a>").pages().size()==1,"Tokyo path pagination");
        check(!Html.resolve(tokyo.url,"/front_notice.brd/_656.405.hash/[공고문]%20채용.hwpx").isEmpty(),"attachment brackets in legacy URLs");
        check(BoardParser.detail("https://www.goe.go.kr/","<h3>역사 기간제 채용</h3><button onclick=\"previewAjax('https://www.goe.go.kr/file.hwp','공고.hwp')\">미리보기</button>","fallback").attachments().size()==1,"Gyeonggi preview URL attachment extraction");
        check(BoardParser.detail("https://work.sen.go.kr/","<h2>채용공고</h2><h3 class='job-opening-modal-tit-area__tit'>가람중학교 역사 기간제 채용</h3>","fallback").title().contains("가람중학교"),"Seoul title excludes generic navigation heading");
        String targetDetail="<script>var PostFiles=[[\"목표.hwpx\",\"\",\"/front_notice.brd/_123.1.hash/target.hwpx\"]];</script><table><tr><th>역사 기간제 채용</th></tr></table><div class='context_view'>목표 공고 본문입니다. 중고등부 역사 기간제교사 한 명을 모집합니다.</div>"+legacy;
        BoardParser.Detail tokyoDetail=BoardParser.detail(tokyo.url,targetDetail,"동경한국학교 역사 기간제 채용");
        check(tokyoDetail.body().startsWith("목표 공고")&&tokyoDetail.attachments().size()==1,"Tokyo detail uses own content and PostFiles, not related posts");
        String th="<a href='/front_notice.brd?shell=/index.shell:225&amp;mode=view&amp;num=301'>중등부 역사 기간제교사 채용</a>";
        check(BoardParser.listing(tokyo,tokyo.url,th).items().size()==1,"Tokyo ordinary board links");
        check(!BoardParser.listing(goe,goe.url,"<html>Access Denied</html>").warning().isEmpty(),"unrecognized board must fail visibly");
        check(BoardParser.listing(goe,goe.url,"<p>등록된 공고가 없습니다</p>").empty(),"explicit empty list supported");
        Source generic=Model.source("x","학교","https://example.org/list","GENERIC","서울","가람중학교");
        check(BoardParser.listing(generic,generic.url,"<a href='/view.do?nttSn=3'>역사 기간제</a><a href='javascript:alert(1)'>링크</a>").items().size()==1,"generic board safe links");
        check(Html.resolve("https://a.test/","javascript:alert(1)").isEmpty(),"reject executable URLs");
        check(Html.resolve("https://a.test/","https://u:p@a.test/x").isEmpty(),"reject embedded credentials");
        check(Html.resolve("https://a.test/a/b","../c").equals("https://a.test/c"),"relative links");
        check(Html.parse("<p>역사 &amp; 사회</p><script>영어</script>").text().equals("역사 & 사회"),"HTML entities and script exclusion");
        BoardParser.Detail bd=BoardParser.detail("https://a.test/view","<nav>역사</nav><article><h3>가람중학교 역사 기간제 채용</h3><p>접수기간 2026.11.30</p><a href='/file.pdf'>공고문.pdf</a></article>","fallback");
        check(bd.title().contains("가람중학교")&&bd.attachments().size()==1,"detail title and attachments");
        check(!bd.body().contains("nav"),"navigation removed");
        String xml="<hp:section xmlns:hp='urn:test'><hp:p><hp:t>역사 기간제교사</hp:t></hp:p></hp:section>";
        check(HanText.read(zip(xml)).contains("역사 기간제교사"),"HWPX text extraction");
        try{HanText.read(zip("<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><x>&e;</x>"));throw new AssertionError("XXE accepted");}catch(Exception expected){check(true,"reject external XML entities");}
        check(HanText.read(cfb(false)).contains("역사 기간제"),"HWP 5 mini stream extraction");
        check(HanText.read(cfb(true)).contains("역사 기간제"),"HWP 5 compressed records");
        try{HanText.read(new byte[]{0,1,2,3});throw new AssertionError();}catch(IOException e){check(true,"invalid document rejected");}
        Instant time=Instant.parse("2026-11-26T02:59:00Z");
        check(Schedule.next(time,720).toInstant().equals(Instant.parse("2026-11-26T03:00:00Z")),"next noon in Korea");
        check(Schedule.next(Instant.parse("2026-11-26T09:01:00Z"),1080).toInstant().equals(Instant.parse("2026-11-27T09:00:00Z")),"evening next day");
        check(Schedule.next(time,720).getZone().equals(Classifier.KST),"Korea schedule regardless device timezone");
        Net.Response euc=new Net.Response("역사".getBytes(Charset.forName("EUC-KR")),"text/html; charset=euc-kr","https://a.test/");
        check(euc.text().equals("역사"),"legacy Korean charset");
        collectorTests(sh);
        expandedSelectionTests();
        KeywordYearTests.run();
        articleTests();
        windowAndKeywordTests();
        datedCollectionTests();
        SchoolBoardTests.run();
        PaginationTests.run();
        PortalTests.run();
        CatalogTests.run();
        GoyangTests.run();
        InternationalTests.run();
        ListingCacheTests.run();
        NoticeAndDateTests.run();
        System.out.println("\n"+passed+" checks passed");
    }
    static void collectorTests(String list)throws Exception{
        long now=Instant.parse("2026-11-26T03:00:00Z").toEpochMilli();List<String> requests=new ArrayList<>();
        Collector collector=new Collector((url,max,ref)->{
            requests.add(url);if(url.contains("BD_selectRecDetail")||url.endsWith(".hwp"))throw new AssertionError("detail or file request");
            return new Net.Response(list.getBytes(StandardCharsets.UTF_8),"text/html; charset=utf-8",url);
        });
        Collector.Result result=collector.collect(sen,Map.of(),now);
        check(result.notices().size()==1&&result.notices().get(0).status==Status.MATCH,"title matches collected without detail access");
        check(requests.stream().noneMatch(x->x.contains("BD_selectRecDetail")),"no body or attachment network requests");
        String mixed="<a href='/work/search/recInfo/BD_selectRecDetail.do?q_rcrtSn=11'><h4 class='list_title'>가람중학교 영어 교원 채용</h4><p>학교의 역사와 전통</p></a><a href='/work/search/recInfo/BD_selectRecDetail.do?q_rcrtSn=12'><h4 class='list_title'>가람중학교 역사 채용</h4><p>접수기간 2026/11/26 ~ 2026/11/30</p><p>채용기간 2026/12/01 ~ 2027/02/28</p></a>";
        mixed=mixed.replace("</a>","<time datetime='2026-11-26'></time></a>");
        final String datedMixed=mixed;Collector filtered=new Collector((url,max,ref)->new Net.Response(datedMixed.getBytes(StandardCharsets.UTF_8),"text/html",url));
        Collector.Result selected=filtered.collect(sen,Map.of(),now);
        check(selected.listed()==2&&selected.checked()==1&&selected.notices().size()==1,"history only in card metadata does not match English title");
        check(selected.notices().get(0).deadlineLabel.startsWith("2026-11-30"),"deadline from list excludes employment end date");
        Notice old=result.notices().get(0);String renamed="<a href='/work/search/recInfo/BD_selectRecDetail.do?q_rcrtSn=123456'>가람중학교 영어 교원 채용</a>";
        Collector renamedCollector=new Collector((url,max,ref)->new Net.Response(renamed.getBytes(StandardCharsets.UTF_8),"text/html",url));
        check(renamedCollector.collect(sen,Map.of(old.id,old),now).notices().get(0).status==Status.EXCLUDED,"renaming history title to English removes previous match");
        Collector blocked=new Collector((url,max,ref)->new Net.Response("Access Denied".getBytes(StandardCharsets.UTF_8),"text/html",url));
        try{blocked.collect(sen,Map.of(),now);throw new AssertionError("blocked list silently accepted");}catch(IllegalStateException expected){check(true,"blocked list is not an empty success");}
    }
    static void expandedSelectionTests()throws Exception{
        Source school=Model.source("school","추가한 학교","https://school.example.org/list","AUTO","경기","");
        for(String title:List.of("2027학년도 교원 채용 공고","동경한국학교 교직원채용 공고","기간제교원  채용 공고","교직원\u00a0채용 공고")){
            check(Classifier.titleMatches(school,title),"school broad title accepted: "+title);
            check(!Classifier.titleMatches(sen,title),"Seoul stays history-only: "+title);
            check(!Classifier.titleMatches(goe,title),"Gyeonggi stays history-only: "+title);
        }
        check(Classifier.titleMatches(tokyo,"동경한국학교 교원 채용 공고"),"Tokyo broadened automatically");
        check(Classifier.titleMatches(school,"역사 채용"),"school history match retained");
        check(!Classifier.titleMatches(school,"학교 축제 안내"),"school unrelated title excluded");
        check(Classifier.titleMatches("교원 채용","https://school.goe.go.kr/list"),"school subdomain is not central recruitment portal");
        check(Classifier.titleMatches("교원 채용","https://school.example.org/list?next=work.sen.go.kr"),"policy uses parsed host, not substring");
        Source central=sen.copy();central.kind="GENERIC";check(!Classifier.titleMatches(central,"교원 채용"),"central URL rule independent of adapter override");
        String list="<a href='/view.do?nttSn=1'>2027학년도 교원 채용</a><a href='/view.do?nttSn=2'>2027학년도 교직원 채용</a><a href='/view.do?nttSn=3'>행사 안내</a>";
        list=list.replace("</a>","<time datetime='2026-11-26'></time></a>");
        final String datedList=list;List<String> requests=new ArrayList<>();Collector c=new Collector((url,max,ref)->{requests.add(url);return response(url,datedList);});
        Collector.Result result=c.collect(school,Map.of(),System.currentTimeMillis());
        check(result.notices().size()==2&&result.checked()==2&&requests.size()==1,"added school collects broad titles with list request only");
        Notice n=result.notices().get(0);check(n.sourceUrl.equals(school.url)&&Classifier.matches(n),"source policy snapshot carried into notice");
        n.favorite=true;n.read=true;n.body="legacy body and attachment analysis";Classifier.reclassify(n);
        check(n.status==Status.MATCH&&n.body.isEmpty()&&n.favorite&&n.read,"broad migration removes legacy body, preserves flags");
        n.body="본문을 읽었어요";n.bodyFetchedAt=1;String hash=n.contentHash();Classifier.reclassify(n);
        check(n.body.equals("본문을 읽었어요")&&n.contentHash().equals(hash),"new body cache retained without changing notification fingerprint");
        n.sourceUrl=goe.url;n.keywords=new ArrayList<>(goe.keywords);Classifier.reclassify(n);check(n.status==Status.EXCLUDED,"same broad title excluded on central source");
    }
    static Net.Response response(String url,String html){return new Net.Response(html.getBytes(StandardCharsets.UTF_8),"text/html; charset=utf-8",url);}
    static void articleTests()throws Exception{
        String base="https://school.example.org/view.do?nttSn=1";
        String html="<nav>메뉴 정보</nav><article><p>공고 본문입니다. 교원 채용 과목과 근무 조건은 본문 내용 그대로 보여줍니다.</p><table><tr><td>모집 분야</td><td>역사</td></tr></table><div class='attachments'><a href='/file.hwp'>첨부파일 공고문.hwp</a></div><a href='/file.pdf'>첨부 문서</a><script>실행 금지</script><p style='display: none'>숨김 정보</p></article><aside>다른 채용 공고</aside>";
        List<String> calls=new ArrayList<>();ArticleReader reader=new ArticleReader((url,max,ref)->{calls.add(url);return response(url,html);});
        ArticleReader.Article a=reader.read(base);
        check(a.body().contains("공고 본문")&&a.body().contains("모집 분야 | 역사"),"article body and table text readable");
        check(!a.body().contains("첨부 문서")&&!a.body().contains("공고문.hwp")&&!a.body().contains("실행 금지")&&!a.body().contains("숨김 정보")&&!a.body().contains("메뉴 정보"),"files, scripts, hidden elements and navigation removed");
        check(calls.size()==1&&calls.get(0).equals(base),"article opening makes HTML request only");
        String seoul="<div class='job-opening-modal'><h3>포털 제목</h3><div class='job-detail-area__con'><p>서울 원문 본문</p></div><div class='job-detail-area__row'>참고파일.hwp</div></div>";
        check(ArticleReader.parse(sen.url,seoul).body().equals("서울 원문 본문"),"Seoul selects body excluding reference files");
        String gyeonggi="<div class='contents goeAttach'><div class='recruit_sub_wrap'><h4>근무지역</h4><div class='recruit_cont_box3'>지도와 주소</div><h4>상세요강</h4><div class='recruit_cont_box3'><p>경기 원문 본문</p></div><h4>유의사항</h4><div class='recruit_cont_box3'>다른 영역</div></div></div>";
        check(ArticleReader.parse(goe.url,gyeonggi).body().equals("경기 원문 본문"),"Gyeonggi chooses detailed description, not map or benefits");
        check(ArticleReader.parse(tokyo.url,"<div class='context_view'>동경학교 교원 채용 공고 본문입니다.</div><script>var PostFiles=['file.hwp'];</script>").body().startsWith("동경학교"),"Tokyo own body independent of attachment arrays");
        String parent="<article><iframe src='/content/body'></iframe><iframe src='/viewer?file=a.pdf'></iframe><iframe src='https://outside.test/content'></iframe></article>";
        calls.clear();ArticleReader framed=new ArticleReader((url,max,ref)->{calls.add(url);return response(url,url.equals(base)?parent:"<article>본문 프레임입니다. 해당 프레임의 채용 내용을 앱 안에서 읽을 수 있어요.</article>");});
        check(framed.read(base).body().contains("본문 프레임")&&calls.size()==2&&calls.get(1).endsWith("/content/body"),"same-host body frame allowed; viewers and external frames not fetched");
        calls.clear();ArticleReader linked=new ArticleReader((url,max,ref)->{calls.add(url);return response(url,"<article><p>파일은 원문에서 확인</p><iframe src='/previewAjax?file=a.hwp'></iframe><a href='/download/1'>다운로드</a></article>");});
        check(linked.read(base).body().equals("파일은 원문에서 확인")&&calls.size()==1,"document viewers never requested");
        ArticleReader binary=new ArticleReader((url,max,ref)->new Net.Response(new byte[]{1,2},"application/pdf",url));
        try{binary.read(base);throw new AssertionError("binary accepted");}catch(IOException expected){check(true,"binary response rejected");}
        final int[] count={0};ArticleReader file=new ArticleReader((url,max,ref)->{count[0]++;return response(url,html);});
        try{file.read("https://school.example.org/file.hwp");throw new AssertionError("attachment requested");}catch(IOException expected){check(count[0]==0,"direct file URL blocked before transport");}
        ArticleReader failed=new ArticleReader((url,max,ref)->{throw new IOException("offline");});
        try{failed.read(base);throw new AssertionError("offline accepted");}catch(IOException expected){check(true,"article network failure reported");}
        try{ArticleReader.parse(base,"<h1>사용제약에 따른 안내</h1>");throw new AssertionError("blocked content accepted");}catch(IOException expected){check(true,"body restriction page reported");}
        ArticleReader image=new ArticleReader((url,max,ref)->response(url,"<article><img src='/poster.png'></article>"));
        check(image.read(base).body().contains("이미지"),"image-only article clearly directs to original without fetching images");
        ArticleReader unknown=new ArticleReader((url,max,ref)->response(url,"<body>홈페이지 메뉴와 로그인만 있어요.</body>"));
        try{unknown.read(base);throw new AssertionError("menu accepted as article");}catch(IOException expected){check(true,"unrecognized body fails instead of showing whole site");}
    }
    static void windowAndKeywordTests()throws Exception{
        LocalDate today=LocalDate.of(2026,11,27);
        check(!Selection.inWindow("2024-12-31",today),"publication before last year excluded");
        check(Selection.inWindow("2025/01/01",today),"last January first included");
        check(Selection.inWindow("2026.12.31",today),"current December last included");
        check(!Selection.inWindow("2027-01-01",today),"future publication year excluded");
        check(!Selection.inWindow("",today)&&!Selection.inWindow("2026-13-45",today),"unknown or invalid publication excluded");
        check(!Selection.inWindow("2025-12-31",LocalDate.of(2027,1,1))&&Selection.inWindow("2026-01-01",LocalDate.of(2027,1,1)),"year window advances at Korean new year");
        Notice n=recentNotice();n.title="2030학년도 역사 채용";n.published="2025-12-15";Classifier.reclassify(n,today);check(n.status==Status.EXCLUDED,"unrequested future academic year excluded despite valid publication");
        n.title="2026학년도 역사 채용";n.published="2024-12-31";Classifier.reclassify(n,today);check(n.status==Status.EXCLUDED,"current school-year title cannot revive old publication");
        n.published="";n.firstSeen=System.currentTimeMillis();n.deadline=Long.MAX_VALUE;Classifier.reclassify(n,today);check(n.status==Status.EXCLUDED,"scrape time and closing date cannot stand in for missing publication");
        Source custom=Model.source("custom","학교","https://school.test/list","AUTO","동경","");custom.keywords=Selection.keywords("생활 지도, 면접 공고\n생활지도\nInterview");
        check(custom.keywords.size()==3&&Classifier.titleMatches(custom,"생활지도 강사 모집")&&Classifier.titleMatches(custom,"INTERVIEW 안내"),"custom keywords use OR, ignore spaces/case and remove duplicates");
        check(!Classifier.titleMatches(custom,"역사 교원 채용"),"custom keywords replace defaults");
        Source copy=custom.copy();copy.keywords.clear();check(custom.keywords.size()==3,"editing source keyword copy does not mutate saved source");
        check(!Selection.validKeywords(Selection.keywords(" , \n")),"blank keyword set rejected rather than matching everything");
        check(custom.region.equals("기타 지역")&&Selection.region("동경").equals("기타 지역"),"legacy Tokyo region maps to other region");
        check(Selection.regionMatches("동경",Set.of("기타 지역"))&&!Selection.regionMatches("서울",Set.of("경기","기타 지역")),"region filter distinguishes Seoul Gyeonggi and other");
        Source changed=sen.copy();changed.keywords=List.of("교직원 채용");check(Classifier.titleMatches(changed,"교직원 채용 안내")&&!Classifier.titleMatches(changed,"역사 채용"),"education-board keywords are editable too");
        String html="<a href='/view.do?nttSn=1'><h4>2027학년도 역사 채용</h4><span>등록일 : 2025/12/20</span><p>접수기간 2026/01/01~2026/01/07</p></a>";
        check(BoardParser.listing(senCopyGeneric(),"https://a.test/list",html).items().get(0).published().equals("2025-12-20"),"registered date selected instead of deadline or title year");
        Html.Node tree=Html.parse("<table><tr><th>제목</th><th>작성일</th></tr><tr><td><a href='/view.do?nttSn=1'>2027 역사 채용</a></td><td>2024.12.31</td></tr></table>");
        Html.Node anchor=tree.all().stream().filter(x->x.tag.equals("a")).findFirst().orElseThrow();check(Selection.publishedIn(anchor).equals("2024-12-31"),"generic table publication column supported");
        tree=Html.parse("<div><a href='/view.do?nttSn=1'>역사 채용</a><a href='/view.do?nttSn=2'><time datetime='2026-11-26'></time>다른 글</a></div>");anchor=tree.all().stream().filter(x->x.tag.equals("a")).findFirst().orElseThrow();check(Selection.publishedIn(anchor).isEmpty(),"neighboring post date never assigned to undated item");
        tree=Html.parse("<li><a href='/view.do?nttSn=1'>역사 채용</a><time itemprop='dateModified' datetime='2026-11-27'></time></li>");anchor=tree.all().stream().filter(x->x.tag.equals("a")).findFirst().orElseThrow();check(Selection.publishedIn(anchor).isEmpty(),"modification timestamp is not a publication date");
    }
    static Source senCopyGeneric(){return Model.source("generic","학교","https://a.test/list","GENERIC","서울","");}
    static String tokyoRow(int id,String title,String date){return "[[["+id+",'"+id+"',0,0],':','/front_notice.brd/_"+id+"."+id+".hash/?shell=/index.shell:225','',''],['','',0],['"+date+"','10:00',4],[],1,'"+title+"','']";}
    static String tokyoPage(String... rows){return "<script>var Posts=[[],"+String.join(",",rows)+"];</script>";}
    static void datedCollectionTests()throws Exception{
        long now=Instant.parse("2026-11-27T03:00:00Z").toEpochMilli();Source source=senCopyGeneric();
        String dated="<a href='/view.do?nttSn=1'>2027 역사 채용<span>등록일: 2024-12-31</span></a><a href='/view.do?nttSn=2'>2027학년도 역사 채용<span>등록일: 2025-01-01</span></a>";
        Collector c=new Collector((url,max,ref)->response(url,dated));Collector.Result result=c.collect(source,Map.of(),now);
        check(result.notices().size()==1&&result.notices().get(0).published.equals("2025-01-01"),"collection excludes old publications and persists verified date");
        c=new Collector((url,max,ref)->response(url,"<a href='/view.do?nttSn=1'>2026학년도 역사 채용<p>접수기간 2026/11/28</p></a>"));result=c.collect(source,Map.of(),now);
        check(result.notices().isEmpty()&&result.partial()&&result.errors().get(0).contains("게시일"),"undated matches excluded with visible explanation");
        List<String> requests=new ArrayList<>();String first=tokyoPage(tokyoRow(100,"2027학년도 교직원 채용","2026/11/26"))+"<a href='/front_notice.brd/02..hash?shell=/index.shell:225'>2</a><a href='/front_notice.brd/066..hash?shell=/index.shell:225'>66</a>";
        String second=tokyoPage(tokyoRow(90,"교원 채용","2024/12/31"))+"<a href='/front_notice.brd/03..hash?shell=/index.shell:225'>3</a><a href='/front_notice.brd/066..hash?shell=/index.shell:225'>66</a>";
        c=new Collector((url,max,ref)->{requests.add(url);return response(url,Selection.tokyoPage(url)==1?first:second);});result=c.collect(tokyo,Map.of(),now);
        check(result.notices().size()==1&&requests.size()==2&&!result.partial(),"Tokyo stops normally at old date frontier without range-limit warning");
        check(requests.stream().noneMatch(u->u.contains("066..")),"Tokyo pagination does not jump to last archive page");
        check(BoardParser.listing(tokyo,tokyo.url,first).items().get(0).published().equals("2026-11-26"),"Tokyo publication parsed from structured row date");
        Source special=tokyo.copy();special.keywords=List.of("생활지도");String one=tokyoPage(tokyoRow(101,"2026학년도 생활지도 담당자 모집","2026/11/26"));
        c=new Collector((url,max,ref)->response(url,one));check(c.collect(special,Map.of(),now).notices().size()==1,"Tokyo custom keyword works without hardcoded recruitment prefilter");
        List<Link> mixed=List.of(new Link("오래된 고정글","https://a.test/1","","2024-01-01",true),new Link("현재글","https://a.test/2","","2026-01-01",false));
        check(!Selection.oldTokyoPage(tokyo,mixed,LocalDate.of(2026,11,27)),"old pinned notices do not stop pages containing recent posts");
        check(!Selection.oldTokyoPage(tokyo,List.of(new Link("게시일 미상","https://a.test/1")),LocalDate.of(2026,11,27)),"unknown dates never imply archive frontier reached");
    }
    static byte[] zip(String xml)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(b)){z.putNextEntry(new ZipEntry("Contents/section0.xml"));z.write(xml.getBytes(StandardCharsets.UTF_8));z.closeEntry();}return b.toByteArray();}
    static void put(byte[] b,int p,int value){ByteBuffer.wrap(b,p,4).order(ByteOrder.LITTLE_ENDIAN).putInt(value);}
    static void shortAt(byte[] b,int p,int value){b[p]=(byte)value;b[p+1]=(byte)(value>>8);}
    static void entry(byte[] b,int p,String name,int type,int start,int size){byte[] n=(name+"\0").getBytes(StandardCharsets.UTF_16LE);System.arraycopy(n,0,b,p,n.length);shortAt(b,p+64,n.length);b[p+66]=(byte)type;put(b,p+116,start);put(b,p+120,size);}
    static byte[] cfb(boolean compressed)throws IOException{
        byte[] b=new byte[2560];byte[] sig={(byte)0xd0,(byte)0xcf,0x11,(byte)0xe0,(byte)0xa1,(byte)0xb1,0x1a,(byte)0xe1};System.arraycopy(sig,0,b,0,8);
        shortAt(b,26,3);shortAt(b,28,0xfffe);shortAt(b,30,9);shortAt(b,32,6);put(b,44,1);put(b,48,1);put(b,56,4096);put(b,60,2);put(b,64,1);put(b,68,-2);put(b,72,0);
        for(int i=0;i<109;i++)put(b,76+4*i,-1);put(b,76,0);
        for(int i=0;i<128;i++){put(b,512+4*i,-1);put(b,1536+4*i,-1);}put(b,512,-3);for(int i=1;i<=3;i++)put(b,512+4*i,-2);put(b,1536,-2);put(b,1540,-2);
        byte[] text="역사 기간제".getBytes(StandardCharsets.UTF_16LE);byte[] record=new byte[4+text.length];put(record,0,67|(text.length<<20));System.arraycopy(text,0,record,4,text.length);
        if(compressed){Deflater def=new Deflater(6,true);def.setInput(record);def.finish();byte[] x=new byte[64];int n=def.deflate(x);def.end();record=Arrays.copyOf(x,n);}
        entry(b,1024,"Root Entry",5,3,128);entry(b,1152,"FileHeader",2,0,64);entry(b,1280,"Section0",2,1,record.length);put(b,2048+36,compressed?1:0);System.arraycopy(record,0,b,2112,record.length);return b;
    }
}
