package model;

import enums.PaymentMethod;
import java.time.LocalDateTime;

public class PaymentRecord {

    private final String ticketNo;
    private final String plate;
    private final PaymentMethod method;
    private final LocalDateTime paidTime;

    private final int durationHours;
    private final double parkingFee;
    private final double finePaid;
    private final double totalDue;

    private final double amountPaid;
    private final double balance;

    public PaymentRecord(String ticketNo,
                         String plate,
                         PaymentMethod method,
                         LocalDateTime paidTime,
                         int durationHours,
                         double parkingFee,
                         double finePaid,
                         double amountPaid) {

        this.ticketNo = ticketNo;
        this.plate = plate;
        this.method = method;
        this.paidTime = paidTime;
        this.durationHours = durationHours;
        this.parkingFee = parkingFee;
        this.finePaid = finePaid;
        this.totalDue = parkingFee + finePaid;
        this.amountPaid = amountPaid;
        this.balance = amountPaid - totalDue;
    }

    public String getTicketNo() { return ticketNo; }
    public String getPlate() { return plate; }
    public PaymentMethod getMethod() { return method; }
    public LocalDateTime getPaidTime() { return paidTime; }
    public int getDurationHours() { return durationHours; }
    public double getParkingFee() { return parkingFee; }
    public double getFinePaid() { return finePaid; }
    public double getTotalDue() { return totalDue; }
    public double getAmountPaid() { return amountPaid; }
    public double getBalance() { return balance; }

    public boolean isFullyPaid() {
        return balance >= 0;
    }
}
