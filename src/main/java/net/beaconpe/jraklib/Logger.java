package net.beaconpe.jraklib;

/**
 * Interface for an implementation of a logger wrapper that JRakLib can use.
 */
public interface Logger {
    void info(String message);
    void critical(String message);
    void emergency(String message);
}
