package kr.personal.historyjobs.core;

import java.util.*;
import static kr.personal.historyjobs.core.Model.*;

/** International school additions, applied once at catalog version 3. */
public final class InternationalCatalog {
    private InternationalCatalog(){}
    private static Source entry(String id,String name,String url,String region,String words){if(id.equals("goyang-international"))url=InternationalBoards.GOYANG;if(SchoolBoards.ntt(url))url=BoardParser.query(url,"listCo","50");Source s=Model.source(id,name,url,"AUTO",region,name);s.keywords=Selection.keywords(words);return s;}
    public static List<Source> defaults(){return List.of(
        entry("seoul-international","서울국제고등학교","https://sghs.sen.hs.kr/79021/subMenu.do","서울","교사 OR 기간제 교원"),
        entry("sejong-international","세종국제고등학교","https://sjgl.sjeduhs.kr/sjgl-h/na/ntt/selectNttList.do?mi=6656&bbsId=3814","기타 지역","교사 AND 공고"),
        entry("goyang-international","고양국제고등학교","https://ggg.hs.kr/?menu_no=168","경기","교사"),
        entry("dongtan-international","동탄국제고등학교","https://www.dtg.hs.kr/030101","경기","교사"),
        entry("cheongshim-international","청심국제중고등학교","https://csia.hs.kr/?act=board.list&code=1029","경기","교사 AND 공고 OR 기간제 교원"),
        entry("daewon-international","대원국제중학교","http://daewon.ms.kr/?act=board.list&code=1310","서울","기간제교원 OR 기간제 교원 OR 교사"));}
    public static void install(List<Source> sources){
        for(Source requested:defaults()){
            Source existing=sources.stream().filter(s->s.id.equals(requested.id)||SchoolCatalog.key(s.url).equals(SchoolCatalog.key(requested.url))).findFirst().orElse(null);
            if(existing!=null){existing.keywords=new ArrayList<>(requested.keywords);if(requested.id.equals("goyang-international"))existing.url=requested.url;}else if(sources.size()<50)sources.add(requested);
        }
    }
}
