import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

// MainFrame is the Welcome screen of the application
public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("PULSE Patient Portal");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(Theme.banner("PULSE Patient Portal",
                "Healthcare information and appointment system"), BorderLayout.NORTH);

        JPanel background = new JPanel(new GridBagLayout());
        background.setBackground(Theme.BG);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(new LineBorder(new Color(200, 215, 225)),
                new EmptyBorder(30, 35, 30, 35)));

        JLabel heading = new JLabel("Welcome to PULSE");
        heading.setFont(Theme.HEADING);
        heading.setForeground(Theme.PRIMARY);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subheading = new JLabel("Select an option to continue");
        subheading.setFont(Theme.BODY);
        subheading.setForeground(Theme.NEUTRAL);
        subheading.setAlignmentX(Component.CENTER_ALIGNMENT);

        Theme.PulseButton appointmentButton =
                new Theme.PulseButton("Make Appointment", Theme.PRIMARY);
        Theme.PulseButton scheduleButton =
                new Theme.PulseButton("Update Schedule", Theme.ACCENT);
        Theme.PulseButton diseaseButton =
                new Theme.PulseButton("Manage Disease Information", Theme.PRIMARY);
        Theme.PulseButton paymentButton =
                new Theme.PulseButton("Make Payment", Theme.ACCENT);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.setOpaque(false);
        buttons.add(appointmentButton);
        buttons.add(scheduleButton);
        buttons.add(diseaseButton);
        buttons.add(paymentButton);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);

        appointmentButton.addActionListener(e -> {
            new MakeAppointment();
            dispose();
        });

        scheduleButton.addActionListener(e -> {
            new UpdateSchedule();
            dispose();
        });

        diseaseButton.addActionListener(e -> {
            new ManageDisease();
            dispose();
        });

        paymentButton.addActionListener(e -> {
            new MakePayment();
            dispose();
        });

        card.add(heading);
        card.add(Box.createVerticalStrut(10));
        card.add(subheading);
        card.add(Box.createVerticalStrut(28));
        card.add(buttons);

        background.add(card);
        add(background, BorderLayout.CENTER);

        setVisible(true);
    }
}
