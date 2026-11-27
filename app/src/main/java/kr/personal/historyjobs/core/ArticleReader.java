package kr.personal.historyjobs.core;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/** On-demand, text-only article reader. Never executes scripts or fetches linked files. */
public final class ArticleReader {
    public interface Transport {Net.Response get(String url,int limit,String referer)throws Exception;}
    public record Article(String body,String url) {}
    public record Part(String body,List<String> frames,boolean imageOnly) {}
    private final Transport net;
    public ArticleReader(Transport net){this.net=net;}
    public static boolean isFileUrl(String url){
        return url.trim().toLowerCase(Locale.ROOT).matches("(?s).*(?:\\.(?:hwpx?|pdf|docx?|xlsx?|pptx?|zip|rar|7z|txt|csv|png|jpe?g|gif|webp)(?:$|[?&#/])|filedown|download|downfile|file/download|docviewer|synap|/viewer|filepreview|previewajax).*" );
    }
    public static boolean htmlType(String mime){String m=mime.toLowerCase(Locale.ROOT).split(";",2)[0].trim();return Set.of("text/html","application/xhtml+xml","text/plain").contains(m);}
    public Article read(String input)throws Exception{
        String initial=Html.resolve(input,input);if(initial.isEmpty()||isFileUrl(initial))throw new IOException("본문 페이지 주소가 아니에요");
        ArrayDeque<String> queue=new ArrayDeque<>();queue.add(initial);Set<String> visited=new HashSet<>();LinkedHashSet<String> texts=new LinkedHashSet<>();boolean imageOnly=false;String lastUrl=initial;
        while(!queue.isEmpty()&&visited.size()<3){
            if(Thread.currentThread().isInterrupted())throw new java.io.InterruptedIOException();
            String url=queue.remove();if(!visited.add(url))continue;
            try{
                Net.Response r=net.get(url,4_000_000,initial);lastUrl=r.url();
                if(isFileUrl(lastUrl)||!htmlType(r.mime()))throw new IOException("첨부파일은 가져오지 않아요. 원문 페이지에서 확인해 주세요");
                String html=r.text();Part p;
                if(r.mime().toLowerCase(Locale.ROOT).startsWith("text/plain")&&!html.matches("(?s).*<[A-Za-z][^>]*>.*"))p=new Part(Html.clean(html),List.of(),false);
                else {String request=SchoolBoards.articleRequest(lastUrl,html);if(!request.isEmpty()&&!visited.contains(request)){queue.addFirst(request);continue;}p=parse(lastUrl,html);}
                if(!p.body().isEmpty())texts.add(p.body());imageOnly|=p.imageOnly();
                if(p.body().length()>=50)break;
                for(String frame:p.frames())if(Html.sameHost(lastUrl,frame)&&!visited.contains(frame))queue.add(frame);
            }catch(Exception e){if(visited.size()==1)throw e;}
        }
        String body=String.join("\n\n",texts);
        if(body.isEmpty()&&imageOnly)body="이 공고의 본문은 이미지로 되어 있어요. 원문에서 이미지를 확인해 주세요.";
        if(body.isEmpty())throw new IOException("본문 영역을 찾지 못했어요. 원문에서 확인해 주세요");
        return new Article(body.substring(0,Math.min(body.length(),120_000)),lastUrl);
    }
    public static Part parse(String url,String html)throws IOException{
        Html.Node root=Html.parse(html),chosen=null;int best=0;
        String whole=root.text();if(whole.contains("사용제약에 따른 안내")||whole.matches("(?is)^\\s*(?:access denied|forbidden).*"))throw new IOException("사이트에서 접근을 제한했어요. 원문 연결을 확인해 주세요");
        for(Html.Node n:root.all()){
            if(suppressed(n)||!Set.of("article","section","div","td","main","li").contains(n.tag))continue;
            String raw=(n.attr("id")+" "+n.attr("class")).toLowerCase(Locale.ROOT),token=raw.replaceAll("[-_\\s]","");int score=0;
            if(InternationalBoards.bodyCell(url,n))score=950;
            else if(GoyangBoard.bodyCell(url,n))score=940;
            else if(SchoolSites.aspBodyCell(url,n))score=920;
            else if(token.equals("bovcon"))score=900;
            else if(token.equals("tmpcontentsarea"))score=880;
            else if(token.contains("boardreadbody"))score=860;
            else if(SchoolBoards.seoulBodyCell(n))score=850;
            else if(token.contains("contextview"))score=800;
            else if(token.contains("bbsvcont")||token.contains("bbsvcn"))score=780;
            else if(token.contains("jobdetailareacon"))score=750;
            else if(raw.contains("recruit_cont_box")&&previousHeading(n).contains("상세요강"))score=750;
            else if(token.matches(".*(?:viewcont(?:ent)?|viewcon|boardcontent|nttcn|boardtxt|boardcn|bbscontent|bbscont|contentbody|contentview|readcontent|boardtext).*"))score=400;
            else if(token.matches(".*(?:boardview|bbsview|recruitview|recruitsubwrap).*"))score=120;
            else if(n.tag.equals("article"))score=90;
            else if(n.tag.equals("main"))score=40;
            if(score>best){chosen=n;best=score;}
        }
        List<String> frames=new ArrayList<>();Html.Node scope=chosen==null?root:chosen;
        for(Html.Node n:scope.all())if(n.tag.equals("iframe")&&!suppressed(n)){
            String u=Html.resolve(url,n.attr("src")),hint=(u+" "+n.attr("id")+" "+n.attr("class")).toLowerCase(Locale.ROOT);
            if(!u.isEmpty()&&Html.sameHost(url,u)&&!isFileUrl(u)&&!hint.matches(".*(?:preview|viewer|file|map|video|youtube|comment|reply).*"))frames.add(u);
        }
        if(chosen==null)return new Part("",frames,false);
        StringBuilder b=new StringBuilder();boolean[] image={false};append(chosen,b,image);
        String body=Html.clean(b.toString());return new Part(body.substring(0,Math.min(body.length(),120_000)),frames,body.isEmpty()&&image[0]);
    }
    private static String previousHeading(Html.Node n){if(n.parent==null)return "";List<Html.Node> siblings=n.parent.children;int pos=siblings.indexOf(n);for(int i=pos-1;i>=0;i--){Html.Node p=siblings.get(i);if(p.tag.equals("#text")&&p.text().isEmpty())continue;return p.text();}return "";}
    private static boolean suppressed(Html.Node n){for(Html.Node p=n;p!=null;p=p.parent)if(skip(p))return true;return false;}
    private static final Set<String> SKIP=Set.of("script","style","noscript","nav","header","footer","aside","button","input","select","textarea","object","embed","svg","canvas");
    private static final Pattern SKIP_CONTAINER=Pattern.compile("(?:^|\\s)(?:attachments?|attach(?:file|list|wrap|area|box)[\\w-]*|attach[_-][\\w-]*|file[_-]?(?:list|area|down|wrap|box|info|upload)[\\w-]*|download[\\w-]*|board[_-]?file[\\w-]*|bbs[_-]?file[\\w-]*|atchfile[\\w-]*|postfiles|job-detail-area__row|pagination|paging|related[\\w-]*|comment[\\w-]*|reply[\\w-]*|previouspost|nextpost)(?:\\s|$)");
    private static boolean skip(Html.Node n){
        if(n.attr("id").equals("hwpEditorBoardContent")||Arrays.asList(n.attr("class").split("\\s+")).contains("hwp_editor_board_content"))return true;
        if(SKIP.contains(n.tag)||n.attrs.containsKey("hidden")||n.attr("aria-hidden").equals("true")||n.attr("style").matches("(?is).*(?:display\\s*:\\s*none|visibility\\s*:\\s*hidden).*"))return true;
        String token=(n.attr("class")+" "+n.attr("id")).toLowerCase(Locale.ROOT);
        if(SKIP_CONTAINER.matcher(token).find())return true;
        if(n.tag.equals("a")&&(isFileUrl(n.attr("href"))||isFileUrl(n.attr("title"))||n.attr("onclick").matches("(?is).*(?:preview|download|filedown).*")))return true;
        return false;
    }
    private static void append(Html.Node n,StringBuilder b,boolean[] image){
        if(skip(n)||n.tag.equals("iframe"))return;
        if(n.tag.equals("#text")){b.append(n.raw);return;}
        if(n.tag.equals("img")){image[0]=true;String alt=n.attr("alt").trim();if(!alt.isEmpty())b.append("\n[이미지: ").append(alt).append("]\n");return;}
        boolean block=Set.of("p","div","tr","li","dt","dd","h1","h2","h3","h4","h5","br","section","article","table").contains(n.tag);
        if(block)b.append('\n');for(Html.Node c:n.children){append(c,b,image);if(c.tag.equals("td")||c.tag.equals("th"))b.append(" | ");}if(block)b.append('\n');
    }
}
