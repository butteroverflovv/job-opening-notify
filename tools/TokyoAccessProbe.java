import kr.personal.historyjobs.core.*;
import static kr.personal.historyjobs.core.Model.*;
import java.net.*;import java.util.*;
public class TokyoAccessProbe {
 static Source s=Model.defaults().get(2);
 static void check(String name,boolean pages)throws Exception{
  try{var r=Net.get(s.url,4_000_000,pages?"http://www.tokos.ed.jp/":s.url);var h=r.text();var l=BoardParser.listing(s,r.url(),h);
   System.out.println(name+" bytes="+r.bytes().length+" url="+r.url()+" listed="+l.items().size()+" warning="+l.warning());
   if(pages&&!l.pages().isEmpty()){Thread.sleep(1500);String next=l.pages().stream().filter(x->x.contains("/02..")).findFirst().orElse(l.pages().get(0));var n=Net.get(next,4_000_000,r.url());var p=BoardParser.listing(s,n.url(),n.text());System.out.println(name+" page2="+p.items().size()+" warning="+p.warning());}
  }catch(Exception e){System.out.println(name+" error="+e.getClass().getSimpleName()+": "+e.getMessage());}
 }
 public static void main(String[] args)throws Exception{
  check("direct",false);Thread.sleep(1500);
  CookieHandler.setDefault(new CookieManager(null,CookiePolicy.ACCEPT_ORIGINAL_SERVER));
  try{var r=Net.get("http://www.tokos.ed.jp/",4_000_000,"");System.out.println("homepage bytes="+r.bytes().length);}catch(Exception e){System.out.println("homepage "+e.getMessage());}
  Thread.sleep(1500);check("session",true);
 }
}
