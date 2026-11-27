package com.clenova.app;
public final class AgentSessionPolicyTest{
    static void expect(boolean value){if(!value)throw new AssertionError("Session expiry assertion failed");}
    public static void main(String[] args){
        long last=1700000000000L,ttl=AgentSessionPolicy.TTL_MS;
        expect(!AgentSessionPolicy.expired(last,last));
        expect(!AgentSessionPolicy.expired(last,last+ttl-1));
        expect(AgentSessionPolicy.expired(last,last+ttl));
        expect(AgentSessionPolicy.expired(last,last+ttl+1));
        expect(AgentSessionPolicy.expired(0,last));
        expect(AgentSessionPolicy.expired(last,last-1));
        expect(AgentSessionPolicy.remaining(last,last+ttl-1)==1);
        expect(AgentSessionPolicy.remaining(last,last+ttl)==0);
        expect(AgentSessionPolicy.remaining(last,last)==ttl);
        System.out.println("Agent session expiry: 9 assertions passed");
    }
}
