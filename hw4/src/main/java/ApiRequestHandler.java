import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public class ApiRequestHandler implements Runnable {
  SkiersCache dataCache;
  DatabaseHandler databaseHandler;
  private final HttpServletRequest req;
  private final HttpServletResponse res;

  public ApiRequestHandler(HttpServletRequest req, HttpServletResponse res, SkiersCache dataCache, DatabaseHandler databaseHandler) {
    this.req = req;
    this.res = res;
    this.dataCache = SkiersCache.getInstance();
    this.databaseHandler = DatabaseHandler.getInstance();
  }

  @Override
  public void run() {
    try {
      String urlPath = req.getPathInfo();
      String[] urlParts = urlPath.split("/");
      int resortID = Integer.parseInt(urlParts[1]);
      String seasonID = urlParts[3];
      String dayID = urlParts[5];

      int cachedNoOfSkiers = dataCache.getNoOfSkiers(resortID, seasonID, dayID);
      if (cachedNoOfSkiers > 0) {
        sendSuccessResponse(res, "Mission Ridge", cachedNoOfSkiers);
      } else {
        int noOfSkiers = databaseHandler.getUniqueSkiers(resortID, seasonID, dayID);
        if (noOfSkiers >= 0) {
          sendSuccessResponse(res, "Mission Ridge", noOfSkiers);
          dataCache.cacheResult(resortID, seasonID, dayID, noOfSkiers);
        } else {
          sendErrorResponse(res, HttpServletResponse.SC_NOT_FOUND, Constants.MSG_DATA_NOT_FOUND);
        }
      }
    } catch (NumberFormatException e) {
      try {
        sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST, Constants.MSG_INVALID_INPUTS);
      } catch (Exception ex) {
          throw new RuntimeException(e);
      }
    } catch (IOException | RuntimeException e) {
      try {
        sendErrorResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Constants.COMMON_ERROR_MESSAGE);
      } catch (Exception ex) {
        throw new RuntimeException(e);
      }
    }
  }

  private void sendSuccessResponse(HttpServletResponse res, String time, int noOfSkiers) throws IOException {
    res.setContentType("application/json");
    res.setStatus(HttpServletResponse.SC_CREATED);
    JSONObject message = new JSONObject();
    message.put("time", time);
    message.put("numSkiers", noOfSkiers);
    res.getWriter().write(message.toString());
  }

  private void sendErrorResponse(HttpServletResponse res, int statusCode, String message) throws IOException {
    res.setStatus(statusCode);
    res.setContentType("application/json");
    res.getWriter().write("{\"message\": \"" + message + "\"}");
    System.out.println("Error Response Sent.");
  }

}
