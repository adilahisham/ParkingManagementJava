package enums;

//represent the type of parking spot
//each constant stores its hourly rate
//to retrieve the rate: spotType.getHourlyRate()

public enum SpotType{
    COMPACT(2),
    REGULAR(5),
    HANDICAPPED(2),
    RESERVED(10);

    private final double hourlyRate;

    //constructor
    SpotType(double hourlyRate){
        this.hourlyRate = hourlyRate;
    }

    //getter
    public double getHourlyRate(){
    return hourlyRate;
    }
}

