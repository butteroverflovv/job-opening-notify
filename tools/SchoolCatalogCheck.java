import kr.personal.historyjobs.core.*;
import static kr.personal.historyjobs.core.Model.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public final class SchoolCatalogCheck {
    public static void main(String[] args)throws Exception{
        Path out=Path.of("catalog-live");Files.createDirectories(out);ExecutorService pool=Executors.newFixedThreadPool(4);List<Future<String>> results=new ArrayList<>();
        for(Source source:SchoolCatalog.defaults())results.add(pool.submit(()->{
            String summary=source.id+" kind="+BoardParser.kind(source)+" keywords="+source.keywords;
            try{
                var response=Net.get(source.url,4_000_000,source.url);Files.writeString(out.resolve(source.id+"-shell.html"),response.text());
                var listing=BoardParser.listing(source,response.url(),response.text());
                if(listing.items().isEmpty()&&!listing.pages().isEmpty()){response=Net.get(listing.pages().get(0),4_000_000,source.url);Files.writeString(out.resolve(source.id+"-list.html"),response.text());listing=BoardParser.listing(source,response.url(),response.text());}
                long dated=listing.items().stream().filter(l->!l.published().isEmpty()).count();
                summary+=" listed="+listing.items().size()+" dated="+dated+" warning="+listing.warning();
                List<Link> entries=listing.items();var target=entries.stream().filter(l->Classifier.matches(source,l.title(),l.duty())&&Selection.academicYearMatches(l.title(),source.url,LocalDate.now(Classifier.KST))).findFirst().or(()->entries.stream().findFirst());
                if(target.isPresent())try{String body=new ArticleReader(Net::getHtml).read(target.get().url()).body();summary+=" body="+body.length();Files.writeString(out.resolve(source.id+"-body.txt"),body);}catch(Exception e){summary+=" bodyError="+e.getMessage();}
                if(!listing.warning().isEmpty()||dated!=listing.items().size())summary+=" METADATA_FAILURE";
                else {List<String> calls=new ArrayList<>();var result=new Collector((url,max,ref)->{calls.add(url);return Net.get(url,max,ref);}).collect(source,Map.of(),System.currentTimeMillis());summary+=" collected="+result.checked()+" pages="+calls.size()+" partial="+result.partial()+" errors="+result.errors();}
            }catch(Exception e){summary+=" ERROR="+e.getClass().getSimpleName()+": "+e.getMessage();}
            Files.writeString(out.resolve(source.id+"-summary.txt"),summary);System.out.println(summary);return summary;
        }));
        pool.shutdown();for(Future<String> result:results)result.get();
    }
}
