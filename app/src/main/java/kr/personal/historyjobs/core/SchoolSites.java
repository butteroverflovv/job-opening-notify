package kr.personal.historyjobs.core;

import java.net.URI;
import java.util.*;
import static kr.personal.historyjobs.core.Model.*;

/** Public legacy school board routes; validates board identifiers rather than executing scripts. */
public final class SchoolSites {
    private SchoolSites(){}
    private static String host(String url){try{return Objects.toString(URI.create(url).getHost(),"").toLowerCase(Locale.ROOT).replaceFirst("^www\\.","");}catch(Exception e){return "";}}
    private static String path(String url){try{return URI.create(url).getPath();}catch(Exception e){return "";}}
    public static String kind(String url){
        var q=SchoolBoards.query(url);String p=path(url);
        if(q.getOrDefault("act","").matches("board\\.(?:list|read)")||q.getOrDefault("act","").equals("news.main")&&q.containsKey("comm"))return "SCHOOL_ACT";
        if(p.endsWith("/bbs/board.php")&&q.getOrDefault("bo_table","").matches("[A-Za-z0-9_]{1,50}")||(host(url).equals("ewha-gfh.hs.kr")&&p.matches("/0201(?:/[0-9]+)?/?")||host(url).equals("dtg.hs.kr")&&p.matches("/030101(?:/[0-9]+)?/?")))return "SCHOOL_GNU";
        if(host(url).equals("gcfl.or.kr")&&p.matches("/notification/notice(?:_read)?\\.asp"))return "SCHOOL_ASP";
        return "";
    }
    private static String board(String url){var q=SchoolBoards.query(url);if(q.containsKey("bo_table"))return q.get("bo_table");return path(url).split("/").length>1?path(url).split("/")[1]:"";}
    public static boolean detail(Source source,String url){
        var q=SchoolBoards.query(url);var expected=SchoolBoards.query(source.url);
        return switch(kind(source.url)){
            case "SCHOOL_ACT" -> expected.getOrDefault("act","").equals("news.main")?
                q.getOrDefault("act","").equals("news.main")&&q.getOrDefault("comm","").equals(expected.get("comm"))&&q.getOrDefault("mcode","").equals(expected.get("mcode"))&&q.getOrDefault("sid","").matches("[0-9]{1,20}"):
                q.getOrDefault("act","").equals("board.read")&&q.getOrDefault("code","").equals(expected.get("code"))&&q.getOrDefault("id",q.getOrDefault("num","")).matches("[0-9]{1,20}");
            case "SCHOOL_GNU" -> board(url).equals(board(source.url))&&(q.getOrDefault("wr_id","").matches("[0-9]{1,20}")&&path(url).endsWith("/board.php")||path(url).matches("/"+java.util.regex.Pattern.quote(board(source.url))+"/[0-9]{1,20}/?"));
            case "SCHOOL_ASP" -> path(url).equals("/notification/notice_read.asp")&&q.getOrDefault("idx","").matches("[0-9]{1,20}");
            default -> false;
        };
    }
    public static String recordUrl(Source source,String page,Html.Node node){
        String url=Html.resolve(page,node.attr("href"));if(!detail(source,url))return "";var q=SchoolBoards.query(url);var fields=SchoolBoards.query(source.url);String base=source.url;
        if(kind(source.url).equals("SCHOOL_ASP")){base=Html.resolve(source.url,"notice_read.asp");for(String key:List.of("idx","lv1","lv3","page"))base=BoardParser.query(base,key,key.equals("lv3")?"0":key.equals("page")?"1":q.get("idx"));return base;}
        if(kind(source.url).equals("SCHOOL_ACT")){
            if(fields.getOrDefault("act","").equals("news.main"))return BoardParser.query(base,"sid",q.get("sid"));
            base=BoardParser.query(base,"act","board.read");for(String key:List.of("num","id"))if(q.containsKey(key))base=BoardParser.query(base,key,q.get(key));return base;
        }
        return url;
    }
    public static boolean aspBodyCell(String url,Html.Node node){
        return host(url).equals("gcfl.or.kr")&&path(url).equals("/notification/notice_read.asp")&&node.tag.equals("td")&&node.parent!=null&&node.parent.tag.equals("tr")&&node.parent.parent!=null&&node.parent.parent.tag.equals("table")&&node.parent.parent.attr("cellspacing").equals("10")&&node.parent.parent.attr("width").equals("675");
    }
    public static String title(Html.Node link){
        String title=link.attr("title").trim();if(!title.isEmpty()&&!title.matches("(?:새창.*|.*새 창.*|상세보기|글보기)"))return title;
        for(Html.Node n:link.all())if(Arrays.asList(n.attr("class").split("\\s+")).contains("title"))return n.text().trim();
        return link.text();
    }
    public static String published(Source source,Html.Node link){
        if(kind(source.url).equals("SCHOOL_GNU")){String date=GnuDates.listPublished(link);if(!date.isEmpty())return date;}
        String found=Selection.publishedIn(link);if(!found.isEmpty()||kind(source.url).isEmpty())return found;
        for(Html.Node scope=link;scope!=null;scope=scope.parent){
            if(scope.tag.equals("body")||scope.tag.equals("table"))break;
            if(scope.tag.equals("tr")){for(Html.Node cell:scope.children)if(cell.tag.equals("td")&&cell.text().trim().matches("[0-9]{4}[./-][0-9]{1,2}[./-][0-9]{1,2}\\.?")){String date=Selection.date(cell.text());if(!date.isEmpty())return date;}break;}
        }
        return "";
    }
}
