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
    static void verdict(Status want,String title,String body,String hint,boolean complete){check(Classifier.classify(title,body,hint,complete).status()==want,"classify: "+title+" => "+want);}
    static Source sen=Model.defaults().get(0),goe=Model.defaults().get(1),tokyo=Model.defaults().get(2);
    public static void main(String[] args)throws Exception{
        verdict(Status.MATCH,"가람중학교 역사 기간제교사 채용","","",true);
        verdict(Status.MATCH,"가람고등학교 기간제교원(역사) 채용 공고","","",true);
        verdict(Status.MATCH,"가람고등학교 한국사 기간제교사 채용","","",true);
        verdict(Status.EXCLUDED,"가람중학교 역사 시간강사 채용","","",true);
        verdict(Status.EXCLUDED,"가람초등학교 기간제교사 채용","역사","",true);
        verdict(Status.EXCLUDED,"가람중학교 영어 기간제교사 채용","과목: 영어","",true);
        verdict(Status.MATCH,"기간제교사 채용","가람중학교\n모집 과목: 역사\n기간제교원 1명","",true);
        verdict(Status.REVIEW,"가람고등학교 기간제교사 채용","","",false);
        verdict(Status.EXCLUDED,"가람중학교 국어 기간제교사, 역사 시간강사 채용","","",true);
        verdict(Status.EXCLUDED,"가람중학교 기간제교사(국어), 시간강사(역사)","","",true);
        verdict(Status.MATCH,"가람중학교 역사 기간제 및 영어 시간강사","","",true);
        verdict(Status.REVIEW,"역사 교원 채용","기간제교사","",true);
        verdict(Status.REVIEW,"동경한국학교 역사 기간제교사 채용","","동경한국학교",true);
        verdict(Status.MATCH,"동경한국학교 역사 기간제교사 채용","중등부 교사 모집","동경한국학교",true);
        verdict(Status.REVIEW,"교원 채용","가람고등학교\n채용 분야: 역사\n기간제교사","",false);
        verdict(Status.EXCLUDED,"가람고등학교 영어 기간제교사 채용","학교의 역사와 교육철학을 이해하는 자","",true);
        verdict(Status.EXCLUDED,"가람중학교 역사교육활동 보조인력 기간제근로자 채용","","",true);
        verdict(Status.REVIEW,"가람중학교 영어 기간제교사 채용","","",false);
        Notice hashItem=new Notice();hashItem.title="가람중학교 역사 기간제";hashItem.body="역사 기간제 모집 · 조회수 10";String beforeHash=hashItem.contentHash();hashItem.body="역사 기간제 모집 · 조회수 11";check(beforeHash.equals(hashItem.contentHash()),"view count changes do not produce revised alerts");
        Classifier.Deadline d=Classifier.deadline("접수기간: 2026. 11. 26. ~ 2026. 11. 30. 15:00",LocalDate.of(2026,11,26));
        check(Instant.ofEpochMilli(d.epoch()).equals(Instant.parse("2026-11-30T06:00:00Z")),"deadline KST conversion");
        d=Classifier.deadline("접수기간 2026/12/28 ~ 1/3",LocalDate.of(2026,12,26));
        check(Instant.ofEpochMilli(d.epoch()).atZone(Classifier.KST).toLocalDate().equals(LocalDate.of(2027,1,3)),"deadline year rollover");
        check(Classifier.deadline("계약기간 2026/12/1 ~ 2027/2/28",LocalDate.now()).epoch()==0,"do not use employment end as application deadline");
        check(Classifier.deadline("접수기간 2026.13.40",LocalDate.now()).epoch()==0,"invalid date ignored");
        check(Classifier.deadline("접수기간 2026/11/30",LocalDate.now()).label().contains("원문"),"unknown closing time disclosed");
        String sh="<html><body><table><tr><td><a href=\"javascript:fnView('123456');\">가람중학교 역사 기간제교사 채용</a></td></tr></table><a href='?q_currPage=2'>2</a></body></html>";
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
        System.out.println("\n"+passed+" checks passed");
    }
    static void collectorTests(String list)throws Exception{
        long now=Instant.parse("2026-11-26T03:00:00Z").toEpochMilli();
        Collector c=new Collector((url,max,ref)->{
            String html=url.contains("BD_selectRecDetail")?"<article><h3>가람중학교 역사 기간제교사 채용</h3><p>역사 기간제교사를 모집합니다.</p><p>접수기간 2026. 11. 26. ~ 2026. 11. 30. 16:00</p></article>":list;
            return new Net.Response(html.getBytes(StandardCharsets.UTF_8),"text/html; charset=utf-8",url);
        },(response,file)->"역사 기간제교사");
        Collector.Result result=c.collect(sen,new HashMap<>(),now);
        check(result.notices().size()==1&&result.notices().get(0).status==Status.MATCH,"end-to-end fixture collection");
        Notice old=result.notices().get(0);Map<String,Notice> known=Map.of(old.id,old);
        Collector failure=new Collector((url,max,ref)->{if(url.contains("BD_selectRecDetail"))throw new IOException("timeout");return new Net.Response(list.getBytes(StandardCharsets.UTF_8),"text/html",url);},(r,f)->"");
        check(failure.collect(sen,known,now+7*3600000L).notices().isEmpty(),"transient failure preserves previous confirmed item");
        Collector.Result r=failure.collect(sen,Map.of(),now);
        check(r.notices().size()==1&&r.notices().get(0).status==Status.REVIEW,"failed detail retained for review");
        Collector blocked=new Collector((url,max,ref)->new Net.Response("Access Denied".getBytes(StandardCharsets.UTF_8),"text/html",url),(x,f)->"");
        try{blocked.collect(sen,Map.of(),now);throw new AssertionError("blocked list silently accepted");}catch(IllegalStateException expected){check(true,"blocked list is not an empty success");}
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
