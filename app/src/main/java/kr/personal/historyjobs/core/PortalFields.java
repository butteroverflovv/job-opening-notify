package kr.personal.historyjobs.core;

import java.util.*;
import static kr.personal.historyjobs.core.Model.*;

/** Only labeled metadata inside this list record; never page navigation or article prose. */
public final class PortalFields {
    private PortalFields(){}
    public static String duty(Source source,Html.Node link){
        if(!Classifier.educationBoard(source.url))return "";
        Html.Node scope=link;
        while(scope!=null){
            if(scope.text().length()>5000)return "";
            LinkedHashSet<String> values=new LinkedHashSet<>();
            for(Html.Node label:scope.all())if(!SchoolBoards.insideScript(label)&&Set.of("직무분야","과목(담당업무)","과목","담당업무").contains(Selection.compact(label.text()))&&label.parent!=null){
                boolean after=false;
                for(Html.Node sibling:label.parent.children){
                    if(sibling==label){after=true;continue;}
                    if(!after)continue;String value=sibling.text().trim();
                    if(value.isEmpty())continue;
                    if(value.matches("^(?:접수기간|채용기간|채용인원|모집정보|보수/임금|직무분야|과목).*"))break;
                    if(value.length()<=1000)values.add(label.text()+" · "+value);break;
                }
            }
            if(!values.isEmpty())return String.join("\n",values);
            if(scope.tag.equals("li")||scope.tag.equals("tr")||scope.tag.equals("article"))break;
            Html.Node parent=scope.parent;if(parent==null||parent.tag.equals("root")||parent.tag.equals("body")||parent.tag.equals("ul")||parent.tag.equals("table"))break;
            long other=parent.all().stream().filter(n->n.tag.equals("a")&&n!=link&&!n.attr("href").isEmpty()&&!n.attr("href").equals(link.attr("href"))&&!BoardParser.isAttachment(n.attr("href"))).count();
            if(other>0)break;scope=parent;
        }
        return "";
    }
}
