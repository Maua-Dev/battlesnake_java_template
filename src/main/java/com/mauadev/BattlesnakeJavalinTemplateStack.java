package com.mauadev;

import software.constructs.Construct;

import java.util.List;
import java.util.Map;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.SecretValue;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.cloudwatch.ComparisonOperator;
import software.amazon.awscdk.services.cloudwatch.MetricOptions;
import software.amazon.awscdk.services.cloudwatch.actions.SnsAction;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Policy;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.User;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionUrlAuthType;
import software.amazon.awscdk.services.lambda.FunctionUrlOptions;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.sns.ITopic;
import software.amazon.awscdk.services.sns.Topic;

public class BattlesnakeJavalinTemplateStack extends Stack {
    public BattlesnakeJavalinTemplateStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public BattlesnakeJavalinTemplateStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        final String projectName = System.getenv("PROJECT_NAME");
        final String awsAccountId = System.getenv("AWS_ACCOUNT_ID");

        if (projectName == null || awsAccountId == null) {
            throw new IllegalArgumentException("PROJECT_NAME and AWS_ACCOUNT_ID environment variables are required.");
        }

        Function lambdaFn = Function.Builder.create(this, "BattleSnakeLambda")
                .runtime(Runtime.JAVA_21)
                .code(Code.fromAsset("../src/target/your-lambda-code.jar")) // Adjust path to your JAR file
                .handler("com.example.Handler::handleRequest") // Use Java handler format
                .timeout(Duration.seconds(15))
                .build();

        var lambdaUrl = lambdaFn.addFunctionUrl(FunctionUrlOptions.builder()
                .authType(FunctionUrlAuthType.NONE)
                .build());

        final String password = projectName + "UserPassword7@";

        User user = User.Builder.create(this, projectName + "User")
                .userName(projectName + "User")
                .passwordResetRequired(true)
                .password(SecretValue.unsafePlainText(password))
                .build();

        Policy policy = Policy.Builder.create(this, "Policy")
                .statements(List.of(
                        PolicyStatement.Builder.create()
                                .actions(List.of("lambda:*"))
                                .resources(List.of(lambdaFn.getFunctionArn()))
                                .build()
                ))
                .build();

        policy.addStatements(
                PolicyStatement.Builder.create()
                        .actions(List.of("logs:*"))
                        .resources(List.of("arn:aws:logs:*:*:*"))
                        .build()
        );

        policy.attachToUser(user);
        user.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("IAMUserChangePassword"));

        var alarm = lambdaFn.metricInvocations(MetricOptions.builder()
                .period(Duration.hours(6))
                .build()
        ).createAlarm(this, projectName + "LambdaAlarm", software.amazon.awscdk.services.cloudwatch.CreateAlarmOptions.builder()
                .threshold(5000)
                .evaluationPeriods(1)
                .comparisonOperator(ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD)
                .build()
        );

        ITopic topic = Topic.fromTopicArn(this, projectName + "Topic", String.format("arn:aws:sns:%s:%s:sns-battlesnake", this.getRegion(), awsAccountId));
        alarm.addAlarmAction(new SnsAction(topic));

        CfnOutput.Builder.create(this, projectName + "Url")
                .value(lambdaUrl.getUrl())
                .exportName(projectName + "UrlValue")
                .build();

        CfnOutput.Builder.create(this, projectName + "UserOutput")
                .value(user.getUserName())
                .exportName(projectName + "UserValue")
                .build();

        CfnOutput.Builder.create(this, projectName + "FirstTimeUserPassword")
                .value(password)
                .exportName(projectName + "FirstTimeUserPasswordValue")
                .build();
        
        CfnOutput.Builder.create(this, projectName + "LambdaConsole")
                .value(String.format("https://%s.console.aws.amazon.com/lambda/home?region=%s#/functions/%s?tab=code", this.getRegion(), this.getRegion(), lambdaFn.getFunctionName()))
                .exportName(projectName + "LambdaConsoleValue")
                .build();
    }
}
