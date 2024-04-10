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
  public static final String AWS_ACCESS_KEY = "";
  public static final String AWS_SECRET_KEY = "";
  public static final String AWS_SESSION_TOKEN = "";
  public static final String AWS_REGION = "us-west-2";
}
