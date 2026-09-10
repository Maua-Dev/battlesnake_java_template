package com.mauadev;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mauadev.code.Handler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes do Handler - percorrem o caminho completo
 * API Gateway -> Handler -> Logic.
 */
public class HandlerTest {

    private static final List<String> DIRECOES = List.of("up", "down", "left", "right");
    private static final Gson GSON = new Gson();

    private Handler handler;
    private Context context;

    @BeforeEach
    public void setUp() {
        handler = new Handler();
        context = new TestContext();
    }

    // --- Helpers ---

    /** Monta um estado de jogo minimo com cabeca e pescoco. */
    private String gameStateJson(int hx, int hy, int nx, int ny) {
        return String.format("""
                {
                  "game": {"id": "partida-de-teste", "timeout": 500},
                  "turn": 4,
                  "board": {
                    "height": 11, "width": 11,
                    "food": [{"x": 5, "y": 5}],
                    "hazards": [],
                    "snakes": []
                  },
                  "you": {
                    "id": "minha-cobra", "name": "MinhaCobra", "health": 100,
                    "body": [{"x": %d, "y": %d}, {"x": %d, "y": %d}, {"x": %d, "y": %d}],
                    "head": {"x": %d, "y": %d},
                    "length": 3
                  }
                }
                """, hx, hy, nx, ny, nx, ny - 1, hx, hy);
    }

    private APIGatewayProxyRequestEvent request(String path, String body) {
        APIGatewayProxyRequestEvent req = new APIGatewayProxyRequestEvent();
        req.setPath(path);
        req.setBody(body);
        return req;
    }

    private Map<String, String> parseBody(String json) {
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        return GSON.fromJson(json, type);
    }

    // --- T1: info retorna campos obrigatorios ---

    @Test
    @DisplayName("T1 - GET / retorna os campos obrigatorios da cobra")
    public void testInfo_retornaCamposObrigatorios() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("/", null), context);

        assertEquals(200, response.getStatusCode());
        Map<String, String> body = parseBody(response.getBody());
        assertEquals("1", body.get("apiversion"));
        assertNotNull(body.get("author"));
        assertNotNull(body.get("color"));
        assertNotNull(body.get("head"));
        assertNotNull(body.get("tail"));
    }

    // --- T2: move retorna direcao valida ---

    @Test
    @DisplayName("T2 - POST /move retorna uma direcao valida")
    public void testMove_retornaDirecaoValida() {
        String stateJson = gameStateJson(5, 4, 4, 4);

        for (int i = 0; i < 50; i++) {
            APIGatewayProxyResponseEvent response = handler.handleRequest(request("/move", stateJson), context);
            assertEquals(200, response.getStatusCode());
            Map<String, String> body = parseBody(response.getBody());
            assertTrue(DIRECOES.contains(body.get("move")),
                    "Direcao invalida: " + body.get("move"));
        }
    }

    // --- T3: nunca volta contra o pescoco ---

    @Test
    @DisplayName("T3a - nao volta para a esquerda quando pescoco esta a esquerda")
    public void testMove_naoVoltaEsquerda() {
        String state = gameStateJson(5, 4, 4, 4); // pescoco a esquerda
        for (int i = 0; i < 50; i++) {
            APIGatewayProxyResponseEvent resp = handler.handleRequest(request("/move", state), context);
            assertNotEquals("left", parseBody(resp.getBody()).get("move"));
        }
    }

    @Test
    @DisplayName("T3b - nao volta para a direita quando pescoco esta a direita")
    public void testMove_naoVoltaDireita() {
        String state = gameStateJson(5, 4, 6, 4); // pescoco a direita
        for (int i = 0; i < 50; i++) {
            APIGatewayProxyResponseEvent resp = handler.handleRequest(request("/move", state), context);
            assertNotEquals("right", parseBody(resp.getBody()).get("move"));
        }
    }

    @Test
    @DisplayName("T3c - nao desce quando pescoco esta abaixo")
    public void testMove_naoDesce() {
        String state = gameStateJson(5, 4, 5, 3); // pescoco abaixo
        for (int i = 0; i < 50; i++) {
            APIGatewayProxyResponseEvent resp = handler.handleRequest(request("/move", state), context);
            assertNotEquals("down", parseBody(resp.getBody()).get("move"));
        }
    }

    @Test
    @DisplayName("T3d - nao sobe quando pescoco esta acima")
    public void testMove_naoSobe() {
        String state = gameStateJson(5, 4, 5, 5); // pescoco acima
        for (int i = 0; i < 50; i++) {
            APIGatewayProxyResponseEvent resp = handler.handleRequest(request("/move", state), context);
            assertNotEquals("up", parseBody(resp.getBody()).get("move"));
        }
    }

    // --- T6: comportamento sem safe moves (nao deve lancar excecao) ---

    @Test
    @DisplayName("T6 - comportamento definido quando nao ha safe moves")
    public void testMove_comportamentoSemSafeMoves() {
        // Cabeca no canto, pescoco acima, corpo a direita
        String stateJson = """
                {
                  "game": {"id": "teste", "timeout": 500},
                  "turn": 1,
                  "board": {"height": 11, "width": 11, "food": [], "hazards": [], "snakes": []},
                  "you": {
                    "id": "s1", "name": "cobra", "health": 100,
                    "body": [{"x": 0, "y": 0}, {"x": 0, "y": 1}, {"x": 1, "y": 0}],
                    "head": {"x": 0, "y": 0}, "length": 3
                  }
                }
                """;
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("/move", stateJson), context);
        assertEquals(200, response.getStatusCode());
        Map<String, String> body = parseBody(response.getBody());
        assertTrue(DIRECOES.contains(body.get("move")), "Direcao invalida: " + body.get("move"));
    }

    // --- start e end retornam 200 ---

    @Test
    @DisplayName("POST /start retorna 200")
    public void testStart_retorna200() {
        String state = gameStateJson(5, 4, 4, 4);
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("/start", state), context);
        assertEquals(200, response.getStatusCode());
    }

    @Test
    @DisplayName("POST /end retorna 200")
    public void testEnd_retorna200() {
        String state = gameStateJson(5, 4, 4, 4);
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("/end", state), context);
        assertEquals(200, response.getStatusCode());
    }
}