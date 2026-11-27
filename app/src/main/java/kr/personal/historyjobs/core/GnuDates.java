package kr.personal.historyjobs.core;

import java.time.*;
import java.util.*;
import java.util.regex.*;
import static kr.personal.historyjobs.core.Model.*;

/** Never infers a publication year from a title, month/day, or clock-only list cell. */
public final class GnuDates {
    private GnuDates(){}
    private static final Pattern SHORT=Pattern.compile("(?<![0-9])([0-9]{2})[-./]([0-9]{1,2})[-./]([0-9]{1,2})(?![0-9])");
    private static boolean css(Html.Node n,String token){return Arrays.asList(n.attr("class").split("\\s+")).contains(token);}
    private static String full(String text,LocalDate today){
        String date=Selection.date(text);if(!date.isEmpty())return date;Matcher m=SHORT.matcher(text);
        if(m.find())try{int year=Integer.parseInt(m.group(1));year+=year>today.getYear()%100+1?1900:2000;return LocalDate.of(year,Integer.parseInt(m.group(2)),Integer.parseInt(m.group(3))).toString();}catch(DateTimeException ignored){}return "";
    }
    public static String listPublished(Html.Node link){
        for(Html.Node scope=link;scope!=null;scope=scope.parent){
            if(scope.tag.equals("body")||scope.tag.equals("table"))break;
            if(scope!=link&&!scope.tag.equals("tr")&&!scope.tag.equals("li")&&scope.all().stream().filter(n->n.tag.equals("a")&&!n.attr("href").isEmpty()).count()>1)break;
            for(Html.Node n:scope.all())if(css(n,"td_datetime")||css(n,"bo_date")||css(n,"wr_datetime")||n.tag.equals("time")){
                String date=Selection.date(n.attr("datetime")+" "+n.attr("title")+" "+n.text());if(!date.isEmpty())return date;
            }
            if(scope.tag.equals("tr")||scope.tag.equals("li"))break;
        }return "";
    }
    public static String detailPublished(String html,LocalDate today){
        Html.Node root=Html.parse(html);
        for(Html.Node n:root.all()){
            String token=(n.attr("id")+" "+n.attr("class")).toLowerCase(Locale.ROOT);
            if(token.matches(".*(?:modified|updated|deadline|closing|period).*")||n.attr("itemprop").equals("dateModified"))continue;
            boolean info=false;for(Html.Node p=n;p!=null;p=p.parent)if(p.attr("id").equals("bo_v_info"))info=true;
            boolean published=n.attr("itemprop").equals("datePublished")||css(n,"if_date")||css(n,"bo_v_date")||css(n,"view_datetime")||info&&(n.tag.equals("time")||css(n,"datetime")||css(n,"td_datetime"));
            if(published){String date=full(n.attr("datetime")+" "+n.attr("content")+" "+n.attr("title")+" "+n.text(),today);if(!date.isEmpty())return date;}
        }
        for(Html.Node n:root.all())if(n.attr("id").equals("bo_v_info")||n.text().length()<=160&&(n.tag.equals("tr")||n.tag.equals("dl")||(n.attr("id")+" "+n.attr("class")).matches("(?i).*(?:date|info).*"))){
            boolean body=false;for(Html.Node p=n;p!=null;p=p.parent)if(p.attr("id").equals("bo_v_con")||p.attr("id").equals("bo_v_file")||p.tag.equals("script")||p.tag.equals("style")||p.attr("itemprop").equals("dateModified"))body=true;if(body)continue;
            Matcher m=Pattern.compile("(?:작성일|등록일|게시일)[\\s:：|]*((?:[0-9]{4}|[0-9]{2})[-./][0-9]{1,2}[-./][0-9]{1,2})").matcher(n.text());if(m.find()){String date=full(m.group(1),today);if(!date.isEmpty())return date;}
        }return "";
    }
    private static Link dated(Link link,String date){return new Link(link.title(),link.url(),link.summary(),date,link.pinned(),link.duty());}
    public static List<Link> resolve(Source source,List<Link> items,Collector.Transport net,Map<String,String> memo,LocalDate today){
        if(!BoardParser.kind(source).equals("SCHOOL_GNU"))return items;List<Link> out=new ArrayList<>(items);
        int first=-1;for(int i=0;i<out.size();i++)if(!out.get(i).pinned()){first=i;break;}
        for(int i=0;i<out.size();i++){
            Link link=out.get(i);if(link.published().isEmpty()&&(i==first||Classifier.matches(source,link.title(),link.duty())&&Selection.academicYearMatches(link.title(),source.url,today)))out.set(i,fetch(link,source,net,memo,today));
        }
        // One non-pinned anchor per page; only if it is old do we verify every
        // remaining regular date before using the common old-page stop rule.
        if(first>=0){LocalDate anchor=Selection.published(out.get(first).published());if(anchor!=null&&anchor.isBefore(Selection.start(today)))for(int i=0;i<out.size();i++)if(!out.get(i).pinned()&&out.get(i).published().isEmpty())out.set(i,fetch(out.get(i),source,net,memo,today));}
        return out;
    }
    private static Link fetch(Link link,Source source,Collector.Transport net,Map<String,String> memo,LocalDate today){
        if(!link.published().isEmpty()||Thread.currentThread().isInterrupted())return link;
        if(!memo.containsKey(link.url())){String date="";try{var response=net.get(link.url(),2_000_000,source.url);if(ArticleReader.htmlType(response.mime()))date=detailPublished(response.text(),today);}catch(Exception ignored){}memo.put(link.url(),date);}
        return dated(link,memo.get(link.url()));
    }
}
