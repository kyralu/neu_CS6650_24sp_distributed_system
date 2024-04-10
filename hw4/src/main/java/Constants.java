public class Constants {
  public static final String MSG_METHOD_NOT_ALLOWED = "POST Method not allowed. Use GET method instead.";
  public static final String MSG_DATA_NOT_FOUND = "Resort not found";
  public static final String MSG_INVALID_INPUTS = "Invalid Inputs Provided";
  public static final String ERROR_INSERT_CACHE = "Could not insert data into Cache.";
  public static final String SERVLET_EST = "Servlet initialized.";
  public static final String DATABASE_CON_EST = "Database Connection established.";
  public static final String CACHE_EST = "CACHE Connection established.";
  public static final String DB_CONNECTION_ERROR_MESSAGE = "Error connecting to DynamoDB";
  public static final String CACHE_CONNECTION_ERROR_MESSAGE = "Error connecting to Redis Cache";
  public static final String CACHE_FETCH_ERROR_MESSAGE = "Could not extract data from Redis";
  public static final String COMMON_ERROR_MESSAGE = "Something went wrong.";
  public static final String UNIQUE_SKIERS_URL_PATTERN = "/\\d+/seasons/\\d+/day/\\d+/skiers";

  // Cache Redis
  public static final String REDIS_HOST = "localhost";
  public static final Integer REDIS_PORT = (Integer) 6379;

  // AWS DynamoDB
  public static final String TABLE_NAME = "SkierLiftRideData";
  public static final String AWS_ACCESS_KEY = "ASIA2UC3BCS5TSUKM43Y";
  public static final String AWS_SECRET_KEY = "g/FRGzh2m/qf2JWkfDu2khj1NZ4KZMhmb9x73w3l";
  public static final String AWS_SESSION_TOKEN = "IQoJb3JpZ2luX2VjEND//////////wEaCXVzLXdlc3QtMiJHMEUCIQDNp2N1055SViKVSJFmQS/KeL2JRCQ9KtlRCtO4Rjq4LwIgAzhrh0+P5TCNHvoT5goIPk280Z1LBbXxuc3FmelCybkquwII+f//////////ARAAGgw3MzAzMzUzNTE5OTUiDKieyK+txMVBjHgzNyqPAnN00bkrMu4SethmPzEcspIsypsL6md9AgRKt5vzp8dfX66lwyEbN14lel0AloOEGHrZe4APPku92f1xk8mIn0ihiNc+kc72vGOsqjO3diUDmh037Esm9KDTq82qWsUf80X+bWQQHItDcNDIxJnYlPhTHt5QSQMOZ4aHlP6VulvwWzoGea71DXSmQ4aPx4LnlcYNe8MCpQhytMyEWjuQ5b67Uw0DqST9cA8nE1GenwqVPORBF1eave3uD5sbhH0frCBeQKtGD77xKB2nuluG/qFggy5oSveYjEgwgq5OfO4JNSZFR2UUpNgc4AlF9KBibtZcSnSfpLrLw3rbmeBNkfugF4CIuVb6KjR2UqWumT8w8I7SsAY6nQF0OcuhkaApvcoZHhGhVmIbS5c4CsHAb+o2kMgqLFnSW5Zi4iFC6uhwdiepeW8w4eeGie7RpcEM6PpVsjxMG1qBzrIqmG+YzibzQC1WsGNpqgZodocw1RnBug+5wBDFjkATbyhM3JKolAmVWjyTRiej/ysoGAgqKbW/kXuRrXECCpO8cvqG0z4JpdZ8tXTTQp3b+9VU63EuOWjcjWdT";
  public static final String AWS_REGION = "us-west-2";
}
