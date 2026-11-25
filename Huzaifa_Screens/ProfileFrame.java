import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.*;

// Manage Doctor Profile + Manage Patient Profile (one screen, two use cases).
// Flow: choose role + enter account ID -> view current profile -> Edit Profile -> Save / Cancel.
public class ProfileFrame extends JFrame {

    private static final Color BORDER_GRAY = new Color(180, 195, 205);
    private static final Color HINT_GRAY = new Color(120, 130, 140);
    private static final DateTimeFormatter DOB_FORMAT =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    // key, label, input type (text / combo), hint shown under the field
    private static final String[][] DOCTOR_FIELDS = {
        {"specialty", "Specialization", "combo", "e.g. Cardiologist"},
        {"qualifications", "Qualifications", "text", "e.g. MBBS, FCPS (Cardiology)"},
        {"experience", "Years of Experience", "text", "Whole number, e.g. 8"},
        {"location", "Location (City)", "combo", "e.g. Islamabad"},
        {"contact", "Contact Number", "text", "e.g. 0300-1234567 or +923001234567"},
        {"fee", "Consultation Fee (Rs.)", "text", "Whole number, e.g. 3000"}
    };
    private static final String[][] PATIENT_FIELDS = {
        {"name", "Full Name", "text", "e.g. Ali Khan"},
        {"contact", "Contact Number", "text", "e.g. 0300-1234567 or +923001234567"},
        {"dob", "Date of Birth", "text", "Format DD/MM/YYYY, e.g. 15/08/2001"},
        {"address", "Address", "text", "House, street, city"}
    };

    private String role = "doctor";
    private String accountId = "";
    private Map<String, String> data = new LinkedHashMap<>();
    private Component currentScreen;

    private final Map<String, JComponent> inputs = new LinkedHashMap<>();
    private final Map<String, JLabel> messages = new LinkedHashMap<>();

