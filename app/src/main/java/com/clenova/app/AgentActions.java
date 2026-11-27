package com.clenova.app;
import java.util.Locale;

/** Local, explicitly bounded routing; no AI or arbitrary action execution. */
final class AgentActions {
    static final String[] TITLES={"Find space to recover","Review duplicate photos","Review unused apps","Review file protection","Review downloads","Review large videos","Review screenshots","Review similar photos","Review large files","Review unnecessary files"};
    static final String[] DESCRIPTIONS={
        "Check accessible photos and videos and show cleanup candidates.","Find exact duplicate photos and keep at least one copy.","Check available Android usage history and review rarely used apps.","Open protection settings and choose what stays protected.","Review files you select in Android Files.","Find accessible large videos for review.","Find accessible screenshots for review.","Find visually similar photos for manual review.","Check selected files and folders for large files.","Review selected temporary-looking files and old installers."};
    static final String[] BUTTONS={"Start Smart Scan","Review duplicates","Review apps","Open protection","Open Downloads","Review videos","Review screenshots","Review similar photos","Open Large files","Open Unnecessary files"};
    static int route(String request){
        String q=request.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," ");
        // Reject ambiguous multi-action requests and arbitrary instructions. Always show plan before routing.
        if(q.contains(" and ")||q.contains(";")||q.contains(" except ")||q.contains(" without ")||q.contains("don't ")||q.contains("do not ")||q.contains(" not "))return -1;
        if(q.matches("(review|find|show)( my)? (exact )?duplicate (photos|pictures|images)"))return 1;
        if(q.matches("(find|review|show)( my)? (unused apps|apps i rarely use|rarely used apps)"))return 2;
        if(q.matches("(protect|review|manage)( my)? (files|photos|file protection|protection settings)"))return 3;
        if(q.matches("(review|show|organize)( my)? downloads"))return 4;
        if(q.matches("(review|find|show)( my)? large videos"))return 5;
        if(q.matches("(review|find|show)( my)? screenshots"))return 6;
        if(q.matches("(review|find|show)( my)? similar photos"))return 7;
        if(q.matches("(review|find|show)( my)? large files"))return 8;
        if(q.matches("(review|find|show)( my)? (unnecessary files|temporary files|junk files)"))return 9;
        if(q.matches("(help me )?(free up( [0-9]+(\\.[0-9]+)? (gb|mb))?( space| storage)?|find space to recover|scan( my)? (phone|storage)|clean( my)? phone)"))return 0;
        if(q.matches("(mera |mere )?(phone|mobile)( ko| ka)? (clean|saaf) (kar do|karo|karde)")||q.matches("(storage|space) (khali|free|clear) (kar do|karo|karde)")||q.matches("(फोन|मोबाइल) (साफ|क्लीन) (करो|कर दो)")||q.matches("स्टोरेज खाली (करो|कर दो)"))return 0;
        if(q.matches("duplicate (photos|photo) (dikhao|dikhado|dhundo)")||q.equals("डुप्लिकेट फोटो दिखाओ"))return 1;
        if(q.matches("unused apps (dikhao|dikhado|dhundo)")||q.equals("कम इस्तेमाल वाले ऐप दिखाओ"))return 2;
        return -1;
    }
}
