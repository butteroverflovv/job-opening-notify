import kr.personal.historyjobs.core.*;
import static kr.personal.historyjobs.core.Model.*;
import java.time.*;
import java.util.*;

public final class KeywordYearTests {
    static void ok(boolean value,String label){CoreTests.check(value,label);}
    public static void run()throws Exception{
        LocalDate today=LocalDate.of(2026,11,27);
        List<String> words=Selection.keywords("교원 선발 OR 교원 임용 or 기간제,교원선발\n교사");
        ok(words.equals(List.of("교원 선발","교원 임용","기간제","교사")),"OR mixes with existing separators and deduplicates phrases");
        ok(Selection.titleMatches("2027학년도 교원 임용후보자 공개선발 안내",words),"second OR alternative accepts Seoul appointment title");
        ok(!Selection.titleMatches("2027학년도 입학전형 공고",words),"OR alternatives exclude unrelated titles");
        ok(Selection.keywords("History,Coordinator,교원OR선발").size()==3,"embedded or letters are literal, not separators");
        ok(!Selection.validKeywords(Selection.keywords("or OR ,\n")),"operators alone cannot match all titles");
        ok(Selection.titleMatches("기간제 채용",List.of("교원 선발 OR 기간제")),"previously stored OR input is recognized");
        Source school=Model.source("school","학교","https://sshs.sen.hs.kr/75052/subMenu.do","AUTO","서울","");school.keywords=words;
        for(String title:List.of("2026학년도 교사 채용","2027 학년도 교원 임용","2026학년도 및 2027학년도 교원 선발"))
            ok(Selection.academicYearMatches(title,school.url,today),"current/next school-year title accepted: "+title);
        for(String title:List.of("2025학년도 교원 선발","2028학년도 교원 선발","2026년 교원 선발","교원 선발","12026학년도 교원 선발"))
            ok(!Selection.academicYearMatches(title,school.url,today),"missing or unrelated academic year excluded: "+title);
        ok(Selection.academicYearMatches("역사 교사 채용",Model.defaults().get(0).url,today)&&Selection.academicYearMatches("2025학년도 역사 교사 채용",Model.defaults().get(1).url,today),"both education recruitment portals exempt from school-year condition");
        ok(!Selection.academicYearMatches("교사 채용","https://school.goe.go.kr/list?next=work.sen.go.kr",today),"school-year exemption uses actual recruitment portal URL");
        ok(!Selection.academicYearMatches("2026학년도 교원 선발",school.url,LocalDate.of(2027,1,1))&&Selection.academicYearMatches("2028학년도 교원 선발",school.url,LocalDate.of(2027,1,1)),"current and next academic years advance at Korean new year");
        Notice n=new Notice();n.sourceUrl=school.url;n.url=school.url;n.title="2025학년도 교원 선발";n.published="2025-11-27";n.keywords=words;n.favorite=true;n.read=true;n.status=Status.MATCH;
        Classifier.reclassify(n,today);
        ok(n.status==Status.EXCLUDED&&!Selection.eligible(n,today)&&n.favorite&&n.read&&n.reason.contains("학년도"),"saved old school notice reclassified without losing user flags");
        n.title="2027학년도 교원 임용";Classifier.reclassify(n,today);ok(n.status==Status.MATCH,"valid school year and OR alternatives both required");
        String html="<table><tr><th>제목</th><th>등록일</th></tr>";
        String[] titles={"2025학년도 교사 채용","교사 채용","2026학년도 교원 선발","2027학년도 기간제 채용","2028학년도 교원 임용"};
        for(int i=0;i<titles.length;i++)html+="<tr><td><a href='/view.do?nttSn="+(i+1)+"'>"+titles[i]+"</a></td><td>2026-11-27</td></tr>";
        html+="</table>";final String list=html;Source generic=school.copy();generic.url="https://school.test/list";
        List<String> calls=new ArrayList<>();Collector c=new Collector((url,max,ref)->{calls.add(url);return CoreTests.response(url,list);});
        long now=today.atStartOfDay(Classifier.KST).toInstant().toEpochMilli();
        Collector.Result result=c.collect(generic,Map.of(),now);
        ok(result.listed()==5&&result.checked()==2&&result.notices().size()==2&&!result.partial()&&calls.size()==1,"school collection excludes old/missing/future academic year using list titles only");
        Notice old=new Notice();old.id=Collector.id(generic,"https://school.test/view.do?nttSn=1");old.published="2026-11-27";
        result=c.collect(generic,Map.of(old.id,old),now);
        ok(result.notices().stream().anyMatch(x->x.id.equals(old.id)&&x.status==Status.EXCLUDED),"known old school item receives exclusion update");
    }
}
