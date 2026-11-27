package kr.personal.historyjobs.core;

import java.net.URI;
import java.util.*;
import static kr.personal.historyjobs.core.Model.*;

/** Goyang International's public mobile notice board (not Goyang Foreign Language's POST form). */
public final class InternationalBoards {
    private InternationalBoards(){}
    public static final String GOYANG="http://m.ggg.hs.kr/html/sub04/sub04_1-1.asp?menu_no=168";
    public static boolean goyang(String url){try{URI u=URI.create(url);return Set.of("ggg.hs.kr","www.ggg.hs.kr","m.ggg.hs.kr").contains(Objects.toString(u.getHost(),"").toLowerCase(Locale.ROOT))&&SchoolBoards.query(url).getOrDefault("menu_no","").equals("168");}catch(Exception e){return false;}}
    private static boolean css(Html.Node n,String name){return Arrays.asList(n.attr("class").split("\\s+")).contains(name);}
    public static String article(String id){return BoardParser.query(BoardParser.query(GOYANG,"board_mode","view"),"bno",id);}
    public static BoardParser.Listing listing(String page,String html){
        Html.Node root=Html.parse(html);LinkedHashMap<String,Link> posts=new LinkedHashMap<>();LinkedHashSet<String> pages=new LinkedHashSet<>();
        for(Html.Node node:root.all())if(node.tag.equals("a")&&!SchoolBoards.insideScript(node)){
            String url=Html.resolve(page,node.attr("href"));if(!goyang(url))continue;var q=SchoolBoards.query(url);String id=q.getOrDefault("bno","");
            if(q.getOrDefault("board_mode","").equals("view")&&id.matches("[0-9]{1,20}")){
                String title="",date="";for(Html.Node child:node.all()){if(child.tag.equals("h2"))title=child.text().replaceFirst("^공지\\s*","");if(css(child,"b_info"))date=Selection.date(child.text());}
                if(title.isEmpty())continue;String key=article(id);Link item=new Link(title,key,node.text(),date,css(node,"b_notice"));Link old=posts.get(key);if(old==null||old.pinned()&&!item.pinned())posts.put(key,item);
            }else if(id.isEmpty()&&q.getOrDefault("page","").matches("[1-9][0-9]{0,5}"))pages.add(BoardParser.query(GOYANG,"page",q.get("page")));
        }
        boolean empty=posts.isEmpty()&&root.text().matches("(?s).*(?:등록된.*없|검색.*없).*" );
        return new BoardParser.Listing(new ArrayList<>(posts.values()),new ArrayList<>(pages),empty,posts.isEmpty()&&!empty?"고양국제고 공지사항 목록을 해석하지 못했어요. 연결 상태를 확인해 주세요.":"");
    }
    public static boolean bodyCell(String url,Html.Node node){return goyang(url)&&node.tag.equals("li")&&css(node,"b_con");}
}
