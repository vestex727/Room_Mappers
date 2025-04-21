import java.util.ArrayList;
import javax.swing.*;
import java.awt.*;


public class MapGenerator extends JPanel{
    double currentDistance;
    ArrayList<Movement> movements;

    private double revolutionsToDistance(double revolutions) {return currentDistance = 7 * Math.PI * revolutions;}

    public void addMovement(double revolutions, boolean objectIsInRange, Direction direction) {
        movements.add(new Movement(revolutionsToDistance(revolutions), objectIsInRange, direction));
    }

    private int currentX, currentY;

    public MapGenerator() {
        this.currentX = 250;  // Start in the center of the panel
        this.currentY = 250;
        main(new String[] {});
    }

    public MapGenerator(ArrayList<Movement> movements) {
        this.movements = movements;
        this.currentX = 500;  // Start in the center of the panel
        this.currentY = 500;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Set up the graphics (background color and drawing color)
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());  // Set the background to black
        g.setColor(Color.WHITE);

        // Draw the origin point
        g.fillOval(currentX - 5, currentY - 5, 10, 10); // Drawing the origin (starting point)

        // Process each record and draw the corresponding movement
        for (Movement record : movements) {
            moveAndDraw(g, (int) record.getDistance(), record.getDirection(), record.objectInRange());
        }
    }

    private void moveAndDraw(Graphics g, int distance, Direction direction, boolean draw) {
        int previousX = currentX;
        int previousY = currentY;

        // Move based on direction
        switch (direction) {
            case NORTH:
                currentY -= distance;
                break;
            case SOUTH:
                currentY += distance;
                break;
            case EAST:
                currentX += distance;
                break;
            case WEST:
                currentX -= distance;
                break;
        }

        // Draw the movement as a line
        if(draw) g.drawLine(previousX, previousY, currentX, currentY);
    }

    public static void main(String[] args) {
        // Create some sample records
        ArrayList<Movement> records = new ArrayList<>();
        records.add(new Movement(100, true, Direction.NORTH));
        records.add(new Movement(10, false, Direction.NORTH));
        records.add(new Movement(10, true, Direction.NORTH));
        records.add(new Movement(10, true, Direction.NORTH));
        records.add(new Movement(50, true, Direction.EAST));
        records.add(new Movement(150, false, Direction.SOUTH));
        records.add(new Movement(75, true, Direction.WEST));


        // Set up the JFrame to display the map
        JFrame frame = new JFrame("Visual Map");
        MapGenerator mapPanel = new MapGenerator(records);
        frame.add(mapPanel);
        frame.setSize(500, 500);  // Set window size
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
