package com.clenova.app;

/** Wall-clock expiry; clock rollback fails closed. */
final class AgentSessionPolicy {
    static final long TTL_MS=24L*60*60*1000;
    static boolean expired(long last,long now){return last<=0||now<last||now-last>=TTL_MS;}
    static long remaining(long last,long now){return expired(last,now)?0:TTL_MS-(now-last);}
}
