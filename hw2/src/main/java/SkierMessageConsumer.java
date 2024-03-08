import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

public class SkierMessageConsumer {

    private final static String QUEUE_NAME = "skiersQueue";
    private final static String HOST = "localhost";
    private static final ConcurrentHashMap<String, Integer> skierLiftRides = new ConcurrentHashMap<>();

    public static void main(String[] argv) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);
        final Connection connection = factory.newConnection();
        final Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            processMessage(message);
        };

        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
    }

    private static void processMessage(String message) {
        Gson gson = new Gson();
        LiftRide liftRide = gson.fromJson(message, LiftRide.class);
        // Implement your logic to process the message. For example, update the ConcurrentHashMap.
        // Assuming LiftRide has a skierID and vertical value for simplicity.
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
