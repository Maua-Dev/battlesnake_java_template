package com.mauadev.code;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.LambdaFunctionUrlRequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mauadev.model.GameState; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

public class Snake implements RequestHandler<LambdaFunctionUrlRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger log = LoggerFactory.getLogger(Snake.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(LambdaFunctionUrlRequestEvent request, Context context) {

        String path = request.getRequestContext().getHttp().getPath();
        log.info("Request recebido para o path: {}", path);

        try {
            switch (path) {
                case "/":

                    return createResponse(Map.of(
                        "apiversion", "1",
                        "author", "mauadev",
                        "color", "#FF00FF", // Rosa-choque!
                        "head", "default",
                        "tail", "default"
                    ));
                case "/start":
                    log.info("Jogo iniciado!");
                    return createResponse(null); // Resposta OK vazia
                case "/move":
                    // A lógica principal da sua cobra
                    GameState moveState = objectMapper.readValue(request.getBody(), GameState.class);
                    String move = decideMove(moveState);
                    log.info("Turno {}: movendo para {}", moveState.turn, move);
                    return createResponse(Map.of("move", move));
                case "/end":
                    log.info("Jogo finalizado!");
                    return createResponse(null); // Resposta OK vazia
                default:
                    return new APIGatewayProxyResponseEvent()
                        .withStatusCode(404)
                        .withBody("Path nao encontrado: " + path);
            }
        } catch (Exception e) {
            log.error("Erro ao processar o request", e);
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Erro interno do servidor");
        }
    }

    /**
     * Lógica de decisão de movimento (exemplo MUITO simples).
     * ✅ É AQUI QUE VOCÊ DEVE COLOCAR SUA INTELIGÊNCIA!
     */
    
    private String decideMove(GameState gameState) {
        // Estratégia simples: apenas tenta não bater na parede à sua frente (indo para cima).
        if (gameState.you.head.y == gameState.board.height - 1) {
            return "left"; // Se estiver no topo, vira para a esquerda.
        }
        return "up"; // Senão, continua subindo.
    }

    /**
     * Helper para criar uma resposta HTTP 200 com corpo JSON.
     */
    private APIGatewayProxyResponseEvent createResponse(Object body) {
        try {
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(Collections.singletonMap("Content-Type", "application/json"));
            
            if (body != null) {
                response.withBody(objectMapper.writeValueAsString(body));
            }
            return response;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Erro ao serializar resposta JSON", e);
            return new APIGatewayProxyResponseEvent().withStatusCode(500);
        }
    }
}