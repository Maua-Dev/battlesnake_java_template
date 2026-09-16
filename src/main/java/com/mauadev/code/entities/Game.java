package com.mauadev.code.entities;

/** Metadados da partida. */
public class Game {
    private String id;
    private int timeout;

    public Game() {}

    public String getId() { return id; }
    public int getTimeout() { return timeout; }
    public void setId(String id) { this.id = id; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
}