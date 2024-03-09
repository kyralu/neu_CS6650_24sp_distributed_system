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


@WebServlet(name = "SkiersServlet", urlPatterns = {"/"})
public class SkiersServlet extends HttpServlet {
    private ObjectPool<Channel> channelPool;

    @Override
    public void init() throws ServletException {
        try {
            // Initialize RabbitMQ connection
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");

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
        int resortID;
        int seasonID;
        int dayID;
        int skierID;
        try {
            resortID = Integer.parseInt(pathParts[1]);
            seasonID = Integer.parseInt(pathParts[3]);
            dayID = Integer.parseInt(pathParts[5]);
            skierID = Integer.parseInt(pathParts[7]);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, "Invalid skier ID.", HttpServletResponse.SC_BAD_REQUEST);
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
        if (urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"error\":\"Missing parameters\"}");
            return;
        }

        String[] urlParts = urlPath.split("/");
        if (urlParts.length < 3) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid URL format\"}");
            return;
        }

        Channel channel = null;
        String queueName = "skiersQueue";
        //channel.queueDeclare(queueName, true, false, false, null);

        try {
            if (urlPath.matches("/skiers/\\d+/seasons/[^/]+/days/[^/]+/skiers/\\d+")) {
                String dayIDStr = urlParts[6]; // Adjust index according to your URL structure

                // Validate dayID
                int dayID;
                try {
                    dayID = Integer.parseInt(dayIDStr);
                    if (dayID < 1 || dayID > 366) {
                        sendErrorResponse(response, "Invalid day ID", HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }
                } catch (NumberFormatException e) {
                    sendErrorResponse(response, "Day ID must be a number", HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                LiftRide liftRide = gson.fromJson(request.getReader(), LiftRide.class);
                if (liftRide == null || liftRide.getLiftID() <= 0 || liftRide.getTime() <= 0) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"error\":\"Invalid request body\"}");
                    return;
                }

                // Serialize and send to RabbitMQ
                String message = gson.toJson(liftRide);
                channel = channelPool.borrowObject(); // Obtain a channel
                channel.basicPublish("", queueName, null, message.getBytes());
                response.setStatus(HttpServletResponse.SC_CREATED);
                response.getWriter().write("{\"message\":\"Lift ride recorded and queued\"}");

            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error2\":\"Invalid URL path\"}");
            }

        } catch (JsonSyntaxException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid JSON format\"}");
        } catch (Exception e) {
            sendErrorResponse(response, "Server error: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (channel != null) {
                try {
                    channelPool.returnObject(channel); // Return channel to the pool
                } catch (Exception e) {
                    // Handle channel return exception
                }
            }
        }
    }

    private void sendErrorResponse(HttpServletResponse response, String message, int statusCode) throws IOException {
        response.setStatus(statusCode);
        response.getWriter().write(String.format("{\"error\":\"%s\"}", message));
    }

    public static class NewSeasonRequest {
        private String year;

        // Constructor
        public NewSeasonRequest(String year) {
            this.year = year;
        }

        // Getter
        public String getYear() {
            return year;
        }

        // Setter
        public void setYear(String year) {
            this.year = year;
        }
    }

    public static class LiftRide {
        private int liftID;
        private int time;

        // Constructor
        public LiftRide(int liftID, int time) {
            this.liftID = liftID;
            this.time = time;
        }

        // Getter for liftID
        public int getLiftID() {
            return liftID;
        }

        // Setter for liftID
        public void setLiftID(int liftID) {
            this.liftID = liftID;
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
//install rabbitmq on ec2 instance & create and run these on different instances?
//consumer not processing the msg? is the msg not done correctly?
