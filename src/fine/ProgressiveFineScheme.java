package fine;

public class ProgressiveFineScheme implements FineScheme {

    @Override
    public double calculateFine(long overstayHours) {
        // Early exit - most important safeguard
        if (overstayHours < 1) {
            return 0.0;
        }

        double fine = 50.0;  // base fine for any overstay (first day)

        if (overstayHours > 24) {
            fine += 100.0;
        }
        if (overstayHours > 48) {
            fine += 150.0;
        }
        if (overstayHours > 72) {
            fine += 200.0;
        }

        return fine;
    }

    @Override
    public String getSchemeName() {
        return "Progressive Fine Scheme";
    }
}