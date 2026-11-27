package com.clenova.app;
import android.net.Uri;
import java.util.*;

/** Screenshot approval requires fresh eligibility, unchanged MediaStore identity metadata and manual choice. */
final class ScreenshotApproval {
    static final int MAX_BATCH=100;
    static List<Uri> verify(SmartMediaScan.Result before,SmartMediaScan.Result fresh,Set<String> selected,Set<String> keep){
        if(selected.isEmpty()||selected.size()>MAX_BATCH||!fresh.imagesScanned)return Collections.emptyList();
        Map<String,SmartMediaScan.Item> current=new HashMap<>();
        for(SmartMediaScan.Item i:fresh.groups.get(SmartMediaScan.SCREENSHOTS))current.put(i.uri.toString(),i);
        List<Uri> result=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(SmartMediaScan.Item old:before.groups.get(SmartMediaScan.SCREENSHOTS)){
            String id=old.uri.toString();if(!selected.contains(id))continue;
            SmartMediaScan.Item item=current.get(id);
            if(keep.contains(id)||old.protectedItem||item==null||item.protectedItem||!old.image||!item.image
                ||old.generation<=0||old.generation!=item.generation||old.modified<=0||old.modified!=item.modified
                ||old.bytes!=item.bytes||old.width!=item.width||old.height!=item.height
                ||!Objects.equals(old.name,item.name)||!Objects.equals(old.path,item.path)
                ||!Objects.equals(old.volume,item.volume)||!Objects.equals(old.mime,item.mime)||!seen.add(id))return Collections.emptyList();
            result.add(item.uri);
        }
        return result.size()==selected.size()?result:Collections.emptyList();
    }
}
