import kr.personal.historyjobs.core.*;
import static kr.personal.historyjobs.core.Model.*;
import java.time.*;
import java.util.*;

public final class PortalTests {
    static void ok(boolean value,String label){CoreTests.check(value,label);}
    static String senCard(int id,String title,String field,String subject){return "<li class='flex_cont'><a href='/work/search/recInfo/BD_selectRecDetail.do?q_rcrtSn="+id+"'><div class='employ_cont'><span class='s_title'>선덕중학교 | 등록일 : 2026-11-27</span><h4 class='list_title'>"+title+"</h4><div class='list_tag'><p class='tag_title'>과목(담당업무)</p><span class='border-r'>"+subject+"</span><p class='tag_title'>채용인원</p><span>2</span></div><div class='list_tag'><p class='tag_title'>직무분야</p><span class='text-back-gray'>"+field+"</span></div></div></a></li>";}
    static String goeCard(int id,String title,String field){return "<li><a href=\"javascript:goView('"+id+"')\"><div class='cont_txt'><div class='cont_top'>가람중학교 등록일 : 2026/11/27</div><p class='cont_tit'>"+title+"</p><div class='cont_btm'><p><em class='btm_tit'>직무분야</em>"+field+"</p></div></div></a></li>";}
    public static void run()throws Exception{
        Source sen=Model.regularSources().get(0),goe=Model.regularSources().get(1);
        String html="<nav>역사</nav><ul>"+senCard(100,"신규교사 채용 공고","정규교원 > 보통교과 > 도덕사회과","역사(1명), 체육(1명)")+senCard(101,"수학 신규교사 채용","정규교원 > 수학과","수학")+"</ul>";
        var parsed=BoardParser.listing(sen,sen.url,html);
        ok(parsed.items().size()==2&&parsed.items().get(0).duty().contains("역사(1명)")&&parsed.items().get(0).duty().contains("도덕사회과"),"Seoul explicit subject and duty metadata both parsed");
        ok(!parsed.items().get(1).duty().contains("역사"),"neighbor card metadata and history navigation do not leak");
        ok(Classifier.matches(sen,parsed.items().get(0).title(),parsed.items().get(0).duty())&&!Classifier.titleMatches(sen,parsed.items().get(0).title()),"subject-only history accepted without changing original title");
        String ghtml="<ul>"+goeCard(200,"사립학교 신규교사 공개채용","국어, 역사, 체육")+goeCard(201,"영어 교사 채용","영어")+"</ul>";
        var gp=BoardParser.listing(goe,goe.url,ghtml);
        ok(gp.items().get(0).duty().contains("국어, 역사, 체육")&&!gp.items().get(1).duty().contains("역사"),"Gyeonggi duty label with following text node parsed per card");
        long now=Instant.parse("2026-11-27T03:00:00Z").toEpochMilli();List<String> requests=new ArrayList<>();
        Collector c=new Collector((url,max,ref)->{requests.add(url);return CoreTests.response(url,html);});
        var result=c.collect(sen,Map.of(),now);
        ok(result.checked()==1&&result.notices().size()==1&&requests.equals(List.of(sen.url)),"regular portal duty matching needs list requests only");
        Notice n=result.notices().get(0);ok(n.duty.contains("역사")&&Selection.eligible(n,LocalDate.of(2026,11,27))&&n.reason.contains("직무분야"),"saved selection and notification gate use same subject metadata");
        Source temp=Model.defaults().get(0);ok(Classifier.matches(temp,"교원 채용","직무분야 · 역사"),"Seoul existing temporary source also gains duty selection");
        ok(Classifier.matches(Model.defaults().get(1),"교원 채용","직무분야 · 역사"),"Gyeonggi existing temporary source also gains duty selection");
        Source school=Model.source("school","학교","https://school.test/list","GENERIC","서울","");
        ok(!Classifier.matches(school,"2026학년도 영어 채용","역사")&&BoardParser.listing(school,school.url,"<a href='/view.do?nttSn=10'><h4>영어 채용</h4><p><em>직무분야</em>역사</p></a>").items().get(0).duty().isEmpty(),"custom school title rules are unchanged");
        String unlabeled=senCard(300,"영어 채용","영어","영어").replace("</a>","<p>학교의 역사와 전통</p></a>");
        var unrelated=BoardParser.listing(sen,sen.url,unlabeled).items().get(0);
        ok(!Classifier.matches(sen,unrelated.title(),unrelated.duty()),"unlabeled list prose containing history is excluded");
        ok(SchoolBoards.query(goe.url).get("srchOcptCd").equals("D")&&!goe.url.equals(Model.defaults().get(1).url),"Gyeonggi regular registration has distinct D category query");
        ok(SchoolBoards.query(sen.url).get("q_srchJob").startsWith("jd001,")&&SchoolBoards.query(sen.url).get("jobDuty").equals("jd001001002"),"Seoul regular request preserves full selected social-studies category");
        ok(Selection.pageNumber(goe,goe.url)==1&&Selection.pageNumber(goe,BoardParser.query(goe.url,"currPage","2"))==2,"Gyeonggi pageIndex is row count, currPage is actual page number");
        for(Source source:List.of(sen,goe)){
            String next=BoardParser.query(source.url,SchoolBoards.pageKey(source),"2");
            ok(Model.recruitmentCategory(source.url).equals(Model.recruitmentCategory(next)),"regular category retained when paging: "+source.id);
        }
        List<Source> sources=new ArrayList<>(List.of(Model.defaults().get(0),Model.defaults().get(1)));Model.addRegularSources(sources);Model.addRegularSources(sources);
        ok(sources.size()==4,"old catalog adds both regular boards exactly once");
        Source custom=sen.copy();custom.id="my-regular";custom.name="내 정규채용";custom.enabled=false;custom.keywords=List.of("역사 채용");sources=new ArrayList<>(List.of(custom));Model.addRegularSources(sources);
        ok(sources.size()==2&&sources.get(0)==custom&&!custom.enabled&&custom.keywords.equals(List.of("역사 채용")),"migration recognizes previously added category and preserves settings");
        ok(Model.defaults().size()==25&&Model.defaults().get(2).id.equals("tokyo"),"new installs retain existing defaults and append regular boards");
    }
}
