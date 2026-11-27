package com.clenova.app;

import java.text.Normalizer;
import java.util.Locale;

/** Offline, accent-tolerant, multi-keyword FAQ matching. */
final class FaqSearch {
    private static String normalize(String text){
        return Normalizer.normalize(text,Normalizer.Form.NFD).toLowerCase(Locale.ROOT)
            .replaceAll("[\\u0300-\\u036f]","").trim();
    }
    static boolean matches(String query,String text){
        String q=normalize(query),haystack=normalize(text);
        if(q.isEmpty())return true;
        for(String term:q.split("\\s+"))if(!haystack.contains(term))return false;
        return true;
    }
}
