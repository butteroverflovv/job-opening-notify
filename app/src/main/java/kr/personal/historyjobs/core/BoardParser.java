package kr.personal.historyjobs.core;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import static kr.personal.historyjobs.core.Model.*;

public final class BoardParser {
    private BoardParser() {}
    public record Listing(List<Link> items,List<String> pages,boolean empty,String warning) {}
    public record Detail(String title,String body,List<Link> attachments,List<String> frames) {}
    private static final Pattern ID=Pattern.compile("(?:q_rcrtSn|pbancSn|nttSn|articleNo|boardSeq|seq|num|no)\\s*[=:]\\s*['\"]?(\\d+)",Pattern.CASE_INSENSITIVE);
    private static final Pattern QUOTED=Pattern.compile("['\"](\\d{2,})['\"]");
    public static Listing listing(Source source,String page,String html) {
        Html.Node root=Html.parse(html);Map<String,Link> links=new LinkedHashMap<>();LinkedHashSet<String> pages=new LinkedHashSet<>();
        String security=SchoolBoards.securityMessage(root);if(!security.isEmpty())return new Listing(List.of(),List.of(),false,security);
        if(kind(source).equals("SCHOOL_SEN")){String request=SchoolBoards.seoulRequest(page,html,root,false);if(!request.isEmpty())return new Listing(List.of(),List.of(request),false,"");}
        for(Html.Node n:root.all()){
            if(SchoolBoards.insideScript(n))continue;
            String raw=n.attr("href");if(raw.trim().isEmpty())raw=n.attr("data-url");if(raw.trim().isEmpty())raw=n.attr("data-href");
            String action=n.attr("onclick");String text=n.text().replaceAll("\\s+"," ").trim();
            if(text.length()>2000)continue;
            String schoolUrl=SchoolBoards.recordUrl(source,page,n);String url=schoolUrl.isEmpty()?Html.resolve(page,raw):schoolUrl;
            String handler=(raw+" "+action).replace("&amp;","&");
            if(schoolUrl.isEmpty()&&(url.trim().isEmpty()||raw.startsWith("javascript:")))url=fromAction(source,page,handler);
            if(!url.trim().isEmpty()&&(Html.sameHost(source.url,url)||Html.sameHost(page,url))&&isDetail(source,url)&&!text.trim().isEmpty()){
                Link old=links.get(url);
                if(old==null||old.title().equals("상세보기")||old.title().equals("보기")){
                    String title=text;
                    for(Html.Node child:n.all()){
                        if(child.attr("class").matches(".*(?:list_title|cont_tit|board_title|subject).*" )&&!child.text().trim().isEmpty()){
                            title=child.text().replaceAll("^(?:마감임박|접수중|NEW)\\s*", "").trim();
                            String school=Classifier.school(text);if(!school.isEmpty()&&!title.contains(school))title=school+" "+title;
                            break;
                        }
                    }
                    if(text.equals("상세보기")||text.equals("보기")){
                        Html.Node box=n.parent;
                        for(int k=0;k<4&&box!=null;k++,box=box.parent){String t=box.text();if(t.contains("채용")&&t.length()<1800){title=t.replaceAll("\\s+"," ");break;}}
                    }
                    links.put(url,new Link(title,url,n.text(),Selection.publishedIn(n),Selection.pinned(n)));
                }
            } else if(!url.trim().isEmpty()&&Html.sameHost(page,url)&&!url.equals(page)&&isPage(raw,text))pages.add(url);
            if(handler.matches("(?is).*(?:page|paging|goPage|movePage|searchPage).*")){
                Matcher m=Pattern.compile("(?i)(?:goPaging|opMovePage|fnPage|goPage|movePage|searchPage|page|paging)[\\w]*\\s*\\(\\s*['\"]?(\\d{1,3})").matcher(handler);
                if(m.find()){int p=Integer.parseInt(m.group(1));if(p>0&&p<=30){String key=SchoolBoards.pageKey(source);pages.add(query(page,key,String.valueOf(p)));}}
            }
        }
        if(kind(source).equals("TOKYO")){
            for(Link link:TokyoBoard.links(page,html,source.schoolHint))links.putIfAbsent(link.url(),link);
            for(Html.Node node:root.all()){String u=Html.resolve(page,node.attr("href"));if(u.matches(".*front_notice\\.brd/0\\d+\\.\\.[^/?]+(?:\\?.*)?"))pages.add(u);}
        }
        String body=root.text();boolean empty=body.matches("(?s).*(?:등록된\\s*(?:게시물|공고|글|자료)(?:이|가)?\\s*없|검색(?:된|결과| 결과).*없|조회된.*없|검색건수\\s*0\\s*건).*" );
        if(body.contains("사용제약에 따른 안내"))return new Listing(List.of(),List.of(),false,"사이트에서 사용제약 안내를 반환했어요. 원문 주소·접근 상태를 확인해 주세요.");
        String warn=links.isEmpty()&&!empty?"공고 링크를 해석하지 못했어요. 게시판 구조 또는 접근 제한을 확인해 주세요.":"";
        return new Listing(new ArrayList<>(links.values()),new ArrayList<>(pages),empty,warn);
    }
    public static String kind(Source s){
        if((s.kind.equals("AUTO")||s.kind.equals("GENERIC"))&&SchoolBoards.ntt(s.url))return "SCHOOL_NTT";
        if((s.kind.equals("AUTO")||s.kind.equals("GENERIC"))&&SchoolBoards.senMenu(s.url))return "SCHOOL_SEN";
        if(!s.kind.equals("AUTO"))return s.kind;
        if(s.url.contains("work.sen.go.kr"))return "SEOUL";
        if(s.url.contains("goe.go.kr/recruit"))return "GYEONGGI";
        if(s.url.contains("tokos.ed.jp"))return "TOKYO";
        return "GENERIC";
    }
    private static boolean isDetail(Source s,String url){
        return switch(kind(s)){
            case "SCHOOL_NTT" -> SchoolBoards.ntt(url)&&url.contains("selectNttInfo.do")&&SchoolBoards.query(url).getOrDefault("nttSn","").matches("[0-9]{1,20}");
            case "SCHOOL_SEN" -> SchoolBoards.query(url).getOrDefault("nttId","").matches("[0-9]{1,20}")&&(url.contains("/subMenu.do?")||url.contains("/dggb/module/board/selectBoardDetailAjax.do?"));
            case "SEOUL" -> url.contains("BD_selectRecDetail")&&url.contains("q_rcrtSn=");
            case "GYEONGGI" -> url.contains("hnfpPbancInfoView")&&url.contains("pbancSn=");
            case "TOKYO" -> url.contains("front_notice.brd")&&url.matches(".*[?&](?:num|no|seq|id|nttSn)=.*") || url.matches(".*[?&](?:articleNo|nttSn)=.*");
            default -> url.matches("(?i).*(?:[?&](?:nttSn|articleNo|boardSeq|seq|num|no|id|idx|wr_id)=|/view[/.?]|/read[/.?]|selectNttInfo|mode=view|view.do|View.do).*" )&&!isAttachment(url);
        };
    }
    private static boolean isPage(String raw,String title){return raw.matches(".*[?&](?:page|pageIndex|q_currPage|pageNum|pageNo)=\\d+.*")||title.matches("(?:다음|다음 페이지|[2-9]|[12][0-9]|›|»)");}
    public static String fromAction(Source s,String base,String action){
        Matcher explicit=Pattern.compile("['\"]([^'\"]*(?:\\.do|\\.brd|\\.php|\\.html)[^'\"]*)['\"]").matcher(action);
        if(explicit.find()){String u=Html.resolve(base,explicit.group(1));if(!u.trim().isEmpty()&&isDetail(s,u))return u;}
        if(!action.matches("(?is).*(?:detail|view|read|info|select|fnView|goView).*"))return "";
        Matcher m=ID.matcher(action);String id=m.find()?m.group(1):"";
        if(id.trim().isEmpty()){Matcher q=QUOTED.matcher(action);if(q.find())id=q.group(1);}
        if(id.trim().isEmpty()){Matcher q=Pattern.compile("\\(\\s*(\\d{2,})\\s*[,)]").matcher(action);if(q.find())id=q.group(1);}
        if(id.trim().isEmpty())return "";
        return switch(kind(s)){
            case "SEOUL" -> "https://work.sen.go.kr/work/search/recInfo/BD_selectRecDetail.do?q_rcrtSn="+id;
            case "GYEONGGI" -> "https://www.goe.go.kr/recruit/ad/func/pb/hnfpPbancInfoView.do?mi=10502&pbancSn="+id;
            case "TOKYO" -> query(query(base,"mode","view"),"num",id);
            default -> "";
        };
    }
    public static Detail detail(String url,String html,String fallback){
        if(url.contains("tokos.ed.jp")){Detail legacy=TokyoBoard.detail(url,html,fallback);if(legacy!=null)return legacy;}
        Html.Node root=Html.parse(html),chosen=null;
        int best=-99999;
        for(Html.Node n:root.all()){
            if(!Set.of("article","section","div","td","main","body","form").contains(n.tag))continue;
            String token=(n.attr("class")+" "+n.attr("id")).toLowerCase(Locale.ROOT);
            int score=0;
            if(n.attr("class").contains("recruit_sub_wrap"))score+=500;
            if(n.attr("id").equals("pbancInfoForm"))score+=450;
            if(n.attr("id").equals("contents")&&token.contains("job-opening"))score+=400;
            if(token.matches(".*(?:board.?view|bbs.?view|view.?con|view.?cont|ntt.?cn|detail.?con|board.?con|recruit.?view).*"))score+=150;
            if(n.tag.equals("article"))score+=70;
            if(n.tag.equals("main"))score+=40;
            String txt=n.text();if(txt.length()<30)continue;
            if(txt.contains("채용")||txt.contains("모집"))score+=20;
            if(txt.contains("접수"))score+=15;
            score-=Math.min(50,txt.length()/3000);
            if(score>best){best=score;chosen=n;}
        }
        if(chosen==null)chosen=root;
        String title="";
        for(Html.Node n:root.all())if(n.attr("class").contains("job-opening-modal-tit-area__tit")){title=n.text();break;}
        for(Html.Node n:root.all())if(title.isEmpty()&&Set.of("h1","h2","h3","h4").contains(n.tag)&&!n.text().matches("채용공고|채용조건|상세요강|채용방법.*")&&n.text().length()<300&&(n.text().contains("채용")||n.text().contains("모집"))){title=n.text();break;}
        if(title.trim().isEmpty())title=fallback;
        LinkedHashMap<String,Link> files=new LinkedHashMap<>();List<String> frames=new ArrayList<>();
        for(Html.Node n:root.all()){
            if(n.tag.equals("iframe")){String u=Html.resolve(url,n.attr("src"));if(Html.sameHost(url,u))frames.add(u);}
            String u=Html.resolve(url,n.attr("href"));String txt=n.text();
            if(isAttachment(n.attr("title")))txt=n.attr("title");
            Matcher preview=Pattern.compile("previewAjax\\(\\s*['\"]([^'\"]+)['\"]\\s*,\\s*['\"]([^'\"]+)['\"]").matcher(n.attr("onclick"));
            if(preview.find()){String fileUrl=Html.resolve(url,preview.group(1));if(!fileUrl.isEmpty()&&isAttachment(fileUrl))files.put(fileUrl,new Link(preview.group(2),fileUrl));}
            if(!u.trim().isEmpty()&&(isAttachment(u)||isAttachment(txt))){if(files.size()<10)files.put(u,new Link(txt.trim().isEmpty()?"첨부파일":txt,u));}
        }
        String body=chosen.text();if(body.length()>120_000)body=body.substring(0,120_000);
        return new Detail(title,body,new ArrayList<>(files.values()),frames);
    }
    public static boolean isAttachment(String s){return s.matches("(?is).*(?:\\.(?:hwpx?|pdf)(?:$|[?&#\\s])|fileDown|download|downFile|file/download).*" );}
    public static String query(String url,String key,String value){
        String[] a=url.split("\\?",2);List<String> fields=new ArrayList<>();
        if(a.length>1)for(String f:a[1].split("&"))if(!f.split("=",2)[0].equals(key))fields.add(f);
        try { fields.add(key+"="+URLEncoder.encode(value,"UTF-8")); } catch(java.io.UnsupportedEncodingException e) { throw new IllegalStateException(e); }return a[0]+"?"+String.join("&",fields);
    }
}
