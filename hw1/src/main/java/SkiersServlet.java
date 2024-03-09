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

@WebServlet(name = "SkiersServlet", urlPatterns = {"/skiers"})
public class SkiersServlet extends HttpServlet {

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
            skierID = Integer.parseInt(pathParts[7]); // Correctly fetching the skierID based on the new understanding
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

        // Determine the type of POST request
        try {
            if (urlPath.matches("/\\d+/seasons/[^/]+/days/[^/]+/skiers/\\d+")) {
                // Extract dayID from urlParts
                String dayIDStr = urlParts[5];

                // Validate dayID
                int dayID;
                try {
                    dayID = Integer.parseInt(dayIDStr);
                    if (dayID < 1 || dayID > 366) {
                        sendErrorResponse(response, "Invalid inputs", HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }
                } catch (NumberFormatException e) {
                    sendErrorResponse(response, "Invalid inputs", HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                // Write a new lift ride for the skier
                LiftRide liftRide = gson.fromJson(request.getReader(), LiftRide.class);
                if (liftRide == null || liftRide.getLiftID() <= 0 || liftRide.getTime() <= 0) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"error\":\"Invalid request body\"}");
                    return;
                }
                // Process the lift ride here
                response.setStatus(HttpServletResponse.SC_CREATED);
                response.getWriter().write("{\"message\":\"Write successful\"}");

            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\":\"Invalid URL path\"}");
            }
        } catch (JsonSyntaxException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid JSON format\"}");
        }
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


    private void sendErrorResponse(HttpServletResponse response, String message, int statusCode) throws IOException {
        response.setStatus(statusCode);
        PrintWriter out = response.getWriter();
        out.println("{\"error\": \"" + message + "\"}");
        out.flush();
    }
}
