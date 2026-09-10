package com.mauadev.code;

// Ponte entre o AWS Lambda e a logica da sua cobra.
//
// Voce NAO precisa mexer aqui. Este arquivo recebe o evento do API Gateway,
// descobre qual rota do Battlesnake foi chamada e repassa para Logic.java.
//
// Rotas da API (https://docs.battlesnake.com/api):
//   GET  /        -> aparencia da cobra
//   POST /start   -> a partida comecou
//   POST /move    -> escolha a jogada deste turno
//   POST /end     -> a partida acabou

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.mauadev.code.entities.GameState;

import java.util.Collections;
import java.util.Map;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Gson GSON = new Gson();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        String path = request.getPath();
        Object responseBody = null;

        try {
            switch (path) {
                case "/":
                    responseBody = Logic.info();
                    break;
                case "/start":
                    handleStart(request, context);
                    break;
                case "/move":
                    responseBody = handleMove(request, context);
                    break;
                case "/end":
                    handleEnd(request, context);
                    break;
                default:
                    return response
                            .withStatusCode(404)
                            .withBody(GSON.toJson(Map.of("error", "Path not found: " + path)));
            }

            response.setStatusCode(200);
            response.setHeaders(Collections.singletonMap("Content-Type", "application/json"));
            if (responseBody != null) {
                response.setBody(GSON.toJson(responseBody));
            }

        } catch (Exception e) {
            context.getLogger().log("ERROR: " + e.getMessage());
            response.setStatusCode(500);
            response.setBody(GSON.toJson(Map.of("error", e.getMessage())));
        }

        return response;
    }

    /** POST /start */
    private void handleStart(APIGatewayProxyRequestEvent request, Context context) {
        GameState state = parseState(request);
        context.getLogger().log("JOGO COMECOU (partida " + (state != null && state.getGame() != null ? state.getGame().getId() : "?") + ")");
        if (state != null) Logic.start(state);
    }

    /** POST /move */
    private Map<String, String> handleMove(APIGatewayProxyRequestEvent request, Context context) {
        GameState state = parseState(request);
        String chosen = (state != null) ? Logic.getMove(state) : "up";
        context.getLogger().log("MOVE " + (state != null ? state.getTurn() : "?") + ": " + chosen);
        return Map.of("move", chosen);
    }

    /** POST /end */
    private void handleEnd(APIGatewayProxyRequestEvent request, Context context) {
        GameState state = parseState(request);
        context.getLogger().log("FIM DE JOGO apos " + (state != null ? state.getTurn() : "?") + " turnos");
        if (state != null) Logic.end(state);
    }

    /** Desserializa o corpo da requisicao como GameState. Retorna null se falhar. */
    private GameState parseState(APIGatewayProxyRequestEvent request) {
        try {
            return GSON.fromJson(request.getBody(), GameState.class);
        } catch (Exception e) {
            return null;
        }
    }
}