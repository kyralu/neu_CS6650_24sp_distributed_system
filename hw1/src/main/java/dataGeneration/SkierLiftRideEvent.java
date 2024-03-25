package dataGeneration;

public class SkierLiftRideEvent {
    public int skierID;

    public int resortID;
    public int liftID;
    public String seasonID;
    public String dayID;

    public int time;

    public SkierLiftRideEvent(int skierID, int resortID, int liftID, String seasonID, String dayID, int time) {
        if (seasonID == null || dayID == null) {
            throw new IllegalArgumentException("seasonID and dayID cannot be null");
        }

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
