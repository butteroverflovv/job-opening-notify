import kr.personal.historyjobs.core.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Process-local URL handler verifies request construction without changing production TLS behavior. */
public final class GoyangTransportTests {
    static int opened=0,posts=0,gets=0;
    static final class Connection extends HttpURLConnection {
        final ByteArrayOutputStream sent=new ByteArrayOutputStream();
        Connection(URL url){super(url);opened++;}
        public void connect(){} public void disconnect(){} public boolean usingProxy(){return false;}
        public OutputStream getOutputStream(){return sent;}
        public int getResponseCode(){
            if(url.getHost().equals("www.gyfl.hs.kr")){
                if(!getRequestMethod().equals("POST")||!getDoOutput()||url.getQuery()!=null||!getRequestProperty("Content-Type").startsWith("application/x-www-form-urlencoded"))throw new AssertionError("Expected POST to physical form endpoint");
                String encoded=sent.toString(StandardCharsets.UTF_8);Map<String,String> body=SchoolBoards.query("https://x.test/?"+encoded);
                if(!Set.of("list","view").contains(body.get("board"))||!body.get("boardtable").equals("alim")||body.containsKey("fname1")||!body.get("mode").isEmpty())throw new AssertionError("Unsafe POST body");
                if(body.get("board").equals("view")&&!body.get("no").equals("6807"))throw new AssertionError("Wrong record ID");posts++;
            }else{if(!getRequestMethod().equals("GET")||sent.size()!=0)throw new AssertionError("Other boards must stay GET");gets++;}
            return 200;
        }
        public Map<String,List<String>> getHeaderFields(){return Map.of();}
        public long getContentLengthLong(){return -1;}
        public String getContentType(){return "text/html; charset=UTF-8";}
        public InputStream getInputStream(){return new ByteArrayInputStream("<article>공개 공고 본문</article>".getBytes(StandardCharsets.UTF_8));}
    }
    public static void main(String[] args)throws Exception{
        URL.setURLStreamHandlerFactory(protocol->protocol.equals("https")?new URLStreamHandler(){protected URLConnection openConnection(URL u){return new Connection(u);}}:null);
        String list=GoyangTests.source().url,article=GoyangBoard.article(list,"6807");
        Net.get(list,10000,list);Net.get(GoyangBoard.page(list,2),10000,list);var response=Net.getHtml(article,10000,list);
        if(!response.url().equals(article)||!response.text().contains("공개 공고 본문"))throw new AssertionError("Logical article URL or response lost");
        Net.get("https://other.test/list?page=2",10000,"");int before=opened;
        try{Net.get(list+"&board=exec",10000,list);throw new AssertionError("Unsafe action reached transport");}catch(IOException expected){}
        if(opened!=before||posts!=3||gets!=1)throw new AssertionError("Unexpected request routing");
        System.out.println("PASS Goyang list/page/article use read-only POST; other boards use GET; mutation blocked before networking");
    }
}