    public ProfileFrame() {
        setTitle("PULSE - Manage Profile");
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1100, (int) (screen.width * 0.9)), Math.min(720, (int) (screen.height * 0.9)));
        setMinimumSize(new Dimension(Math.min(900, getWidth()), Math.min(600, getHeight())));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(Theme.banner("Manage Profile",
            "Doctors and patients can add or update their details"), BorderLayout.NORTH);

        showRoleScreen();
        setVisible(true);
    }

    private boolean isDoctor() { return role.equals("doctor"); }
    private String[][] fields() { return isDoctor() ? DOCTOR_FIELDS : PATIENT_FIELDS; }

    // ---------------------------------------------------------------- screen helpers

    private void setScreen(JPanel card) {
        JPanel background = new JPanel(new GridBagLayout());
        background.setBackground(Theme.BG);
        background.add(card);
        JScrollPane scroll = new JScrollPane(background);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        if (currentScreen != null) remove(currentScreen);
        currentScreen = scroll;
        add(scroll, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JPanel newCard(int vertical, int horizontal) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(new LineBorder(new Color(200, 215, 225)),
            new EmptyBorder(vertical, horizontal, vertical, horizontal)));
        return card;
    }

    private JLabel centered(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private JPanel buttonRow(Theme.PulseButton... buttons) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        row.setOpaque(false);
        for (Theme.PulseButton b : buttons) {
            b.setFont(new Font("Arial", Font.BOLD, 16));
            b.setBorder(new EmptyBorder(12, 30, 12, 30));
            row.add(b);
        }
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        return row;
    }

    private String val(String key) {
        String v = data.get(key);
        return v == null ? "" : v;
    }

    // ---------------------------------------------------------------- screen 1: choose role

    private void showRoleScreen() {
        JPanel card = newCard(40, 80);

        JLabel heading = centered("Who are you?", new Font("Arial", Font.BOLD, 36), Theme.PRIMARY);
        JLabel sub = centered("Choose your account type and enter your account ID",
            new Font("Arial", Font.PLAIN, 18), Color.DARK_GRAY);

        JRadioButton doctorRadio = new JRadioButton("Doctor", isDoctor());
        JRadioButton patientRadio = new JRadioButton("Patient", !isDoctor());
        ButtonGroup group = new ButtonGroup();
        for (JRadioButton r : new JRadioButton[]{doctorRadio, patientRadio}) {
            r.setFont(new Font("Arial", Font.BOLD, 22));
            r.setBackground(Color.WHITE);
            r.setFocusPainted(false);
            group.add(r);
        }
        JPanel radios = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 0));
        radios.setOpaque(false);
        radios.add(doctorRadio);
        radios.add(patientRadio);
        radios.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel idLabel = centered("Account ID", Theme.HEADING, Color.DARK_GRAY);
        JTextField idField = new JTextField(accountId, 14);
        idField.setFont(new Font("Arial", Font.PLAIN, 20));
        idField.setHorizontalAlignment(JTextField.CENTER);
        idField.setBorder(new CompoundBorder(new LineBorder(BORDER_GRAY), new EmptyBorder(6, 10, 6, 10)));
        idField.setMaximumSize(new Dimension(300, 44));
        idField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = centered("Doctor ID looks like D101, Patient ID looks like P1001",
            new Font("Arial", Font.PLAIN, 13), HINT_GRAY);
        JLabel error = centered(" ", new Font("Arial", Font.BOLD, 14), Theme.DANGER);

        Theme.PulseButton continueButton = new Theme.PulseButton("Continue", Theme.PRIMARY);
        Theme.PulseButton homeButton = new Theme.PulseButton("Back to Home", Theme.NEUTRAL);

        Runnable proceed = () -> {
            String r = doctorRadio.isSelected() ? "doctor" : "patient";
            String id = idField.getText().trim().toUpperCase();
            String pattern = r.equals("doctor") ? "D\\d{3,6}" : "P\\d{3,6}";
            if (id.isEmpty()) {
                error.setText("Please enter your account ID.");
            } else if (!id.matches(pattern)) {
                error.setText(r.equals("doctor")
                    ? "A doctor ID must start with D followed by digits (e.g. D101)."
                    : "A patient ID must start with P followed by digits (e.g. P1001).");
            } else {
                role = r;
                accountId = id;
                data = ProfileStore.load(role, accountId);
                if (data.isEmpty()) {
                    showForm();       // profile not added yet -> empty form
                } else {
                    showView();       // show current profile details
                }
            }
        };
        continueButton.addActionListener(e -> proceed.run());
        idField.addActionListener(e -> proceed.run());
        homeButton.addActionListener(e -> {
            new MainFrame();
            dispose();
        });

        card.add(heading);
        card.add(Box.createVerticalStrut(10));
        card.add(sub);
        card.add(Box.createVerticalStrut(30));
        card.add(radios);
        card.add(Box.createVerticalStrut(30));
        card.add(idLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(idField);
        card.add(Box.createVerticalStrut(6));
        card.add(hint);
        card.add(Box.createVerticalStrut(14));
        card.add(error);
        card.add(Box.createVerticalStrut(18));
        card.add(buttonRow(continueButton, homeButton));
        setScreen(card);
    }

    // ---------------------------------------------------------------- screen 2: view profile

    private void showView() {
        JPanel card = newCard(34, 70);

        card.add(centered(isDoctor() ? "Doctor Profile" : "Patient Profile",
            new Font("Arial", Font.BOLD, 34), Theme.PRIMARY));
        card.add(Box.createVerticalStrut(6));
        card.add(centered("Account ID: " + accountId, new Font("Arial", Font.PLAIN, 16), Theme.NEUTRAL));
        card.add(Box.createVerticalStrut(22));

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 10, 7, 10);
        c.anchor = GridBagConstraints.WEST;
        int row = 0;
        if (isDoctor() && !val("name").isEmpty()) {
            addViewRow(grid, c, row++, "Doctor Name", "Dr. " + val("name"));
        }
        for (String[] f : fields()) {
            String text = val(f[0]);
            if (text.isEmpty()) {
                text = "Not added yet";
            } else if (f[0].equals("fee")) {
                text = "Rs. " + String.format("%,d", Integer.parseInt(text));
            } else if (f[0].equals("experience")) {
                text = text + (text.equals("1") ? " year" : " years");
            }
            addViewRow(grid, c, row++, f[1], text);
        }
        grid.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(grid);
        card.add(Box.createVerticalStrut(18));

        if (isDoctor()) {
            JLabel info = new JLabel("<html><div style='text-align:center; width:480px'>"
                + "These details are shown to patients when they search for and view your doctor profile."
                + "</div></html>");
            info.setFont(new Font("Arial", Font.PLAIN, 14));
            info.setOpaque(true);
            info.setBackground(Theme.INFO_BG);
            info.setBorder(new CompoundBorder(new LineBorder(Theme.INFO_BORDER), new EmptyBorder(10, 14, 10, 14)));
            info.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(info);
            card.add(Box.createVerticalStrut(22));
        }

        Theme.PulseButton edit = new Theme.PulseButton("Edit Profile", Theme.PRIMARY);
        Theme.PulseButton back = new Theme.PulseButton("Switch User", Theme.NEUTRAL);
        edit.addActionListener(e -> showForm());
        back.addActionListener(e -> showRoleScreen());
        card.add(buttonRow(edit, back));
        setScreen(card);
    }

    private void addViewRow(JPanel grid, GridBagConstraints c, int row, String label, String value) {
        c.gridy = row;
        c.gridx = 0;
        JLabel l = new JLabel(label + ":");
        l.setFont(new Font("Arial", Font.BOLD, 17));
        l.setForeground(Theme.NEUTRAL);
        grid.add(l, c);
        c.gridx = 1;
        JLabel v = new JLabel(value);
        v.setFont(new Font("Arial", Font.PLAIN, 17));
        if (value.equals("Not added yet")) v.setForeground(HINT_GRAY);
        grid.add(v, c);
    }

    // ---------------------------------------------------------------- screen 3: edit / add profile

    private void showForm() {
        boolean existing = !data.isEmpty();
        JPanel card = newCard(30, 60);
        inputs.clear();
        messages.clear();

        card.add(centered((isDoctor() ? "Doctor Profile" : "Patient Profile")
            + (existing ? " - Edit" : " - Add Details"), new Font("Arial", Font.BOLD, 30), Theme.PRIMARY));
        card.add(Box.createVerticalStrut(6));
        card.add(centered("Account ID: " + accountId, new Font("Arial", Font.PLAIN, 16), Theme.NEUTRAL));
        card.add(Box.createVerticalStrut(14));

        if (!existing) {
            JLabel info = new JLabel("<html><div style='width:520px'>No profile details found yet. "
                + "Please fill in the details below and click Save.</div></html>");
            info.setFont(new Font("Arial", Font.PLAIN, 14));
            info.setOpaque(true);
            info.setBackground(Theme.INFO_BG);
            info.setBorder(new CompoundBorder(new LineBorder(Theme.INFO_BORDER), new EmptyBorder(8, 12, 8, 12)));
            info.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(info);
            card.add(Box.createVerticalStrut(14));
        }

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        int row = 0;
        for (String[] f : fields()) {
            String key = f[0];

            c.gridx = 0;
            c.gridy = row;
            c.insets = new Insets(8, 6, 0, 16);
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0;
            JLabel label = new JLabel(f[1] + " *");
            label.setFont(new Font("Arial", Font.BOLD, 16));
            grid.add(label, c);

            JComponent input;
            if (f[2].equals("combo")) {
                java.util.List<String> options = key.equals("specialty")
                    ? DoctorDatabase.SPECIALTIES : DoctorDatabase.LOCATIONS;
                JComboBox<String> combo = new JComboBox<>(options.toArray(new String[0]));
                combo.setEditable(true);
                combo.setSelectedItem(val(key));
                combo.setBackground(Color.WHITE);
                input = combo;
            } else {
                JTextField tf = new JTextField(val(key));
                tf.setBorder(new CompoundBorder(new LineBorder(BORDER_GRAY), new EmptyBorder(4, 8, 4, 8)));
                input = tf;
            }
            input.setFont(new Font("Arial", Font.PLAIN, 16));
            input.setPreferredSize(new Dimension(400, 38));
            c.gridx = 1;
            c.insets = new Insets(8, 0, 0, 0);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;
            grid.add(input, c);
            inputs.put(key, input);

            JLabel msg = new JLabel(f[3]);
            msg.setFont(new Font("Arial", Font.PLAIN, 12));
            msg.setForeground(HINT_GRAY);
            c.gridy = row + 1;
            c.insets = new Insets(1, 0, 0, 0);
            grid.add(msg, c);
            messages.put(key, msg);

            row += 2;
        }
        grid.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(grid);
        card.add(Box.createVerticalStrut(10));

        JLabel status = centered(" ", new Font("Arial", Font.BOLD, 14), Theme.DANGER);
        card.add(status);
        card.add(Box.createVerticalStrut(12));

        Theme.PulseButton save = new Theme.PulseButton("Save", Theme.ACCENT);
        Theme.PulseButton cancel = new Theme.PulseButton("Cancel", Theme.NEUTRAL);
        save.addActionListener(e -> saveProfile(existing, status));
        cancel.addActionListener(e -> {
            // Cancel: nothing is applied, go back to the saved profile
            if (existing) showView();
            else showRoleScreen();
        });
        card.add(buttonRow(save, cancel));
        setScreen(card);
    }

    // ---------------------------------------------------------------- save + validation

    private String readInput(String key) {
        JComponent comp = inputs.get(key);
        if (comp instanceof JTextField) return ((JTextField) comp).getText().trim();
        Object item = ((JComboBox<?>) comp).getEditor().getItem();
        return item == null ? "" : item.toString().trim();
    }

    private void saveProfile(boolean existing, JLabel status) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String[] f : fields()) values.put(f[0], readInput(f[0]));

        Map<String, String> errors = validate(values);

        // reset every field to its hint, then show the errors found
        for (String[] f : fields()) {
            messages.get(f[0]).setText(f[3]);
            messages.get(f[0]).setForeground(HINT_GRAY);
            markField(f[0], false);
        }
        if (!errors.isEmpty()) {
            for (Map.Entry<String, String> e : errors.entrySet()) {
                messages.get(e.getKey()).setText(e.getValue());
                messages.get(e.getKey()).setForeground(Theme.DANGER);
                markField(e.getKey(), true);
            }
            status.setText("Please correct the highlighted fields and click Save again.");
            return;
        }

        Map<String, String> updated = new LinkedHashMap<>(data);
        updated.putAll(values);
        if (!ProfileStore.save(role, accountId, updated)) {
            status.setText("Could not save the profile file. Check that the folder is writable.");
            return;
        }
        data = updated;
        JOptionPane.showMessageDialog(this,
            (isDoctor() ? "Doctor" : "Patient") + " profile "
                + (existing ? "updated" : "saved") + " successfully.",
            "Profile Saved", JOptionPane.INFORMATION_MESSAGE);
        showView();
    }

    private void markField(String key, boolean bad) {
        JComponent comp = inputs.get(key);
        if (comp instanceof JTextField) {
            comp.setBorder(new CompoundBorder(new LineBorder(bad ? Theme.DANGER : BORDER_GRAY, bad ? 2 : 1),
                new EmptyBorder(bad ? 3 : 4, bad ? 7 : 8, bad ? 3 : 4, bad ? 7 : 8)));
        }
    }

    // Checks the required details and entered values. Also tidies the values (e.g. contact number).
    private Map<String, String> validate(Map<String, String> v) {
        Map<String, String> err = new LinkedHashMap<>();

        if (isDoctor()) {
            if (v.get("specialty").isEmpty()) err.put("specialty", "Specialization is required.");
            else if (!v.get("specialty").matches("[A-Za-z][A-Za-z .&()-]{2,49}"))
                err.put("specialty", "Enter a valid specialization (letters only).");

            if (v.get("qualifications").isEmpty()) err.put("qualifications", "Qualifications are required.");
            else if (v.get("qualifications").length() < 2 || v.get("qualifications").length() > 100)
                err.put("qualifications", "Qualifications must be 2 to 100 characters.");

            String exp = v.get("experience");
            if (exp.isEmpty()) err.put("experience", "Years of experience is required.");
            else if (!exp.matches("\\d{1,2}")) err.put("experience", "Enter a whole number (0 to 60).");
            else if (Integer.parseInt(exp) > 60) err.put("experience", "Experience cannot be more than 60 years.");
            else v.put("experience", String.valueOf(Integer.parseInt(exp)));

            if (v.get("location").isEmpty()) err.put("location", "Location is required.");
            else if (!v.get("location").matches("[A-Za-z][A-Za-z .-]{1,39}"))
                err.put("location", "Enter a valid city name (letters only).");

            checkContact(v, err);

            String fee = v.get("fee").replace(",", "");
            if (fee.isEmpty()) err.put("fee", "Consultation fee is required.");
            else if (!fee.matches("\\d{1,6}")) err.put("fee", "Enter the fee as a whole number.");
            else if (Integer.parseInt(fee) < 100 || Integer.parseInt(fee) > 100000)
                err.put("fee", "Fee must be between Rs. 100 and Rs. 100,000.");
            else v.put("fee", String.valueOf(Integer.parseInt(fee)));
        } else {
            if (v.get("name").isEmpty()) err.put("name", "Patient name is required.");
            else if (!v.get("name").matches("[A-Za-z][A-Za-z .'-]{1,59}"))
                err.put("name", "Name can contain only letters and spaces.");

            checkContact(v, err);

            String dob = v.get("dob");
            if (dob.isEmpty()) {
                err.put("dob", "Date of birth is required.");
            } else {
                try {
                    LocalDate date = LocalDate.parse(dob, DOB_FORMAT);
                    if (date.isAfter(LocalDate.now())) err.put("dob", "Date of birth cannot be in the future.");
                    else if (date.getYear() < 1900) err.put("dob", "Enter a valid date of birth (year 1900 or later).");
                } catch (DateTimeParseException ex) {
                    err.put("dob", "Invalid date. Use DD/MM/YYYY (e.g. 15/08/2001).");
                }
            }

            if (v.get("address").isEmpty()) err.put("address", "Address is required.");
            else if (v.get("address").length() < 5 || v.get("address").length() > 120)
                err.put("address", "Address must be 5 to 120 characters.");
        }
        return err;
    }

    private void checkContact(Map<String, String> v, Map<String, String> err) {
        String contact = v.get("contact");
        if (contact.isEmpty()) {
            err.put("contact", "Contact number is required.");
            return;
        }
        String digits = contact.replace(" ", "").replace("-", "");
        if (!digits.matches("\\+?\\d{10,13}")) err.put("contact", "Enter a valid number (10 to 13 digits).");
        else v.put("contact", digits);
    }
}
