package kr.personal.historyjobs;

import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import kr.personal.historyjobs.core.*;
import static kr.personal.historyjobs.core.Model.*;

/** Local-only disposable optimization, separate from notices and user backups. */
public final class ListingCacheStore {
    private static ListingCacheStore instance;
    public static synchronized ListingCacheStore get(Context c){if(instance==null)instance=new ListingCacheStore(c.getApplicationContext());return instance;}
    private final AtomicFile file;
    private final LinkedHashMap<String,ListingCache.State> states=new LinkedHashMap<>();
    private ListingCacheStore(Context c){
        file=new AtomicFile(new File(c.getFilesDir(),"listing-cache.json"));
        if(file.getBaseFile().exists())try(InputStream in=file.openRead()){
            JSONObject root=new JSONObject(new String(Net.read(in,512_000),StandardCharsets.UTF_8));
            if(root.optInt("version")!=1)return;
            JSONArray rows=root.getJSONArray("sources");
            for(int i=0;i<Math.min(rows.length(),50);i++){
                JSONObject row=rows.getJSONObject(i);String id=row.getString("id"),config=row.getString("config");
                if(id.length()>200||!config.matches("[0-9a-f]{64}"))continue;
                List<String> records=new ArrayList<>();JSONArray values=row.getJSONArray("records");
                for(int j=0;j<Math.min(values.length(),ListingCache.MAX_RECORDS);j++)records.add(values.optString(j));
                states.put(id,new ListingCache.State(config,row.getLong("lastFull"),records));
            }
        }catch(Exception ignored){states.clear();}
    }
    public synchronized ListingCache.State state(String id){return states.get(id);}
    public synchronized void put(List<Source> current,String id,ListingCache.State state){
        Set<String> active=new HashSet<>();for(Source s:current)active.add(s.id);
        states.keySet().retainAll(active);if(active.contains(id)&&state!=null&&id.length()<=200&&(states.containsKey(id)||states.size()<50))states.put(id,state);save();
    }
    public synchronized void remove(String id){if(states.remove(id)!=null)save();}
    public synchronized void clear(){states.clear();file.delete();}
    private void save(){
        FileOutputStream out=null;
        try{
            JSONArray rows=new JSONArray();for(var entry:states.entrySet()){ListingCache.State state=entry.getValue();rows.put(new JSONObject().put("id",entry.getKey()).put("config",state.config()).put("lastFull",state.lastFull()).put("records",new JSONArray(state.records())));}
            byte[] data=new JSONObject().put("version",1).put("sources",rows).toString().getBytes(StandardCharsets.UTF_8);if(data.length>512_000)return;
            out=file.startWrite();out.write(data);file.finishWrite(out);
        }catch(Exception ignored){if(out!=null)file.failWrite(out);}
    }
}
