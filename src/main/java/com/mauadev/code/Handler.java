package com.mauadev.code;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.mauadev.code.entities.Board;
import com.mauadev.code.entities.Coordinate;
import com.mauadev.code.entities.GameState;
import com.mauadev.code.entities.Snake;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // Gson é uma biblioteca para converter objetos Java para JSON e vice-versa.
    private static final Gson gson = new Gson();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        String path = request.getPath();
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
                    // Se a rota não for encontrada, retorna um erro 404
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
     * Responde ao endpoint / com as informações da sua cobra. 🐍
     */
    private Map<String, String> handleInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("apiversion", "1");
        info.put("author", "Leo");
        info.put("color", "#888888"); // Ex: Cinza
        info.put("head", "default");
        info.put("tail", "default");
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
     */
    private Map<String, String> handleMove(APIGatewayProxyRequestEvent request, Context context) {
        // AQUI VAI A LÓGICA DA SUA COBRA!
        // O corpo da requisição (request.getBody()) contém o estado atual do tabuleiro.
        // Você deve analisá-lo para tomar uma decisão inteligente.

        // Exemplo de um objeto tabuleiro:
        // {
        // "height": 11,
        // "width": 11,
        // "food": [
        //     {"x": 5, "y": 5},
        //     {"x": 9, "y": 0},
        //     {"x": 2, "y": 6}
        // ],
        // "hazards": [
        //     {"x": 0, "y": 0},
        //     {"x": 0, "y": 1},
        //     {"x": 0, "y": 2}
        // ],
        // "snakes": [
        //     {"id": "snake-one", ... },
        //     {"id": "snake-two", ... },
        //     {"id": "snake-three", ... }
        // ]
        // }

        // O tabuleiro deve ser acessado usando: 
        // Type mapType = new TypeToken<Map<String, String>>() {}.getType();
        // Map<String, String> body = gson.fromJson(response.getBody(), mapType);
        
        // Exemplo de lógica muito simples: sempre se mover para cima.
        // CUIDADO: Isso fará sua cobra bater na parede rapidamente!

        String requestBody = request.getBody();
        GameState gameState = gson.fromJson(requestBody, GameState.class);
        Board board = gameState.getBoard();
        Snake you = gameState.getYou();
        
        System.out.println("--- Turno: " + gameState.getTurn() + " ---");
        System.out.println("Minha vida: " + you.getHealth());
        System.out.println("Posição da minha cabeça: (" + you.getHead().getX() + ", " + you.getHead().getY() + ")");

        if (!board.getFood().isEmpty()) {
            Coordinate firstFood = board.getFood().get(0);
            System.out.println("Comida mais próxima em: (" + firstFood.getX() + ", " + firstFood.getY() + ")");
        }

        // Lógica do movimento (exemplo simples)
        // Agora que você tem o estado do jogo, pode implementar uma lógica mais inteligente aqui.

        Map<String, String> move = new HashMap<>();
        move.put("move", "up");
        move.put("shout", "Estou indo para cima!"); // Opcional

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