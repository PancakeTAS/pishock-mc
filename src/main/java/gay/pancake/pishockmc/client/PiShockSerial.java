package gay.pancake.pishockmc.client;

import com.fazecast.jSerialComm.SerialPort;
import com.google.gson.Gson;
import gay.pancake.pishockmc.client.PiShockAPI.ActionDuration;
import gay.pancake.pishockmc.client.PiShockAPI.ActionType;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Interface to the PiShock Hub via Serial connection.
 */
public class PiShockSerial implements Closeable {

    /** Command to send to the PiShock Hub. */
    record Command(String cmd, CommandData value) { }
    /** Data to send to the PiShock Hub for the 'operate' command. */
    record CommandData(int id, String op, int intensity, int duration) { }

    private static final Gson GSON = new Gson();

    /** Serial port to connect to. */
    private final SerialPort port;
    /** Output stream to write to the serial port. */
    private final OutputStream write;

    /**
     * Create a new PiShock Serial connection.
     *
     * @param port Serial port to connect to
     */
    public PiShockSerial(SerialPort port) {
        port.setBaudRate(115200);
        port.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);
        port.openPort();

        this.port = port;
        this.write = port.getOutputStream();
    }

    /**
     * Call the PiShock Serial API.
     *
     * @param device Shocker device ID
     * @param op Type of action to perform
     * @param intensity Intensity of the action (ignored for BEEP)
     * @param duration Duration of the action
     * @return CompletableFuture of the HTTP response
     */
    public CompletableFuture<Boolean> call(int device, ActionType op, int intensity, ActionDuration duration) {
        var command = new Command("operate", new CommandData(device, op.code, intensity, duration.internal));
        var json = GSON.toJson(command) + "\n";

        return CompletableFuture.supplyAsync(() -> {
            try {
                this.write.write(json.getBytes(StandardCharsets.US_ASCII));
                this.write.flush();
            } catch (Exception e) {
                System.err.println("Failed to call PiShock Serial: " + json);
                e.printStackTrace(System.err);
                return false;
            }

            return true;
        });
    }

    /**
     * Close the serial connection.
     *
     * @throws IOException If an I/O error occurs
     */
    public void close() throws IOException {
        this.write.close();
        this.port.closePort();﻿
    }

    /**
     * List all available serial ports in human-readable format.
     *
     * @return Map of serial ports to their human-readable names.
     */
    public static Map<SerialPort, String> list() {
        return Arrays.stream(SerialPort.getCommPorts()).collect(Collectors.toMap(port -> port, port -> port.getSystemPortName() + ": " + port.getDescriptivePortName()));
    }

}
