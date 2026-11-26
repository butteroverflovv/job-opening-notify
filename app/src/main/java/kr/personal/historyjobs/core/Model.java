package kr.personal.historyjobs.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public final class Model {
    private Model() {}
    public enum Status { MATCH, REVIEW, EXCLUDED }
    public static class Source {
        public String id="", name="", url="", kind="AUTO", region="서울/경기", schoolHint="";
        public boolean enabled=true;
        public long lastAttempt=0, lastSuccess=0;
        public String message="아직 확인하지 않았어요";
        public Source copy() {
            Source s=new Source(); s.id=id;s.name=name;s.url=url;s.kind=kind;s.region=region;s.schoolHint=schoolHint;
            s.enabled=enabled;s.lastAttempt=lastAttempt;s.lastSuccess=lastSuccess;s.message=message;return s;
        }
    }
    public record Link(String title, String url) {}
    public static class Notice {
        public String id="", sourceId="", sourceName="", url="", title="", school="", region="", body="", reason="", fingerprint="", deadlineLabel="", published="";
        public Status status=Status.REVIEW;
        public List<Link> attachments=new ArrayList<>();
        public long deadline=0, firstSeen=0, lastSeen=0;
        public boolean favorite=false, read=false, complete=false;
        public String contentHash() {
            StringBuilder b=new StringBuilder(title).append('|').append(body.replaceAll("조회\\s*(?:수)?\\s*[:：|]?\\s*\\d+", "")).append('|').append(deadlineLabel).append('|').append(status);
            attachments.forEach(a->b.append('|').append(a.title()).append('|').append(a.url()));
            return hash(b.toString());
        }
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
        out.add(source("tokyo","동경한국학교","http://www.tokos.ed.jp/front_notice.brd?shell=/index.shell:225","TOKYO","동경","동경한국학교"));
        return out;
    }
    public static Source source(String id,String name,String url,String kind,String region,String hint) {
        Source s=new Source();s.id=id;s.name=name;s.url=url;s.kind=kind;s.region=region;s.schoolHint=hint;return s;
    }
}
