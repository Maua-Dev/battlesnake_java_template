package com.mauadev.code;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.Collections;
import java.util.Map;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        // Cria um objeto de resposta
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            // Pega um parâmetro da query string, se existir
            String name = "Mundo";
            Map<String, String> queryParams = request.getQueryStringParameters();
            if (queryParams != null && queryParams.containsKey("name")) {
                name = queryParams.get("name");
            }
            
            String body = String.format("{ \"message\": \"Olá, %s!\" }", name);

            // Define a resposta de sucesso
            response.setStatusCode(200);
            response.setHeaders(Collections.singletonMap("Content-Type", "application/json"));
            response.setBody(body);

        } catch (Exception e) {
            // Em caso de erro
            response.setStatusCode(500);
            response.setBody(String.format("{ \"error\": \"%s\" }", e.getMessage()));
        }

        return response;
    }
}