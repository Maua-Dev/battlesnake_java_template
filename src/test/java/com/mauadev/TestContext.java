package com.mauadev;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class TestContext implements Context {
    @Override public String getAwsRequestId() { return "test-request-id"; }
    @Override public String getLogGroupName() { return "/aws/lambda/test"; }
    @Override public String getLogStreamName() { return "test-stream"; }
    @Override public String getFunctionName() { return "test-function"; }
    @Override public String getFunctionVersion() { return "1"; }
    @Override public String getInvokedFunctionArn() { return "arn:test"; }
    @Override public CognitoIdentity getIdentity() { return null; }
    @Override public ClientContext getClientContext() { return null; }
    @Override public int getRemainingTimeInMillis() { return 5000; }
    @Override public int getMemoryLimitInMB() { return 512; }
    @Override public LambdaLogger getLogger() {
        return message -> System.out.println("[LAMBDA] " + message);
    }
}