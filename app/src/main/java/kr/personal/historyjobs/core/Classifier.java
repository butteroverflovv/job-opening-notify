package kr.personal.historyjobs.core;

import java.time.*;
import java.util.*;
import java.util.regex.*;
import static kr.personal.historyjobs.core.Model.*;

public final class Classifier {
    private Classifier() {}
    public static final ZoneId KST=ZoneId.of("Asia/Seoul");
    private static final Pattern SCHOOL=Pattern.compile("[가-힣A-Za-z·]{2,25}(?:중·고등학교|중고등학교|고등학교|중학교|초등학교)");
    public static boolean educationBoard(String url){
        try{java.net.URI uri=java.net.URI.create(url);String host=Objects.toString(uri.getHost(),"").toLowerCase(Locale.ROOT);String path=Objects.toString(uri.getPath(),"");
            return host.equals("work.sen.go.kr")||((host.equals("goe.go.kr")||host.equals("www.goe.go.kr"))&&(path.equals("/recruit")||path.startsWith("/recruit/")));
        }catch(Exception ignored){return false;}
    }
    public static boolean titleMatches(String title,String boardUrl){return Selection.titleMatches(title,Selection.defaults(boardUrl));}
    public static boolean titleMatches(Source source,String title){return Selection.titleMatches(title,source.keywords);}
    public static boolean dutyMatches(String boardUrl,String duty){return educationBoard(boardUrl)&&Selection.compact(duty).contains("역사");}
    public static boolean matches(Source source,String title,String duty){return titleMatches(source,title)||dutyMatches(source.url,duty);}
    public static boolean matches(Notice n){String board=n.sourceUrl.isEmpty()?n.url:n.sourceUrl;return Selection.titleMatches(n.title,n.keywords==null?Selection.defaults(board):n.keywords)||dutyMatches(board,n.duty);}
    public record Verdict(Status status,String reason,String school) {}
    public static boolean titleMatches(String title){return title!=null&&title.contains("역사");}
    public static Verdict classify(String title,String body,String hint,boolean complete){
        String name=school(title==null?"":title);if(name.isEmpty())name=hint;
        return new Verdict(titleMatches(title)?Status.MATCH:Status.EXCLUDED,
            titleMatches(title)?"제목에 ‘역사’ 포함":"제목에 ‘역사’가 없어 제외",name);
    }
    /** Reapply current selection to saved data without relying on old body/attachment results. */
    public static void reclassify(Notice n){reclassify(n,LocalDate.now(KST));}
    public static void reclassify(Notice n,LocalDate today){
        String name=school(n.title);if(!name.isEmpty())n.school=name;
        n.status=Selection.eligible(n,today)?Status.MATCH:Status.EXCLUDED;
        n.reason=!Selection.inWindow(n.published,today)?(n.published.isEmpty()?"게시일을 확인하지 못해 제외":"작년·올해 게시물이 아니어서 제외"):!Selection.academicYearMatches(n,today)?"제목에 올해·다음해 학년도가 없어 제외":dutyMatches(n.sourceUrl.isEmpty()?n.url:n.sourceUrl,n.duty)?"직무분야·과목에 ‘역사’ 포함":matches(n)?"게시판의 수집 키워드와 일치":"게시판의 수집 키워드와 일치하지 않아 제외";
        if(n.bodyFetchedAt<=0)n.body="";
        n.attachments.clear();n.complete=true;n.fingerprint=n.contentHash();
    }
    public static String school(String text){Matcher m=SCHOOL.matcher(text);return m.find()?m.group():text.contains("동경한국학교")?"동경한국학교":"";}
    public static void enrich(Notice n,String hint,boolean complete){
        Verdict v=classify(n.title,n.body,hint,complete);n.status=v.status();n.reason=v.reason();n.school=v.school();n.complete=complete;
        Deadline d=deadline(n.body,LocalDate.now(KST));n.deadline=d.epoch;n.deadlineLabel=d.label;
    }
    public record Deadline(long epoch,String label) {}
    private static final Pattern DATE=Pattern.compile("(?:(20\\d{2})\\s*[./년-]\\s*)?(\\d{1,2})\\s*[./월-]\\s*(\\d{1,2})(?:\\s*[.일])?");
    public static Deadline deadline(String body,LocalDate today){
        String[] lines=body.split("\\n");
        for(int i=0;i<lines.length;i++){
            String line=lines[i];
            if(!line.matches(".*(?:접수\\s*기간|접수\\s*마감|서류\\s*접수|원서\\s*접수|지원\\s*기간).*"))continue;
            if(line.length()<18&&i+1<lines.length)line+=" "+lines[i+1];
            Matcher m=DATE.matcher(line);List<LocalDate> ds=new ArrayList<>();int end=0,year=today.getYear();
            while(m.find()){
                try{if(m.group(1)!=null)year=Integer.parseInt(m.group(1));LocalDate d=LocalDate.of(year,Integer.parseInt(m.group(2)),Integer.parseInt(m.group(3)));if(!ds.isEmpty()&&d.isBefore(ds.get(ds.size()-1))&&m.group(1)==null)d=d.plusYears(1);ds.add(d);end=m.end();}catch(DateTimeException ignored){}
            }
            if(ds.isEmpty())continue;
            LocalDate last=ds.get(ds.size()-1);LocalTime time=LocalTime.of(23,59,59);boolean explicit=false;
            String suffix=line.substring(Math.min(end,line.length()));Matcher tm=Pattern.compile("(\\d{1,2})\\s*(?::|시)\\s*(\\d{1,2})?").matcher(suffix);
            if(tm.find())try{time=LocalTime.of(Integer.parseInt(tm.group(1)),tm.group(2)==null?0:Integer.parseInt(tm.group(2)));explicit=true;}catch(DateTimeException ignored){}
            return new Deadline(ZonedDateTime.of(last,time,KST).toInstant().toEpochMilli(),last+(explicit?" "+time:" · 시각은 원문 확인"));
        }
        return new Deadline(0,"");
    }
    public static boolean potentiallyRelevant(String title){return titleMatches(title);}
}
