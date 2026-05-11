package model;

public class ParkingSession {
    private String ticketNo;
    private Vehicle vehicle;
    private String spotId;
    private String entryTime;
    private String exitTime;
    private String fineScheme;  

    public ParkingSession(String ticketNo, Vehicle vehicle, String spotId, String entryTime, String fineScheme) {
        this.ticketNo = ticketNo;
        this.vehicle = vehicle;
        this.spotId = spotId;
        this.entryTime = entryTime;
        this.exitTime = null; // initially null
        this.fineScheme = fineScheme; // store scheme
    }

    // Getter
    public String getFineScheme() {
        return fineScheme;
    }

    public void setFineScheme(String fineScheme) {
        this.fineScheme = fineScheme;
    }

    // existing getters
    public String getTicketNo() { return ticketNo; }
    public Vehicle getVehicle() { return vehicle; }
    public String getPlate() { return vehicle.getPlate(); }
    public String getSpotId() { return spotId; }
    public String getEntryTime() { return entryTime; }
    public boolean hasHcCard() { return vehicle.hasHcCard(); }
    public boolean isVIP() { return vehicle.isVIP(); }
    public String getExitTime() { return exitTime; }
    public void setExitTime(String exitTime) { this.exitTime = exitTime; }
}
