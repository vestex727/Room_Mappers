import java.util.ArrayList;
import javax.swing.*;
import java.awt.*;


public class MapGenerator{
    private ArrayList<Position> positions = new ArrayList<>();

    public void addPosition(Position position) {
        positions.add(position);
    }

    public void show(){
        final int width = 500;
        final int height = 500;
        var panel = new JPanel(){

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                // Set up the graphics (background color and drawing color)
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());  // Set the background to black
                g.setColor(Color.WHITE);
                g.translate(width/2, height/2);
                ((Graphics2D)g).scale(1, -1);

                // Draw the origin point
                g.fillOval(- 5, - 5, 10, 10); // Drawing the origin (starting point)

                float cx = 0;
                float cy = 0;
                for (var pos : positions) {
                    g.drawLine((int) cx, (int) cy, (int) pos.x(), (int) pos.y());
                    cx = pos.x();
                    cy = pos.y();
                }
            }
        };

        // Set up the JFrame to display the map
        JFrame frame = new JFrame("Visual Map");
        frame.add(panel);
        frame.setSize(width, height);  // Set window size
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }

    public static void main(String ... args){
        var gen = new MapGenerator();
        gen.addPosition(new Position(0,0,0));
        gen.addPosition(new Position(0,50,0));
        gen.addPosition(new Position(50,100,0));
        gen.addPosition(new Position(100,75,0));
        gen.show();
    }
}
