// One doctor record stored in the PULSE doctor database.
public class Doctor {
    public final String id, name, specialty, location, hospital, qualifications, timings;
    public final int fee, experience;
    public final boolean available;
    public final String[] symptoms;

    public Doctor(String id, String name, String specialty, String location, String hospital,
                  int fee, int experience, String qualifications, String timings,
                  boolean available, String[] symptoms) {
        this.id = id;
        this.name = name;
        this.specialty = specialty;
        this.location = location;
        this.hospital = hospital;
        this.fee = fee;
        this.experience = experience;
        this.qualifications = qualifications;
        this.timings = timings;
        this.available = available;
        this.symptoms = symptoms;
    }

    public String title() { return "Dr. " + name; }
    public String feeText() { return String.format("Rs. %,d", fee); }
}
