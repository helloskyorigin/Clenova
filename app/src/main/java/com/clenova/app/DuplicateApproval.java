package com.clenova.app;
import android.net.Uri;
import java.util.*;

final class DuplicateApproval {
    static final int MAX_BATCH=100;
    static List<Uri> verified(SmartMediaScan.Result before,SmartMediaScan.Result fresh,Set<String> selected){
        if(selected.isEmpty()||selected.size()>MAX_BATCH)return Collections.emptyList();
        Map<String,SmartMediaScan.DuplicateGroup> current=new HashMap<>();
        for(SmartMediaScan.DuplicateGroup g:fresh.duplicates)current.put(g.fingerprint,g);
        List<Uri> result=new ArrayList<>();
        Set<String> seen=new HashSet<>();
        for(SmartMediaScan.DuplicateGroup old:before.duplicates){
            SmartMediaScan.DuplicateGroup group=current.get(old.fingerprint);
            for(SmartMediaScan.Item item:old.items){
                String key=item.uri.toString();
                if(!selected.contains(key))continue;
                if(item.protectedItem||group==null)return Collections.emptyList();
                SmartMediaScan.Item candidate=null;boolean kept=false;
                for(SmartMediaScan.Item check:group.items){
                    String id=check.uri.toString();
                    if(id.equals(key))candidate=check;
                    if(!selected.contains(id)&&check.protectedItem)kept=true;
                }
                if(!kept||candidate==null||candidate.protectedItem||candidate.bytes!=item.bytes||item.generation<=0||item.generation!=candidate.generation||item.modified!=candidate.modified||!seen.add(key))return Collections.emptyList();
                result.add(candidate.uri);
            }
        }
        return result.size()==selected.size()?result:Collections.emptyList();
    }
}
