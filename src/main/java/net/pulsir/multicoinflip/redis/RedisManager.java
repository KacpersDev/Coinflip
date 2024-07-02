package net.pulsir.multicoinflip.redis;

import lombok.Getter;
import net.pulsir.multicoinflip.MultiCoinFlip;
import net.pulsir.multicoinflip.coinflip.CoinFlip;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import redis.clients.jedis.*;

import java.util.UUID;

@Getter
public class RedisManager {

    private final JedisPool jedisPool;

    public RedisManager(boolean auth){
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(64);
        jedisPoolConfig.setMaxTotal(64);

        if (!auth) {
            ClassLoader previous = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(Jedis.class.getClassLoader());
            this.jedisPool = new JedisPool(jedisPoolConfig,
                    MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getString("redis.host"),
                    MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getInt("redis.port"), Protocol.DEFAULT_TIMEOUT);
            Thread.currentThread().setContextClassLoader(previous);
        } else {
            ClassLoader previous = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(Jedis.class.getClassLoader());
            this.jedisPool = new JedisPool(jedisPoolConfig,
                    MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getString("redis.host"),
                    MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getInt("redis.port"),
                    Protocol.DEFAULT_TIMEOUT,
                    MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getString("redis.password"));
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    public Jedis getJedis(){
        return this.jedisPool.getResource();
    }

    public void subscribe(){
        Jedis subscriber = getJedis();
        subscriber.connect();

        new Thread("subscriber") {
            @Override
            public void run(){
                String[] channels = {"coinflip_inventory", "coinflip_inventory_delete"};
                subscriber.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        if (channel.equalsIgnoreCase("coinflip_inventory")) {
                            UUID player = UUID.fromString(message.split("<splitter>")[0]);
                            String name = message.split("<splitter>")[1];
                            double amount = Double.parseDouble(message.split("<splitter>")[2]);

                            MultiCoinFlip.getInstance().getCoinFlipManager().getCoinFlipMap()
                                    .put(player, new CoinFlip(name, null, player, null, amount));
                        } else if (channel.equalsIgnoreCase("coinflip_inventory_delete")) {
                            UUID player = UUID.fromString(message);
                            MultiCoinFlip.getInstance().getCoinFlipManager().getCoinFlipMap().remove(player);
                        }
                    }
                }, channels);
            }
        }.start();
    }

    public void publish(String channel, String message){
        try (Jedis publisher = getJedis()) {
            publisher.publish(channel, message);
        }
    }
}
