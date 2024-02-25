package io.swagger.client;

import io.swagger.client.api.ResortsApi;
import io.swagger.client.model.ResortsList;

public class TestApiExample {
    public static void main(String[] args) {
        // Create an instance of the ApiClient
        ApiClient client = new ApiClient();

        // Set the base path to the address of your EC2 instance where the API is hosted
        client.setBasePath("http://ec2-54-184-98-219.us-west-2.compute.amazonaws.com:8080");

        // If your API requires authentication, set it up here. For example:
        // client.setApiKey("YOUR_API_KEY");
        // For this example, we'll assume no authentication is required.

        // Pass the configured ApiClient to the ResortsApi
        ResortsApi apiInstance = new ResortsApi(client);

        try {
            // Attempt to call an API method; in this case, getResorts() which should return a list of resorts
            ResortsList result = apiInstance.getResorts();
            System.out.println("API call successful, response: " + result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ResortsApi#getResorts");
            e.printStackTrace();
        }
    }
}
