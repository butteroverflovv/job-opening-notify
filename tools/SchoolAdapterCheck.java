import kr.personal.historyjobs.core.*;
import java.nio.file.*;import java.util.*;
public class SchoolAdapterCheck {
 public static void main(String[] args)throws Exception{
  var g=Model.source("gsh","경기과고","https://gs-h.goesw.kr/gs-h/na/ntt/selectNttList.do?mi=1275&bbsId=1088","AUTO","경기","");g.keywords=List.of("교사");
  var s=Model.source("sshs","서울과고","https://sshs.sen.hs.kr/75052/subMenu.do","AUTO","서울","");s.keywords=List.of("교원");
  for(var src:List.of(g,s)){
   Thread.sleep(800);var first=Net.get(src.url,4_000_000,src.url);Files.createDirectories(Path.of("science-diagnostics"));Files.writeString(Path.of("science-diagnostics",src.id+"-app-shell.html"),first.text());System.out.println(src.name+" firstURL="+first.url()+" bytes="+first.bytes().length);var list=BoardParser.listing(src,first.url(),first.text());
   if(list.items().isEmpty()&&!list.pages().isEmpty()){Thread.sleep(800);var ajax=Net.get(list.pages().get(0),4_000_000,src.url);Files.writeString(Path.of("science-diagnostics",src.id+"-app-ajax.html"),ajax.text());System.out.println(src.name+" ajaxURL="+ajax.url()+" bytes="+ajax.bytes().length);list=BoardParser.listing(src,ajax.url(),ajax.text());}
   long dated=list.items().stream().filter(l->!l.published().isEmpty()).count(),matched=list.items().stream().filter(l->Classifier.titleMatches(src,l.title())).count();
   System.out.println(src.name+" kind="+BoardParser.kind(src)+" listed="+list.items().size()+" dated="+dated+" keywordMatches="+matched+" warning="+list.warning());
   if(list.items().isEmpty()||dated!=list.items().size()||!list.warning().isEmpty())throw new AssertionError(src.name+" list metadata");
   var target=list.items().stream().filter(l->Classifier.titleMatches(src,l.title())).findFirst();if(target.isEmpty())throw new AssertionError("Expected sample recruitment title not found");
   try{var body=new ArticleReader(Net::getHtml).read(target.get().url());System.out.println(src.name+" bodyCharacters="+body.body().length());Files.createDirectories(Path.of("science-diagnostics"));Files.writeString(Path.of("science-diagnostics",src.id+"-app-body.txt"),body.body());}catch(Exception e){System.out.println(src.name+" bodyError="+e.getMessage());throw e;}
  }
 }
}
