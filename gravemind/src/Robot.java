import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Robot implements AutoCloseable{
    private final SerialPort port;
    private final InputStream in;
    private final OutputStream out;

    public Robot(String port, int baud){
        this.port = SerialPort.getCommPort(port);
        this.port.setBaudRate(baud);
        this.port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);

        this.in = this.port.getInputStream();
        this.out = this.port.getOutputStream();

        if (!this.port.openPort()) {
            throw new RuntimeException("Failed to open port");
        }

        new Thread(this::readLoop).start();
    }

    public synchronized void send(Command command) {
        try{
            out.write(command.ordinal());
            out.flush();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public synchronized void sendRaw(int customCommand) {
        try{
            out.write(customCommand);
            out.flush();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void readLoop(){
        byte[] buffer = new byte[256];
        while (true) {
            try {
                int type = in.read();
                switch (type){
                    case 0xAA -> { // response to command
                        int result = in.readNBytes(buffer, 0, 12);
                        if(result!=12)throw new RuntimeException("Couldn't read all bytes");
                        ByteBuffer bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
                        int input = bb.getInt();
                        int ticksLeft = bb.getInt();
                        int ticksRight = bb.getInt();
                        System.out.printf("Received -> Input: %d | mm: %d | sense: %d%n", input, ticksLeft, ticksRight);
                    }
                    case 0xBB -> { // distance sensor
                        int result = in.readNBytes(buffer, 0, 1);
                        if(result!=1)throw new RuntimeException("Couldn't read all bytes");
                        ByteBuffer bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
                        var near = bb.get()!=0;
                        System.out.println("Received -> Near: " + near);
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
        try{
            if(in!=null)in.close();
        }catch (IOException ignore){}
        try{
            if(out!=null)out.close();
        }catch (IOException ignore){}
        if(port !=null) port.closePort();
    }

}
