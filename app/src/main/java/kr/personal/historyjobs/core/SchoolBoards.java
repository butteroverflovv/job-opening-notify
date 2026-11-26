package kr.personal.historyjobs.core;

import java.net.*;
import java.util.*;
import java.util.regex.*;
import static kr.personal.historyjobs.core.Model.*;

/** Public school CMS routes. Reads typed IDs and form values, never executes page scripts. */
public final class SchoolBoards {
    private SchoolBoards(){}
    public static final String SEN_LIST="/dggb/module/board/selectBoardListAjax.do";
    public static final String SEN_DETAIL="/dggb/module/board/selectBoardSenDetailAjax.do";
    public static boolean ntt(String url){try{return URI.create(url).getPath().matches(".*/na/ntt/selectNtt(?:List|Info)\\.do");}catch(Exception e){return false;}}
    public static boolean senMenu(String url){try{URI u=URI.create(url);String h=Objects.toString(u.getHost(),"").toLowerCase(Locale.ROOT);return h.matches(".*\\.sen\\.(?:hs|ms|es|sc)\\.kr")&&u.getPath().matches("/[0-9]+/subMenu\\.do");}catch(Exception e){return false;}}
    public static Map<String,String> query(String url){Map<String,String> out=new LinkedHashMap<>();try{String raw=URI.create(url).getRawQuery();if(raw!=null)for(String p:raw.split("&")){String[] kv=p.split("=",2);out.put(URLDecoder.decode(kv[0],"UTF-8"),kv.length==1?"":URLDecoder.decode(kv[1],"UTF-8"));}}catch(Exception ignored){}return out;}
    public static boolean insideScript(Html.Node n){for(Html.Node p=n;p!=null;p=p.parent)if(p.tag.equals("script")||p.tag.equals("style"))return true;return false;}
    private static boolean css(Html.Node n,String name){return Arrays.asList(n.attr("class").split("\\s+")).contains(name);}
    private static String fieldsUrl(String base,Map<String,String> fields){String url=base;for(var field:fields.entrySet())url=BoardParser.query(url,field.getKey(),field.getValue());return url;}
    public static String seoulRequest(String page,String html,Html.Node root,boolean detail){
        String path;try{path=URI.create(page).getPath();}catch(Exception e){return "";}
        if(!path.matches("/[0-9]+/subMenu\\.do"))return "";
        Html.Node form=null;for(Html.Node n:root.all())if(n.tag.equals("form")&&n.attr("id").equals("boardFrm")&&!insideScript(n)){form=n;break;}
        if(form==null||!html.contains(SEN_LIST))return "";
        Map<String,String> fields=new LinkedHashMap<>();Set<String> allowed=Set.of("bbsId","bbsTyCode","customRecordCountPerPage","pageIndex","searchCondition","searchKeyword");
        for(Html.Node n:form.all())if(n.tag.equals("input")&&allowed.contains(n.attr("name"))&&!insideScript(n))fields.put(n.attr("name"),n.attr("value"));
        if(!fields.getOrDefault("bbsId","").matches("BBSMSTR_[0-9]{1,30}"))return "";
        fields.put("customRecordCountPerPage","10");fields.putIfAbsent("pageIndex","1");
        if(!fields.get("pageIndex").matches("[0-9]{1,4}"))return "";
        String id=query(page).getOrDefault("nttId","");
        if(detail){if(!id.matches("[0-9]{1,20}"))return "";fields.put("nttId",id);}
        else if(!id.isEmpty())return "";
        return fieldsUrl(Html.resolve(page,detail?SEN_DETAIL:SEN_LIST),fields);
    }
    public static String articleRequest(String page,String html){try{if(!URI.create(page).getPath().matches("/[0-9]+/subMenu\\.do"))return "";}catch(Exception e){return "";}return seoulRequest(page,html,Html.parse(html),true);}
    public static String recordUrl(Source source,String page,Html.Node node){
        String type=BoardParser.kind(source);
        if(type.equals("SCHOOL_NTT")&&(css(node,"nttInfoBtn")||css(node,"nttRowLink"))){
            String id=node.attr("data-id");if(!id.matches("[0-9]{1,20}"))return "";
            String endpoint=Html.resolve(page,"selectNttInfo.do");Map<String,String> values=query(page),fallback=query(source.url),fields=new LinkedHashMap<>();
            for(String key:List.of("mi","bbsId")){String v=values.getOrDefault(key,fallback.getOrDefault(key,""));if(!v.matches("[0-9]{1,20}"))return "";fields.put(key,v);}
            String cls=values.getOrDefault("clasHmpgId",fallback.getOrDefault("clasHmpgId",""));if(cls.matches("[A-Za-z0-9_-]{1,100}"))fields.put("clasHmpgId",cls);
            fields.put("nttSn",id);return fieldsUrl(endpoint,fields);
        }
        if(type.equals("SCHOOL_SEN")){
            Matcher m=Pattern.compile("^\\s*(?:return\\s+)?fnView\\(\\s*['\"](BBSMSTR_[0-9]{1,30})['\"]\\s*,\\s*['\"]?([0-9]{1,20})['\"]?\\s*\\)\\s*;?\\s*(?:return\\s+false;?)?\\s*$").matcher(node.attr("onclick"));
            if(!m.matches())return "";
            String expected=query(page).getOrDefault("bbsId","");if(!expected.isEmpty()&&!expected.equals(m.group(1)))return "";
            String publicPage=source.url;
            try{if(!URI.create(publicPage).getPath().matches("/[0-9]+/subMenu\\.do"))publicPage=fieldsUrl(Html.resolve(page,"/dggb/module/board/selectBoardDetailAjax.do"),Map.of("bbsId",m.group(1)));}catch(Exception e){return "";}
            return BoardParser.query(publicPage,"nttId",m.group(2));
        }
        return "";
    }
    public static String pageKey(Source s){return switch(BoardParser.kind(s)){case "SEOUL"->"q_currPage";case "GYEONGGI","SCHOOL_NTT"->"currPage";case "SCHOOL_SEN"->"pageIndex";default->"page";};}
    public static boolean seoulBodyCell(Html.Node n){
        if(!n.tag.equals("td")||n.parent==null||!n.parent.tag.equals("tr"))return false;
        boolean content=false;for(Html.Node c:n.parent.children)if(c.tag.equals("th")&&c.text().equals("내용"))content=true;if(!content)return false;
        for(Html.Node p=n.parent.parent;p!=null;p=p.parent)if(p.tag.equals("table"))return css(p,"board_type01_tb_readform");return false;
    }
    public static String securityMessage(Html.Node root){String text=root.text();return text.contains("웹 보안 필터링 시스템이 동작")&&text.contains("차단 정보")?"사이트가 보안 안내 페이지를 반환했어요. 원문 연결 상태를 확인해 주세요.":"";}
}
