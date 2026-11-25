import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

// Home screen of the PULSE Patient Portal (Search for Doctors + Manage Profile + Online Diagnosis use cases).
public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("PULSE Patient Portal");
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1100, (int) (screen.width * 0.9)), Math.min(720, (int) (screen.height * 0.9)));
        setMinimumSize(new Dimension(Math.min(900, getWidth()), Math.min(600, getHeight())));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(Theme.banner("PULSE Patient Portal",
            "Find doctors, check availability and book appointments"), BorderLayout.NORTH);

        JPanel background = new JPanel(new GridBagLayout());
        background.setBackground(Theme.BG);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(new LineBorder(new Color(200, 215, 225)),
            new EmptyBorder(50, 70, 50, 70)));

        JLabel heading = new JLabel("Welcome to PULSE");
        heading.setFont(new Font("Arial", Font.BOLD, 44));
        heading.setForeground(Theme.PRIMARY);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel steps = new JLabel("<html><div style='text-align:center; width:720px'>"
            + "Search by specialty, symptoms, doctor name, location or fee range. "
            + "Suggestions appear as you type, then pick a doctor, check "
            + "availability and proceed to book an appointment. Doctors and patients "
            + "can also manage their own profile, and doctors can hold online consultations.</div></html>");
        steps.setFont(new Font("Arial", Font.PLAIN, 22));
        steps.setAlignmentX(Component.CENTER_ALIGNMENT);

        Theme.PulseButton searchButton = new Theme.PulseButton("Search for Doctors", Theme.PRIMARY);
        Theme.PulseButton profileButton = new Theme.PulseButton("Manage Profile", Theme.ACCENT);
        Theme.PulseButton diagnosisButton = new Theme.PulseButton("Online Diagnosis", Theme.PRIMARY);
        Theme.PulseButton exitButton = new Theme.PulseButton("Exit", Theme.NEUTRAL);

        Font bigButtonFont = new Font("Arial", Font.BOLD, 22);
        for (Theme.PulseButton b : new Theme.PulseButton[]{searchButton, profileButton, diagnosisButton, exitButton}) {
            b.setFont(bigButtonFont);
            b.setBorder(new EmptyBorder(18, 32, 18, 32));
        }

        searchButton.addActionListener(e -> {
            new Signup();
            dispose();
        });
        profileButton.addActionListener(e -> {
            new ProfileFrame();
            dispose();
        });
        diagnosisButton.addActionListener(e -> {
            new OnlineDiagnosisFrame();
            dispose();
        });
        exitButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to exit?",
                "Exit PULSE", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) System.exit(0);
        });

        JPanel buttons = new JPanel(new GridLayout(2, 2, 20, 20));
        buttons.setOpaque(false);
        buttons.add(searchButton);
        buttons.add(profileButton);
        buttons.add(diagnosisButton);
        buttons.add(exitButton);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(heading);
        card.add(Box.createVerticalStrut(24));
        card.add(steps);
        card.add(Box.createVerticalStrut(44));
        card.add(buttons);

        background.add(card);
        add(background, BorderLayout.CENTER);
        setVisible(true);
    }
}
