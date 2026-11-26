package kr.personal.historyjobs.core;

import java.time.*;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;
import static kr.personal.historyjobs.core.Model.*;

/** Shared date, keyword and region rules for collection, saved lists and notifications. */
public final class Selection {
    private Selection(){}
    public static List<String> defaults(String url){
        if(url==null||url.isEmpty()||Classifier.educationBoard(url))return new ArrayList<>(List.of("역사"));
        try{String host=java.net.URI.create(url).getHost();if(host!=null&&(host.equals("tokos.ed.jp")||host.endsWith(".tokos.ed.jp")))return new ArrayList<>(List.of("교원 채용","교직원 채용"));}catch(Exception ignored){}
        return new ArrayList<>(List.of("역사","교원 채용","교직원 채용"));
    }
    public static String compact(String text){return Normalizer.normalize(Objects.toString(text,""),Normalizer.Form.NFC).replaceAll("[\\s\\p{Z}\\u200B]+","").toLowerCase(Locale.ROOT);}
    public static List<String> keywords(String input){
        LinkedHashMap<String,String> unique=new LinkedHashMap<>();for(String part:input.split("[,，\\n\\r]+")){String t=part.trim();String key=compact(t);if(!key.isEmpty())unique.putIfAbsent(key,t);}return new ArrayList<>(unique.values());
    }
    public static boolean titleMatches(String title,List<String> words){if(title==null||words==null)return false;String t=compact(title);for(String k:words){String w=compact(k);if(!w.isEmpty()&&t.contains(w))return true;}return false;}
    public static boolean validKeywords(List<String> words){return words!=null&&!words.isEmpty()&&words.size()<=30&&words.stream().allMatch(w->!compact(w).isEmpty()&&w.length()<=100);}
    public static String region(String raw){if("서울".equals(raw))return "서울";if("경기".equals(raw))return "경기";return "기타 지역";}
    public static boolean regionMatches(String notice,Set<String> selected){return selected.contains(region(notice));}
    private static final Pattern DATE=Pattern.compile("(?<![0-9])((?:19|20)[0-9]{2})\\s*[./년-]\\s*([0-9]{1,2})\\s*[./월-]\\s*([0-9]{1,2})(?:일|\\.)?(?![0-9])");
    public static String date(String raw){Matcher m=DATE.matcher(Objects.toString(raw,""));if(m.find())try{return LocalDate.of(Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2)),Integer.parseInt(m.group(3))).toString();}catch(DateTimeException ignored){}return "";}
    public static LocalDate published(String raw){String d=date(raw);return d.isEmpty()?null:LocalDate.parse(d);}
    public static LocalDate start(LocalDate today){return LocalDate.of(today.getYear()-1,1,1);}
    public static boolean inWindow(String raw,LocalDate today){LocalDate d=published(raw);return d!=null&&!d.isBefore(start(today))&&d.getYear()<=today.getYear();}
    public static boolean eligible(Notice n,LocalDate today){return Classifier.matches(n)&&inWindow(n.published,today);}
    /** Only metadata labels, date fields, and table columns; never a title's school year or employment dates. */
    public static String publishedIn(Html.Node link){
        Html.Node scope=link;
        for(int depth=0;scope!=null&&depth<5;depth++,scope=scope.parent){
            if(scope.tag.equals("table")||scope.tag.equals("body")||scope.tag.equals("root"))break;
            if(scope.text().length()>5000)break;
            if(scope!=link){long anchors=scope.all().stream().filter(n->n.tag.equals("a")&&!n.attr("href").isEmpty()&&!n.attr("href").startsWith("#")&&!BoardParser.isAttachment(n.attr("href"))).count();if(anchors>1)break;}
            Matcher labeled=Pattern.compile("(?:등록일(?:자)?|작성일(?:자)?|게시일(?:자)?|게시날짜)\\s*[:：]?\\s*((?:19|20)[0-9]{2}\\s*[./년-]\\s*[0-9]{1,2}\\s*[./월-]\\s*[0-9]{1,2})").matcher(scope.text());
            if(labeled.find()){String d=date(labeled.group(1));if(!d.isEmpty())return d;}
            for(Html.Node n:scope.all()){
                String token=(n.attr("class")+" "+n.attr("id")).toLowerCase(Locale.ROOT);String item=n.attr("itemprop");
                if(item.equals("dateModified")||token.matches(".*(?:deadline|closing|hire|apply|period|modified).*"))continue;
                boolean metadata=item.equals("datePublished")||n.tag.equals("time")||token.matches(".*(?:^|[\\s_-])(?:date|regdate|regdt|reg_dt|writedate|wr_date|created_at|pubdate|published|boarddate)(?:$|[\\s_-]).*");
                if(metadata){String d=date(n.attr("datetime")+" "+n.attr("content")+" "+n.text());if(!d.isEmpty())return d;}
            }
            if(scope.tag.equals("tr")){
                Html.Node table=scope.parent;while(table!=null&&!table.tag.equals("table"))table=table.parent;
                if(table!=null){List<Html.Node> cells=new ArrayList<>();for(Html.Node n:scope.children)if(n.tag.equals("td")||n.tag.equals("th"))cells.add(n);
                    for(Html.Node row:table.all())if(row.tag.equals("tr")){List<Html.Node> head=new ArrayList<>();for(Html.Node n:row.children)if(n.tag.equals("th"))head.add(n);if(head.size()!=cells.size())continue;for(int i=0;i<head.size();i++)if(head.get(i).text().matches("(?:등록일|작성일|게시일|날짜|등록일자|작성일자)")){String d=date(cells.get(i).text());if(!d.isEmpty())return d;}}
                }break;
            }
            if(scope.tag.equals("li")||scope.tag.equals("article"))break;
        }return "";
    }
    public static boolean pinned(Html.Node n){for(int i=0;n!=null&&i<4;i++,n=n.parent){String token=(n.attr("class")+" "+n.attr("id")).toLowerCase(Locale.ROOT);if(token.matches(".*(?:^|[\\s_-])(?:notice|pinned|topnotice|top_notice|fixed)(?:$|[\\s_-]).*"))return true;if(n.tag.equals("tr")||n.tag.equals("li"))break;}return false;}
    public static int tokyoPage(String url){Matcher m=Pattern.compile("/([0-9]+)\\.\\.").matcher(url);return m.find()?Integer.parseInt(m.group(1)):1;}
    public static List<String> nextPages(Source s,String current,List<String> pages){
        if(!BoardParser.kind(s).equals("TOKYO"))return pages;
        int next=tokyoPage(current)+1;return pages.stream().filter(p->tokyoPage(p)==next).limit(1).collect(java.util.stream.Collectors.toList());
    }
    public static boolean oldTokyoPage(Source s,List<Link> items,LocalDate today){
        if(!BoardParser.kind(s).equals("TOKYO")||items.isEmpty())return false;boolean dated=false;
        for(Link l:items){if(l.pinned())continue;LocalDate d=published(l.published());if(d==null||!d.isBefore(start(today)))return false;dated=true;}return dated;
    }
}
