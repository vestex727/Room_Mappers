import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class Robot implements AutoCloseable{
    private final SerialPort port;
    private final ArrayList<MovementListener> movementListeners = new ArrayList<>();
    private final ArrayList<DistanceListener> distanceListeners = new ArrayList<>();
    private final ArrayList<CommandAwkListener> commandAwkListeners = new ArrayList<>();

    public interface CommandAwkListener{
        void handle(int commandId);
    }

    public interface DistanceListener {
        void handle(float near);
    }

    public interface MovementListener{
        void handle(Position m);
    }

    public Robot(String port, int baud){
        this.port = SerialPort.getCommPort(port);
        this.port.setBaudRate(baud);
//        this.port.setComPortParameters(baud, 8, 1, 0);
        this.port.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING | SerialPort.TIMEOUT_READ_BLOCKING, 50000, 50000);

        if (!this.port.openPort(50, 4096, 4096)) {
            throw new RuntimeException("Failed to open port");
        }

        new Thread(this::readLoop).start();
    }

    public void addCommandAwkListener(CommandAwkListener listener){
        synchronized (commandAwkListeners){
            commandAwkListeners.add(listener);
        }
    }

    public void addDistanceListener(DistanceListener listener){
        synchronized (distanceListeners){
            distanceListeners.add(listener);
        }
    }

    public void addMovementListener(MovementListener listener){
        synchronized (movementListeners){
            movementListeners.add(listener);
        }
    }

    public synchronized void send(Command command) {
        sendRaw(command.ordinal());
    }

    public synchronized void sendRaw(int customCommand) {
        try {
            port.getOutputStream().write(customCommand);
            port.getOutputStream().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        };
    }

    private int readByte(){
        try {
            return port.getInputStreamWithSuppressedTimeoutExceptions().read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] readBytes(int number){
        try {
            return port.getInputStreamWithSuppressedTimeoutExceptions().readNBytes(number);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readLoop(){
        while (true) {
            try {
                var type = readByte();
                switch (type){
                    case 0xAA -> { // response to command
                        int command = readByte();
                        synchronized (commandAwkListeners){
                            for(var listener : commandAwkListeners) listener.handle(command);
                        }
                    }
                    case 0xBB -> { // movement data
                        ByteBuffer bb = ByteBuffer.wrap(readBytes(12)).order(ByteOrder.LITTLE_ENDIAN);
                        var movement = new Position(bb.getFloat(), bb.getFloat(), bb.getFloat());
                        synchronized (movementListeners){
                            for(var listener : movementListeners) listener.handle(movement);
                        }
                    }
                    case 0xCC -> { // distance sensor
                        var near = (float)ByteBuffer.wrap(readBytes(2)).order(ByteOrder.LITTLE_ENDIAN).getShort();
                        synchronized (distanceListeners){
                            for(var listener : distanceListeners) listener.handle(near);
                        }
                    }
                    case 0xDD -> { //debug
                        while(true){
                            var in = readByte();
                            if(in==0)break;
                            System.err.print((char)in);
                        }
                    }
                    case -1 -> { // end of stream
                        Thread.sleep(10);
//                        System.out.println("End of stream");
//                        return;
                    }
                    default -> System.out.println("Invalid receiving command type: " + type);
                }
            } catch (Exception e) {
                System.err.println("Error reading from serial port: " + e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        if(port !=null) port.closePort();
    }

}
