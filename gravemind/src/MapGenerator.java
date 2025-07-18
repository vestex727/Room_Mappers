import java.util.ArrayList;
import javax.swing.*;
import java.awt.*;


public class MapGenerator{
    private ArrayList<Data> data = new ArrayList<>();
    private float distance = 0.0f;

    private record Data(Position pos, float distance){
    }

    public synchronized void addPosition(Position position) {
        data.add(new Data(position, distance));
    }

    public synchronized void updateDistance(float distance){
        if(distance>1500)this.distance=0;
        else this.distance = distance;
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
                g.translate(width/2, height/2);
                ((Graphics2D)g).scale(1, -1);

                // Draw the origin point
                g.fillOval(- 5, - 5, 10, 10); // Drawing the origin (starting point)

                synchronized (MapGenerator.this){
                    for (var data : data) {
                        g.setColor(Color.YELLOW);
                        g.drawLine(
                                (int) data.pos().x(),
                                (int) data.pos().y(),
                                (int) (data.pos().x() + data.distance*Math.cos(data.pos.angle() - Math.PI/2)),
                                (int) (data.pos().y() + data.distance*Math.sin(data.pos.angle() - Math.PI/2))
                        );
                    }
                }

                float cx = 0;
                float cy = 0;
                ((Graphics2D) g).setStroke(new BasicStroke(2));
                synchronized (MapGenerator.this){
                    for (var data : data) {
                        g.setColor(Color.RED);
                        g.drawLine((int) cx, (int) cy, (int) data.pos().x(), (int) data.pos().y());
                        cx = data.pos().x();
                        cy = data.pos().y();
                    }
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
        gen.updateDistance(5);
        gen.addPosition(new Position(0,0,1));

        gen.addPosition(new Position(0,50,0));
        gen.addPosition(new Position(50,100,0));
        gen.addPosition(new Position(100,75,1));
        gen.addPosition(new Position(75,10000,0));
        gen.addPosition(new Position(101,75,1));
        gen.show();
    }
}
