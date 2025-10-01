package com.mauadev.code.entities;
import java.util.List;

public class Board {

    // Atributos do tabuleiro

    private int height;
    private int width;
    private List<Coordinate> food;
    private List<Coordinate> hazards;
    private List<Snake> snakes;

    public Board(){
    }

    public Board(
        String height,
        String width,
        List<Coordinate> food,
        List<Coordinate> hazards,
        List<Snake> snakes
    ){
        this.height = Integer.parseInt(height);
        this.width = Integer.parseInt(width);
        this.food = food;
        this.hazards = hazards;
        this.snakes = snakes;
    }

    public int getHeight() {
        return height;
    }
    public int getWidth() {
        return width;
    }
    public List<Coordinate> getFood() {
        return food;
    }
    public List<Coordinate> getHazards() {
        return hazards;
    }
    public List<Snake> getSnakes() {
        return snakes;
    }

    public void setHeight(int height) {
        this.height = height;
    }
    public void setWidth(int width) {
        this.width = width;
    }
    public void setFood(List<Coordinate> food) {
        this.food = food;
    }
    public void setHazards(List<Coordinate> hazards) {
        this.hazards = hazards;
    }
    public void setSnakes(List<Snake> snakes) {
        this.snakes = snakes;
    }

}

