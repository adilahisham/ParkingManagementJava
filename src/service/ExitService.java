package service;

import data.DataStore;
import enums.FineReason;
import fine.FineScheme;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import model.FineRecord;
import model.ParkingSession;
import model.ParkingSpot;
import model.PaymentRecord;
import model.Vehicle;

public class ExitService {

    private final DataStore dataStore;
    private final FineScheme activeFineScheme;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ExitService(DataStore dataStore, FineScheme activeFineScheme) {
        this.dataStore = dataStore;
        this.activeFineScheme = activeFineScheme;
    }

    // ===============================
    //  PREVIEW EXIT (NO DB WRITES)
    // ===============================
    public PaymentRecord previewExit(ParkingSession session, LocalDateTime exitTime) {
        if (session == null) return null;

        long hours = calculateHoursCeiling(session.getEntryTime(), exitTime);
        System.out.println("=== PREVIEW DEBUG ===");
        System.out.println("Plate: " + session.getVehicle().getPlate());
        System.out.println("Entry (string): " + session.getEntryTime());
        System.out.println("Exit (param):   " + exitTime);
        System.out.println("Calculated hours: " + hours);
        System.out.println("Overstay hours:   " + (hours > 24 ? hours - 24 : 0));
        double parkingFee = hours * getHourlyRate(session, session.getVehicle());

        // Use the scheme that was active WHEN THIS VEHICLE ENTERED
        FineScheme scheme = mapStringToScheme(session.getFineScheme());
        if (scheme == null) {
            scheme = activeFineScheme; // fallback for old sessions or missing data
        }

        // Calculate potential new fines using the correct scheme
        double newFine = calculatePreviewFines(session, hours, scheme);

        // Past unpaid fines (unchanged)
        List<FineRecord> pastUnpaid = dataStore.getUnpaidFinesByPlate(session.getVehicle().getPlate());
        double pastFineTotal = pastUnpaid.stream().mapToDouble(FineRecord::getAmount).sum();

        double totalFines = pastFineTotal + newFine;

        return new PaymentRecord(
            session.getTicketNo(),
            session.getVehicle().getPlate(),
            null,
            exitTime,
            (int) hours,
            parkingFee,
            totalFines,
            0
        );
    }

    // ===============================
    //  FINALIZE EXIT (WRITE TO DB)
    // ===============================
    public PaymentRecord confirmExit(ParkingSession session, LocalDateTime exitTime,
                                     PaymentRecord payment, boolean markAllFinesPaid) {

        Vehicle vehicle = session.getVehicle();
        String plate = vehicle.getPlate();
        String exitTimeStr = exitTime.format(FORMATTER);

        long hours = calculateHoursCeiling(session.getEntryTime(), exitTime);
        double parkingFee = hours * getHourlyRate(session, vehicle);

        FineScheme scheme = mapStringToScheme(session.getFineScheme());
        if (scheme == null) scheme = activeFineScheme;

        List<FineRecord> newFines = generateFinalFines(session, hours, exitTimeStr, scheme);
        for (FineRecord fine : newFines) {
            dataStore.addFine(fine);
        }
        System.out.println("[DEBUG] New fines created during confirmExit: " + newFines.size());
        newFines.forEach(f -> 
        System.out.println("  → " + f.getReason() + " : RM " + f.getAmount() + " (unpaid)"));   

        double amountLeft = payment.getAmountPaid() - payment.getParkingFee();
        List<FineRecord> unpaidFines = dataStore.getUnpaidFinesByPlate(plate);

        for (FineRecord fine : unpaidFines) {
            if (amountLeft <= 0) break;
            double toPay = Math.min(amountLeft, fine.getAmount());
            dataStore.reduceFineAmount(fine, toPay);
            amountLeft -= toPay;
        }

        dataStore.closeSession(session.getTicketNo(), exitTimeStr, (int) hours, parkingFee);
        dataStore.setSpotAvailable(session.getSpotId());

        PaymentRecord finalizedPayment = new PaymentRecord(
                session.getTicketNo(),
                plate,
                payment.getMethod(),
                exitTime,
                (int) hours,
                parkingFee,
                payment.getFinePaid(),
                payment.getAmountPaid()
        );

        dataStore.createPayment(finalizedPayment);

        System.out.println("Exit finalized for ticket: " + session.getTicketNo());
        return finalizedPayment;
    }

