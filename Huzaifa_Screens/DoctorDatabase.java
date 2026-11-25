import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

// Stored doctor information (sample data) plus the search logic used by the Search screen.
public final class DoctorDatabase {

    public static final List<Doctor> ALL = new ArrayList<>();
    public static final List<String> SPECIALTIES = new ArrayList<>();
    public static final List<String> NAMES = new ArrayList<>();
    public static final List<String> SYMPTOMS = new ArrayList<>();
    public static final List<String> LOCATIONS = new ArrayList<>();
    public static final Map<String, String> SYMPTOM_TO_SPECIALTY = new TreeMap<>();

    private static final Map<String, String[]> SYMPTOMS_BY_SPECIALTY = new LinkedHashMap<>();
    private static final Map<String, String> QUALIFICATIONS = new HashMap<>();
    private static final String[] TIMINGS = {
        "Mon-Sat, 9 AM - 2 PM", "Mon-Fri, 4 PM - 8 PM", "Tue-Sat, 10 AM - 3 PM",
        "Mon-Fri, 11 AM - 5 PM", "Mon-Thu, 3 PM - 7 PM", "Mon-Sat, 5 PM - 9 PM"
    };
    private static final String[] HOSPITALS = {
        "Al-Shifa Care Hospital", "City Medical Center", "Noor Health Clinic",
        "Green Valley Hospital", "Rehman Medical Complex"
    };

    private DoctorDatabase() { }

    private static void specialty(String spec, String quals, String... symptoms) {
        QUALIFICATIONS.put(spec, quals);
        SYMPTOMS_BY_SPECIALTY.put(spec, symptoms);
    }

    private static void doc(String name, String spec, String city, int fee, int exp, boolean avail) {
        int n = ALL.size();
        ALL.add(new Doctor("D" + (101 + n), name, spec, city, HOSPITALS[n % HOSPITALS.length],
            fee, exp, QUALIFICATIONS.get(spec), TIMINGS[n % TIMINGS.length], avail,
            SYMPTOMS_BY_SPECIALTY.get(spec)));
    }

