package com.mauadev;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import com.mauadev.code.Handler;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para a classe Handler.
 */
public class HandlerTest {

    private static final List<String> DIRECOES_VALIDAS = List.of("up", "down", "left", "right");

    private Handler handler;
    private Context testContext;
    private Gson gson;

    // @BeforeEach garante que este método seja executado antes de cada @Test
    @BeforeEach
    public void setUp() {
        handler = new Handler();
        gson = new Gson();
        // Criamos um Context de teste "mockado" para passar para o handler.
        testContext = new TestContext();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Monta um estado de jogo com a cabeça e o pescoço nas posições indicadas.
     * O terceiro segmento do corpo só existe para a cobra ter tamanho 3.
     */
    private String gameStateJson(int headX, int headY, int neckX, int neckY) {
        return String.format("""
        {
          "game": {
            "id": "game-id-123",
            "ruleset": {"name": "standard", "version": "v1.2.3"},
            "timeout": 500
          },
          "turn": 4,
          "board": {
            "height": 11,
            "width": 11,
            "food": [{"x": 5, "y": 5}],
            "hazards": [{"x": 3, "y": 3}],
            "snakes": []
          },
          "you": {
            "id": "snake-id-yours",
            "name": "MySnake",
            "health": 90,
            "body": [{"x": %d, "y": %d}, {"x": %d, "y": %d}, {"x": %d, "y": %d}],
            "head": {"x": %d, "y": %d},
            "length": 3,
            "shout": "Going for food!"
          }
        }
        """, headX, headY, neckX, neckY, neckX, neckY - 1, headX, headY);
    }

    /** Estado de jogo padrão: cabeça em (5,4) e pescoço à esquerda, em (4,4). */
    private String gameStateJson() {
        return gameStateJson(5, 4, 4, 4);
    }

    private APIGatewayProxyResponseEvent chamar(String path, String body) {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath(path);
        request.setBody(body);
        return handler.handleRequest(request, testContext);
    }

    private Map<String, String> corpoComoMapa(APIGatewayProxyResponseEvent response) {
        Type mapType = new TypeToken<Map<String, String>>() {}.getType();
        return gson.fromJson(response.getBody(), mapType);
    }

    // ------------------------------------------------------------------
    // Rotas
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Teste da rota / (Info) - Deve retornar informações da cobra com status 200")
    public void testHandleInfo_shouldReturnSnakeInfo() {
        // Act
        APIGatewayProxyResponseEvent response = chamar("/", null);

        // Assert
        assertEquals(200, response.getStatusCode());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
        assertNotNull(response.getBody());

        Map<String, String> body = corpoComoMapa(response);

        assertEquals("1", body.get("apiversion"));
        assertNotNull(body.get("author"));
        assertNotNull(body.get("color"));
        assertNotNull(body.get("head"));
        assertNotNull(body.get("tail"));
    }

    @Test
    @DisplayName("Teste da rota /start - Deve retornar status 200 OK sem corpo")
    public void testHandleStart_shouldReturn200OK() {
        APIGatewayProxyResponseEvent response = chamar("/start", gameStateJson());

        assertEquals(200, response.getStatusCode());
        // O handler para /start não define um corpo de resposta
        assertTrue(response.getBody() == null || response.getBody().isEmpty());
    }

    @Test
    @DisplayName("Teste da rota /move - Deve devolver uma direção válida")
    public void testHandleMove_shouldReturnValidDirection() {
        APIGatewayProxyResponseEvent response = chamar("/move", gameStateJson());

        assertEquals(200, response.getStatusCode(), "O status code da resposta deve ser 200");
        assertNotNull(response.getBody(), "O corpo da resposta não pode ser nulo");

        String direcao = corpoComoMapa(response).get("move");

        assertTrue(DIRECOES_VALIDAS.contains(direcao), "Direção inválida: " + direcao);
    }

    @Test
    @DisplayName("Teste da rota /end - Deve retornar status 200 OK sem corpo")
    public void testHandleEnd_shouldReturn200OK() {
        APIGatewayProxyResponseEvent response = chamar("/end", gameStateJson());

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody() == null || response.getBody().isEmpty());
    }

    // ------------------------------------------------------------------
    // Stage do API Gateway
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Rotas com o stage na frente (/dev/...) devem responder 200")
    public void testStagePrefixedPaths_shouldReturn200() {
        // O API Gateway REST entrega o caminho prefixado pelo nome do stage.
        // Antes da normalização, todos estes caiam no default e devolviam 404.
        assertEquals(200, chamar("/dev", null).getStatusCode(), "GET /dev deveria responder 200");
        assertEquals(200, chamar("/dev/start", gameStateJson()).getStatusCode(), "POST /dev/start deveria responder 200");
        assertEquals(200, chamar("/dev/move", gameStateJson()).getStatusCode(), "POST /dev/move deveria responder 200");
        assertEquals(200, chamar("/dev/end", gameStateJson()).getStatusCode(), "POST /dev/end deveria responder 200");
    }

