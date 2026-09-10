package com.mauadev.code;

// Bem-vindo ao
// __________         __    __  .__                               __
// \______   \_____ _/  |__/  |_|  |   ____   ______ ____ _____  |  | __ ____
//  |    |  _/\__  \   __\   __\  | _/ __ \ /  ___//    \__  \ |  |/ // __ \
//  |    |   \ / __ \|  |  |  | |  |_\  ___/ \___ \|   |  \/ __ \|    <\  ___/
//  |________/(______/__|  |__| |____/\_____>______>___|__(______/__|__\_____>
//
// ESTE E O ARQUIVO QUE VOCE VAI EDITAR. Todo o resto do projeto existe
// so para levar o estado do jogo ate as quatro funcoes abaixo.
//
// Para comecar, ja deixamos pronta a logica que impede a sua cobra de andar
// para tras (ela morreria na hora). Os TODOs marcam os proximos passos.
// Documentacao: https://docs.battlesnake.com

import com.mauadev.code.entities.Coordinate;
import com.mauadev.code.entities.GameState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Logica da cobra. E AQUI que voce programa a inteligencia da snake.
 * <p>
 * GET  /      -> {@link #info()}<br>
 * POST /start -> {@link #start(GameState)}<br>
 * POST /move  -> {@link #getMove(GameState)}  foco principal<br>
 * POST /end   -> {@link #end(GameState)}
 */
public class Logic {

    private static final Random RANDOM = new Random();

    /**
     * GET / - chamado quando voce cadastra a cobra e a cada partida.
     * Controla a aparencia dela. Opcoes de cabeca, cauda e cor:
     * https://docs.battlesnake.com/guides/customizations
     */
    public static java.util.Map<String, String> info() {
        java.util.Map<String, String> info = new java.util.HashMap<>();
        info.put("apiversion", "1");
        info.put("author", "");          // TODO: coloque aqui o SEU usuario do Battlesnake
        info.put("color", "#8B0000");    // TODO: escolha a cor da sua cobra
        info.put("head", "tiger-king");  // TODO: escolha a cabeca
        info.put("tail", "hook");        // TODO: escolha a cauda
        return info;
    }

    /**
     * POST /start - chamado uma vez, quando a partida comeca.
     * Bom lugar para preparar qualquer estado inicial.
     */
    public static void start(GameState state) {
        // state.getTurn(), state.getBoard(), state.getYou() estao disponiveis
    }

    /**
     * POST /end - chamado uma vez, quando a partida termina.
     */
    public static void end(GameState state) {
        // Voce pode analisar o estado final para saber se venceu ou perdeu.
    }

    /**
     * POST /move - chamado a cada turno. Aqui mora a inteligencia da sua cobra.
     * Deve retornar "up", "down", "left" ou "right".
     * Exemplo do JSON recebido: https://docs.battlesnake.com/api/example-move
     *
     * @param state estado atual do jogo
     * @return direcao escolhida
     */
    public static String getMove(GameState state) {
        List<String> isMoveSafe = new ArrayList<>(Arrays.asList("up", "down", "left", "right"));

        // --- Impedir que a cobra ande para tras (ja implementado) ---
        // O pescoco e a parte do corpo logo atras da cabeca. Voltar por cima
        // dele e morte certa, entao removemos aquela direcao da lista.
        Coordinate myHead = state.getYou().getHead();
        List<Coordinate> body = state.getYou().getBody();

        if (body != null && body.size() >= 2) {
            Coordinate myNeck = body.get(1);

            if (myNeck.getX() < myHead.getX()) {
                // pescoco a esquerda da cabeca -> nao va para a esquerda
                isMoveSafe.remove("left");
            } else if (myNeck.getX() > myHead.getX()) {
                // pescoco a direita da cabeca -> nao va para a direita
                isMoveSafe.remove("right");
            } else if (myNeck.getY() < myHead.getY()) {
                // pescoco abaixo da cabeca -> nao desca
                isMoveSafe.remove("down");
            } else if (myNeck.getY() > myHead.getY()) {
                // pescoco acima da cabeca -> nao suba
                isMoveSafe.remove("up");
            }
        }

        // 2. Impedir que a cobra saia do tabuleiro (paredes)
        if (state.getBoard() != null) {
            int boardWidth = state.getBoard().getWidth();
            int boardHeight = state.getBoard().getHeight();

            if (myHead.getX() + 1 >= boardWidth) {
                isMoveSafe.remove("right");
            }
            if (myHead.getX() - 1 < 0) {
                isMoveSafe.remove("left");
            }
            if (myHead.getY() + 1 >= boardHeight) {
                isMoveSafe.remove("up");
            }
            if (myHead.getY() - 1 < 0) {
                isMoveSafe.remove("down");
            }
        }

        // 3. Impedir que a cobra bata no próprio corpo
        if (body != null) {
            for (Coordinate segment : body) {
                if (segment.getX() == myHead.getX() + 1 && segment.getY() == myHead.getY()) {
                    isMoveSafe.remove("right");
                }
                if (segment.getX() == myHead.getX() - 1 && segment.getY() == myHead.getY()) {
                    isMoveSafe.remove("left");
                }
                if (segment.getX() == myHead.getX() && segment.getY() == myHead.getY() + 1) {
                    isMoveSafe.remove("up");
                }
                if (segment.getX() == myHead.getX() && segment.getY() == myHead.getY() - 1) {
                    isMoveSafe.remove("down");
                }
            }
        }

        // TODO: Passo 3 — impedir que a cobra bata nas adversárias
        // List<Snake> opponents = state.getBoard().getSnakes();

        // Sobrou alguma direcao segura?
        if (isMoveSafe.isEmpty()) {
            // Emergencia: todas as direcoes sao perigosas.
            // Escolhemos uma ao acaso entre as 4 - melhor do que travar.
            List<String> allMoves = Arrays.asList("up", "down", "left", "right");
            return allMoves.get(RANDOM.nextInt(allMoves.size()));
        }

        // Escolhe uma direcao segura ao acaso.
        String chosen = isMoveSafe.get(RANDOM.nextInt(isMoveSafe.size()));

        // TODO: Passo 4 - ir atras da comida em vez de sortear, para nao morrer de fome
        // List<Coordinate> food = state.getBoard().getFood();

        return chosen;
    }
}