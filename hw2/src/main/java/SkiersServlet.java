import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import javax.servlet.ServletException;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;


@WebServlet(name = "SkiersServlet", urlPatterns = {"/skiers"})
public class SkiersServlet extends HttpServlet {
    private ObjectPool<Channel> channelPool;
    private final static String HOST = "localhost";

    @Override
    public void init() throws ServletException {
        try {
            // Initialize RabbitMQ connection
            ConnectionFactory factory = new ConnectionFactory();

            factory.setHost(HOST);
            factory.setUsername("kyra");
            factory.setPassword("123");

            Connection connection = factory.newConnection();

            // Initialize the channel pool with the connection
            channelPool = new GenericObjectPool<>(new RMQChannelFactory(connection));

        } catch (Exception e) {
            throw new ServletException("Failed to create channel pool", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String pathInfo = request.getPathInfo(); // e.g., /12/seasons/2019/days/1/skiers/1
        if (pathInfo == null || pathInfo.equals("/")) {
            sendErrorResponse(response, "Missing parameters.", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String[] pathParts = pathInfo.split("/");
        // Now expecting 8 parts due to leading empty string and the full path structure
        if (pathParts.length != 8) {
            sendErrorResponse(response, "Invalid parameters.", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Assuming pathParts[1] is resortID, pathParts[3] is seasonID, pathParts[5] is dayID, and pathParts[7] is skierID
        int resortID, skierID;
        String seasonID, dayID;
        try {
            resortID = Integer.parseInt(pathParts[1]);
            skierID = Integer.parseInt(pathParts[7]);
            seasonID = pathParts[3];
            dayID = pathParts[5];
        } catch (NumberFormatException e) {
            sendErrorResponse(response, "Invalid number format in URL.", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Assuming you would fetch the skier's total vertical for the day from your data source here
        int totalVertical = 20000; // Dummy value for demonstration

        out.println("{");
        out.println("\"resortID\": \"" + resortID + "\",");
        out.println("\"seasonID\": \"" + seasonID + "\",");
        out.println("\"dayID\": \"" + dayID + "\",");
        out.println("\"skierID\": \"" + skierID + "\",");
        out.println("\"totalVertical\": " + totalVertical);
        out.println("}");
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        String urlPath = request.getPathInfo();
        Gson gson = new Gson();

        // Validate the URL path
        if (urlPath == null || urlPath.isEmpty() || !urlPath.matches("/\\d+/seasons/[^/]+/days/[^/]+/skiers/\\d+")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid URL format\"}");
            return;
        }

        String[] urlParts = urlPath.split("/");
        int resortID, skierID;
        String seasonID, dayID;
        try {
            resortID = Integer.parseInt(urlParts[1]);
            skierID = Integer.parseInt(urlParts[7]);
            seasonID = urlParts[3];
            dayID = urlParts[5];

            // Validate dayID
            try {
                int day = Integer.parseInt(dayID);
                if (day < 1 || day > 366) {
                    sendErrorResponse(response, "Invalid day ID", HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            } catch (NumberFormatException e) {
                sendErrorResponse(response, "Day ID must be a number", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

        } catch (NumberFormatException e) {
            sendErrorResponse(response, "Invalid number format in URL.", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Parse the body to get liftID and time
        LiftRide liftRideFromBody = gson.fromJson(request.getReader(), LiftRide.class);
        if (liftRideFromBody == null || liftRideFromBody.getLiftID() <= 0 || liftRideFromBody.getTime() <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid request body\"}");
            return;
        }

        // Create a LiftRide object with full information
        LiftRide liftRide = new LiftRide(skierID, resortID, liftRideFromBody.getLiftID(), seasonID, dayID, liftRideFromBody.getTime());

        // Serialize and send to RabbitMQ
        String message = gson.toJson(liftRide);
        Channel channel = null;
        String queueName = "skiersQueue";
        //channel.queueDeclare(queueName, true, false, false, null);

        try {
            channel = channelPool.borrowObject(); // Obtain a channel from the pool
            channel.basicPublish("", queueName, null, message.getBytes());
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write("{\"message\":\"Lift ride recorded and queued\"}");
        } catch (Exception e) {
            sendErrorResponse(response, "Server error: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (channel != null) {
                try {
                    channelPool.returnObject(channel); // Return the channel to the pool
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void sendErrorResponse(HttpServletResponse response, String message, int statusCode) throws IOException {
        response.setStatus(statusCode);
        response.getWriter().write(String.format("{\"error\":\"%s\"}", message));
    }

    public static class LiftRide {
        private int skierID;
        private int resortID;
        private int liftID;
        private String seasonID;
        private String dayID;
        private int time;

        // Constructor
        public LiftRide(int skierID, int resortID, int liftID, String seasonID, String dayID, int time) {
            this.skierID = skierID;
            this.resortID = resortID;
            this.liftID = liftID;
            this.seasonID = seasonID;
            this.dayID = dayID;
            this.time = time;
        }

        // Getter for skierID
        public int getSkierID() {
            return skierID;
        }

        // Setter for skierID
        public void setSkierID(int skierID) {
            this.skierID = skierID;
        }

        // Getter for resortID
        public int getResortID() {
            return resortID;
        }

        // Setter for resortID
        public void setResortID(int resortID) {
            this.resortID = resortID;
        }

        // Getter for liftID
        public int getLiftID() {
            return liftID;
        }

        // Setter for liftID
        public void setLiftID(int liftID) {
            this.liftID = liftID;
        }

        // Getter for seasonID
        public String getSeasonID() {
            return seasonID;
        }

        // Setter for seasonID
        public void setSeasonID(String seasonID) {
            this.seasonID = seasonID;
        }

        // Getter for dayID
        public String getDayID() {
            return dayID;
        }

        // Setter for seasonID
        public void setDayID(String dayID) {
            this.dayID = dayID;
        }

        // Getter for time
        public int getTime() {
            return time;
        }

        // Setter for time
        public void setTime(int time) {
            this.time = time;
        }
    }

}

//TODO:
//will the curr queue always there?
//how to clear the queue and read the msg from the rabbitmq page
//consumer not processing the msg? is the msg not done correctly?
