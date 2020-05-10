package net.limework.skLimework.DoNotUse;

import org.json.JSONObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

public class Tester {
    public static void main(String[] args) throws InterruptedException {
        JedisPoolConfig jconfig = new JedisPoolConfig();
        jconfig.setMaxTotal(100);
        jconfig.setMaxIdle(100);
        jconfig.setMinIdle(1);
        JedisPool d = new JedisPool(jconfig, "192.168.0.112", 6379, 400, "yHy0d2zdBlRmaSPj3CiBwEv5V3XxBTLTrCsGW7ntBnzhfxPxXJS6Q1aTtR6DSfAtCZr2VxWnsungXHTcF94a4bsWEpGAvjL6XMU");
        Jedis dd = d.getResource();
        JSONObject J = new JSONObject();
        J.put("Message", "something::something::something");
        int x = 0;
        while (true) {
            x++;
            System.out.println(x);
            dd.publish("fs", J.toString() );
            if (x == 1000) break;
            Thread.sleep(1000);

        }
    }
}
