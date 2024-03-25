package dataGeneration;

import java.util.Random;

public class DataGenerator {

    private static final Random random = new Random();

    public static SkierLiftRideEvent generateRandomEvent() {
        int skierID = 1 + random.nextInt(100000); // SkierID between 1 and 100000
        int resortID = 1 + random.nextInt(10);    // ResortID between 1 and 10
        int liftID = 1 + random.nextInt(40);      // LiftID between 1 and 40
        String seasonID = "2024";                 // SeasonID is 2024
        String dayID = String.valueOf(1 + random.nextInt(360)); // DayID between 1 and 360
        int time = 1 + random.nextInt(360);       // Time between 1 and 360

        return new SkierLiftRideEvent(skierID, resortID, liftID, seasonID, dayID, time);
    }

}

