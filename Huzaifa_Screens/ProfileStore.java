import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

// Saves doctor and patient profiles to a small file (pulse_profiles.properties) so they
// are still there the next time PULSE is opened. Role is "doctor" or "patient".
public final class ProfileStore {

    private static final File FILE = new File("pulse_profiles.properties");
    private static final Properties DATA = new Properties();

    static {
        if (FILE.exists()) {
            try (FileInputStream in = new FileInputStream(FILE)) {
                DATA.load(in);
            } catch (IOException ignored) {
                // start with an empty store
            }
        }
    }

    private ProfileStore() { }

    // Returns the saved profile of this account (empty map = profile not added yet).
    // Sample doctors from DoctorDatabase start with their existing details.
    public static Map<String, String> load(String role, String id) {
        Map<String, String> profile = new LinkedHashMap<>();
        String prefix = role + "." + id + ".";
        for (String key : DATA.stringPropertyNames())
            if (key.startsWith(prefix)) profile.put(key.substring(prefix.length()), DATA.getProperty(key));

        if (profile.isEmpty() && role.equals("doctor")) {
            for (Doctor d : DoctorDatabase.ALL) {
                if (d.id.equalsIgnoreCase(id)) {
                    profile.put("name", d.name);
                    profile.put("specialty", d.specialty);
                    profile.put("qualifications", d.qualifications);
                    profile.put("experience", String.valueOf(d.experience));
                    profile.put("location", d.location);
                    profile.put("fee", String.valueOf(d.fee));
                    break;
                }
            }
        }
        return profile;
    }

    // Saves the profile. Returns false if the file could not be written.
    public static boolean save(String role, String id, Map<String, String> profile) {
        String prefix = role + "." + id + ".";
        for (Map.Entry<String, String> e : profile.entrySet())
            DATA.setProperty(prefix + e.getKey(), e.getValue());
        try (FileOutputStream out = new FileOutputStream(FILE)) {
            DATA.store(out, "PULSE profiles");
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