    static {
        specialty("General Physician", "MBBS, FCPS (Medicine)",
            "fever", "cough", "fatigue", "headache", "body pain", "flu");
        specialty("Cardiologist", "MBBS, FCPS (Cardiology)",
            "chest pain", "palpitations", "high blood pressure", "shortness of breath");
        specialty("Dermatologist", "MBBS, FCPS (Dermatology)",
            "skin rash", "acne", "hair loss", "itching", "allergy");
        specialty("Orthopedic Surgeon", "MBBS, FCPS (Orthopedics)",
            "joint pain", "back pain", "knee pain", "fracture");
        specialty("Pediatrician", "MBBS, DCH, FCPS (Pediatrics)",
            "child fever", "diarrhea in children", "growth concerns", "childhood cough");
        specialty("Neurologist", "MBBS, FCPS (Neurology)",
            "migraine", "dizziness", "numbness", "seizures");
        specialty("Gynecologist", "MBBS, FCPS (Gynecology)",
            "pregnancy checkup", "irregular periods", "pelvic pain");
        specialty("ENT Specialist", "MBBS, FCPS (ENT)",
            "sore throat", "ear pain", "sinus", "hearing loss");
        specialty("Psychiatrist", "MBBS, FCPS (Psychiatry)",
            "anxiety", "depression", "insomnia", "stress");
        specialty("Gastroenterologist", "MBBS, FCPS (Gastroenterology)",
            "stomach pain", "acidity", "nausea", "constipation", "bloating", "vomiting");
        specialty("Ophthalmologist", "MBBS, FCPS (Ophthalmology)",
            "blurred vision", "eye pain", "red eyes");
        specialty("Dentist", "BDS, FCPS (Dentistry)",
            "toothache", "gum bleeding", "bad breath");

        // name, specialty, city, fee (Rs.), years of experience, available
        doc("Sana Ali", "General Physician", "Islamabad", 3000, 8, true);
        doc("Rabia Aslam", "General Physician", "Karachi", 2000, 5, true);
        doc("Faisal Mehmood", "General Physician", "Lahore", 2500, 12, true);
        doc("Nadia Hashmi", "General Physician", "Rawalpindi", 2200, 6, false);
        doc("Waqar Ahmed", "General Physician", "Peshawar", 1800, 9, true);
        doc("Saima Gul", "General Physician", "Multan", 1500, 4, true);

        doc("Ahmed Raza", "Cardiologist", "Islamabad", 5000, 15, true);
        doc("Imran Qureshi", "Cardiologist", "Rawalpindi", 4500, 10, true);
        doc("Asad Mirza", "Cardiologist", "Lahore", 6000, 18, false);
        doc("Farah Naz", "Cardiologist", "Karachi", 5500, 12, true);
        doc("Shahid Khattak", "Cardiologist", "Peshawar", 4000, 9, true);

        doc("Hina Malik", "Dermatologist", "Islamabad", 3500, 10, false);
        doc("Zainab Siddiqui", "Dermatologist", "Islamabad", 4000, 6, true);
        doc("Mehwish Raza", "Dermatologist", "Lahore", 3000, 7, true);
        doc("Adeel Hussain", "Dermatologist", "Karachi", 3500, 11, true);
        doc("Sobia Anwar", "Dermatologist", "Rawalpindi", 2800, 5, true);

        doc("Usman Tariq", "Orthopedic Surgeon", "Rawalpindi", 4000, 12, true);
        doc("Haris Javed", "Orthopedic Surgeon", "Islamabad", 5000, 14, true);
        doc("Salman Butt", "Orthopedic Surgeon", "Lahore", 4500, 13, false);
        doc("Danish Memon", "Orthopedic Surgeon", "Karachi", 4200, 9, true);
        doc("Zubair Khan", "Orthopedic Surgeon", "Peshawar", 3500, 10, true);

        doc("Ayesha Khan", "Pediatrician", "Islamabad", 2500, 9, true);
        doc("Maria Qadir", "Pediatrician", "Lahore", 2800, 8, true);
        doc("Owais Rana", "Pediatrician", "Karachi", 2400, 6, false);
        doc("Komal Shahid", "Pediatrician", "Rawalpindi", 2000, 5, true);
        doc("Ibrahim Yousaf", "Pediatrician", "Multan", 1800, 7, true);

        doc("Bilal Hussain", "Neurologist", "Lahore", 6000, 14, true);
        doc("Omar Farid", "Neurologist", "Islamabad", 7000, 17, true);
        doc("Rukhsar Bibi", "Neurologist", "Karachi", 6500, 12, false);
        doc("Taimoor Gill", "Neurologist", "Rawalpindi", 5500, 9, true);
        doc("Anum Zafar", "Neurologist", "Peshawar", 5000, 8, true);

        doc("Fatima Noor", "Gynecologist", "Lahore", 4500, 11, false);
        doc("Samina Akram", "Gynecologist", "Islamabad", 5000, 16, true);
        doc("Nimra Saeed", "Gynecologist", "Rawalpindi", 3500, 7, true);
        doc("Humaira Baig", "Gynecologist", "Karachi", 4000, 13, true);
        doc("Rubina Shah", "Gynecologist", "Quetta", 3000, 10, true);

        doc("Kamran Shah", "ENT Specialist", "Rawalpindi", 3000, 7, true);
        doc("Junaid Iqbal", "ENT Specialist", "Islamabad", 3800, 10, true);
        doc("Sidra Aman", "ENT Specialist", "Lahore", 3200, 6, true);
        doc("Fahad Lodhi", "ENT Specialist", "Karachi", 3500, 9, false);
        doc("Yasir Bashir", "ENT Specialist", "Faisalabad", 2500, 8, true);

        doc("Sadia Rehman", "Psychiatrist", "Karachi", 5000, 13, true);
        doc("Hassan Ijaz", "Psychiatrist", "Islamabad", 6000, 15, true);
        doc("Laiba Tahir", "Psychiatrist", "Lahore", 4500, 6, true);
        doc("Mustafa Kamal", "Psychiatrist", "Rawalpindi", 4000, 9, true);
        doc("Areeba Fatima", "Psychiatrist", "Peshawar", 3500, 5, false);

        doc("Tahir Mehmood", "Gastroenterologist", "Karachi", 5500, 16, false);
        doc("Mariam Javed", "Gastroenterologist", "Islamabad", 6000, 12, true);
        doc("Shoaib Anjum", "Gastroenterologist", "Lahore", 5000, 11, true);
        doc("Rida Nasir", "Gastroenterologist", "Rawalpindi", 4500, 8, true);
        doc("Kashif Rauf", "Gastroenterologist", "Multan", 3500, 10, true);

        doc("Hamza Farooq", "Ophthalmologist", "Lahore", 3500, 9, true);
        doc("Amna Latif", "Ophthalmologist", "Islamabad", 4000, 11, true);
        doc("Furqan Ali", "Ophthalmologist", "Karachi", 3800, 14, true);
        doc("Iqra Sheikh", "Ophthalmologist", "Rawalpindi", 3000, 6, false);
        doc("Rehan Dar", "Ophthalmologist", "Quetta", 2800, 7, true);

        doc("Naveed Akhtar", "Dentist", "Peshawar", 2000, 8, true);
        doc("Mahnoor Aziz", "Dentist", "Islamabad", 3000, 7, true);
        doc("Talha Chaudhry", "Dentist", "Lahore", 2500, 9, true);
        doc("Sumaira Khalid", "Dentist", "Karachi", 2800, 12, true);
        doc("Arslan Mughal", "Dentist", "Rawalpindi", 2200, 5, false);

        // Suggestion lists for the dropdowns
        TreeSet<String> sp = new TreeSet<>(), nm = new TreeSet<>(),
            sy = new TreeSet<>(), lo = new TreeSet<>();
        for (Doctor d : ALL) {
            sp.add(d.specialty);
            nm.add(d.name);
            lo.add(d.location);
            for (String s : d.symptoms) {
                sy.add(s);
                SYMPTOM_TO_SPECIALTY.putIfAbsent(s, d.specialty);
            }
        }
        SPECIALTIES.addAll(sp);
        NAMES.addAll(nm);
        SYMPTOMS.addAll(sy);
        LOCATIONS.addAll(lo);
    }

