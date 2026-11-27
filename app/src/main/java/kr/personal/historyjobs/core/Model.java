package kr.personal.historyjobs.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public final class Model {
    private Model() {}
    public enum Status { MATCH, REVIEW, EXCLUDED }
    public static class Source {
        public String id="", name="", url="", kind="AUTO", region="기타 지역", schoolHint="";
        public List<String> keywords=new ArrayList<>();
        public boolean enabled=true;
        public long lastAttempt=0, lastSuccess=0;
        public String message="아직 확인하지 않았어요";
        public Source copy() {
            Source s=new Source(); s.id=id;s.name=name;s.url=url;s.kind=kind;s.region=region;s.schoolHint=schoolHint;s.keywords=new ArrayList<>(keywords);
            s.enabled=enabled;s.lastAttempt=lastAttempt;s.lastSuccess=lastSuccess;s.message=message;return s;
        }
    }
    public record Link(String title,String url,String summary,String published,boolean pinned,String duty) {
        public Link(String title,String url,String summary,String published,boolean pinned){this(title,url,summary,published,pinned,"");}
        public Link(String title,String url){this(title,url,"");}
        public Link(String title,String url,String summary){this(title,url,summary,"",false);}
        public Link(String title,String url,String summary,String published){this(title,url,summary,published,false);}
    }
    public static class Notice {
        public String id="", sourceId="", sourceName="", sourceUrl="", url="", title="", school="", region="", body="", reason="", fingerprint="", deadlineLabel="", published="", duty="";
        public List<String> keywords=null;
        public Status status=Status.REVIEW;
        public List<Link> attachments=new ArrayList<>();
        public long deadline=0, firstSeen=0, lastSeen=0, bodyFetchedAt=0;
        public boolean favorite=false, read=false, complete=false;
        public String contentHash() { return hash(title+"|"+url); }

    }
    public static String hash(String s) {
        try { byte[] h=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder b=new StringBuilder();for(byte x:h)b.append(String.format(Locale.ROOT,"%02x",x&255));return b.toString();
        } catch(Exception e) { throw new IllegalStateException(e); }
    }
    public static List<Source> defaults() {
        List<Source> out=new ArrayList<>();
        out.add(source("sen","서울교육일자리포털","https://work.sen.go.kr/work/search/recInfo/BD_selectSrchRecInfo.do?jobType=jd002&jobCategory=jd002001&jobDuty=all0&q_tabId=jobTab&q_srchJob=jd002,%EA%B3%84%EC%95%BD%EC%A0%9C%EA%B5%90%EC%9B%90,jd002001,%EA%B8%B0%EA%B0%84%EC%A0%9C%EA%B5%90%EC%9B%90,jd002001-all,%EC%A0%84%EC%B2%B4","SEOUL","서울",""));
        out.add(source("goe","경기도교육청","https://www.goe.go.kr/recruit/ad/func/pb/hnfpPbancList.do?mi=10502","GYEONGGI","경기",""));
        out.add(source("tokyo","동경한국학교","http://www.tokos.ed.jp/front_notice.brd?shell=/index.shell:225","TOKYO","기타 지역","동경한국학교"));
        out.addAll(regularSources());return out;
    }
    public static List<Source> regularSources(){return List.of(
        source("sen-regular","서울교육일자리포털 · 정규교원","https://work.sen.go.kr/work/search/recInfo/BD_selectSrchRecInfo.do?jobType=jd001&jobCategory=jd001001&jobDuty=jd001001002&q_srchType=rcrtTtl&q_srchText=&q_currPage=1&q_rowPerPage=15&q_srchArea=&q_srchSchl=&q_tabId=&q_sortBy=regDt&q_mySrchArea=&q_srchJob=jd001%2C%EC%A0%95%EA%B7%9C%EA%B5%90%EC%9B%90%2Cjd001001%2C%EB%B3%B4%ED%86%B5%EA%B5%90%EA%B3%BC%2Cjd001001002%2C%EB%8F%84%EB%8D%95%EC%82%AC%ED%9A%8C%EA%B3%BC&q_recClosed=closed&q_today=&q_jobCategory=","SEOUL","서울",""),
        source("goe-regular","경기도교육청 · 사립 정규교원","https://www.goe.go.kr/recruit/ad/func/pb/hnfpPbancList.do?mi=10502&srchOcptCd=D&srchOcptNm=%EC%82%AC%EB%A6%BD%ED%95%99%EA%B5%90%20%EA%B5%90%EC%9B%90%EC%A0%95%EA%B7%9C%EC%B1%84%EC%9A%A9&srchEcptDl=N&currPage=1","GYEONGGI","경기",""));
    }
    public static String recruitmentCategory(String url){
        if(!Classifier.educationBoard(url))return "";
        var query=SchoolBoards.query(url);
        if(java.net.URI.create(url).getHost().equalsIgnoreCase("work.sen.go.kr"))return query.getOrDefault("jobType","").equals("jd001")||query.getOrDefault("q_srchJob","").startsWith("jd001,")?"SEOUL_REGULAR":"SEOUL_OTHER";
        return query.getOrDefault("srchOcptCd","A").equals("D")?"GOE_REGULAR":"GOE_OTHER";
    }
    public static void addRegularSources(List<Source> sources){
        for(Source extra:regularSources())if(sources.size()<50&&sources.stream().noneMatch(s->s.id.equals(extra.id)||recruitmentCategory(s.url).equals(recruitmentCategory(extra.url))))sources.add(extra);
    }
    public static Source source(String id,String name,String url,String kind,String region,String hint) {
        Source s=new Source();s.id=id;s.name=name;s.url=url;s.kind=kind;s.region=Selection.region(region);s.schoolHint=hint;s.keywords=Selection.defaults(url);return s;
    }
}
