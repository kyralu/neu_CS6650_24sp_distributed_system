import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisClient {

    private static final JedisPool pool;

    static {
        // Configure the pool options as needed
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(50); // Example configuration
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(2);
        // Initialize the JedisPool
        pool = new JedisPool(poolConfig, "localhost", 6379, 3000, null); // Adjust host, port, timeout, and password
    }

    public static JedisPool getPool() {
        return pool;
    }
}