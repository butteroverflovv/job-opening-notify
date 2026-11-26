package kr.personal.historyjobs.core;
import java.util.*;
import static kr.personal.historyjobs.core.Model.*;
final class TokyoBoard {
    private TokyoBoard(){}
    static List<Link> links(String base,String html,String school){
        List<?> posts=ArrayLiteral.posts(html);List<Link> out=new ArrayList<>();
        for(int i=1;i<posts.size();i++){
            if(!(posts.get(i) instanceof List<?>))continue;List<?> row=(List<?>)posts.get(i);
            if(row.size()<6||!(row.get(0) instanceof List<?>))continue;
            List<?> head=(List<?>)row.get(0);if(head.size()<3)continue;
            String u=Html.resolve(base,String.valueOf(head.get(2))),title=String.valueOf(row.get(5));
            if(!u.contains(".brd/_")||title.trim().isEmpty())continue;
            if(!school.isEmpty()&&!title.contains(school))title=school+" "+title;
            String published="";if(row.get(2) instanceof List<?> date&&!date.isEmpty())published=Selection.date(String.valueOf(date.get(0)));
            out.add(new Link(title,u,"",published));
        }
        return out;
    }
    static BoardParser.Detail detail(String base,String html,String fallback){
        Html.Node root=Html.parse(html);String body="",title=fallback;
        for(Html.Node n:root.all()){
            if(n.attr("class").matches(".*(?:^|\\s)context_view(?:\\s|$).*"))body=n.text();
            if(n.tag.equals("th")&&n.text().length()<300&&n.text().matches(".*(?:채용|모집|임용|採用|募集).*")){
                title=n.text();if(!title.contains("동경한국학교"))title="동경한국학교 "+title;
            }
        }
        if(body.length()<30)return null;
        LinkedHashMap<String,Link> files=new LinkedHashMap<>();
        for(Object value:ArrayLiteral.named(html,"PostFiles")){
            if(!(value instanceof List<?>))continue;List<?> row=(List<?>)value;if(row.size()<3)continue;
            String u=Html.resolve(base,String.valueOf(row.get(2))),name=String.valueOf(row.get(0));
            if(!u.isEmpty()&&name.matches("(?i).*\\.(?:hwpx?|pdf)$"))files.put(u,new Link(name,u));
        }
        return new BoardParser.Detail(title,body,new ArrayList<>(files.values()),List.of());
    }
}
