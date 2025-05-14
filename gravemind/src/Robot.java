import com.fazecast.jSerialComm.SerialPort;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class Robot implements AutoCloseable{
    private final SerialPort port;
    private final ArrayList<MovementListener> movementListeners = new ArrayList<>();
    private final ArrayList<NearListener> nearListeners = new ArrayList<>();
    private final ArrayList<CommandAwkListener> commandAwkListeners = new ArrayList<>();

    public interface CommandAwkListener{
        void handle(int commandId);
    }

    public interface NearListener{
        void handle(boolean near);
    }

    public interface MovementListener{
        void handle(Position m);
    }

    public Robot(String port, int baud){
        this.port = SerialPort.getCommPort(port);
        this.port.setBaudRate(baud);
        this.port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);

        if (!this.port.openPort()) {
            throw new RuntimeException("Failed to open port");
        }

        new Thread(this::readLoop).start();
    }

    public void addCommandAwkListener(CommandAwkListener listener){
        synchronized (commandAwkListeners){
            commandAwkListeners.add(listener);
        }
    }

    public void addNearListener(NearListener listener){
        synchronized (nearListeners){
            nearListeners.add(listener);
        }
    }

    public void addMovementListener(MovementListener listener){
        synchronized (movementListeners){
            movementListeners.add(listener);
        }
    }

    public synchronized void send(Command command) {
        port.writeBytes(new byte[]{(byte) command.ordinal()}, 1);
        port.flushIOBuffers();
    }

    public synchronized void sendRaw(int customCommand) {
        port.writeBytes(new byte[]{(byte) customCommand}, 1);
        port.flushIOBuffers();
    }

    private int readByte(){
        var b = new byte[1];
        var read = port.readBytes(b, 1);
        if(read == -1)return -1;
        return ((int)b[0])&0xFF;
    }

    private byte[] readBytes(int number){
        var b = new byte[number];
        var read = port.readBytes(b, number);
        if(read == -1)return null;
        if(read != number)throw new RuntimeException("Wrong number of bytes read");
        return b;
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
                        var near = readByte()!=0;
                        synchronized (nearListeners){
                            for(var listener : nearListeners) listener.handle(near);
                        }
                    }
                    case 0xDD -> { //debug
                        while(true){
                            var in = readByte();
                            if(in==-1||in==0)break;
                            System.err.print((char)in);
                        }
                    }
                    case -1 -> { // end of stream
                        Thread.sleep(10);
                        System.out.println("End of stream");
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
