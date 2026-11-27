import kr.personal.historyjobs.core.*;
import static kr.personal.historyjobs.core.Model.*;
import java.util.*;
import java.time.*;

public final class AllSchoolKeywordTests {
    static void ok(boolean value,String text){CoreTests.check(value,text);}
    static void cases(Source s,String yes,String no){ok(Classifier.matches(s,"2027학년도 "+yes,"")&&!Classifier.matches(s,"2027학년도 "+no,"역사"),"school keyword AND/OR audit: "+s.id);}
    public static void run()throws Exception{
        List<Source> all=new ArrayList<>(SchoolCatalog.defaults());all.addAll(InternationalCatalog.defaults());
        for(Source s:all)switch(s.id){
            case "seoul-fl" -> cases(s,"교원 선발","사무직원 채용");
            case "ewha-fl","hanyoung-fl" -> {cases(s,"교원 선발","사무직원 채용");ok(Classifier.titleMatches(s,"교사 채용"),"teacher alternative preserved: "+s.id);}
            case "gwacheon-fl" -> cases(s,"사무직원 채용","교사 모집");
            case "dongducheon-fl","suwon-fl","sejong-international" -> {cases(s,"교사 채용 공고","사무직원 채용 공고");ok(!Classifier.titleMatches(s,"교사 채용"),"notice AND term required: "+s.id);}
            case "anyang-fl" -> {cases(s,"교사 채용","사무직원 채용");ok(!Classifier.titleMatches(s,"교사 모집"),"hiring AND term required: "+s.id);}
            case "seongnam-fl" -> {cases(s,"교사 채용","교사 모집");ok(Classifier.titleMatches(s,"교원 선발"),"faculty phrase alternative preserved");}
            case "seoul-international" -> {cases(s,"기간제 교원 채용","사무직원 채용");ok(Classifier.titleMatches(s,"교사 모집"),"Seoul International teacher alternative preserved");}
            case "cheongshim-international" -> {cases(s,"교사 채용 공고","교사 연수");ok(Classifier.titleMatches(s,"기간제교원 모집"),"Cheongshim OR phrase survives AND branch");}
            case "daewon-international" -> {cases(s,"기간제 교원 채용","사무직원 채용");ok(Classifier.titleMatches(s,"교사 채용"),"Daewon teacher alternative preserved");}
            default -> cases(s,"기간제교사 채용","사무직원 채용");
        }
        Source tokyo=Model.defaults().get(2);cases(tokyo,"교직원 채용","사무직원 모집");
        cases(SchoolBoardTests.gsh(),"교사 채용","학교 사무직원 채용");cases(SchoolBoardTests.sshs(),"교원 선발","학교 원서 접수");
        Source anyang=all.stream().filter(s->s.id.equals("anyang-fl")).findFirst().orElseThrow();String reported="2026학년도 안양외국어고등학교 사무직원(시설관리 대체직) 공개경쟁채용(2차) 최종 합격자 공고";
        ok(!Classifier.titleMatches(anyang,reported),"reported Anyang school-space-office title cannot manufacture teacher keyword");
        String actualShape=SchoolBoardTests.GSH.replace("2027학년도 경기과학고등학교 교사 공모 최종 합격자 발표",reported);
        var result=new Collector((url,max,ref)->CoreTests.response(url,actualShape)).collect(anyang,Map.of(),LocalDate.of(2026,11,27).atStartOfDay(Classifier.KST).toInstant().toEpochMilli());
        ok(result.checked()==0&&result.notices().isEmpty(),"reported Anyang title is excluded through NTT parsing and real collection path");
        ok(!Selection.titleMatches("학교\u00a0사무직원 채용",List.of("교사 AND 채용"))&&!Selection.titleMatches("지역 사업 채용",List.of("역사 AND 채용")),"Unicode whitespace and other unrelated word fragments never create short keywords");
        ok(Selection.titleMatches("기간제 교원채용",List.of("기간제교원"))&&Selection.titleMatches("정규교원 채용공고",List.of("교원 채용"))&&Selection.titleMatches("교 사 채용",List.of("교사 AND 채용")),"intended spacing and attached teacher/faculty compounds remain supported");
        for(String clause:List.of("교사 AND 채용","교사 and 채용","교사 AND 채용 OR 교원 선발"))ok(!Selection.titleMatches(reported,List.of(clause)),"AND operator form rejects reported false match: "+clause);
        Source custom=Model.source("new-school","새 학교","https://new.test/list","GENERIC","경기","");custom.keywords=List.of("교사 AND 채용");cases(custom,"교사 채용","학교 사무직원 채용");
        Notice saved=new Notice();saved.id="saved-anyan";saved.url=anyang.url;saved.sourceUrl=anyang.url;saved.keywords=anyang.keywords;saved.title=reported;saved.published="2026-11-27";saved.favorite=true;saved.status=Status.MATCH;
        Classifier.reclassify(saved,LocalDate.of(2026,11,27));ok(saved.status==Status.EXCLUDED&&saved.favorite,"saved false match reclassifies without deleting bookmark");
        for(Source portal:Model.defaults())if(Classifier.educationBoard(portal.url))ok(Classifier.matches(portal,"사무직원 채용","역사"),"explicit central portal duty exception remains: "+portal.id);
    }
}
