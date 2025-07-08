import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

public class StopwatchTimerApp extends JFrame implements ActionListener {
    private DialPanel dialPanel;
    private JLabel timeLabel, dateLabel, weatherLabel, alarmStatusLabel;
    private JButton startButton, stopButton, resetButton, timerButton, switchToStopwatchButton, lapButton,
            darkModeButton, soundButton, alarmButton;
    private JTextArea lapDisplay;
    private JScrollPane lapScrollPane;

    private Timer stopwatchTimer, clockTimer, countdownTimer;
    private boolean isRunning = false;
    private boolean isTimerMode = false;
    private boolean isDarkMode = false;
    private int stopwatchElapsed = 0;
    private int countdownSeconds = 0;
    private static final int TIMER_DELAY = 1000;
    private ArrayList<String> lapTimes = new ArrayList<>();
    private File soundFile = new File("alarm.wav");
    private String alarmTime = null;

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JPanel stopwatchPanel;
    private JPanel alarmPanel;

    public StopwatchTimerApp() {
        setTitle("Advanced Stopwatch + Timer");
        setSize(700, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();
        JMenu modeMenu = new JMenu("Mode");
        JMenuItem stopwatchItem = new JMenuItem("Stopwatch/Timer");
        JMenuItem alarmItem = new JMenuItem("Alarm");
        stopwatchItem.addActionListener(e -> cardLayout.show(mainPanel, "stopwatch"));
        alarmItem.addActionListener(e -> cardLayout.show(mainPanel, "alarm"));
        modeMenu.add(stopwatchItem);
        modeMenu.add(alarmItem);
        menuBar.add(modeMenu);
        setJMenuBar(menuBar);

        // Stopwatch Panel Layout
        stopwatchPanel = new JPanel(new BorderLayout());
        JPanel centerPanel = new JPanel(new GridBagLayout());
        dialPanel = new DialPanel();
        dialPanel.setPreferredSize(new Dimension(600, 500));
        centerPanel.add(dialPanel);
        stopwatchPanel.add(centerPanel, BorderLayout.CENTER);

        timeLabel = new JLabel("00:00:00", SwingConstants.CENTER);
        timeLabel.setFont(new Font("Verdana", Font.BOLD, 28));
        stopwatchPanel.add(timeLabel, BorderLayout.NORTH);

        dateLabel = new JLabel("", SwingConstants.CENTER);
        dateLabel.setFont(new Font("Serif", Font.BOLD, 18));

        weatherLabel = new JLabel("Weather: Loading...", SwingConstants.CENTER);
        weatherLabel.setFont(new Font("Serif", Font.BOLD, 20));

        alarmStatusLabel = new JLabel("\u23F0 No Alarm Set", SwingConstants.CENTER);
        alarmStatusLabel.setFont(new Font("Serif", Font.ITALIC, 16));
        alarmStatusLabel.setForeground(Color.BLUE);

        JPanel verticalLabelsPanel = new JPanel();
        verticalLabelsPanel.setLayout(new BoxLayout(verticalLabelsPanel, BoxLayout.Y_AXIS));
        verticalLabelsPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        verticalLabelsPanel.add(Box.createVerticalGlue());
        verticalLabelsPanel.add(dateLabel);
        verticalLabelsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        verticalLabelsPanel.add(weatherLabel);
        verticalLabelsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        verticalLabelsPanel.add(alarmStatusLabel);
        verticalLabelsPanel.add(Box.createVerticalGlue());

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(verticalLabelsPanel, BorderLayout.CENTER);
        leftPanel.setPreferredSize(new Dimension(200, 150));
        stopwatchPanel.add(leftPanel, BorderLayout.EAST);

        lapDisplay = new JTextArea(6, 10);
        lapDisplay.setEditable(false);
        lapScrollPane = new JScrollPane(lapDisplay);
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(lapScrollPane, BorderLayout.CENTER);
        rightPanel.setPreferredSize(new Dimension(120, 150));
        stopwatchPanel.add(rightPanel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        resetButton = new JButton("Reset");
        timerButton = new JButton("Set Timer");
        switchToStopwatchButton = new JButton("Switch to Stopwatch");
        lapButton = new JButton("Lap");
        darkModeButton = new JButton("Dark Mode");
        soundButton = new JButton("Choose Sound");
        alarmButton = new JButton("Set Alarm");
        switchToStopwatchButton.setVisible(false);

        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(lapButton);
        buttonPanel.add(timerButton);
        buttonPanel.add(switchToStopwatchButton);
        buttonPanel.add(darkModeButton);
        buttonPanel.add(soundButton);
        buttonPanel.add(alarmButton);

        stopwatchPanel.add(buttonPanel, BorderLayout.SOUTH);

        alarmPanel = new JPanel();
        alarmPanel.setLayout(new BoxLayout(alarmPanel, BoxLayout.Y_AXIS));
        alarmPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        alarmPanel.add(new JLabel("\u23F0 Alarm Panel"));
        alarmPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        alarmPanel.add(new JButton("Set New Alarm (Coming Soon)"));

        mainPanel = new JPanel();
        cardLayout = new CardLayout();
        mainPanel.setLayout(cardLayout);
        mainPanel.add(stopwatchPanel, "stopwatch");
        mainPanel.add(alarmPanel, "alarm");

        add(mainPanel, BorderLayout.CENTER);

        stopwatchTimer = new Timer(TIMER_DELAY, e -> {
            stopwatchElapsed += TIMER_DELAY;
            dialPanel.setSeconds((stopwatchElapsed / 1000) % 60);
            timeLabel.setText(formatTime(stopwatchElapsed / 1000));
        });

        countdownTimer = new Timer(TIMER_DELAY, e -> {
            if (countdownSeconds > 0) {
                countdownSeconds--;
                dialPanel.setSeconds(countdownSeconds % 60);
                timeLabel.setText(formatTime(countdownSeconds));
            } else {
                countdownTimer.stop();
                isRunning = false;
                playSound();
                JOptionPane.showMessageDialog(this, "\u23F0 Timer Completed!");
                updateButtonStates();
            }
        });

        clockTimer = new Timer(TIMER_DELAY, e -> {
            updateDateTime();
            checkAlarm();
        });
        clockTimer.start();

        updateDateTime();
        fetchWeather();

        startButton.addActionListener(this);
        stopButton.addActionListener(this);
        resetButton.addActionListener(this);
        timerButton.addActionListener(this);
        switchToStopwatchButton.addActionListener(this);
        lapButton.addActionListener(this);
        darkModeButton.addActionListener(this);
        soundButton.addActionListener(this);
        alarmButton.addActionListener(this);

        updateButtonStates();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void fetchWeather() {
        new Thread(() -> {
            try {
                URL url = new URL("https://wttr.in/?format=3");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                Scanner sc = new Scanner(conn.getInputStream());
                if (sc.hasNext())
                    weatherLabel.setText("\uD83C\uDF24 " + sc.nextLine());
                sc.close();
            } catch (IOException e) {
                weatherLabel.setText("Weather: Unavailable");
                System.err.println("Weather fetch failed: " + e.getMessage());
            }
        }).start();
    }

    private void checkAlarm() {
        if (alarmTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            String now = sdf.format(new Date());
            if (now.equals(alarmTime)) {
                alarmTime = null;
                playSound();
                JOptionPane.showMessageDialog(this, "\u23F0 Alarm Time Reached!");
            }
        }
    }

    private void playSound() {
        try {
            if (soundFile != null && soundFile.exists()) {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);
                clip.start();
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        } catch (Exception ex) {
            System.err.println("Audio error: " + ex.getMessage());
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void updateDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy '<br>' hh:mm:ss a");
        dateLabel.setText("<html>" + sdf.format(new Date()) + "</html>");
    }

    private void updateButtonStates() {
        startButton.setEnabled(!isRunning);
        stopButton.setEnabled(isRunning);
        resetButton.setEnabled(true);
        lapButton.setEnabled(!isTimerMode && isRunning);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == startButton && !isRunning) {
            if (isTimerMode) countdownTimer.start();
            else stopwatchTimer.start();
            isRunning = true;
        } else if (src == stopButton && isRunning) {
            if (isTimerMode) countdownTimer.stop();
            else stopwatchTimer.stop();
            isRunning = false;
        } else if (src == resetButton) {
            stopwatchTimer.stop();
            countdownTimer.stop();
            isRunning = false;
            stopwatchElapsed = 0;
            countdownSeconds = 0;
            dialPanel.setSeconds(0);
            timeLabel.setText("00:00:00");
            isTimerMode = false;
            lapTimes.clear();
            lapDisplay.setText("");
            switchToStopwatchButton.setVisible(false);
        } else if (src == timerButton) {
            String input = JOptionPane.showInputDialog(this, "Enter timer duration in seconds:");
            if (input != null && input.matches("\\d+")) {
                countdownSeconds = Integer.parseInt(input);
                isTimerMode = true;
                stopwatchTimer.stop();
                dialPanel.setSeconds(countdownSeconds % 60);
                timeLabel.setText(formatTime(countdownSeconds));
                JOptionPane.showMessageDialog(this, "Timer set! Click Start.");
                switchToStopwatchButton.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Please enter a valid positive number.");
            }
        } else if (src == switchToStopwatchButton) {
            isTimerMode = false;
            stopwatchElapsed = 0;
            countdownTimer.stop();
            timeLabel.setText("00:00:00");
            dialPanel.setSeconds(0);
            switchToStopwatchButton.setVisible(false);
        } else if (src == lapButton && isRunning && !isTimerMode) {
            String lap = "Lap " + (lapTimes.size() + 1) + ": " + formatTime(stopwatchElapsed / 1000);
            lapTimes.add(lap);
            lapDisplay.append(lap + "\n");
        } else if (src == darkModeButton) {
            toggleDarkMode();
        } else if (src == soundButton) {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                soundFile = chooser.getSelectedFile();
            }
        } else if (src == alarmButton) {
            String input = JOptionPane.showInputDialog(this, "Set alarm time (HH:mm):");
            if (input != null && input.matches("([01]\\d|2[0-3]):[0-5]\\d")) {
                alarmTime = input;
                JOptionPane.showMessageDialog(this, "Alarm set for " + alarmTime);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid time format. Use HH:mm (24-hr).");
            }
        }
        updateButtonStates();
    }

    private void toggleDarkMode() {
        Color bg = isDarkMode ? Color.WHITE : Color.DARK_GRAY;
        Color fg = isDarkMode ? Color.BLACK : Color.WHITE;
        getContentPane().setBackground(bg);
        timeLabel.setForeground(fg);
        dateLabel.setForeground(fg);
        weatherLabel.setForeground(fg);
        alarmStatusLabel.setForeground(isDarkMode ? Color.BLUE : Color.CYAN);
        lapDisplay.setBackground(bg);
        lapDisplay.setForeground(fg);
        isDarkMode = !isDarkMode;
    }

    class DialPanel extends JPanel {
        private int seconds = 0;
        public void setSeconds(int sec) {
            this.seconds = sec;
            repaint();
        }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int radius = Math.min(w, h) / 2 - 20;
            int cx = w / 2;
            int cy = h / 2;
            g2.setColor(Color.LIGHT_GRAY);
            g2.fillOval(cx - radius, cy - radius, 2 * radius, 2 * radius);
            g2.setColor(Color.BLACK);
            g2.drawOval(cx - radius, cy - radius, 2 * radius, 2 * radius);
            for (int i = 0; i < 60; i++) {
                double angle = Math.toRadians(i * 6 - 90);
                int x1 = (int) (cx + radius * Math.cos(angle));
                int y1 = (int) (cy + radius * Math.sin(angle));
                int x2 = (int) (cx + (radius - 10) * Math.cos(angle));
                int y2 = (int) (cy + (radius - 10) * Math.sin(angle));
                g2.drawLine(x1, y1, x2, y2);
                if (i % 5 == 0) {
                    String label = (i == 0 ? "60" : String.valueOf(i));
                    int tx = (int) (cx + (radius - 25) * Math.cos(angle));
                    int ty = (int) (cy + (radius - 25) * Math.sin(angle));
                    g2.setFont(new Font("Arial", Font.PLAIN, 12));
                    g2.drawString(label, tx - 8, ty + 5);
                }
            }
            double handAngle = Math.toRadians(seconds * 6 - 90);
            int hx = (int) (cx + (radius - 20) * Math.cos(handAngle));
            int hy = (int) (cy + (radius - 20) * Math.sin(handAngle));
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(3));
            g2.drawLine(cx, cy, hx, hy);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(StopwatchTimerApp::new);
    }
}
