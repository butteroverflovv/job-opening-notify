package kr.personal.historyjobs.core;

import java.time.*;
import java.util.*;
import java.util.regex.*;
import static kr.personal.historyjobs.core.Model.*;

public final class Classifier {
    private Classifier() {}
    public static final ZoneId KST=ZoneId.of("Asia/Seoul");
    private static final Pattern SCHOOL=Pattern.compile("[가-힣A-Za-z·]{2,25}(?:중·고등학교|중고등학교|고등학교|중학교|초등학교)");
    private static final Pattern HISTORY=Pattern.compile("한국사|세계사|역사");
    private static final Pattern PERIOD=Pattern.compile("기간\\s*제(?:\\s*(?:교원|교사))?");
    private static final Pattern HOURLY=Pattern.compile("시간\\s*강사");
    private static final Pattern OTHER=Pattern.compile("국어|영어|수학|물리|화학|생물|지구과학|과학|지리|일반사회|도덕|윤리|체육|음악|미술|정보|보건|사서|영양|특수");
    public record Verdict(Status status,String reason,String school) {}
    public static Verdict classify(String title,String body,String hint,boolean complete){
        String t=Html.clean(title),b=Html.clean(body),all=t+"\n"+b;
        String school=school(t);if(school.trim().isEmpty())school=school(b);if(school.trim().isEmpty())school=hint;
        boolean secondary=school.contains("중학교")||school.contains("고등학교")||school.contains("중고등")||all.matches("(?s).*(?:학교급|학교별)\\s*[:：|]?\\s*(?:중학교|고등학교).*" );
        if(school.contains("동경한국학교"))secondary=all.matches("(?s).*(?:중등|고등|중학|고교|中等|高等).*" );
        if(school.endsWith("초등학교")&&!secondary)return new Verdict(Status.EXCLUDED,"초등학교 공고",school);
        if(t.matches(".*(?:기간제\\s*근로자|교육공무직|교육실무사|행정실무|보조인력).*" )&&!t.matches(".*기간\\s*제\\s*(?:교사|교원).*"))return new Verdict(Status.EXCLUDED,"교사 외 직종 공고",school);
        boolean ht=HISTORY.matcher(t).find(),pt=PERIOD.matcher(t).find();
        boolean historyHourly=(t+b).matches("(?s).*(?:역사|한국사|세계사)(?:과)?\\s*[(:：]?\\s*시간\\s*강사.*")||t.matches("(?s).*시간\\s*강사\\s*[(（][^）)]*(?:역사|한국사|세계사)[^）)]*[）)].*");
        boolean historyPeriod=(t+b).matches("(?s).*(?:역사|한국사|세계사)(?:과)?\\s*[(:：]?\\s*기간\\s*제.*")||t.matches("(?s).*기간\\s*제(?:\\s*(?:교사|교원))?\\s*[(（][^）)]*(?:역사|한국사|세계사)[^）)]*[）)].*");
        if(historyHourly&&!historyPeriod)return new Verdict(Status.EXCLUDED,"역사 시간강사 공고",school);
        if(ht&&pt&&secondary&&!HOURLY.matcher(t).find())return new Verdict(Status.MATCH,"제목에서 역사·기간제, 학교급 확인",school);
        if(historyPeriod&&secondary&&!historyHourly)return new Verdict(complete?Status.MATCH:Status.REVIEW,complete?"본문에서 역사 기간제 모집 확인":"역사 기간제 후보 · 일부 내용 확인 실패",school);
        if(HOURLY.matcher(t).find()&&!pt&&!PERIOD.matcher(b).find())return new Verdict(Status.EXCLUDED,"시간강사 공고",school);
        boolean bodyHistory=false;
        for(String line:b.split("\\n")){
            String compact=line.replaceAll("\\s+"," ");
            if(HISTORY.matcher(compact).find()&&(compact.matches(".*(?:채용\\s*분야|모집\\s*분야|채용\\s*과목|모집\\s*과목|담당\\s*과목|교과|과목|채용\\s*예정|채용\\s*인원).*" )||compact.matches(".*(?:역사|한국사|세계사)(?:과)?\\s*[|:：]\\s*\\d+.*")))bodyHistory=true;
        }
        if((ht||bodyHistory)&&secondary&&(pt||PERIOD.matcher(b).find())&&!historyHourly){
            if(!complete)return new Verdict(Status.REVIEW,"역사 기간제 후보 · 일부 내용 확인 실패",school);
            if(HOURLY.matcher(all).find())return new Verdict(Status.REVIEW,"여러 채용 형태가 섞여 있어 원문 확인 필요",school);
            return new Verdict(Status.MATCH,"역사 모집 과목·기간제·학교급 확인",school);
        }
        if(ht||bodyHistory){return new Verdict(Status.REVIEW,!secondary?"중·고등학교급 확인 필요":"역사 모집 여부 또는 채용 형태 확인 필요",school);}
        if(!complete)return new Verdict(Status.REVIEW,"본문 또는 첨부를 읽지 못해 역사 모집 여부 확인 필요",school);
        if(OTHER.matcher(t).find()&&!t.matches(".*(?:외|등|여러|각|및|,|·).*"))return new Verdict(Status.EXCLUDED,"다른 과목 공고",school);
        if(t.matches(".*(?:채용|모집|기간제|교원).*"))return new Verdict(Status.EXCLUDED,"확인한 본문·첨부에서 역사 모집 근거를 찾지 못함",school);
        return new Verdict(Status.EXCLUDED,"채용 대상 공고 아님",school);
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
    public static boolean potentiallyRelevant(String title){return !title.matches(".*(?:초등학교|유치원).*" )||title.matches(".*(?:중학교|고등학교|중등).*" );}
}
