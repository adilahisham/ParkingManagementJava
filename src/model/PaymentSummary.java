package model;

public class PaymentSummary {

    private final String ticketNo;
    private final String plate;
    private final String exitTime;
    private final int durationHours;
    private final double parkingFee;
    private final double totalFines;
    private final double totalDue;

    public PaymentSummary(String ticketNo,
                          String plate,
                          String exitTime,
                          int durationHours,
                          double parkingFee,
                          double totalFines) {

        this.ticketNo = ticketNo;
        this.plate = plate;
        this.exitTime = exitTime;
        this.durationHours = durationHours;
        this.parkingFee = parkingFee;
        this.totalFines = totalFines;
        this.totalDue = parkingFee + totalFines;
    }

    public String getTicketNo() { return ticketNo; }
    public String getPlate() { return plate; }
    public String getExitTime() { return exitTime; }
    public int getDurationHours() { return durationHours; }
    public double getParkingFee() { return parkingFee; }
    public double getTotalFines() { return totalFines; }
    public double getTotalDue() { return totalDue; }
}
