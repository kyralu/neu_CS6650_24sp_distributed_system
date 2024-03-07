package client2;

import dataGeneration.DataGenerator;
import dataGeneration.SkierLiftRideEvent;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.google.gson.Gson;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MultithreadedClientv2 {
    private static final int TOTAL_POSTS = 200000;
    private static final int INITIAL_POSTS_PER_THREAD = 1000;
    private static final int INITIAL_THREAD_COUNT = 8;
    private static final int THREAD_COUNT = 2000; // Adjustable thread count
    private static final String BASE_URL = "http://localhost:8080/asgnmt1_client1_war_exploded/";
    private static final Gson gson = new Gson();
    private static final CloseableHttpClient httpClient = HttpClients.createDefault();
    private static final List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
    private static final AtomicInteger successfulRequests = new AtomicInteger(0);
    private static final AtomicInteger unsuccessfulRequests = new AtomicInteger(0);
    private static final String CSV_FILE = "results.csv";

    public static void main(String[] args) throws InterruptedException, IOException {

        BlockingQueue<SkierLiftRideEvent> eventsQueue = new ArrayBlockingQueue<>(TOTAL_POSTS);
        for (int i = 0; i < TOTAL_POSTS; i++) {
            eventsQueue.offer(DataGenerator.generateRandomEvent());
        }

        long startTime = System.currentTimeMillis();

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

        writePerformanceMetrics(startTime, endTime);
    }

    private static boolean postEvent(SkierLiftRideEvent event) {
        long startTime = System.currentTimeMillis();

        try {
            String eventJson = gson.toJson(new LiftRide(event.getLiftID(), event.getTime()));
            String POST_URL = String.format("%s/%d/seasons/%s/days/%s/skiers/%d", BASE_URL, event.getResortID(), event.getSeasonID(), event.getDayID(), event.getSkierID());
            HttpPost post = new HttpPost(POST_URL);
            post.setEntity(new StringEntity(eventJson));
            post.setHeader("Content-type", "application/json");

            HttpResponse response = httpClient.execute(post);

            long endTime = System.currentTimeMillis();
            long latency = endTime - startTime;
            latencies.add(latency);

            int responseCode = response.getStatusLine().getStatusCode();
            writeCsvRecord(startTime, "POST", latency, responseCode);

            EntityUtils.consume(response.getEntity());

            if (responseCode == 201) {
                return true;
            } else {
                return handleRetry(event);
            }

        } catch (Exception e) {
            return false;
        }
    }

    private static void writePerformanceMetrics(long startTime, long endTime) {
        // Calculate and output metrics
        calculateAndOutputMetrics();
    }

    // If the client gets a 5XX/ 4XX Code, retry the request up to 5 times before counting it as a failed request
    private static boolean handleRetry(SkierLiftRideEvent event) {
        int retries = 0;
        while (retries < 5) { // Try up to 5 retries
            try {
                retries++; // Increment retry counter

                String eventJson = gson.toJson(new LiftRide(event.getLiftID(), event.getTime()));
                String POST_URL = String.format("%s/%d/seasons/%s/days/%s/skiers/%d",
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

    private static void writeCsvRecord(long startTime, String requestType, long latency, int responseCode) {
        try (FileWriter fw = new FileWriter(CSV_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(startTime + "," + requestType + "," + latency + "," + responseCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void calculateAndOutputMetrics() {
        if (latencies.isEmpty()) return;

        Collections.sort(latencies);
        long totalLatency = latencies.stream().mapToLong(Long::longValue).sum();
        long minLatency = latencies.get(0);
        long maxLatency = latencies.get(latencies.size() - 1);
        double meanLatency = (double) totalLatency / latencies.size();
        long medianLatency = latencies.get(latencies.size() / 2);
        long p99Latency = latencies.get((int) (latencies.size() * 0.99));
        double totalTimeSeconds = (maxLatency - latencies.get(0)) / 1000.0;
        double throughput = latencies.size() / totalTimeSeconds;

        System.out.println("Mean response time (ms): " + meanLatency);
        System.out.println("Median response time (ms): " + medianLatency);
        System.out.println("Throughput (requests/second): " + throughput);
        System.out.println("p99 response time (ms): " + p99Latency);
        System.out.println("Min response time (ms): " + minLatency);
        System.out.println("Max response time (ms): " + maxLatency);
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
