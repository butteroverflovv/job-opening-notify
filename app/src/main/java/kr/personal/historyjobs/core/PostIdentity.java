package kr.personal.historyjobs.core;

import java.net.*;
import java.util.*;
import java.util.regex.*;
import static kr.personal.historyjobs.core.Model.*;

/** Article identity, kept separate from its current working read URL. */
public final class PostIdentity {
    private PostIdentity(){}
    private static final Pattern TOKYO=Pattern.compile("^(.+\\.brd)/_([0-9]+)\\.[0-9]+\\.[^/]+/?$");
    private static final Set<String> PRESENTATION=Set.of("page","pageindex","pagenum","pageno","currpage","q_currpage","q_rowperpage","rowperpage","listco","sfl","stx","sst","sod","sop","sca","searchcondition","searchkeyword","searchword","searchtext","q_srchtext","q_srchtype");
    public static boolean tokyo(String url){try{String host=Objects.toString(URI.create(url).getHost(),"").toLowerCase(Locale.ROOT);return host.equals("tokos.ed.jp")||host.endsWith(".tokos.ed.jp");}catch(Exception e){return false;}}
    private static String first(Map<String,String> values,String... keys){for(String key:keys){String value=values.getOrDefault(key,"");if(value.matches("[0-9]{1,20}"))return value.replaceFirst("^0+(?!$)","");}return "";}
    private static String field(Map<String,String> url,Map<String,String> source,String name){return url.getOrDefault(name,source.getOrDefault(name,""));}
    private static String encoded(Map<String,String> values){StringJoiner out=new StringJoiner("&");for(var e:new TreeMap<>(values).entrySet())try{out.add(URLEncoder.encode(e.getKey(),"UTF-8")+"="+URLEncoder.encode(e.getValue(),"UTF-8"));}catch(Exception ignored){}return out.toString();}
    public static String key(String sourceUrl,String url){
        if(url==null||url.isEmpty())return "";
        try{
            URI uri=URI.create(url).normalize();String host=Objects.toString(uri.getHost(),"").toLowerCase(Locale.ROOT).replaceFirst("^www\\.","");if(host.isEmpty())return "raw:"+url;
            int port=uri.getPort();if(port>0&&!(port==80&&uri.getScheme().equals("http")||port==443&&uri.getScheme().equals("https")))host+=":"+port;
            String path=Objects.toString(uri.getRawPath(),"/");Map<String,String> q=SchoolBoards.query(url),board=SchoolBoards.query(sourceUrl==null?"":sourceUrl);String id;
            if(tokyo(url)){
                Matcher m=TOKYO.matcher(path);if(m.matches())return host+"|tokyo|"+m.group(1)+"|"+m.group(2).replaceFirst("^0+(?!$)","");
                id=first(q,"num","no","seq","id","nttSn");if(!id.isEmpty()&&path.endsWith(".brd"))return host+"|tokyo|"+path+"|"+id;
            }
            id=first(q,"q_rcrtSn");if(host.equals("work.sen.go.kr")&&!id.isEmpty())return host+"|seoul|"+id;
            id=first(q,"pbancSn");if(host.equals("goe.go.kr")&&!id.isEmpty())return host+"|goe|"+id;
            id=first(q,"nttSn");if(!id.isEmpty()&&SchoolBoards.ntt(url))return host+"|ntt|"+path.substring(0,path.indexOf("/na/ntt/"))+"|"+field(q,board,"bbsId")+"|"+id;
            id=first(q,"nttId");if(!id.isEmpty()&&(host.matches(".*\\.sen\\.(?:hs|ms|es|sc)\\.kr")||path.contains("/dggb/")))return host+"|sen|"+id;
            if(GoyangBoard.supports(url)&&q.getOrDefault("board","").equals("view")&&!(id=first(q,"no")).isEmpty())return host+"|gyfl|12|03|00|"+id;
            if((host.equals("ggg.hs.kr")||host.equals("m.ggg.hs.kr"))&&!(id=first(q,"bno")).isEmpty())return "ggg.hs.kr|mobile|"+field(q,board,"menu_no")+"|"+id;
            String kind=SchoolSites.kind(sourceUrl==null||sourceUrl.isEmpty()?url:sourceUrl);
            if(kind.equals("SCHOOL_GNU")){
                String table=field(q,board,"bo_table");id=first(q,"wr_id");Matcher pretty=Pattern.compile("^/([A-Za-z0-9_]+)/([0-9]{1,20})/?$").matcher(path);
                if(pretty.matches()){table=pretty.group(1);id=pretty.group(2);}
                String base=sourceUrl==null||sourceUrl.isEmpty()?path:Objects.toString(URI.create(sourceUrl).getPath(),"");String prefix=base.contains("/bbs/")?base.substring(0,base.indexOf("/bbs/")):"";
                if(!id.isEmpty())return host+"|gnu|"+prefix+"|"+table+"|"+id;
            }
            if(kind.equals("SCHOOL_ACT")){
                id=first(q,"id","num","sid");if(!id.isEmpty())return host+"|act|"+field(q,board,"code")+"|"+field(q,board,"comm")+"|"+field(q,board,"mcode")+"|"+id;
            }
            if(kind.equals("SCHOOL_ASP")&&!(id=first(q,"idx")).isEmpty())return host+"|asp|"+path.substring(0,path.lastIndexOf('/')+1)+"|"+id;
            boolean identified=!first(q,"nttSn","articleNo","boardSeq","seq","num","no","id","idx","wr_id").isEmpty();Map<String,String> kept=new TreeMap<>();
            for(var e:q.entrySet()){String name=e.getKey().toLowerCase(Locale.ROOT);if(name.startsWith("utm_")||Set.of("fbclid","gclid").contains(name)||identified&&PRESENTATION.contains(name))continue;kept.put(e.getKey(),e.getValue());}
            return host+"|"+path+"|"+encoded(kept);
        }catch(Exception e){return "raw:"+url;}
    }
    public static String id(String sourceId,String sourceUrl,String url){return Model.hash(sourceId+"|"+key(sourceUrl,url));}
    public static String title(String boardUrl,String title){String value=Selection.compact(Objects.toString(title,""));if(tokyo(boardUrl)&&value.startsWith("동경한국학교"))value=value.substring("동경한국학교".length());return value;}
    public static String content(Notice n){return Model.hash(title(n.sourceUrl.isEmpty()?n.url:n.sourceUrl,n.title)+"|"+key(n.sourceUrl,n.url));}
    public static List<Link> unique(Source source,List<Link> links){
        Map<String,Link> out=new LinkedHashMap<>();for(Link link:links)add(source,out,link);return new ArrayList<>(out.values());
    }
    public static void add(Source source,Map<String,Link> out,Link link){String key=key(source.url,link.url());Link old=out.get(key);if(old==null||old.published().isEmpty()&&!link.published().isEmpty()||old.pinned()&&!link.pinned())out.put(key,link);}
}
