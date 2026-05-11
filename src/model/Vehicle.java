package model;

public class Vehicle {
    private String plate;
    private String type; // MOTORCYCLE, CAR, SUV/TRUCK, HANDICAPPED
    private boolean hasHcCard; // only for handicapped vehicles
    private boolean isVIP;      // only VIP cars can park in Reserved spots

    public Vehicle(String plate, String type, boolean hasHcCard, boolean isVIP) {
        this.plate = plate;
        this.type = type;
        this.hasHcCard = hasHcCard;
        this.isVIP = isVIP;
    }

    public String getPlate() { return plate; }
    public String getType() { return type; }
    public boolean hasHcCard() { return hasHcCard; }
    public boolean isVIP() { return isVIP; }
}
