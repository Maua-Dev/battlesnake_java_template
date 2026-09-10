package com.mauadev.code.entities;

/** O pacote completo que chega em /start, /move e /end. */
public class GameState {

    private Game game;
    private int turn;
    private Board board;
    private Snake you;

    public GameState() {}

    public Game getGame() { return game; }
    public int getTurn() { return turn; }
    public Board getBoard() { return board; }
    public Snake getYou() { return you; }

    public void setGame(Game game) { this.game = game; }
    public void setTurn(int turn) { this.turn = turn; }
    public void setBoard(Board board) { this.board = board; }
    public void setYou(Snake you) { this.you = you; }
}