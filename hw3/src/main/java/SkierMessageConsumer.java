import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.HashMap;

public class SkierMessageConsumer {

    private final static String QUEUE_NAME = "skiersQueue";
    private final static String HOST = "ec2-52-12-161-184.us-west-2.compute.amazonaws.com";
    private static final ConcurrentHashMap<String, Integer> skierLiftRides = new ConcurrentHashMap<>();
    private static final int THREAD_POOL_SIZE = 4;
    private DynamoDbClient dynamoDb;
    private final String tableName = "SkierData"; // Change to DynamoDB table name
    private static final String AWS_ACCESS_KEY_ID="";
    private static final String AWS_SECRET_ACCESS_KEY="";
    private final String AWS_SESSION_TOKEN="";

    private static DynamoDbClient dynamoDbClient;

    public static void main(String[] argv) throws IOException, TimeoutException {
        // Initialize DynamoDB client
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY);

        dynamoDbClient = DynamoDbClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.US_WEST_2) // Change to the region
                .build();


        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);
        factory.setUsername("kyra");
        factory.setPassword("123");

        final Connection connection = factory.newConnection();
        final Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            Runnable task = () -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                processMessage(message);
            };
            executorService.submit(task);
        };

        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
    }

    private static void processMessage(String message) {
        Gson gson = new Gson();
        LiftRide liftRide = gson.fromJson(message, LiftRide.class);

        skierLiftRides.merge(liftRide.getSkierID(), liftRide.getVertical(), Integer::sum);
        System.out.println("Updated lift rides for skier: " + liftRide.getSkierID());

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("skierId", AttributeValue.builder().s(liftRide.getSkierID()).build());
        item.put("day", AttributeValue.builder().n("20230314").build()); // Example day, convert appropriately
        item.put("verticalTotals", AttributeValue.builder().n(String.valueOf(liftRide.getVertical())).build());
        // Add more attributes as needed

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName("SkierData")
                .item(item)
                .build());
    }

    public static class LiftRide {
        private String skierID;
        private int vertical;

        // Getters and setters
        public String getSkierID() {
            return skierID;
        }

        public void setSkierID(String skierID) {
            this.skierID = skierID;
        }

        public int getVertical() {
            return vertical;
        }

        public void setVertical(int vertical) {
            this.vertical = vertical;
        }
    }
}
