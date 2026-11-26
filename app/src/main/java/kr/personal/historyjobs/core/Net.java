package kr.personal.historyjobs.core;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

public final class Net {
    private Net() {}
    public record Response(byte[] bytes,String mime,String url) {
        public String text(){
            String hint=mime+" "+new String(bytes,0,Math.min(bytes.length,4096),StandardCharsets.ISO_8859_1);
            Matcher m=Pattern.compile("charset\\s*=\\s*['\"]?([A-Za-z0-9_-]+)",Pattern.CASE_INSENSITIVE).matcher(hint);
            Charset c=StandardCharsets.UTF_8;if(m.find())try{c=Charset.forName(m.group(1));}catch(Exception ignored){}
            return new String(bytes,c);
        }
    }
    public static Response get(String input,int max,String referer)throws IOException{
        return get(input,max,referer,false);
    }
    public static Response getHtml(String input,int max,String referer)throws IOException{return get(input,max,referer,true);}
    private static Response get(String input,int max,String referer,boolean htmlOnly)throws IOException{
        String url=input;
        for(int redirect=0;redirect<6;redirect++){
            if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
            if(htmlOnly&&ArticleReader.isFileUrl(url))throw new IOException("첨부파일은 가져오지 않아요");
            URI u;try{u=URI.create(url);}catch(Exception e){throw new IOException("주소 형식 오류");}
            if(!Set.of("https","http").contains(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null)throw new IOException("지원하지 않는 주소");
            HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
            c.setConnectTimeout(15_000);c.setReadTimeout(20_000);c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36 HistoryJobs/0.1");
            c.setRequestProperty("Accept",htmlOnly?"text/html,application/xhtml+xml,text/plain;q=0.9":"text/html,application/xhtml+xml,application/pdf,*/*;q=0.8");
            if(referer!=null&&!referer.trim().isEmpty())c.setRequestProperty("Referer",referer);
            try{
                int status=c.getResponseCode();
                if(status>=300&&status<400){String next=Html.resolve(url,c.getHeaderField("Location")==null?"":c.getHeaderField("Location"));if(next.trim().isEmpty()||next.equals(url))throw new IOException("리다이렉트 오류");url=next;continue;}
                if(status<200||status>=300)throw new IOException("HTTP "+status+" · 사이트 접속 실패");
                if(htmlOnly&&!ArticleReader.htmlType(Objects.toString(c.getContentType(),"")))throw new IOException("본문 HTML 페이지가 아니에요. 첨부파일은 가져오지 않아요");
                if(c.getContentLengthLong()>max)throw new IOException("다운로드 크기 제한 초과");
                byte[] bytes;try(InputStream stream=c.getInputStream()){bytes=read(stream,max);}
                return new Response(bytes,Objects.toString(c.getContentType(),""),url);
            }finally{c.disconnect();}
        }
        throw new IOException("리다이렉트 횟수 초과");
    }
    public static byte[] read(InputStream in,int max)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;
        while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();if(out.size()+n>max)throw new IOException("파일 크기 제한 초과");out.write(b,0,n);}return out.toByteArray();
    }
}