    // ===============================
    //  Preview fine calculation
    // ===============================
    private double calculatePreviewFines(ParkingSession session, long hours, FineScheme scheme) {
        double total = 0;

        if (hours > 24) {
            double overstay = scheme.calculateFine(hours - 24);
            System.out.println("[PREVIEW FINE] Overstay fine: RM " + overstay);
            total += overstay;
        }

        boolean isReservedViolation = session.getSpotId().contains("RES") && !session.getVehicle().isVIP();
        if (isReservedViolation) {
            System.out.println("[PREVIEW FINE] Reserved violation: +100.0");
            total += 100.0;
        }

        System.out.println("[PREVIEW FINE] Total new fines: RM " + total);
        return total;
    }

    // ===============================
    //  Final fine creation
    // ===============================
    public List<FineRecord> generateFinalFines(ParkingSession session, long hours,
                                                String exitTimeStr, FineScheme scheme) {
        List<FineRecord> fines = new ArrayList<>();
        String plate = session.getVehicle().getPlate();

        if (hours > 24 && !fineExists(plate, FineReason.OVERSTAY_24H)) {
            fines.add(new FineRecord(plate, FineReason.OVERSTAY_24H, scheme.calculateFine(hours - 24),
                    exitTimeStr, false));
        }

        if (session.getSpotId().contains("RES") && !session.getVehicle().isVIP()
                && !fineExists(plate, FineReason.RESERVED_VIOLATION)) {
            fines.add(new FineRecord(plate, FineReason.RESERVED_VIOLATION, 100.0,
                    exitTimeStr, false));
        }

        return fines;
    }

    private boolean fineExists(String plate, FineReason reason) {
        return dataStore.getUnpaidFinesByPlate(plate).stream()
                .anyMatch(f -> f.getReason() == reason);
    }

    private double getHourlyRate(ParkingSession session, Vehicle vehicle) {
        ParkingSpot spot = dataStore.getAllSpots().stream()
                .filter(s -> s.getSpotId().equals(session.getSpotId()))
                .findFirst().orElse(null);

        if (spot == null) return 5.0;

        String spotType = spot.getType().toString().toUpperCase();

        System.out.println("[RATE DEBUG FOR " + vehicle.getPlate() + "]");
        System.out.println("  Spot ID:        " + session.getSpotId());
        System.out.println("  Spot type:      " + spotType);
        System.out.println("  Vehicle type:   " + vehicle.getType());
        System.out.println("  Has HC card:    " + vehicle.hasHcCard());

        if (vehicle.getType().equalsIgnoreCase("HANDICAPPED") && vehicle.hasHcCard()) {
            System.out.println("  → HC card holder detected");
            if ("HANDICAPPED".equals(spotType)) {
                System.out.println("  → FREE (in HC spot)");
                return 0.0;
            } else {
                System.out.println("  → Discounted RM 2.00 (non-HC spot)");
                return 2.0;
            }
        }

        // normal switch...
        double rate = switch (spotType) {
            case "COMPACT"     -> 2.0;
            case "REGULAR"     -> 5.0;
            case "HANDICAPPED" -> vehicle.hasHcCard() ? 0.0 : 2.0;
            case "RESERVED"    -> 10.0;
            default            -> 5.0;
        };
        System.out.println("  → Final rate: RM " + rate);
        return rate;
    }

    private long calculateHoursCeiling(String entryTimeStr, LocalDateTime exitTime) {
        LocalDateTime entry = LocalDateTime.parse(entryTimeStr, FORMATTER);
        long minutes = Duration.between(entry, exitTime).toMinutes();
        return Math.max(1, (long) Math.ceil(minutes / 60.0));
    }

    private FineScheme mapStringToScheme(String schemeName) {
        if (schemeName == null) return null;
        return switch (schemeName) {
            case "Fixed Fine (RM 50)" -> new fine.FixedFineScheme();
            case "Progressive (Tiered)" -> new fine.ProgressiveFineScheme();
            case "Hourly (RM 20/hr)" -> new fine.HourlyFineScheme();
            default -> null;
        };
    }

    public void setActiveFineScheme(FineScheme scheme) { /* immutable */ }
    public FineScheme getActiveFineScheme() { return activeFineScheme; }
}
