import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class GravemindControlPanel extends JFrame{
    private ArrayList<Position> movements = new ArrayList<>();
    private boolean SensorIsOn = false;

    private Robot robot;
    private MapGenerator mapGenerator = new MapGenerator();

    private JTextField inputField;

    public GravemindControlPanel(Robot robot){
        this.robot = robot;
        robot.addMovementListener(mapGenerator::addPosition);
        robot.addMovementListener(System.out::println);
//        robot.addNearListener(near -> System.out.println("Near: " + near));
//        robot.addCommandAwkListener(command -> System.out.println("Command Awk: " + command));

        this.setTitle("Gravemind Control Panel");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(400, 350);
        this.setLayout(new BorderLayout());

        // Create directional buttons
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        Command[] directions = {Command.Forward, Command.Left, Command.Right, Command.Backward};
        for (var direction : directions) {
            JButton button = new JButton(direction.name());
            button.setFocusable(false);

            // Send command when pressed, stop when released
            button.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    robot.send(direction);
                }

                public void mouseReleased(MouseEvent e) {
                    robot.send(Command.Stop);
                }
            });
            buttonPanel.add(button);
        }

        JButton button = new JButton("Make Map");
        button.setFocusable(false);

        // Send command when pressed, stop when released
        button.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                mapGenerator.show();
            }
        });
        buttonPanel.add(button);

        // Input field for sending custom integer commands
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputField = new JTextField();
        JButton sendButton = new JButton("Send");

        // Parse and send custom command on button click
        sendButton.addActionListener(e -> {
            try {
                int customCommand = Integer.parseInt(inputField.getText().trim());
                robot.sendRaw(customCommand);
                inputField.setText("");
            } catch (NumberFormatException ex) {
                System.err.println("Invalid integer input.");
            }
        });

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Add button panel and input panel to main frame
        this.add(buttonPanel, BorderLayout.NORTH);
        this.add(inputPanel, BorderLayout.SOUTH);


        // Handle arrow key input to control the robot
        this.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                var command = keyToCommand(e.getKeyCode());
                if (command != null) {
                    robot.send(command);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                simulateSensor(e);
                robot.send(Command.Stop);
            }
        });

        // Ensure the frame receives keyboard focus
        this.setFocusable(true);
        this.requestFocusInWindow();
        this.setVisible(true);
    }

    private Command keyToCommand(int keyCode) {
        if(keyCode == KeyEvent.VK_ESCAPE) {System.exit(99);}

        switch (keyCode) {
            case KeyEvent.VK_ENTER:
                mapGenerator.show();
                break;
            case KeyEvent.VK_UP, KeyEvent.VK_W:
                return Command.Forward;
            case KeyEvent.VK_DOWN, KeyEvent.VK_S:
                return Command.Backward;
            case KeyEvent.VK_LEFT, KeyEvent.VK_A:
                return Command.Right;  // Swapped intentionally
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D:
                return Command.Left;  // Swapped intentionally
            default:
        };
        return null;
    }

    void simulateSensor(KeyEvent e){
        if(e.getKeyCode() == KeyEvent.VK_C) {
            SensorIsOn = !SensorIsOn;
            System.out.println(SensorIsOn);
        }
        if(e.getKeyCode() == KeyEvent.VK_M) mapGenerator.show();
    }

    public static void main(String[] args) {
        new GravemindControlPanel(new Robot("COM4", 115200));
    }
}
