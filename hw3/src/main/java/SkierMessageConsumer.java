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

import redis.clients.jedis.Jedis;

public class SkierMessageConsumer {

    private final static String QUEUE_NAME = "skiersQueue";
    private final static String HOST = "localhost";
    private static final int THREAD_POOL_SIZE = 4;
//    For DYNAMODB
//    private static final ConcurrentHashMap<String, Integer> skierLiftRides = new ConcurrentHashMap<>();
//    private DynamoDbClient dynamoDb;
//    private final String tableName = "SkierData"; // Change to DynamoDB table name
//    private static final String AWS_ACCESS_KEY_ID="";
//    private static final String AWS_SECRET_ACCESS_KEY="";
//    private final String AWS_SESSION_TOKEN="";
//    private static DynamoDbClient dynamoDbClient;

    public static void main(String[] argv) throws IOException, TimeoutException {
//         Initialize DynamoDB client
//        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY);
//
//        dynamoDbClient = DynamoDbClient.builder()
//                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
//                .region(Region.US_WEST_2) // Change to the region
//                .build();


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

        int skierID = liftRide.getSkierID();
        int liftID = liftRide.getLiftID();
        int vertical = liftRide.getLiftID() * 10;
        String dayID = liftRide.getDayID();
        int resortID = liftRide.getResortID();
        String seasonID = liftRide.getSeasonID();

        try (Jedis jedis = RedisClient.getPool().getResource()) {

            // Add the day to the set of days and seasons the skier has skied
            // For skier N, how many days have they skied this season?
            jedis.sadd("skier:" + skierID + "season:" + seasonID + ":days" , dayID);

            // Increment the vertical total for the skier for the day
            // For skier N, what are the vertical totals for each ski day?" (calculate vertical as liftID*10)
            jedis.hincrBy("skier:" + skierID + ":days:" + dayID, "vertical", vertical);

            // Add the lift ID to the set of lifts the skier rode that day
            // For skier N, show me the lifts they rode on each ski day
            jedis.sadd("skier:" + skierID + ":day:" + dayID + ":lifts", String.valueOf(liftID));

            // Add the skier to the set of unique skiers for the resort for the day
            // How many unique skiers visited resort X on day N?
            jedis.sadd("resort:" + resortID + ":day:" + dayID + ":skiers", String.valueOf(skierID));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public class LiftRide {
        private int skierID;
        private int vertical;
        private String dayID;
        private String seasonID;
        private int resortID;
        private int liftID;
        private int time;

        // Getters and setters for all fields
        public int getSkierID() {
            return skierID;
        }

        public void setSkierID(int skierID) {
            this.skierID = skierID;
        }

        public int getVertical() {
            return vertical;
        }

        public void setVertical(int vertical) {
            this.vertical = vertical;
        }

        public String getDayID() {
            return dayID;
        }

        public void setDayID(String dayID) {
            this.dayID = dayID;
        }

        public String getSeasonID() {
            return seasonID;
        }

        public void setSeasonID(String seasonID) {
            this.seasonID = seasonID;
        }

        public int getResortID() {
            return resortID;
        }

        public void setResortID(int resortID) {
            this.resortID = resortID;
        }

        public int getLiftID() {
            return liftID;
        }

        public void setLiftID(int liftID) {
            this.liftID = liftID;
        }

        public int getTime() {
            return time;
        }

        public void setTime(int time) {
            this.time = time;
        }
    }

}
