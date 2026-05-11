package data;

import java.util.List;
import java.util.Map;
import model.FineRecord;
import model.ParkingSession;
import model.ParkingSpot;
import model.PaymentRecord;

public interface DataStore {

    void connect(); //open database connection
    void close(); //close database connection

    void initSchema(); //create required tables if not exist

    // Parking spot operations
    void upsertSpot(ParkingSpot spot); //insert or update parking spit
    List<ParkingSpot> findAvailableSpots(String spotType); //get all AVAILABLE spots of a specific type
    void setSpotOccupied(String spotId, String plate); //mark a spot as OCCUPIED and store plate num
    void setSpotAvailable(String spotId); //mark a spot as AVAILABLE and clear plate number
    void createSession(model.ParkingSession session); //insert a new parking session(vehicle entry)
    model.ParkingSession getOpenSessionByPlate(String plate); //get the latest open session for a plate
    void closeSession(String ticketNo, String exitTimeISO, int durationHours, double parkingFee); //update session with exit time,duration and fee
    void addFine(model.FineRecord fine); //insert a fine record
    void markAllFinesPaid(String plate, String paidTimeISO); //mark all unpaid fines as paid
    void createPayment(model.PaymentRecord payment); //insert a payment record
    List<PaymentRecord> getPaymentsByTicket(String ticketNo);
    List<FineRecord> getUnpaidFinesByPlate(String plate); // For ExitService
    List<FineRecord> getAllUnpaidFines();               // For ReportingPanel
    double getTotalRevenue();
    int getOccupiedSpotCount();
    int getTotalSpotCount();
    String authenticate(String username, String password);
    List<ParkingSpot> getAvailableSpots(String type);
    double getTotalUnpaidFines();
    List<ParkingSpot> getAllSpots();
    List<ParkingSession> getAllActiveSessions();
    void reduceFineAmount(FineRecord fine, double amountPaid);
    void setActiveFineScheme(String scheme);  // save fine scheme for future entries
    String getActiveFineScheme();             // optional getter
    Map<String, ParkingSession> getOccupiedSpotsMap();



    
}