    // Empty text means "do not filter on this field".
    public static List<Doctor> search(String specialty, String name, String symptom,
                                      String location, int minFee, int maxFee) {
        List<Doctor> result = new ArrayList<>();
        for (Doctor d : ALL) {
            if (!specialty.isEmpty() && !contains(d.specialty, specialty)) continue;
            if (!name.isEmpty() && !contains(d.name, name)) continue;
            if (!symptom.isEmpty() && !matchesSymptom(d, symptom)) continue;
            if (!location.isEmpty() && !contains(d.location, location)) continue;
            if (d.fee < minFee || d.fee > maxFee) continue;
            result.add(d);
        }
        return result;
    }

    // Ranks doctors by how many of the entered criteria they match (best first).
    // Used when no doctor matches every criterion. scores receives each doctor's count.
    public static List<Doctor> closest(String specialty, String name, String symptom, String location,
                                       boolean feeActive, int minFee, int maxFee,
                                       Map<Doctor, Integer> scores) {
        List<Doctor> result = new ArrayList<>();
        for (Doctor d : ALL) {
            int score = 0;
            if (!specialty.isEmpty() && contains(d.specialty, specialty)) score++;
            if (!name.isEmpty() && contains(d.name, name)) score++;
            if (!symptom.isEmpty() && matchesSymptom(d, symptom)) score++;
            if (!location.isEmpty() && contains(d.location, location)) score++;
            if (feeActive && d.fee >= minFee && d.fee <= maxFee) score++;
            if (score > 0) {
                result.add(d);
                scores.put(d, score);
            }
        }
        result.sort((a, b) -> {
            int c = scores.get(b) - scores.get(a);
            if (c != 0) return c;
            if (a.available != b.available) return a.available ? -1 : 1;
            return a.name.compareTo(b.name);
        });
        return result.size() > 10 ? new ArrayList<>(result.subList(0, 10)) : result;
    }

    // Available doctors: same specialty + city first, then same specialty, then same city
    public static List<Doctor> alternatives(Doctor d) {
        List<Doctor> result = new ArrayList<>();
        for (Doctor o : ALL)
            if (o.available && o != d && o.specialty.equals(d.specialty) && o.location.equals(d.location)) result.add(o);
        for (Doctor o : ALL)
            if (o.available && o != d && o.specialty.equals(d.specialty) && !result.contains(o)) result.add(o);
        for (Doctor o : ALL)
            if (o.available && o != d && o.location.equals(d.location) && !result.contains(o)) result.add(o);
        return result;
    }

    public static Doctor findByName(String name) {
        for (Doctor d : ALL) if (d.name.equalsIgnoreCase(name)) return d;
        return null;
    }

    private static boolean contains(String text, String part) {
        return text.toLowerCase().contains(part.toLowerCase());
    }

    private static boolean matchesSymptom(Doctor d, String typed) {
        String t = typed.toLowerCase();
        for (String s : d.symptoms) {
            String sl = s.toLowerCase();
            if (sl.contains(t) || t.contains(sl)) return true;
        }
        return false;
    }
}
