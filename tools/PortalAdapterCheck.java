import kr.personal.historyjobs.core.*;
import static kr.personal.historyjobs.core.Model.*;
import java.util.*;
import java.nio.file.*;

public final class PortalAdapterCheck {
    public static void main(String[] args)throws Exception{
        Path out=Path.of("portal-diagnostics");Files.createDirectories(out);
        List<Source> sources=new ArrayList<>(Model.defaults().subList(0,2));sources.addAll(Model.regularSources());
        for(Source source:sources){
            Thread.sleep(500);var response=Net.get(source.url,4_000_000,source.url);Files.writeString(out.resolve(source.id+".html"),response.text());
            Html.Node root=Html.parse(response.text());
            if(source.id.equals("goe-regular")&&!root.all().stream().anyMatch(n->n.tag.equals("input")&&n.attr("name").equals("srchOcptCd")&&n.attr("value").equals("D")))throw new AssertionError("Gyeonggi regular category was not selected");
            if(source.id.equals("sen-regular")&&!root.all().stream().anyMatch(n->n.tag.equals("input")&&n.attr("name").equals("q_srchJob")&&n.attr("value").startsWith("jd001,")))throw new AssertionError("Seoul regular category was not selected");
            var list=BoardParser.listing(source,response.url(),response.text());
            long withDuty=list.items().stream().filter(l->!l.duty().isEmpty()).count(),title=list.items().stream().filter(l->Classifier.titleMatches(source,l.title())).count(),dutyOnly=list.items().stream().filter(l->!Classifier.titleMatches(source,l.title())&&Classifier.dutyMatches(source.url,l.duty())).count();
            System.out.println(source.id+" listed="+list.items().size()+" withDuty="+withDuty+" titleHistory="+title+" dutyOnlyHistory="+dutyOnly+" warning="+list.warning());
            if(!list.warning().isEmpty()||(!list.empty()&&withDuty==0))throw new AssertionError(source.id+" metadata missing");
            for(Link link:list.items())if(!Classifier.titleMatches(source,link.title())&&Classifier.dutyMatches(source.url,link.duty()))System.out.println(source.id+" additionalMatch="+link.title()+" metadata="+link.duty().replace('\n',' '));
            if(source.id.equals("sen-regular")&&dutyOnly==0)throw new AssertionError("Expected current subject-only history sample");
            if(source.id.endsWith("regular")){
                List<String> calls=new ArrayList<>();var result=new Collector((url,max,ref)->{calls.add(url);return Net.get(url,max,ref);}).collect(source,Map.of(),System.currentTimeMillis());
                String summary=source.id+" pages="+calls.size()+" listed="+result.listed()+" matches="+result.checked()+" partial="+result.partial()+" errors="+result.errors();System.out.println(summary);Files.writeString(out.resolve(source.id+"-summary.txt"),summary);
                if(calls.stream().anyMatch(u->u.contains("InfoView")||u.contains("BD_selectRecDetail")))throw new AssertionError("Collection opened article details");
                if(result.partial())throw new AssertionError(source.id+" regular collection incomplete");
            }
        }
    }
}
