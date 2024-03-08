package dataGeneration;

public class SkierLiftRideEvent {
    public int skierID;

    public int resortID;
    public int liftID;
    public String seasonID;
    public String dayID;

    public int time;

    public SkierLiftRideEvent(int skierID, int resortID, int liftID, String seasonID, String dayID, int time) {
        this.skierID = skierID;
        this.resortID = resortID;
        this.liftID = liftID;
        this.seasonID = seasonID;
        this.dayID = dayID;
        this.time = time;
    }

    // Getters
    public int getSkierID() {
        return skierID;
    }

    public int getResortID() {
        return resortID;
    }

    public int getLiftID() {
        return liftID;
    }

    public String getSeasonID() {
        return seasonID;
    }

    public String getDayID() {
        return dayID;
    }

    public int getTime() {
        return time;
    }

    // Getters and Setters
}
