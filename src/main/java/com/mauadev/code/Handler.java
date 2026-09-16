package com.mauadev.code;

// Ponte entre o AWS Lambda e a logica da sua cobra.
//
// Voce NAO precisa mexer aqui. Este arquivo recebe o evento da Lambda,
// descobre qual rota do Battlesnake foi chamada e repassa para Logic.java.
//
// Rotas da API (https://docs.battlesnake.com/api):
//   GET  /        -> aparencia da cobra
//   POST /start   -> a partida comecou
//   POST /move    -> escolha a jogada deste turno
//   POST /end     -> a partida acabou
//
// O evento chega como Map porque a Lambda tem dois formatos de payload e o
// template aceita os dois:
//   - 1.0, do API Gateway REST: o caminho vem em "path"
//   - 2.0, da Function URL e do HTTP API: vem em "rawPath" e em
//     "requestContext.http.path"
// Tipar a entrada como APIGatewayProxyRequestEvent aceitaria so o 1.0, e todo
// evento 2.0 chegaria com os campos nulos.

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mauadev.code.entities.GameState;

import java.util.HashMap;
import java.util.Map;

public class Handler implements RequestHandler<Map<String, Object>, APIGatewayProxyResponseEvent> {

    private static final Gson GSON = new Gson();

    /** Rotas do Battlesnake, na forma canonica. */
    private static final String[] ROTAS = {"/start", "/move", "/end"};

    /** Nomes de stage que o API Gateway pode colocar na frente do caminho. */
    private static final String[] STAGES = {"dev", "homolog", "prod", "staging"};

