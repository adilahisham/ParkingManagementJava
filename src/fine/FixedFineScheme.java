package fine;

public class FixedFineScheme implements FineScheme {
    @Override
    public double calculateFine(long overstayHours) {
        return (overstayHours > 0) ? 50.0 : 0.0;
    }

    @Override
    public String getSchemeName() {
        return "Fixed Fine (RM 50)";
    }
}