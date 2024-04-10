import java.util.concurrent.Future;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;
import java.util.logging.Logger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@WebServlet("/resorts/*")
public class UniqueSkiersByRSD extends HttpServlet {
  private static final Logger LOGGER = Logger.getLogger(UniqueSkiersByRSD.class.getName());
  SkiersCache dataCache;
  DatabaseHandler databaseHandler;
  ExecutorService executor;

  @Override
  public void init(ServletConfig config) throws ServletException {
    try {
      // Initialize the Servlet
      super.init(config);
      System.out.println(Constants.SERVLET_EST);
//    LOGGER.info(Constants.SERVLET_EST);

      // Initialize the DB
      this.databaseHandler = DatabaseHandler.getInstance();
      System.out.println(Constants.DATABASE_CON_EST);
//    LOGGER.info(Constants.DATABASE_CON_EST);

      // Initialize the Cache
      this.dataCache = SkiersCache.getInstance();
      System.out.println(Constants.CACHE_EST);
//    LOGGER.info(Constants.CACHE_EST);
      executor = Executors.newFixedThreadPool(1000);
    }catch (Exception e){
      e.printStackTrace();
    }
  }

//  @Override
//  protected void doGet(HttpServletRequest req, HttpServletResponse res) {
//      try {
//        String urlPath = req.getPathInfo();
//        System.out.println("\n" + urlPath);
//
//        if (urlPath == null || urlPath.isEmpty()) {
//          sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST, Constants.MSG_INVALID_INPUTS);
//        } else {
//          String[] urlParts = urlPath.split("/");
//
//          if (!isUrlValid(urlPath)) {
//            sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST, Constants.MSG_INVALID_INPUTS);
//            return;
//          }
//
//          int resortID = Integer.parseInt(urlParts[1]);
//          String seasonID = urlParts[3];
//          String dayID = urlParts[5];
//
//          int cachedNoOfSkiers = dataCache.getNoOfSkiers(resortID, seasonID, dayID);
//          if (cachedNoOfSkiers > 0) {
//            sendSuccessResponse(res, "Mission Ridge", cachedNoOfSkiers);
//            System.out.println("Sent Data from Cache." + cachedNoOfSkiers);
//          } else {
//            int noOfSkiers = databaseHandler.getUniqueSkiers(resortID, seasonID, dayID);
//            if (noOfSkiers >= 0) {
//              sendSuccessResponse(res, "Mission Ridge", noOfSkiers);
//              System.out.println("Sent Data from DynamoDB." + noOfSkiers);
//              dataCache.cacheResult(resortID, seasonID, dayID, noOfSkiers);
//              System.out.println("Data Updated to Cache.");
//            } else {
//              sendErrorResponse(res, HttpServletResponse.SC_NOT_FOUND, Constants.MSG_DATA_NOT_FOUND);
//            }
//          }
//        }
//      } catch (Exception e) {
//        try {
//          sendErrorResponse(res, HttpServletResponse.SC_NOT_FOUND, Constants.COMMON_ERROR_MESSAGE);
//          e.printStackTrace();
//        } catch (Exception ex) {
//          ex.printStackTrace();
//        }
//      }
//  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) {
    try {
      String urlPath = req.getPathInfo();
      System.out.println("\n" + urlPath);

      if (urlPath == null || urlPath.isEmpty()) {
        sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST, Constants.MSG_INVALID_INPUTS);
        return;
      }

      String[] urlParts = urlPath.split("/");

      if (!isUrlValid(urlPath)) {
        sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST, Constants.MSG_INVALID_INPUTS);
        return;
      }

      int resortID = Integer.parseInt(urlParts[1]);
      String seasonID = urlParts[3];
      String dayID = urlParts[5];

      // Submit the task to the executor service
      Future<?> future = executor.submit(() -> {
        try {
          int cachedNoOfSkiers = dataCache.getNoOfSkiers(resortID, seasonID, dayID);
          if (cachedNoOfSkiers > 0) {
            sendSuccessResponse(res, "Mission Ridge", cachedNoOfSkiers);
            System.out.println("Sent Data from Cache." + cachedNoOfSkiers);
          } else {
            int noOfSkiers = databaseHandler.getUniqueSkiers(resortID, seasonID, dayID);
            if (noOfSkiers >= 0) {
              sendSuccessResponse(res, "Mission Ridge", noOfSkiers);
              System.out.println("Sent Data from DynamoDB." + noOfSkiers);
              dataCache.cacheResult(resortID, seasonID, dayID, noOfSkiers);
              System.out.println("Data Updated to Cache.");
            } else {
              sendErrorResponse(res, HttpServletResponse.SC_NOT_FOUND, Constants.MSG_DATA_NOT_FOUND);
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
          try {
            sendErrorResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Constants.COMMON_ERROR_MESSAGE);
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      });
      future.get();
    } catch (Exception e) {
      e.printStackTrace();
      try {
        sendErrorResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Constants.COMMON_ERROR_MESSAGE);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }



  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
    sendErrorResponse(res, HttpServletResponse.SC_METHOD_NOT_ALLOWED, Constants.MSG_METHOD_NOT_ALLOWED);
  }


  private void sendErrorResponse(HttpServletResponse res, int statusCode, String message) throws IOException {
    res.setStatus(statusCode);
    res.setContentType("application/json");
    res.getWriter().write("{\"message\": \"" + message + "\"}");
    System.out.println("Error Response Sent.");
//    LOGGER.warning("Error Response Sent: " + message);
  }

  private void sendSuccessResponse(HttpServletResponse res, String time, int noOfSkiers) throws IOException {
    res.setContentType("application/json");
    res.setStatus(HttpServletResponse.SC_CREATED);
    JSONObject message = new JSONObject();
    message.put("time", time);
    message.put("numSkiers", noOfSkiers);
    res.getWriter().write(message.toString());
    System.out.println("Success Response Sent.");
//    LOGGER.info("Success Response Sent: " + message.toString());
  }

  private boolean isUrlValid(String urlPath) {
    Pattern p = Pattern.compile(Constants.UNIQUE_SKIERS_URL_PATTERN);
    Matcher m = p.matcher(urlPath);
    return m.matches();
  }

  @Override
  public void destroy() {
    executor.shutdown();
  }
}

