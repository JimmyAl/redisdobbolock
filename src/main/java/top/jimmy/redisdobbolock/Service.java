package top.jimmy.redisdobbolock;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class Service {

    private static JedisPool pool = null;

    private DistributedLock lock = new DistributedLock(pool);

    int n = 500;

    static {
        JedisPoolConfig config = new JedisPoolConfig();
        //设置最大连接数
        config.setMaxTotal(200);
        //设置最大空闲数
        config.setMaxIdle(8);
        //设置最大等待时间
        config.setMaxWaitMillis(1000*100);
        //有效性检查
        config.setTestOnBorrow(true);

        pool = new JedisPool(config,"127.0.0.1",6379,3000);

    }

    public void seckill(){
        //返回锁的value值，供释放锁时进行判断
        String identifier = lock.addLock("resource",5000,1000);
        System.out.println(Thread.currentThread().getName() + "获得了锁");
        System.out.println(--n);
        //lock.releaseLock("resource",identifier);
    }
}
