package fine;

public interface FineScheme {

    /**
     * @param overstayHours The number of hours AFTER the initial 24-hour grace period.
     * @return The total fine amount in RM.
     */

    // Calculates fine based on how many hours OVER the 24h limit the vehicle stayed
    double calculateFine(long overstayHours);
    String getSchemeName();
}