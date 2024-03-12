import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class SkierMessageConsumer {

    private final static String QUEUE_NAME = "skiersQueue";
    private final static String HOST = "ec2-52-12-161-184.us-west-2.compute.amazonaws.com";
    private static final ConcurrentHashMap<String, Integer> skierLiftRides = new ConcurrentHashMap<>();
    private static final int THREAD_POOL_SIZE = 1;

    public static void main(String[] argv) throws IOException, TimeoutException {
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
