package com.mauadev.code;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.mauadev.code.entities.Coordinate;
import com.mauadev.code.entities.GameState;
import com.mauadev.code.entities.Snake;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // Gson é uma biblioteca para converter objetos Java para JSON e vice-versa.
    private static final Gson gson = new Gson();

    private static final Random RANDOM = new Random();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        String path = normalizePath(request.getPath());
        Object responseBody = null;

        try {
            // Roteador para os diferentes endpoints da API BattleSnake
            switch (path) {
                case "/":
                    // Informações da sua cobra
                    responseBody = handleInfo();
                    break;
                case "/start":
                    // Lógica para o início do jogo
                    handleStart(request, context);
                    break;
                case "/move":
                    // Lógica para decidir o próximo movimento
                    responseBody = handleMove(request, context);
                    break;
                case "/end":
                    // Lógica para o fim do jogo
                    handleEnd(request, context);
                    break;
                default:
                    // Inalcançável: normalizePath só devolve as quatro rotas acima.
                    // Fica como rede de segurança caso ela mude no futuro.
                    // Precisamos passar \ antes das aspas para não dar erro quando convertemos pra json
                    return response.withStatusCode(404).withBody("{\"error\": \"Path not found\"}");
            }

            // Configura a resposta de sucesso
            response.setStatusCode(200);
            response.setHeaders(Collections.singletonMap("Content-Type", "application/json"));
            if (responseBody != null) {
                // Converte o objeto de resposta para uma string JSON
                response.setBody(gson.toJson(responseBody));
            }

        } catch (Exception e) {
            // Em caso de erro em qualquer parte da lógica
            context.getLogger().log("ERROR: " + e.getMessage());
            response.setStatusCode(500);
            response.setBody(String.format("{\"error\": \"%s\"}", e.getMessage()));
        }

        return response;
    }

    /**
     * Descobre qual rota do BattleSnake foi chamada.
     *
     * O API Gateway REST entrega o caminho com o nome do stage na frente: a rota
     * "/move" chega como "/dev/move", e a raiz chega como "/dev". Comparar por
     * igualdade exata devolvia 404 em produção — por isso comparamos pelo sufixo
     * do caminho.
     *
     * @param path caminho cru vindo da requisição (pode ser nulo)
     * @return uma das rotas canônicas: "/", "/start", "/move" ou "/end"
     */
    private String normalizePath(String path) {
        if (path == null) {
            return "/";
        }

        // Tira espaços e barras sobrando nas pontas: "/dev/move/" vira "/dev/move"
        String trimmed = path.trim();
        while (trimmed.endsWith("/") && trimmed.length() > 1) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        for (String route : new String[] { "/start", "/move", "/end" }) {
            if (trimmed.endsWith(route)) {
                return route;
            }
        }

        // A raiz — e qualquer caminho desconhecido — devolve as informações da
        // cobra, que é o que o site do BattleSnake espera de um GET /.
        return "/";
    }

    /**
     * Responde ao endpoint / com as informações da sua cobra. 🐍
     */
    private Map<String, String> handleInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("apiversion", "1");
        info.put("author", ""); // TODO: coloque aqui o SEU usuário do BattleSnake
        info.put("color", "#888888"); // TODO: escolha a cor da sua cobra
        info.put("head", "default"); // TODO: escolha a cabeça
        info.put("tail", "default"); // TODO: escolha a cauda
        return info;
    }

    /**
     * Chamado no início de cada jogo. Não precisa retornar nada.
     */
    private void handleStart(APIGatewayProxyRequestEvent request, Context context) {
        // Você pode usar o corpo da requisição (request.getBody()) para obter o estado inicial do jogo.
        context.getLogger().log("Game Started!");
    }

    /**
     * Chamado a cada turno para decidir o movimento. 🕹️
     *
     * AQUI VAI A LÓGICA DA SUA COBRA! Já deixamos pronta a parte que impede a
     * cobra de andar para trás; os TODOs marcam os próximos passos.
     *
     * Documentação: https://docs.battlesnake.com/api/example-move
     */
    private Map<String, String> handleMove(APIGatewayProxyRequestEvent request, Context context) {
        GameState gameState = gson.fromJson(request.getBody(), GameState.class);
        Snake you = gameState.getYou();

        Map<String, Boolean> isMoveSafe = new HashMap<>();
        isMoveSafe.put("up", true);
        isMoveSafe.put("down", true);
        isMoveSafe.put("left", true);
        isMoveSafe.put("right", true);

        // --- Impedir que a cobra ande para trás (já implementado) ---
        // O pescoço é a parte do corpo logo atrás da cabeça. Voltar por cima
        // dele é morte certa, então marcamos aquela direção como insegura.
        Coordinate head = you.getHead();
        Coordinate neck = you.getBody().get(1);

        if (neck.getX() < head.getX()) {
            // pescoço à esquerda da cabeça -> não vá para a esquerda
            isMoveSafe.put("left", false);
        } else if (neck.getX() > head.getX()) {
            // pescoço à direita da cabeça -> não vá para a direita
            isMoveSafe.put("right", false);
        } else if (neck.getY() < head.getY()) {
            // pescoço abaixo da cabeça -> não desça
            isMoveSafe.put("down", false);
        } else if (neck.getY() > head.getY()) {
            // pescoço acima da cabeça -> não suba
            isMoveSafe.put("up", false);
        }

        // TODO: Passo 1 - impedir que a cobra saia do tabuleiro
        // int boardWidth = gameState.getBoard().getWidth();
        // int boardHeight = gameState.getBoard().getHeight();

        // TODO: Passo 2 - impedir que a cobra bata no próprio corpo
        // List<Coordinate> myBody = you.getBody();

        // TODO: Passo 3 - impedir que a cobra bata nas adversárias
        // List<Snake> opponents = gameState.getBoard().getSnakes();

        // Sobrou alguma direção segura?
        List<String> safeMoves = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : isMoveSafe.entrySet()) {
            if (entry.getValue()) {
                safeMoves.add(entry.getKey());
            }
        }

        Map<String, String> move = new HashMap<>();

        if (safeMoves.isEmpty()) {
            context.getLogger().log(String.format("MOVE %d: sem saída! descendo", gameState.getTurn()));
            move.put("move", "down");
            return move;
        }

        // Escolhe uma direção segura ao acaso.
        String nextMove = safeMoves.get(RANDOM.nextInt(safeMoves.size()));

        // TODO: Passo 4 - ir atrás de comida em vez de sortear
        // List<Coordinate> food = gameState.getBoard().getFood();

        context.getLogger().log(String.format("MOVE %d: %s", gameState.getTurn(), nextMove));
        move.put("move", nextMove);
        return move;
    }

    /**
     * Chamado no final de cada jogo. Não precisa retornar nada.
     */
    private void handleEnd(APIGatewayProxyRequestEvent request, Context context) {
        // Você pode analisar a requisição para saber se venceu ou perdeu.
        context.getLogger().log("Game Ended!");
    }
}
