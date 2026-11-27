package kr.personal.historyjobs.core;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import static kr.personal.historyjobs.core.Model.*;

/** Public Goyang notice form. Only list and view requests are allowed. */
public final class GoyangBoard {
    private GoyangBoard(){}
    public static boolean supports(String url){
        try{URI u=URI.create(url);String host=Objects.toString(u.getHost(),"").toLowerCase(Locale.ROOT);var q=SchoolBoards.query(url);
            return Set.of("http","https").contains(u.getScheme())&&Set.of("www.gyfl.hs.kr","gyfl.hs.kr").contains(host)&&u.getUserInfo()==null&&u.getPath().equals("/main/main.php")&&q.getOrDefault("categoryid","12").equals("12")&&q.getOrDefault("menuid","03").equals("03")&&q.getOrDefault("groupid","00").equals("00");
        }catch(Exception e){return false;}
    }
    public static String endpoint(String url){if(!supports(url))throw new IllegalArgumentException("고양외고 공지사항 주소가 아니에요");URI u=URI.create(url);return u.getScheme()+"://"+u.getRawAuthority()+"/main/main.php";}
    public static Map<String,String> fields(String url){
        endpoint(url);var q=SchoolBoards.query(url);String board=q.getOrDefault("board","list"),page=q.getOrDefault("page","1");
        if(!Set.of("list","view").contains(board)||!q.getOrDefault("mode","").isEmpty()||!page.matches("[1-9][0-9]{0,5}"))throw new IllegalArgumentException("지원하지 않는 게시판 요청이에요");
        Map<String,String> out=new LinkedHashMap<>();out.put("categoryid","12");out.put("menuid","03");out.put("groupid","00");out.put("partid","00");out.put("boardtable","alim");out.put("boardtype","free");out.put("boardkind","");out.put("mode","");out.put("sch_st","");out.put("sch_partid","");out.put("sch_key","sch_title");out.put("sch_nm","");out.put("board",board);out.put("page",page);
        if(board.equals("view")){String id=q.getOrDefault("no","");if(!id.matches("[0-9]{1,20}"))throw new IllegalArgumentException("공고 번호 형식 오류");out.put("no",id);}
        return out;
    }
    private static String encode(String value){try{return URLEncoder.encode(value,"UTF-8");}catch(java.io.UnsupportedEncodingException e){throw new IllegalStateException(e);}}
    public static byte[] body(String url){StringJoiner encoded=new StringJoiner("&");for(var field:fields(url).entrySet())encoded.add(encode(field.getKey())+"="+encode(field.getValue()));return encoded.toString().getBytes(StandardCharsets.UTF_8);}
    public static String page(String source,int page){String url=endpoint(source);for(var e:fields(source).entrySet())if(!e.getKey().equals("no"))url=BoardParser.query(url,e.getKey(),e.getKey().equals("board")?"list":e.getKey().equals("page")?String.valueOf(page):e.getValue());return url;}
    public static String article(String source,String id){String url=page(source,1);url=BoardParser.query(url,"board","view");return BoardParser.query(url,"no",id);}
    private static boolean css(Html.Node node,String name){return Arrays.asList(node.attr("class").split("\\s+")).contains(name);}
    private static boolean inList(Html.Node node){for(Html.Node n=node;n!=null;n=n.parent)if(n.tag.equals("table"))return css(n,"noticelist");return false;}
    private static final Pattern VIEW=Pattern.compile("^\\s*(?:return\\s+)?boardView(?:_m)?\\(\\s*['\"]?([0-9]{1,20})['\"]?\\s*\\)\\s*;?\\s*(?:return\\s+false;?)?\\s*$");
    private static final Pattern PAGE=Pattern.compile("^\\s*(?:return\\s+)?boardPage(?:_m)?\\(\\s*['\"]?([1-9][0-9]{0,5})['\"]?\\s*\\)\\s*;?\\s*(?:return\\s+false;?)?\\s*$");
    public static BoardParser.Listing listing(Source source,String html){
        Html.Node root=Html.parse(html);LinkedHashMap<String,Link> items=new LinkedHashMap<>();LinkedHashSet<String> pages=new LinkedHashSet<>();boolean table=false;
        for(Html.Node node:root.all()){
            if(node.tag.equals("table")&&css(node,"noticelist"))table=true;
            if(!node.tag.equals("a")||SchoolBoards.insideScript(node))continue;
            Matcher view=VIEW.matcher(node.attr("onclick"));
            if(view.matches()&&inList(node)){String title=node.text().trim();if(title.isEmpty())continue;String url=article(source.url,view.group(1));Link item=new Link(title,url,node.text(),Selection.publishedIn(node),Selection.pinned(node));Link old=items.get(url);if(old==null||old.pinned()&&!item.pinned())items.put(url,item);}
            Matcher move=PAGE.matcher(node.attr("onclick"));if(move.matches())pages.add(page(source.url,Integer.parseInt(move.group(1))));
        }
        boolean empty=table&&items.isEmpty()&&root.text().matches("(?s).*(?:등록된.*없|게시물.*없|검색.*없).*" );
        return new BoardParser.Listing(new ArrayList<>(items.values()),new ArrayList<>(pages),empty,items.isEmpty()&&!empty?"고양외고 공지사항 목록을 해석하지 못했어요. 연결 상태를 확인해 주세요.":"");
    }
    public static boolean bodyCell(String url,Html.Node node){if(!supports(url)||!node.tag.equals("td")||!css(node,"content"))return false;for(Html.Node n=node.parent;n!=null;n=n.parent)if(n.tag.equals("table"))return css(n,"noticeview");return false;}
}
