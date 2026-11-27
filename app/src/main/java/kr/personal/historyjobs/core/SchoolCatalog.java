package kr.personal.historyjobs.core;

import java.net.URI;
import java.util.*;
import static kr.personal.historyjobs.core.Model.*;

/** User-requested school subscriptions, installed once per catalog version. */
public final class SchoolCatalog {
    private SchoolCatalog(){}
    private static Source entry(String id,String name,String url,String region,String words){if(SchoolBoards.ntt(url))url=BoardParser.query(url,"listCo","50");Source s=Model.source(id,name,url,"AUTO",region,name);s.keywords=Selection.keywords(words);return s;}
    public static List<Source> defaults(){return List.of(
        entry("sejong-science","세종과학고등학교","https://sjsh.sen.hs.kr/80720/subMenu.do","서울","교사"),
        entry("hansung-science","한성과학고등학교","https://hansungsh.sen.hs.kr/1642/subMenu.do","서울","교사"),
        entry("gyeonggibuk-science","경기북과학고등학교","https://gbs-h.goeujb.kr/gbs-h/na/ntt/selectNttList.do?mi=4118&bbsId=2262","경기","교사"),
        entry("myungduk-fl","명덕외국어고등학교","http://mdfh.or.kr/?act=news.main&comm=com&mcode=1310","서울","교사"),
        entry("seoul-fl","서울외국어고등학교","https://sfl.sen.hs.kr/203923/subMenu.do","서울","교원"),
        entry("ewha-fl","이화여자외국어고등학교","https://www.ewha-gfh.hs.kr/0201","서울","교사 OR 교원"),
        entry("hanyoung-fl","한영외국어고등학교","https://hyfl.sen.hs.kr/145567/subMenu.do","서울","교사 OR 교원"),
        entry("goyang-fl","고양외국어고등학교","https://www.gyfl.hs.kr/main/main.php?categoryid=12&menuid=03&groupid=00","경기","교사"),
        entry("gwacheon-fl","과천외국어고등학교","https://www.gcfl.or.kr/notification/notice.asp","경기","채용"),
        entry("gimpo-fl","김포외국어고등학교","https://gfl.hs.kr/?act=board.list&code=2312","경기","교사"),
        entry("dongducheon-fl","동두천외국어고등학교","https://dfl-h.goedy.kr/dfl-h/na/ntt/selectNttList.do?mi=6398&bbsId=3642","경기","교사 AND 공고"),
        entry("seongnam-fl","성남외국어고등학교","https://snfl-h.goesn.kr/snfl-h/na/ntt/selectNttList.do?mi=27530&bbsId=17181","경기","교사 채용 OR 교원 선발"),
        entry("suwon-fl","수원외국어고등학교","https://www.sawl.hs.kr/sys/bbs/board.php?bo_table=030601","경기","교사 AND 공고"),
        entry("anyang-fl","안양외국어고등학교","https://anyangfl-h.goeay.kr/anyangfl-h/na/ntt/selectNttList.do?mi=9329&bbsId=4646","경기","교사 AND 채용"));}
    public static String key(String url){
        try{URI u=URI.create(url);String host=Objects.toString(u.getHost(),"").toLowerCase(Locale.ROOT).replaceFirst("^www\\.","");String path=Objects.toString(u.getPath(),"/").replaceAll("/+$","");if(InternationalBoards.goyang(url)){host="ggg.hs.kr";path="";}if(host.equals("gcfl.or.kr")&&(path.isEmpty()||path.equals("/main/main.asp")))path="/notification/notice.asp";Map<String,String> values=SchoolBoards.query(url);TreeMap<String,String> fields=new TreeMap<>();
            for(String k:List.of("bbsId","mi","bo_table","act","code","mcode","comm","categoryid","menuid","groupid","menu_no"))if(values.containsKey(k))fields.put(k,values.get(k));
            return host+path+fields;
        }catch(Exception e){return url;}
    }
    public static void install(List<Source> sources){
        for(Source requested:defaults()){
            Source existing=sources.stream().filter(s->s.id.equals(requested.id)||key(s.url).equals(key(requested.url))).findFirst().orElse(null);
            if(existing!=null){existing.keywords=new ArrayList<>(requested.keywords);if(requested.id.equals("gwacheon-fl"))existing.url=requested.url;}
            else if(sources.size()<50)sources.add(requested);
        }
    }
}
