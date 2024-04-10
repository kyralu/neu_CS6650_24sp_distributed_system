import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class SkiersCache {
  private static SkiersCache instance;
  private final Jedis jedis;
  private final JedisPool jedisPool;

  SkiersCache() {
    try {
      jedis = new Jedis(Constants.REDIS_HOST, Constants.REDIS_PORT);
      jedisPool = new JedisPool();
    }catch (Exception e){
      throw new RuntimeException(Constants.CACHE_CONNECTION_ERROR_MESSAGE);
    }
  }

  public static synchronized SkiersCache getInstance() {
    if (instance == null) {
      instance = new SkiersCache();
    }
    return instance;
  }

  private String getCacheKey(int resortID, String seasonID, String dayID) {
    return resortID + seasonID + dayID;
  }

    public int getNoOfSkiers(int resortID, String seasonID, String dayID) {
    int count = -1;
    String cacheKey = getCacheKey(resortID,seasonID,dayID);

    try (Jedis jedis = jedisPool.getResource()) {
      String cachedValue = jedis.get(cacheKey);
      if (cachedValue != null) {
        count = Integer.parseInt(cachedValue);
      }
    } catch (Exception e) {
      throw new RuntimeException(Constants.CACHE_FETCH_ERROR_MESSAGE);
    }
    return count;
  }

  public synchronized void cacheResult(int resortID, String seasonID, String dayID,int value) {
    try (Jedis jedis = jedisPool.getResource()) {
      String cacheKey = getCacheKey(resortID, seasonID, dayID);
      jedis.set(cacheKey, String.valueOf(value));
    }catch (Exception e){
      throw new RuntimeException(Constants.ERROR_INSERT_CACHE);
    }
  }
}
