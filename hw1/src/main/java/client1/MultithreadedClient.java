package client1;

import dataGeneration.DataGenerator;
import dataGeneration.SkierLiftRideEvent;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.google.gson.Gson;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MultithreadedClient {
    private static final int TOTAL_POSTS = 200000;
    private static final int INITIAL_POSTS_PER_THREAD = 1000;
    private static final int INITIAL_THREAD_COUNT = 32;
    private static final int THREAD_COUNT = 128; // Adjustable thread count
    private static final String BASE_URL = "http://localhost:8080/hw1_war_exploded/";
    private static final Gson gson = new Gson();
    private static final CloseableHttpClient httpClient = HttpClients.createDefault();
    private static final AtomicInteger successfulRequests = new AtomicInteger(0);
    private static final AtomicInteger unsuccessfulRequests = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException, IOException {

        BlockingQueue<SkierLiftRideEvent> eventsQueue = new ArrayBlockingQueue<>(TOTAL_POSTS);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < TOTAL_POSTS; i++) {
            eventsQueue.offer(DataGenerator.generateRandomEvent());
        }

        Thread[] threads = new Thread[THREAD_COUNT];

        ExecutorService executor = Executors.newCachedThreadPool();

        //Initial 32 threads, each processing 1000 POST requests
        for (int i = 0; i < INITIAL_THREAD_COUNT; i++) {
            executor.execute(() -> {
                for (int j = 0; j < INITIAL_POSTS_PER_THREAD; j++) {
                    SkierLiftRideEvent event = eventsQueue.poll();
                    if (event != null) {
                        if (postEvent(event)) {
                            successfulRequests.incrementAndGet();
                        } else {
                            unsuccessfulRequests.incrementAndGet();
                        }
                    }
                }
            });
        }


        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(() -> {
                while (!eventsQueue.isEmpty()) {
                    SkierLiftRideEvent event = eventsQueue.poll();
                    if (event != null) {
                        if (postEvent(event)) {
                            successfulRequests.incrementAndGet();
                        } else {
                            unsuccessfulRequests.incrementAndGet();
                        }
                    }
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join(); // Wait for each thread to finish
        }

        httpClient.close();

        long endTime = System.currentTimeMillis();

        long totalTime = endTime - startTime;
        double totalTimeSeconds = totalTime / 1000.0;
        double throughput = TOTAL_POSTS / totalTimeSeconds;

        System.out.println("Total successfulPost requests sent: " + successfulRequests);
        System.out.println("Total run time (seconds)[wall time]: " + totalTimeSeconds);
        System.out.println("Total throughput (requests per second): " + throughput);

    }

    private static boolean postEvent(SkierLiftRideEvent event) {
        try {
            String eventJson = gson.toJson(new MultithreadedClient.LiftRide(event.getLiftID(), event.getTime())); // Assuming SkierLiftRideEvent matches the expected JSON structure
            String postUrl = String.format("%s/skiers/%d/seasons/%s/days/%s/skiers/%d",
                    BASE_URL, event.getResortID(), event.getSeasonID(), event.getDayID(), event.getSkierID());

            HttpPost post = new HttpPost(postUrl);
            post.setEntity(new StringEntity(eventJson));
            post.setHeader("Content-type", "application/json");

            HttpResponse response = httpClient.execute(post);
            EntityUtils.consume(response.getEntity()); // Ensure the response entity is fully consumed

            int responseCode = response.getStatusLine().getStatusCode();

            if (responseCode == HttpServletResponse.SC_CREATED) { // 201 indicates success
                return true;
            } else if (responseCode >= 400 && responseCode <= 599) {
                // responseCode: 400 - 499, Servlet Client error, do not retry
                // responseCode: 500 - 599, Web Server error, attempt retry
                return handleRetry(event);
            } else {
                // Unexpected response code
                return false;
            }

        } catch (IOException e) {
            // Log or handle IOException
            return false;
        }
    }

    // If the client gets a 5XX/ 4XX Code, retry the request up to 5 times before counting it as a failed request
    private static boolean handleRetry(SkierLiftRideEvent event) {
        int retries = 0;
        while (retries < 5) { // Try up to 5 retries
            try {
                retries++; // Increment retry counter

                String eventJson = gson.toJson(new LiftRide(event.getLiftID(), event.getTime()));
                String POST_URL = String.format("%s/skiers/%d/seasons/%s/days/%s/skiers/%d",
                        BASE_URL, event.getResortID(), event.getSeasonID(), event.getDayID(), event.getSkierID());
                HttpPost post = new HttpPost(POST_URL);
                post.setEntity(new StringEntity(eventJson));
                post.setHeader("Content-type", "application/json");

                System.out.println("Retrying... Attempt " + retries + " for Skier ID: " + event.getSkierID());

                HttpResponse response = httpClient.execute(post);
                int responseCode = response.getStatusLine().getStatusCode();

                EntityUtils.consume(response.getEntity()); // Consume the response to release resources

                // Check response code
                if (responseCode == 201) { // HTTP_CREATED
                    System.out.println("Successfully resent event for Skier ID: " + event.getSkierID());
                    return true; // Success
                } else if (responseCode >= 400 && responseCode < 500) {
                    System.out.println("Client error on retry for Skier ID: " + event.getSkierID() + ". Aborting retries.");
                    return false; // Client error, abort retrying
                } // Continue retrying if server error (5XX)
            } catch (Exception e) {
                System.out.println("Exception during retry for Skier ID: " + event.getSkierID() + ": " + e.getMessage());
            }
        }
        System.out.println("Failed to send event for Skier ID: " + event.getSkierID() + " after " + retries + " attempts.");
        return false; // Failed after 5 retries
    }

    static class LiftRide {
        private int liftID;
        private int time;

        public LiftRide(int liftID, int time) {
            this.liftID = liftID;
            this.time = time;
        }
    }
}
