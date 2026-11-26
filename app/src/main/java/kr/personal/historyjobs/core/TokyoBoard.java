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
            if(!u.contains(".brd/_")||!title.matches(".*(?:채용|모집|임용|採用|募集|역사|한국사|세계사).*"))continue;
            if(!school.isEmpty()&&!title.contains(school))title=school+" "+title;
            out.add(new Link(title,u));
        }
        return out;
    }
    static BoardParser.Detail detail(String base,String html,String fallback){
        List<?> posts=ArrayLiteral.posts(html);if(posts.isEmpty())return null;
        List<String> strings=new ArrayList<>();flatten(posts,strings);
        String body="";LinkedHashMap<String,Link> files=new LinkedHashMap<>();
        for(String s:strings){
            if(s.length()>body.length()&&!s.startsWith("/")&&!s.startsWith("http"))body=s;
            String u=Html.resolve(base,s);
            if(!u.isEmpty()&&u.contains(".brd/_")&&s.matches("(?i).*\\.(?:hwpx?|pdf)(?:[?#].*)?")){
                String name=s.substring(s.lastIndexOf('/')+1);try{name=java.net.URLDecoder.decode(name,"UTF-8");}catch(Exception ignored){}
                files.put(u,new Link(name,u));
            }
        }
        if(body.length()<30)return null;
        body=Html.parse(body.replace("\n","<br>")).text();
        return new BoardParser.Detail(fallback,body,new ArrayList<>(files.values()),List.of());
    }
    private static void flatten(Object v,List<String> out){if(v instanceof String)out.add((String)v);else if(v instanceof List<?>)for(Object x:(List<?>)v)flatten(x,out);}
}