    @Test
    @DisplayName("/dev devolve as informações da cobra, e não um 404")
    public void testStageRoot_shouldReturnSnakeInfo() {
        APIGatewayProxyResponseEvent response = chamar("/dev", null);

        assertEquals(200, response.getStatusCode());
        assertEquals("1", corpoComoMapa(response).get("apiversion"));
    }

    @Test
    @DisplayName("/dev/move devolve uma direção válida")
    public void testStagePrefixedMove_shouldReturnValidDirection() {
        APIGatewayProxyResponseEvent response = chamar("/dev/move", gameStateJson());

        String direcao = corpoComoMapa(response).get("move");

        assertTrue(DIRECOES_VALIDAS.contains(direcao), "Direção inválida: " + direcao);
    }

    @Test
    @DisplayName("Barra sobrando no fim do caminho não quebra o roteamento")
    public void testTrailingSlash_shouldStillRoute() {
        assertEquals(200, chamar("/dev/move/", gameStateJson()).getStatusCode());
        assertEquals(200, chamar("/move/", gameStateJson()).getStatusCode());
    }

    @Test
    @DisplayName("Caminho desconhecido cai na rota de informações da cobra")
    public void testUnknownPath_shouldFallBackToInfo() {
        // Com a normalização por sufixo não existe mais 404: qualquer caminho
        // que não seja /start, /move ou /end é tratado como a raiz. É o mesmo
        // comportamento dos templates de Rust e JavaScript.
        APIGatewayProxyResponseEvent response = chamar("/caminho-que-nao-existe", null);

        assertEquals(200, response.getStatusCode());
        assertEquals("1", corpoComoMapa(response).get("apiversion"));
    }

    // ------------------------------------------------------------------
    // Lógica da cobra
    // ------------------------------------------------------------------

    @Test
    @DisplayName("A cobra nunca volta por cima do próprio pescoço")
    public void testMove_neverGoesBackwards() {
        // Cabeça sempre em (5,4); o pescoço muda de lado a cada caso.
        int[][] casos = {
            // neckX, neckY, e a direção que seria andar para trás
            {4, 4}, // pescoço à esquerda  -> não pode ir "left"
            {6, 4}, // pescoço à direita   -> não pode ir "right"
            {5, 3}, // pescoço abaixo      -> não pode ir "down"
            {5, 5}, // pescoço acima       -> não pode ir "up"
        };
        String[] proibidas = {"left", "right", "down", "up"};

        for (int i = 0; i < casos.length; i++) {
            String body = gameStateJson(5, 4, casos[i][0], casos[i][1]);
            String proibida = proibidas[i];

            // O movimento é sorteado, então repetimos para pegar qualquer
            // chance de a direção proibida escapar.
            for (int tentativa = 0; tentativa < 50; tentativa++) {
                APIGatewayProxyResponseEvent response = chamar("/move", body);
                String direcao = corpoComoMapa(response).get("move");

                assertTrue(DIRECOES_VALIDAS.contains(direcao), "Direção inválida: " + direcao);
                assertNotEquals(proibida, direcao,
                    "A cobra andou para trás (" + proibida + ") com o pescoço em ("
                        + casos[i][0] + "," + casos[i][1] + ")");
            }
        }
    }

    /**
     * Uma implementação simples da interface Context para fins de teste.
     * Ela fornece um logger que imprime no console, evitando NullPointerException.
     */
    private static class TestContext implements Context {
        @Override
        public String getAwsRequestId() { return "test-request-id"; }
        @Override
        public String getLogGroupName() { return "test-log-group"; }
        @Override
        public String getLogStreamName() { return "test-log-stream"; }
        @Override
        public String getFunctionName() { return "test-function"; }
        @Override
        public String getFunctionVersion() { return "1.0"; }
        @Override
        public String getInvokedFunctionArn() { return "arn:aws:lambda:us-east-1:123456789012:function:test-function"; }
        @Override
        public com.amazonaws.services.lambda.runtime.CognitoIdentity getIdentity() { return null; }
        @Override
        public com.amazonaws.services.lambda.runtime.ClientContext getClientContext() { return null; }
        @Override
        public int getRemainingTimeInMillis() { return 30000; }
        @Override
        public int getMemoryLimitInMB() { return 1024; }
        @Override
        public LambdaLogger getLogger() {
            // Retorna um logger simples que imprime no console durante o teste.
            return new TestLogger();
        }
    }

    private static class TestLogger implements LambdaLogger {
        @Override
        public void log(String message) {
            System.out.println(message);
        }
        @Override
        public void log(byte[] message) {
            System.out.println(new String(message));
        }
    }
}
