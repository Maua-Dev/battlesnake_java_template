package com.mauadev.code.entities;

public class GameState {

    // Atributos do estado do jogo

    private int turn;
    private Board board;
    private Snake you;

    public GameState(){
    }

    public GameState(
        String turn,
        Board board,
        Snake you
    ){
        this.turn = Integer.parseInt(turn);
        this.board = board;
        this.you = you;
    }

    public int getTurn() {
        return turn;
    }
    public Board getBoard() {
        return board;
    }
    public Snake getYou() {
        return you;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }
    public void setBoard(Board board) {
        this.board = board;
    }
    
}
