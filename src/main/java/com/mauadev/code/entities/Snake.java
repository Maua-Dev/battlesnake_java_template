package com.mauadev.code.entities;
import java.util.List;

public class Snake {

    // Atributos da cobra

    private String id;
    private String name;
    private int health;
    private List<Coordinate> body;
    private Coordinate head;
    private int length;
    private String shout;

    public Snake(){
    }
    public Snake(
        String id,
        String name,
        String health,
        List<Coordinate> body,
        Coordinate head,
        String length,
        String shout
    ){
        this.id = id;
        this.name = name;
        this.health = Integer.parseInt(health);
        this.body = body;
        this.head = head;
        this.length = Integer.parseInt(length);
        this.shout = shout;
    }

    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public int getHealth() {
        return health;
    }
    public List<Coordinate> getBody() {
        return body;
    }
    public Coordinate getHead() {
        return head;
    }
    public int getLength() {
        return length;
    }
    public String getShout() {
        return shout;
    }
    public void setId(String id) {
        this.id = id;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setHealth(int health) {
        this.health = health;
    }
    public void setBody(List<Coordinate> body) {
        this.body = body;
    }
    public void setHead(Coordinate head) {
        this.head = head;
    }
    public void setLength(int length) {
        this.length = length;
        this.body = this.body.subList(0, length);
    }
    public void setShout(String shout) {
        this.shout = shout;
    }
    
}

