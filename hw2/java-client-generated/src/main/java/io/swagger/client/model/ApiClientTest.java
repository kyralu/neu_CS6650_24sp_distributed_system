package io.swagger.client.model;
import io.swagger.client.*;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.*;

public class ApiClientTest {

    public static void main(String[] args) {
        ApiClient client = new ApiClient();

        // Assuming the server is running locally for testing purposes
        client.setBasePath("http://localhost:8080/asgnmt1_server_war_exploded");

        SkiersApi apiInstance = new SkiersApi(client);

        Integer skierID = 123; // Example skierID
        Integer resortID = 12; // Example resortID
        String dayID = "23"; // Example dayID
        String seasonID = "2019"; // Example seasonID

        try {
            // The API call matches the server implementation
            // Assuming the server responds with a JSON object, you should capture the response properly
            Integer response = apiInstance.getSkierDayVertical(resortID, seasonID, dayID, skierID);
            // Assuming SkierVertical is a class that matches the JSON structure returned by your server
            // For demonstration, simply print out the response or the relevant part of it
            System.out.println("API call successful, response: " + response);
        } catch (ApiException e) {
            System.err.println("Exception when calling SkiersApi#getSkierDayVertical");
            e.printStackTrace();
        }
    }
}
