package kr.personal.historyjobs.core;

import java.util.*;
import static kr.personal.historyjobs.core.Model.*;

public final class Collector {
    public interface Transport { Net.Response get(String url,int limit,String referer)throws Exception; }
    public interface DocumentReader { String read(Net.Response response,Link file)throws Exception; }
    public record Result(List<Notice> notices,int listed,int checked,boolean partial,List<String> errors) {}
    private final Transport net;private final DocumentReader reader;
    public Collector(Transport net,DocumentReader reader){this.net=net;this.reader=reader;}
    public Result collect(Source source,Map<String,Notice> known,long now)throws Exception{
        ArrayDeque<String> queue=new ArrayDeque<>();queue.add(source.url);
        Set<String> visited=new HashSet<>(),bodies=new HashSet<>();LinkedHashMap<String,Link> listings=new LinkedHashMap<>();
        List<String> errors=new ArrayList<>();int pages=0;boolean partial=false;long stopAt=System.nanoTime()+90_000_000_000L;long listStopAt=System.nanoTime()+35_000_000_000L;
        while(!queue.isEmpty()&&pages<20&&System.nanoTime()<listStopAt&&!Thread.currentThread().isInterrupted()){
            String page=queue.remove();if(!visited.add(page))continue;
            try{
                Net.Response response=net.get(page,4_000_000,source.url);String html=response.text();
                if(!bodies.add(Model.hash(html)))continue;
                BoardParser.Listing list=BoardParser.listing(source,response.url(),html);
                if(!list.warning().trim().isEmpty())throw new IllegalStateException(list.warning());
                pages++;
                for(Link l:list.items())if(Classifier.potentiallyRelevant(l.title()))listings.putIfAbsent(l.url(),l);
                for(String p:list.pages())if(!visited.contains(p))queue.add(p);
            }catch(Exception e){if(pages==0)throw e;errors.add("목록 일부: "+shortError(e));partial=true;}
        }
        if(!queue.isEmpty()){partial=true;errors.add("한 번에 최대 20쪽까지 확인했어요. 게시판 원문도 확인해 주세요.");}
        List<Link> ordered=new ArrayList<>(listings.values());
        ordered.sort(Comparator.comparingLong(l->{Notice n=known.get(id(source,l.url()));return n==null?0:n.lastSeen;}));
        List<Notice> out=new ArrayList<>();int checked=0;
        for(Link link:ordered){
            String id=id(source,link.url());Notice old=known.get(id);
            if(old!=null&&old.complete&&old.lastSeen>now-6*60*60*1000L)continue;
            if(checked>=100||System.nanoTime()>=stopAt||Thread.currentThread().isInterrupted()){partial=true;if(old==null){Notice n=base(source,link,id,now);n.reason="아직 본문 확인 전 · 다음 확인 때 이어서 읽어요";out.add(n);}continue;}
            Notice n=base(source,link,id,now);boolean complete=true;
            try{
                Net.Response response=net.get(link.url(),4_000_000,source.url);
                BoardParser.Detail d=BoardParser.detail(response.url(),response.text(),link.title());
                n.title=d.title();n.body=d.body();n.attachments.addAll(d.attachments());
                for(String frame:d.frames().subList(0,Math.min(2,d.frames().size())))try{
                    Net.Response fr=net.get(frame,3_000_000,link.url());BoardParser.Detail fd=BoardParser.detail(fr.url(),fr.text(),n.title);
                    n.body+="\n"+fd.body();for(Link file:fd.attachments())if(n.attachments.stream().noneMatch(a->a.url().equals(file.url())))n.attachments.add(file);
                }catch(Exception e){complete=false;}
                // A list page is not a successfully read detail, even when the server returns HTTP 200.
                if(n.body.length()<30||n.body.matches("(?s).*(?:접근이 차단|접근 권한이 없|자동입력 방지|Access Denied|Just a moment).*"))throw new IllegalStateException("본문을 읽지 못했어요");
                int files=0;
                for(Link file:new ArrayList<>(n.attachments)){
                    if(files++>=5){complete=false;break;}
                    try{Net.Response raw=net.get(file.url(),12*1024*1024,link.url());String txt=reader.read(raw,file);if(txt.trim().isEmpty())throw new IllegalStateException("글자 없음");n.body+="\n\n[첨부: "+file.title()+"]\n"+txt;}
                    catch(Exception e){complete=false;errors.add("첨부 확인 실패: "+file.title().substring(0,Math.min(55,file.title().length())));}
                }
                if(n.body.length()>200_000){n.body=n.body.substring(0,200_000);complete=false;}
                if(n.body.matches("(?s).*(?:첨부파일|붙임|첨부 참조|첨부파일 참고).*" )&&n.attachments.isEmpty())complete=false;
                Classifier.enrich(n,source.schoolHint,complete);n.lastSeen=now;
            }catch(Exception e){
                complete=false;n.lastSeen=0;n.complete=false;n.reason="조회 실패 · "+shortError(e);
                // A previous verified result must not be overwritten by a transient failed fetch.
                if(old!=null){partial=true;errors.add(n.title+": "+n.reason);checked++;continue;}
                n.body="본문을 가져오지 못했어요. 원문을 열어 확인해 주세요.";
                Classifier.Verdict verdict=Classifier.classify(n.title,"",source.schoolHint,false);
                n.school=verdict.school();n.status=verdict.status()==Status.EXCLUDED?Status.EXCLUDED:Status.REVIEW;
            }
            if(!complete)partial=true;
            n.fingerprint=n.contentHash();out.add(n);checked++;
        }
        if(System.nanoTime()>=stopAt)errors.add("실행 시간 제한으로 남은 본문은 다음 확인 때 이어서 읽어요.");
        return new Result(out,listings.size(),checked,partial,errors.stream().distinct().limit(15).collect(java.util.stream.Collectors.toList()));
    }
    private static Notice base(Source s,Link l,String id,long now){Notice n=new Notice();n.id=id;n.sourceId=s.id;n.sourceName=s.name;n.region=s.region;n.url=l.url();n.title=l.title();n.firstSeen=now;return n;}
    public static String id(Source s,String url){return Model.hash(s.id+"|"+url);}
    public static String shortError(Exception e){String s=e.getMessage();return s==null?e.getClass().getSimpleName():s.substring(0,Math.min(s.length(),160));}
}
