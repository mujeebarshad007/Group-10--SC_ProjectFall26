import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

// Online appointments (sample data) and the consultation results saved against them.
// Results are kept in pulse_consultations.properties so they survive a restart.
public final class ConsultationStore {

    public static class Appointment {
        public final String id, doctorId, patientId, patientName, time, bookedSymptoms;
        public final boolean timeArrived;                 // has the scheduled time arrived?
        public String status = "Scheduled";               // Scheduled / Incomplete / Completed
        public String symptoms = "", diagnosis = "", prescription = "", instructions = "", completedOn = "";

        Appointment(String id, String doctorId, String patientId, String patientName,
                    String time, String bookedSymptoms, boolean timeArrived) {
            this.id = id;
            this.doctorId = doctorId;
            this.patientId = patientId;
            this.patientName = patientName;
            this.time = time;
            this.bookedSymptoms = bookedSymptoms;
            this.timeArrived = timeArrived;
        }

        public boolean isCompleted() { return status.equals("Completed"); }
    }

    private static final File FILE = new File("pulse_consultations.properties");
    private static final Properties DATA = new Properties();
    private static final List<Appointment> ALL = new ArrayList<>();

    static {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH));
        // id, doctor, patient id, patient name, time, symptoms entered at booking, scheduled time arrived
        add("A5001", "D101", "P1001", "Ali Raza", today + ", 10:00 AM", "fever, cough", true);
        add("A5002", "D101", "P1002", "Ayesha Noor", today + ", 11:30 AM", "headache, fatigue", true);
        add("A5003", "D101", "P1003", "Bilal Ahmed", today + ", 08:00 PM", "body pain", false);
        add("A5004", "D107", "P1004", "Hamza Sheikh", today + ", 10:30 AM", "chest pain, palpitations", true);
        add("A5005", "D107", "P1005", "Sara Khan", today + ", 01:00 PM", "shortness of breath", true);
        add("A5006", "D107", "P1001", "Ali Raza", today + ", 09:00 PM", "high blood pressure", false);
        add("A5007", "D112", "P1002", "Ayesha Noor", today + ", 12:00 PM", "skin rash, itching", true);
        add("A5008", "D112", "P1006", "Usman Tariq", today + ", 07:00 PM", "acne", false);

        if (FILE.exists()) {
            try (FileInputStream in = new FileInputStream(FILE)) {
                DATA.load(in);
            } catch (IOException ignored) {
                // start with the sample data only
            }
            for (Appointment a : ALL) {
                String p = "appt." + a.id + ".";
                a.status = DATA.getProperty(p + "status", a.status);
                a.symptoms = DATA.getProperty(p + "symptoms", "");
                a.diagnosis = DATA.getProperty(p + "diagnosis", "");
                a.prescription = DATA.getProperty(p + "prescription", "");
                a.instructions = DATA.getProperty(p + "instructions", "");
                a.completedOn = DATA.getProperty(p + "completedOn", "");
            }
        }
    }

    private ConsultationStore() { }

    private static void add(String id, String doc, String pid, String pname, String time,
                            String symptoms, boolean arrived) {
        ALL.add(new Appointment(id, doc, pid, pname, time, symptoms, arrived));
    }

    public static List<Appointment> forDoctor(String doctorId) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment a : ALL) if (a.doctorId.equalsIgnoreCase(doctorId)) result.add(a);
        return result;
    }

    // Completed consultations of one patient (their prescriptions become available here).
    public static List<Appointment> completedForPatient(String patientId) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment a : ALL)
            if (a.isCompleted() && a.patientId.equalsIgnoreCase(patientId)) result.add(a);
        return result;
    }

    public static String doctorTitle(String doctorId) {
        for (Doctor d : DoctorDatabase.ALL)
            if (d.id.equalsIgnoreCase(doctorId)) return d.title();
        return "Doctor " + doctorId;
    }

    // Saves the consultation details and status of an appointment. False if the file can't be written.
    public static boolean save(Appointment a) {
        String p = "appt." + a.id + ".";
        DATA.setProperty(p + "status", a.status);
        DATA.setProperty(p + "symptoms", a.symptoms);
        DATA.setProperty(p + "diagnosis", a.diagnosis);
        DATA.setProperty(p + "prescription", a.prescription);
        DATA.setProperty(p + "instructions", a.instructions);
        DATA.setProperty(p + "completedOn", a.completedOn);
        try (FileOutputStream out = new FileOutputStream(FILE)) {
            DATA.store(out, "PULSE consultations");
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
