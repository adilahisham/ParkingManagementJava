package service;

import data.DataStore;
import enums.PaymentMethod;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.JTextArea;
import model.FineRecord;
import model.ParkingSession;
import model.ParkingSpot;
import model.PaymentRecord;
import model.Vehicle;

public class PaymentProcessor {

    private final DataStore dataStore;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PaymentProcessor(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Processes payment: parking fee must be fully paid, fines can be partially paid.
     * Returns true if parking fee is covered.
     */
    public boolean processPayment(ParkingSession session, PaymentMethod method, double amountPaid, LocalDateTime exitTime) {
        if (session == null) return false;

        Vehicle vehicle = session.getVehicle();
        String plate = vehicle.getPlate();
        String ticketNo = session.getTicketNo();

        // Calculate duration in hours (ceiling)
        long hours = calculateHoursCeiling(session.getEntryTime(), exitTime);

        // Determine parking fee based on spot & vehicle
        double parkingFee = calculateParkingFee(session, vehicle, hours);

        if (amountPaid < parkingFee) {
            // Parking fee must be fully paid to exit
            return false;
        }

        //  Apply payment
        double amountLeft = amountPaid;

        // Pay parking fee first
        amountLeft -= parkingFee;

        //  Apply remaining payment to fines
        List<FineRecord> unpaidFines = dataStore.getUnpaidFinesByPlate(plate);
        double finePaidTotal = 0.0;

        for (FineRecord fine : unpaidFines) {
            if (amountLeft <= 0) break;

            double fineAmount = fine.getAmount();
            double paymentToApply = Math.min(fineAmount, amountLeft);

            dataStore.reduceFineAmount(fine, paymentToApply); // partial or full payment
            amountLeft -= paymentToApply;
            finePaidTotal += paymentToApply;
        }

        //  Record payment
        PaymentRecord finalRecord = new PaymentRecord(
                ticketNo,
                plate,
                method,
                exitTime,
                (int) hours,
                parkingFee,
                finePaidTotal,
                amountPaid
        );
        dataStore.createPayment(finalRecord);

        // Close session & release spot
        String exitTimeStr = exitTime.format(FORMATTER);
        dataStore.closeSession(ticketNo, exitTimeStr, (int) hours, parkingFee);
        dataStore.setSpotAvailable(session.getSpotId());

        System.out.println("Unpaid fines for " + plate + ": "
            + dataStore.getUnpaidFinesByPlate(plate).size());


        // Print receipt to console
        printReceipt(finalRecord, unpaidFines);

        return true;
    }

    public boolean processPartialPayment(ParkingSession session, PaymentMethod method, double amountPaid, LocalDateTime exitTime) {
        if (session == null) return false;

        Vehicle vehicle = session.getVehicle();
        String plate = vehicle.getPlate();
        String ticketNo = session.getTicketNo();

        long hours = calculateHoursCeiling(session.getEntryTime(), exitTime);
        double parkingFee = calculateParkingFee(session, vehicle, hours);

        // Get unpaid fines for this plate
        List<FineRecord> unpaidFines = dataStore.getUnpaidFinesByPlate(plate);
        double totalFines = unpaidFines.stream().mapToDouble(FineRecord::getAmount).sum();

        double amountLeft = amountPaid;

        // --- Pay parking fee first ---
        double paidParking = Math.min(amountLeft, parkingFee);
        amountLeft -= paidParking;

        // --- Pay fines partially if remaining amount ---
        double paidFines = 0;
        for (FineRecord fine : unpaidFines) {
            if (amountLeft <= 0) break;
            double fineRemaining = fine.getAmount();
            double pay = Math.min(amountLeft, fineRemaining);
            dataStore.reduceFineAmount(fine, pay);
            paidFines += pay;
            amountLeft -= pay;
        }

        // --- Record payment ---
        PaymentRecord record = new PaymentRecord(
                ticketNo,
                plate,
                method,
                exitTime,
                (int) hours,
                paidParking,
                paidFines,
                amountPaid
        );

        dataStore.createPayment(record);

        // --- Finalize exit if parking fee fully covered ---
        if (paidParking >= parkingFee) {
            dataStore.closeSession(ticketNo, exitTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")), (int) hours, parkingFee);
            dataStore.setSpotAvailable(session.getSpotId());
        }

        // --- Print receipt to console ---
        printReceipt(record, unpaidFines);

        return true;
    }

    // --- Calculate parking fee based on spot & vehicle ---
    private double calculateParkingFee(ParkingSession session, Vehicle vehicle, long hours) {
        ParkingSpot spot = dataStore.getAllSpots().stream()
                .filter(s -> s.getSpotId().equals(session.getSpotId()))
                .findFirst()
                .orElse(null);

        double rate = 5.0; // default fallback

        if (spot != null) {
            String spotType = spot.getType().toString().toUpperCase();

            switch (spotType) {
                case "COMPACT": rate = 2.0; break;
                case "REGULAR": rate = 5.0; break;
                case "HANDICAPPED": rate = vehicle.hasHcCard() ? 0.0 : 2.0; break;
                case "RESERVED": rate = 10.0; break;
            }
        }

        return hours * rate;
    }

    // --- Ceiling rounding for hours ---
    private long calculateHoursCeiling(String entryTimeStr, LocalDateTime exitTime) {
        LocalDateTime entryTime = LocalDateTime.parse(entryTimeStr, FORMATTER);
        long totalMinutes = java.time.Duration.between(entryTime, exitTime).toMinutes();
        return Math.max(1, (long) Math.ceil(totalMinutes / 60.0));
    }

    // --- GUI version: print receipt to JTextArea ---
    public void printReceipt(PaymentRecord p, List<FineRecord> unpaidFines, JTextArea area) {
        StringBuilder sb = buildReceiptString(p, unpaidFines);
        area.setText(sb.toString());
    }

    // --- Console version: print receipt to System.out ---
    public void printReceipt(PaymentRecord p, List<FineRecord> unpaidFines) {
        StringBuilder sb = buildReceiptString(p, unpaidFines);
        System.out.println(sb.toString());
    }

    // --- Helper to build receipt string ---
    private StringBuilder buildReceiptString(PaymentRecord p, List<FineRecord> unpaidFines) {
        StringBuilder sb = new StringBuilder();

        sb.append("========= PARKING RECEIPT =========\n");
        sb.append("Plate:          ").append(p.getPlate()).append("\n");
        sb.append("Ticket No:      ").append(p.getTicketNo()).append("\n");
        sb.append("Exit Time:      ").append(p.getPaidTime().format(FORMATTER)).append("\n");
        sb.append("-----------------------------------\n");
        sb.append("Parking Fee:    RM ").append(String.format("%.2f", p.getParkingFee())).append("\n");
        sb.append("Fines Paid:     RM ").append(String.format("%.2f", p.getFinePaid())).append("\n");

        double remainingFines = unpaidFines.stream().mapToDouble(FineRecord::getAmount).sum();
        sb.append("Remaining Fines: RM ").append(String.format("%.2f", remainingFines)).append("\n");
        sb.append("TOTAL DUE:      RM ").append(String.format("%.2f", p.getTotalDue())).append("\n");
        sb.append("-----------------------------------\n");
        sb.append("Payment Method: ").append(p.getMethod()).append("\n");
        sb.append("Amount Paid:    RM ").append(String.format("%.2f", p.getAmountPaid())).append("\n");
        sb.append("Balance/Change: RM ").append(String.format("%.2f", p.getBalance())).append("\n");
        sb.append("===================================\n");

        return sb;
    }

}
