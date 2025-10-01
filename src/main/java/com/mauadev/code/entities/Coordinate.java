package com.mauadev.code.entities;

public class Coordinate {

    // Atributos da coordenada

    private int x;
    private int y;

    public Coordinate(){
    }

    public Coordinate(
        String x,
        String y
    ){
        this.x = Integer.parseInt(x);
        this.y = Integer.parseInt(y);
    }

    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
    public void setX(int x) {
        this.x = x;
    }
    public void setY(int y) {
        this.y = y;
    }
}
