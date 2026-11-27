package com.clenova.app;
import android.net.Uri;
import java.util.*;

/** Fail closed if any selected video is absent, protected, kept, or has changed metadata. */
final class VideoApproval {
    static final int MAX_BATCH=100;
    static List<Uri> verify(SmartMediaScan.Result before,SmartMediaScan.Result fresh,Set<String> selected,Set<String> keep){
        if(selected.isEmpty()||selected.size()>MAX_BATCH||!fresh.videosScanned)return Collections.emptyList();
        Map<String,SmartMediaScan.Item> current=new HashMap<>();for(SmartMediaScan.Item i:fresh.groups.get(SmartMediaScan.VIDEOS))current.put(i.uri.toString(),i);
        List<Uri> approved=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(SmartMediaScan.Item old:before.groups.get(SmartMediaScan.VIDEOS)){
            String id=old.uri.toString();if(!selected.contains(id))continue;
            SmartMediaScan.Item item=current.get(id);
            if(keep.contains(id)||old.protectedItem||item==null||item.protectedItem||old.generation<=0||item.generation!=old.generation
                ||old.modified<=0||old.modified!=item.modified||old.bytes!=item.bytes||old.duration!=item.duration
                ||old.width!=item.width||old.height!=item.height||!Objects.equals(old.path,item.path)
                ||!Objects.equals(old.name,item.name)||!Objects.equals(old.volume,item.volume)||!Objects.equals(old.mime,item.mime)||!seen.add(id))return Collections.emptyList();
            approved.add(item.uri);
        }
        return approved.size()==selected.size()?approved:Collections.emptyList();
    }
}