    /** Corpo de requisicao que nao da para usar: vira 400, e nao 500. */
    private static class CorpoInvalidoException extends RuntimeException {
        CorpoInvalidoException(String mensagem) {
            super(mensagem);
        }
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(Map<String, Object> request, Context context) {
        try {
            String rota = extrairRota(request);

            if (rota == null) {
                return resposta(404, erro("rota nao encontrada: " + caminhoBruto(request)));
            }

            if (!metodoValido(request, rota)) {
                return resposta(404, erro("metodo nao permitido para a rota: " + caminhoBruto(request)));
            }

            switch (rota) {
                case "/start": {
                    GameState state = lerEstado(request);
                    log(context, "JOGO COMECOU (partida " + state.getGame().getId() + ")");
                    Logic.start(state);
                    return resposta(200, "ok");
                }
                case "/move": {
                    GameState state = lerEstado(request);
                    String jogada = Logic.getMove(state);
                    log(context, "MOVE " + state.getTurn() + ": " + jogada);
                    return resposta(200, Map.of("move", jogada));
                }
                case "/end": {
                    GameState state = lerEstado(request);
                    log(context, "FIM DE JOGO apos " + state.getTurn() + " turnos");
                    Logic.end(state);
                    return resposta(200, "ok");
                }
                default:
                    return resposta(200, Logic.info());
            }

        } catch (CorpoInvalidoException e) {
            log(context, "REQUISICAO INVALIDA: " + e.getMessage());
            return resposta(400, erro(e.getMessage()));

        } catch (Exception e) {
            log(context, "ERROR: " + e);
            return resposta(500, erro(e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // Roteamento
    // ------------------------------------------------------------------

    /**
     * Descobre qual rota do Battlesnake foi chamada, nos dois formatos de evento.
     *
     * <p>Compara pelo sufixo do caminho, entao tolera o nome do stage na frente
     * ("/dev/move") e barra sobrando no fim ("/move/").
     *
     * @return "/", "/start", "/move", "/end", ou {@code null} se a rota for desconhecida
     */
    static String extrairRota(Map<String, Object> request) {
        String caminho = caminhoBruto(request);

        if (caminho == null) {
            return null;
        }

        String limpo = caminho.trim();
        while (limpo.length() > 1 && limpo.endsWith("/")) {
            limpo = limpo.substring(0, limpo.length() - 1);
        }

        String normalizado = limpo;
        for (String stage : STAGES) {
            if (normalizado.equals("/" + stage)) {
                normalizado = "/";
                break;
            } else if (normalizado.startsWith("/" + stage + "/")) {
                normalizado = normalizado.substring(stage.length() + 1);
                break;
            }
        }

        if (normalizado.isEmpty() || normalizado.equals("/")) {
            return "/";
        }

        for (String rota : ROTAS) {
            if (normalizado.equals(rota)) {
                return rota;
            }
        }

        return null;
    }

    static String extrairMetodo(Map<String, Object> request) {
        if (request == null) {
            return null;
        }

        // formato 1.0 (API Gateway REST)
        String metodo = texto(request.get("httpMethod"));
        if (metodo != null) {
            return metodo;
        }

        // formato 2.0 (Function URL e HTTP API)
        Object contexto = request.get("requestContext");
        if (contexto instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> rc = (Map<String, Object>) contexto;
            Object http = rc.get("http");
            if (http instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> httpMap = (Map<String, Object>) http;
                metodo = texto(httpMap.get("method"));
                if (metodo != null) {
                    return metodo;
                }
            }
            return texto(rc.get("httpMethod"));
        }

        return null;
    }

    static boolean metodoValido(Map<String, Object> request, String rota) {
        String metodo = extrairMetodo(request);
        if (metodo == null) {
            return true;
        }

        if ("/".equals(rota)) {
            String httpMethod = texto(request.get("httpMethod"));
            if (httpMethod != null) {
                return httpMethod.equalsIgnoreCase("GET");
            }
            return metodo.equalsIgnoreCase("GET") || metodo.equalsIgnoreCase("POST");
        }

        return metodo.equalsIgnoreCase("POST");
    }

    /** Le o caminho do evento, seja ele no formato 1.0 ou 2.0. */
    @SuppressWarnings("unchecked")
    private static String caminhoBruto(Map<String, Object> request) {
        if (request == null) {
            return null;
        }

        // formato 1.0 (API Gateway REST)
        String caminho = texto(request.get("path"));
        if (caminho != null) {
            return caminho;
        }

        // formato 2.0 (Function URL e HTTP API)
        caminho = texto(request.get("rawPath"));
        if (caminho != null) {
            return caminho;
        }

        Object contexto = request.get("requestContext");
        if (contexto instanceof Map) {
            Map<String, Object> rc = (Map<String, Object>) contexto;
            Object http = rc.get("http");
            if (http instanceof Map) {
                caminho = texto(((Map<String, Object>) http).get("path"));
                if (caminho != null) {
                    return caminho;
                }
            }
            return texto(rc.get("path"));
        }

        return null;
    }

    // ------------------------------------------------------------------
    // Corpo da requisicao
    // ------------------------------------------------------------------

    /** Desserializa o corpo como GameState. Corpo ruim vira 400, e nao 500. */
    private static GameState lerEstado(Map<String, Object> request) {
        String corpo = texto(request.get("body"));

        if (corpo == null || corpo.isBlank()) {
            throw new CorpoInvalidoException("corpo da requisicao vazio");
        }

        GameState state;
        try {
            state = GSON.fromJson(corpo, GameState.class);
        } catch (JsonSyntaxException e) {
            throw new CorpoInvalidoException("JSON invalido: " + e.getMessage());
        }

        if (state == null || state.getYou() == null || state.getYou().getHead() == null) {
            throw new CorpoInvalidoException("estado de jogo incompleto: falta 'you'");
        }
        if (state.getGame() == null) {
            throw new CorpoInvalidoException("estado de jogo incompleto: falta 'game'");
        }

        return state;
    }

    // ------------------------------------------------------------------
    // Resposta
    // ------------------------------------------------------------------

    /** Monta a resposta com Content-Type, inclusive nos erros. */
    private static APIGatewayProxyResponseEvent resposta(int status, Object corpo) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", corpo instanceof String ? "text/plain" : "application/json");

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(status)
                .withHeaders(headers)
                .withBody(corpo instanceof String ? (String) corpo : GSON.toJson(corpo));
    }

    /**
     * HashMap, e nao Map.of: se a excecao vier sem mensagem, Map.of lancaria
     * NullPointerException dentro do proprio tratador de erro.
     */
    private static Map<String, String> erro(String mensagem) {
        Map<String, String> corpo = new HashMap<>();
        corpo.put("error", mensagem == null ? "erro interno" : mensagem);
        return corpo;
    }

    private static String texto(Object valor) {
        return valor instanceof String ? (String) valor : null;
    }

    private static void log(Context context, String mensagem) {
        if (context != null && context.getLogger() != null) {
            context.getLogger().log(mensagem);
        }
    }
}
