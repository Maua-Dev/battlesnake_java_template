package com.mauadev;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mauadev.code.Handler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.HashMap;
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

    /** Evento no formato 1.0 (API Gateway REST): o caminho vem em "path". */
    private Map<String, Object> request(String path, String body) {
        Map<String, Object> req = new HashMap<>();
        req.put("path", path);
        req.put("body", body);
        return req;
    }

    /** Evento no formato 2.0 (Function URL): o caminho vem em "rawPath". */
    private Map<String, Object> requestV2(String path, String body) {
        Map<String, Object> http = new HashMap<>();
        http.put("path", path);
        http.put("method", "POST");

        Map<String, Object> requestContext = new HashMap<>();
        requestContext.put("http", http);

        Map<String, Object> req = new HashMap<>();
        req.put("version", "2.0");
        req.put("rawPath", path);
        req.put("requestContext", requestContext);
        req.put("body", body);
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

    // --- T4: evita parede quando tem opcao ---

    @Test
    @DisplayName("T4 - evita parede quando tem outra opcao")
    public void testMove_evitaParede() {
        // Cobra no canto (0,0), pescoco a direita (1,0):
        // left (x=-1) e down (y=-1) saem do tabuleiro. Pescoço bloqueia right.
        // A única opção segura é up.
        String state = gameStateJson(0, 0, 1, 0);
        for (int i = 0; i < 50; i++) {
            APIGatewayProxyResponseEvent resp = handler.handleRequest(request("/move", state), context);
            String move = parseBody(resp.getBody()).get("move");
            assertNotEquals("left", move, "saiu do tabuleiro para a esquerda");
            assertNotEquals("down", move, "saiu do tabuleiro para baixo");
            assertNotEquals("right", move, "voltou pelo pescoco");
            assertEquals("up", move, "deveria ter escolhido up");
        }
    }

    // --- T5: evita proprio corpo quando tem opcao ---

    @Test
    @DisplayName("T5 - evita proprio corpo quando tem opcao")
    public void testMove_evitaProprioCorpo() {
        // Cabeca em (5,4), pescoco em (4,4) [esquerda], corpo em (5,5) [acima]
        // Restam right e down
        String stateJson = """
                {
                  "game": {"id": "teste", "timeout": 500},
                  "turn": 1,
                  "board": {"height": 11, "width": 11, "food": [], "hazards": [], "snakes": []},
                  "you": {
                    "id": "s1", "name": "cobra", "health": 100,
                    "body": [{"x": 5, "y": 4}, {"x": 4, "y": 4}, {"x": 5, "y": 5}, {"x": 4, "y": 3}],
                    "head": {"x": 5, "y": 4}, "length": 4
                  }
                }
                """;
        for (int i = 0; i < 50; i++) {
            APIGatewayProxyResponseEvent resp = handler.handleRequest(request("/move", stateJson), context);
            String move = parseBody(resp.getBody()).get("move");
            assertNotEquals("left", move, "voltou pelo pescoco");
            assertNotEquals("up", move, "bateu no proprio corpo");
            assertTrue(List.of("right", "down").contains(move));
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

    // --- T7: formato 2.0 (Function URL e HTTP API) ---

    @Test
    @DisplayName("T7a - GET / no formato 2.0 retorna as informacoes da cobra")
    public void testFormatoV2_info() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestV2("/", null), context);

        assertEquals(200, response.getStatusCode());
        assertEquals("1", parseBody(response.getBody()).get("apiversion"));
    }

    @Test
    @DisplayName("T7b - POST /move no formato 2.0 retorna uma direcao valida")
    public void testFormatoV2_move() {
        String state = gameStateJson(5, 4, 4, 4);
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestV2("/move", state), context);

        assertEquals(200, response.getStatusCode());
        String jogada = parseBody(response.getBody()).get("move");
        assertTrue(DIRECOES.contains(jogada), "Direcao invalida: " + jogada);
    }

    @Test
    @DisplayName("T7c - formato 2.0 sem rawPath ainda resolve por requestContext.http.path")
    public void testFormatoV2_semRawPath() {
        String state = gameStateJson(5, 4, 4, 4);
        Map<String, Object> req = requestV2("/move", state);
        req.remove("rawPath");

        APIGatewayProxyResponseEvent response = handler.handleRequest(req, context);

        assertEquals(200, response.getStatusCode());
        assertTrue(DIRECOES.contains(parseBody(response.getBody()).get("move")));
    }

    // --- T8: prefixo de stage e barra sobrando ---

    @Test
    @DisplayName("T8 - caminho com o stage na frente e barra no fim ainda roteia")
    public void testPrefixoDeStage() {
        String state = gameStateJson(5, 4, 4, 4);

        assertEquals(200, handler.handleRequest(request("/dev/move", state), context).getStatusCode());
        assertEquals(200, handler.handleRequest(request("/move/", state), context).getStatusCode());
        assertEquals(200, handler.handleRequest(request("/dev", null), context).getStatusCode());
    }

    @Test
    @DisplayName("T8b - rota realmente desconhecida continua sendo 404")
    public void testRotaDesconhecida_retorna404() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("/nao-existe", null), context);

        assertEquals(404, response.getStatusCode());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
    }

    @Test
    @DisplayName("T8c - prefixo nao reconhecido como stage deve retornar 404")
    public void testRoteamentoExato_urlPermissivaRetorna404() {
        String state = gameStateJson(5, 4, 4, 4);
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("/alguma-coisa/move", state), context);

        assertEquals(404, response.getStatusCode());
    }

    @Test
    @DisplayName("T8d - metodo incorreto retorna 404")
    public void testMetodoIncorreto_retorna404() {
        Map<String, Object> reqGetMove = request("/move", null);
        reqGetMove.put("httpMethod", "GET");
        APIGatewayProxyResponseEvent resp1 = handler.handleRequest(reqGetMove, context);
        assertEquals(404, resp1.getStatusCode());

        Map<String, Object> reqPostInfo = request("/", null);
        reqPostInfo.put("httpMethod", "POST");
        APIGatewayProxyResponseEvent resp2 = handler.handleRequest(reqPostInfo, context);
        assertEquals(404, resp2.getStatusCode());
    }

    // --- T9: corpo invalido vira 400, e nao 200 nem 500 ---

    @Test
    @DisplayName("T9a - POST /move sem corpo retorna 400")
    public void testMove_semCorpo_retorna400() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("/move", null), context);

        assertEquals(400, response.getStatusCode());
        assertNotNull(parseBody(response.getBody()).get("error"));
    }

    @Test
    @DisplayName("T9b - POST /move com JSON quebrado retorna 400")
    public void testMove_jsonQuebrado_retorna400() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("/move", "{isso nao e json}"), context);

        assertEquals(400, response.getStatusCode());
    }

    @Test
    @DisplayName("T9c - POST /move sem o campo 'you' retorna 400, e nao uma jogada inventada")
    public void testMove_semYou_retorna400() {
        String semYou = """
                {"game": {"id": "x", "timeout": 500}, "turn": 1,
                 "board": {"height": 11, "width": 11, "food": [], "hazards": [], "snakes": []}}
                """;
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("/move", semYou), context);

        assertEquals(400, response.getStatusCode());
        assertNull(parseBody(response.getBody()).get("move"), "nao pode devolver jogada para estado invalido");
    }

    // --- T10: toda resposta traz Content-Type ---

    @Test
    @DisplayName("T10 - todas as respostas trazem Content-Type")
    public void testContentTypeSemprePresente() {
        String state = gameStateJson(5, 4, 4, 4);

        assertNotNull(handler.handleRequest(request("/", null), context).getHeaders().get("Content-Type"));
        assertNotNull(handler.handleRequest(request("/move", state), context).getHeaders().get("Content-Type"));
        assertNotNull(handler.handleRequest(request("/move", null), context).getHeaders().get("Content-Type"));
        assertNotNull(handler.handleRequest(request("/nao-existe", null), context).getHeaders().get("Content-Type"));
    }
}