import java.util.Map;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

public class DatabaseHandler {
  private static DatabaseHandler instance;
  private static DynamoDbClient dynamoDbClient;

  private DatabaseHandler() {
    try {
      AwsSessionCredentials awsCreds = AwsSessionCredentials.create(Constants.AWS_ACCESS_KEY, Constants.AWS_SECRET_KEY,
          Constants.AWS_SESSION_TOKEN);

      dynamoDbClient = DynamoDbClient.builder()
          .region(Region.of(Constants.AWS_REGION))
          .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
          .build();

    } catch (Exception e) {
      throw new RuntimeException(Constants.DB_CONNECTION_ERROR_MESSAGE, e);
    }
  }

  public static synchronized DatabaseHandler getInstance() {
    if (instance == null) {
      instance = new DatabaseHandler();
    }
    return instance;
  }

  public int getUniqueSkiers(int resortID, String seasonID, String dayID) {
    int count;
    try {
      ScanRequest scanRequest = ScanRequest.builder()
          .tableName(Constants.TABLE_NAME)
          .filterExpression("resortID = :resortId AND seasonID = :seasonId AND dayID = :dayId")
          .expressionAttributeValues(Map.of(":resortId", AttributeValue.builder().n(String.valueOf(resortID)).build(),
              ":seasonId",AttributeValue.builder().n(seasonID).build(),
              ":dayId", AttributeValue.builder().s(dayID).build()
          ))
          .build();

      ScanResponse response = dynamoDbClient.scan(scanRequest);
      count = response.count();
      System.out.println("NumOfSkiers: "+count);
    } catch (DynamoDbException e) {
      throw new RuntimeException("Error fetching skiers: " + e);
    }
    return count;
  }
}
