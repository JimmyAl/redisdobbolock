package top.jimmy.redisdobbolock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisException;
import java.util.List;
import java.util.UUID;

public class DistributedLock {
    private final JedisPool jedisPool;

    public DistributedLock(JedisPool jedisPool){
        this.jedisPool = jedisPool;
    }


    /**
     * 加锁
     * @param lockName
     * @param acquireTimeOut
     * @param timeout
     * @return 锁标识,value值
     */
    public String addLock(String lockName,long acquireTimeOut,long timeout){
        Jedis conn = null;
        String retIdentifier = null;

        try {
            //获取连接
            conn = jedisPool.getResource();
            //随机生成一个value
            String identifier = UUID.randomUUID().toString();
            //锁名，key值
            String lockkey = "lock:" + lockName;
            //设置超时时间，超时自动释放锁
            int lockExpire = (int) (timeout / 1000);

            //获取锁的超时时间，超过这个时间就放弃获取锁
            long end = System.currentTimeMillis() + acquireTimeOut;
            while (System.currentTimeMillis() < end){
                if (conn.setnx(lockkey,identifier) == 1){
                    conn.expire(lockkey,lockExpire);
                    //返回value值，用于确定释放锁的时间
                    retIdentifier = identifier;
                    return retIdentifier;
                }

                if (conn.ttl(lockkey) == -1){
                    conn.expire(lockkey,lockExpire);
                }

                try {
                    Thread.sleep(10);
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                }

            }

        }catch (JedisException e){
            e.printStackTrace();
        }finally {
            if (conn != null){
                conn.close();
            }
        }
        return retIdentifier;
    }

    /**
     * 释放锁
     * @param lockName
     * @param identifier
     * @return
     */
    public boolean releaseLock(String lockName,String identifier){
        Jedis conn = null;
        String lockkey = "lock:" + lockName;
        boolean retFlag = false;
        try {
            //连接
            conn = jedisPool.getResource();
            while (true){
                //监视lock，准备开始事务
                conn.watch(lockkey);

                //通过前面返回的value值判断是不是该锁，若是，则删除，释放锁
                if (identifier.equals(conn.get(lockkey))){

                    Transaction transaction = conn.multi();//开始事务
                    transaction.del(lockkey);
                    List<Object> result = transaction.exec();
                    if (result == null){
                        continue;
                    }
                    retFlag = true;
                }

                conn.unwatch();
                break;
            }
        }catch (JedisException e){
            e.printStackTrace();
        }finally {
            if (conn != null){
                conn.close();
            }
        }
        return retFlag;
    }

}
