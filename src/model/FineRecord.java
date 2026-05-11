package model;

import enums.FineReason;

public class FineRecord {
    private int id;
    private String plate;
    private FineReason reason;    // use enum
    private double amount;      // current unpaid amount
    private String issuedTime;
    private boolean paid;
    private String paidAt;

    // Constructor without ID
    public FineRecord(String plate, FineReason reason, double amount, String issuedTime, boolean paid) {
        this.plate = plate;
        this.reason = reason;
        this.amount = amount;
        this.issuedTime = issuedTime;
        this.paid = paid;
    }

    // Constructor with ID (DB loaded)
    public FineRecord(int id, String plate, FineReason reason, double amount, String issuedTime, boolean paid, String paidAt) {
        this.id = id;
        this.plate = plate;
        this.reason = reason;
        this.amount = amount;
        this.issuedTime = issuedTime;
        this.paid = paid;
        this.paidAt = paidAt;
    }

    // Getters
    public int getId() { return id; }
    public String getPlate() { return plate; }
    public FineReason getReason() { return reason; }
    public double getAmount() { return amount; }
    public String getIssuedTime() { return issuedTime; }
    public boolean isPaid() { return paid; }
    public String getPaidAt() { return paidAt; }

    // Setters
    public void setPaid(boolean paid) { this.paid = paid; }
    public void setPaidAt(String paidAt) { this.paidAt = paidAt; }

    // Reduce fine by partial payment
    public void reduceAmount(double paidAmount) {
        if (paidAmount >= amount) {
            amount = 0;
            paid = true;
        } else {
            amount -= paidAmount;
        }
    }
}
