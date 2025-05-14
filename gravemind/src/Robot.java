import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Robot implements AutoCloseable{
    private final SerialPort port;

    public Robot(String port, int baud){
        this.port = SerialPort.getCommPort(port);
        this.port.setBaudRate(baud);
        this.port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);

        if (!this.port.openPort()) {
            throw new RuntimeException("Failed to open port");
        }

        new Thread(this::readLoop).start();
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
                        System.out.printf("Received -> Command %d\n", command);
                    }
                    case 0xBB -> { // movement data
                        ByteBuffer bb = ByteBuffer.wrap(readBytes(12)).order(ByteOrder.LITTLE_ENDIAN);
                        var x = bb.getFloat();
                        var y = bb.getFloat();
                        var angle = bb.getFloat();
                        System.out.printf("Received -> [x: %f, y: %f, angle: %f]\n", x, y, angle);
                    }
                    case 0xCC -> { // distance sensor
                        var near = readByte()!=0;
                        System.out.println("Received -> Near: " + near);
                    }
                    case 0 -> {}
                    case -1 -> { // end of stream
                        System.out.println("End of stream");
//                        return;
                    }
                    default -> System.out.println("Invalid receiving command type: " + type);
                }
            } catch (IOException e) {
                System.err.println("Error reading from serial port: " + e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        if(port !=null) port.closePort();
    }

}
