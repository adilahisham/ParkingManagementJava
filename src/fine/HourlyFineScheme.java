package fine;

public class HourlyFineScheme implements FineScheme {
    @Override
    public double calculateFine(long overstayHours) {
        if (overstayHours <= 0) return 0.0;
        return overstayHours * 20.0;
    }

    @Override
    public String getSchemeName() {
        return "Hourly Fine (RM 20/hr)";
    }
}