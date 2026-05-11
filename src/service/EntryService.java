package service;

import data.DataStore;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import model.ParkingSession;
import model.ParkingSpot;
import model.Vehicle;

public class EntryService {

    private final DataStore dataStore;
    private static final DateTimeFormatter ENTRY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public EntryService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public String registerVehicleEntry(Vehicle vehicle, String spotId) {
        // Ticket format
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String ticketNo = "T-" + vehicle.getPlate().toUpperCase().replace(" ", "") + "-" + timestamp;

        ParkingSpot spot = dataStore.getAllSpots().stream()
                .filter(s -> s.getSpotId().equals(spotId))
                .findFirst()
                .orElse(null);

        if (spot == null) return null;

        // Debug: check incoming vehicle
        System.out.println("[ENTRY SERVICE DEBUG] Incoming vehicle:");
        System.out.println("  Plate: " + vehicle.getPlate());
        System.out.println("  Type: " + vehicle.getType());
        System.out.println("  HC card: " + vehicle.hasHcCard());
        System.out.println("  VIP: " + vehicle.isVIP());

        // Check spot suitability
        if (!isSpotSuitable(vehicle, spot.getType().toString())) return null;

        String fineScheme = dataStore.getActiveFineScheme();

        String entryTime = LocalDateTime.now().format(ENTRY_FORMAT);
        ParkingSession session = new ParkingSession(ticketNo, vehicle, spotId, entryTime, fineScheme);

        // Debug: check session's vehicle after creation
        System.out.println("[ENTRY SERVICE DEBUG] Session vehicle after creation:");
        System.out.println("  Type: " + session.getVehicle().getType());
        System.out.println("  HC card: " + session.getVehicle().hasHcCard());

        dataStore.createSession(session);
        dataStore.setSpotOccupied(spotId, vehicle.getPlate());

        return ticketNo;
    }


    private boolean isSpotSuitable(Vehicle vehicle, String spotType) {
        String vType = vehicle.getType().toUpperCase();
        spotType = spotType.toUpperCase();

        // Handicapped vehicles can park in ANY spot
        if (vType.equals("HANDICAPPED")) {
            return true;
        }

        // Normal vehicles follow regular rules
        return switch (spotType) {
            case "COMPACT"     -> vType.equals("MOTORCYCLE") || vType.equals("CAR");
            case "REGULAR"     -> vType.equals("CAR") || vType.equals("SUV/TRUCK");
            case "HANDICAPPED" -> false;  // only handicapped vehicles allowed
            case "RESERVED"    -> vehicle.isVIP();
            default            -> false;
        };
    }
}
