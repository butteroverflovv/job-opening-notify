import kr.personal.historyjobs.core.*;
import static kr.personal.historyjobs.core.Model.*;
import java.time.*;
import java.util.*;

public final class IdentityTests {
    static void ok(boolean value,String message){CoreTests.check(value,message);}
    static Source tokyo=Model.defaults().get(2);
    static String a="http://www.tokos.ed.jp/front_notice.brd/_656.405.24baa395/?shell=/index.shell:225";
    static String b="https://tokos.ed.jp/front_notice.brd/_656.405.24e4a395/?shell=/index.shell:225&page=2#post";
    static void same(Source source,String a,String b,String label){ok(Collector.id(source,a).equals(Collector.id(source,b)),label);}
    static Notice notice(Source source,String url,String title,long seen){Notice n=new Notice();n.sourceId=source.id;n.sourceUrl=source.url;n.url=url;n.id=Model.hash(source.id+"|"+url);n.title=title;n.published="2026-11-27";n.firstSeen=seen;n.lastSeen=seen;n.keywords=source.keywords;return n;}
    public static void run()throws Exception{
        same(tokyo,a,b,"Tokyo actual response token variants share one article identity");
        same(tokyo,a,"http://www.tokos.ed.jp/front_notice.brd/_656.900.changed/?shell=/index.shell:225","Tokyo display position does not replace persistent article number");
        same(tokyo,a,"http://www.tokos.ed.jp/front_notice.brd?mode=view&num=656&shell=/index.shell:225","Tokyo numbered query and path routes identify the same post");
        ok(!Collector.id(tokyo,a).equals(Collector.id(tokyo,a.replace("_656.","_657."))),"distinct Tokyo post numbers remain distinct even with identical titles");
        Source ntt=SchoolBoardTests.gsh();String ntt1="https://gs-h.goesw.kr/gs-h/na/ntt/selectNttInfo.do?bbsId=1088&nttSn=12&currPage=1",ntt2=ntt1.replace("currPage=1","currPage=7&mi=900");same(ntt,ntt1,ntt2,"NTT page and menu aliases share record identity");
        ok(!Collector.id(ntt,ntt1).equals(Collector.id(ntt,ntt1.replace("bbsId=1088","bbsId=1089"))),"NTT board namespace is retained");
        Source sen=SchoolBoardTests.sshs();same(sen,sen.url+"?nttId=100", "https://sshs.sen.hs.kr/dggb/module/board/selectBoardSenDetailAjax.do?bbsId=BBSMSTR_1&nttId=100&pageIndex=5","Seoul school public and Ajax routes share record identity");
        Source portal=Model.defaults().get(0);same(portal,"https://work.sen.go.kr/work/search/recInfo/BD_selectRecDetail.do?q_rcrtSn=12&q_currPage=1","https://work.sen.go.kr/work/search/recInfo/BD_selectRecDetail.do?q_tabId=jobTab&q_currPage=7&q_rcrtSn=12","Seoul portal preserves recruitment ID across query variations");
        Source goe=Model.defaults().get(1);same(goe,"https://www.goe.go.kr/recruit/ad/func/pb/hnfpPbancInfoView.do?pbancSn=12&currPage=1","http://goe.go.kr/recruit/ad/func/pb/hnfpPbancInfoView.do?currPage=9&pbancSn=12","Gyeonggi portal uses recruitment number");
        Source dtg=InternationalCatalog.defaults().stream().filter(s->s.id.equals("dongtan-international")).findFirst().orElseThrow();same(dtg,"https://www.dtg.hs.kr/030101/12","https://www.dtg.hs.kr/bbs/board.php?bo_table=030101&wr_id=12&page=3&sfl=wr_subject&stx=teacher","GNU pretty and query routes share table plus post number");
        Source suwon=SchoolCatalog.defaults().stream().filter(s->s.id.equals("suwon-fl")).findFirst().orElseThrow();same(suwon,suwon.url+"&wr_id=12&page=1",suwon.url+"&page=9&wr_id=12#view","GNU legacy directory paging aliases share identity");
        Source act=SchoolCatalog.defaults().stream().filter(s->s.id.equals("gimpo-fl")).findFirst().orElseThrow();same(act,act.url.replace("board.list","board.read")+"&id=12&page=1",act.url.replace("board.list","board.read")+"&num=12&page=2","ACT id and num aliases use the same board record");
        Source gyfl=SchoolCatalog.defaults().stream().filter(s->s.id.equals("goyang-fl")).findFirst().orElseThrow();same(gyfl,GoyangBoard.article(gyfl.url,"6807"),GoyangBoard.article(gyfl.url,"6807")+"&page=8","Goyang POST view identity ignores list position");
        Source ggg=InternationalCatalog.defaults().stream().filter(s->s.id.equals("goyang-international")).findFirst().orElseThrow();same(ggg,InternationalBoards.article("12"),"https://ggg.hs.kr/?menu_no=168&bno=12&board_mode=view&page=8","Goyang International mobile and desktop routes share menu record");
        Source custom=Model.source("custom","학교","https://a.test/list","GENERIC","서울","");same(custom,"https://a.test/view.do?id=12&page=1&utm_source=a","http://www.a.test/view.do?page=8&id=12#top","generic identified articles normalize presentation and tracking");
        ok(!PostIdentity.key(custom.url,"https://a.test/list?page=1").equals(PostIdentity.key(custom.url,"https://a.test/list?page=2")),"unknown URLs retain potentially meaningful page values");
        String title="2027학년도 교원 채용 공고";Notice read=notice(tokyo,a,"동경한국학교 "+title,100);read.read=true;read.body="저장된 본문";read.bodyFetchedAt=300;read.lastSeen=200;
        Notice fresh=notice(tokyo,b,title,150);fresh.favorite=true;fresh.lastSeen=400;String oldA=read.id,oldB=fresh.id;
        var merged=NoticeDeduplicator.consolidate(List.of(read,fresh));Notice joined=merged.notices().values().iterator().next();
        ok(merged.notices().size()==1&&joined.id.equals(Collector.id(tokyo,a))&&merged.aliases().get(oldA).equals(joined.id)&&merged.aliases().get(oldB).equals(joined.id),"stored URL aliases migrate into a single canonical ID");
        ok(joined.read&&joined.favorite&&joined.firstSeen==100&&joined.lastSeen==400,"read marker, bookmark and earliest discovery survive consolidation");
        ok(joined.body.equals("저장된 본문")&&joined.bodyFetchedAt==300&&joined.url.equals(b),"stored body survives while latest working URL is kept");
        Notice revisit=notice(tokyo,a,title,500);ok(joined.contentHash().equals(revisit.contentHash()),"URL token and cosmetic Tokyo school prefix cannot cause revised alerts");
        Notice old=notice(tokyo,a,title,100);old.read=true;old.body="old";old.bodyFetchedAt=200;Notice revised=notice(tokyo,b,title+" (수정)",400);
        var revision=NoticeDeduplicator.consolidate(List.of(old,revised)).notices().values().iterator().next();ok(!revision.read&&revision.bodyFetchedAt==0,"a real changed title retains unread state and does not reuse old body");
        Notice other=notice(tokyo,a.replace("_656.","_657."),title,400);ok(NoticeDeduplicator.consolidate(List.of(revisit,other)).notices().size()==2,"same title and date never merge genuinely separate article numbers");
        Notice secondSubscription=notice(tokyo,a,title,400);secondSubscription.sourceId="other-board";ok(NoticeDeduplicator.consolidate(List.of(revisit,secondSubscription)).notices().size()==2,"independent board subscriptions keep their own filter membership");
        Notice missing1=notice(tokyo,"",title,400),missing2=notice(tokyo,"",title,400);missing1.id=Model.hash("missing1");missing2.id=Model.hash("missing2");ok(NoticeDeduplicator.consolidate(List.of(missing1,missing2)).notices().size()==2,"missing article identity never destroys unrelated backup records");
        ok(!Classifier.titleMatches(tokyo,"2027학년도 초등 교원 채용")&&!Classifier.titleMatches(tokyo,"2027학년도 초등부 교직원 채용"),"Tokyo elementary titles excluded before collection");
        ok(Classifier.titleMatches(tokyo,"2027학년도 중고등부 교원 채용")&&Classifier.titleMatches(custom,"2027학년도 초등학교 역사 채용"),"elementary exclusion is limited to Tokyo and keeps secondary posts");
        Notice elementary=notice(tokyo,a.replace("_656.","_658."),"2027학년도 초등 교원 채용",500);elementary.favorite=true;Classifier.reclassify(elementary,LocalDate.of(2026,11,27));ok(elementary.status==Status.EXCLUDED&&elementary.reason.contains("초등")&&elementary.favorite,"saved Tokyo elementary notice reclassifies without deleting bookmark");
        String p1=CoreTests.tokyoPage(CoreTests.tokyoRow(656,title,"2026/11/27"),CoreTests.tokyoRow(655,"2027학년도 초등 교원 채용","2026/11/27"))+"<a href='/front_notice.brd/02..hash?shell=/index.shell:225'>2</a>";
        String p2=CoreTests.tokyoPage(CoreTests.tokyoRow(656,title,"2026/11/27").replace(".hash/",".different/"),CoreTests.tokyoRow(654,"2027학년도 중등 교원 채용","2026/11/27"));
        var result=new Collector((url,max,ref)->CoreTests.response(url,Selection.tokyoPage(url)==1?p1:p2)).collect(tokyo,Map.of(),LocalDate.of(2026,11,27).atStartOfDay(Classifier.KST).toInstant().toEpochMilli());
        ok(result.listed()==3&&result.notices().size()==2&&result.checked()==2&&!result.partial(),"cross-page token duplicates collapse while elementary results are excluded");
        ok(ListingCache.fingerprint(new Link(title,a,"","2026-11-27")).equals(ListingCache.fingerprint(new Link(title,b,"","2026-11-27"))),"cache fingerprints also ignore variable article URL tokens");
        if(System.getProperty("tokyo.list")!=null){var first=BoardParser.listing(tokyo,tokyo.url,java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("tokyo.list"))));var second=BoardParser.listing(tokyo,tokyo.url,java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("tokyo.detail"))));List<Link> both=new ArrayList<>(first.items());both.addAll(second.items());ok(PostIdentity.unique(tokyo,both).size()<both.size(),"actual Tokyo list and detail response aliases collapse by record ID");}
    }
}
