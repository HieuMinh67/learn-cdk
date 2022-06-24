package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.pipelines.CodePipeline;
import software.amazon.awscdk.pipelines.CodePipelineSource;
import software.amazon.awscdk.pipelines.ShellStep;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.apigateway.IResource;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

public class CdkStack extends Stack {
    public CdkStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public CdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        List<String> functionPackagingInstructions = Arrays.asList(
                "/bin/sh",
                "-c",
                "cd HealthCheck " +
                        "&& mvn clean install " +
                        "&& ls /asset-input/HealthCheck/target/" +
                        "&& cp /asset-input/HealthCheck/target/health-checker.jar /asset-output/"
        );

        BundlingOptions.Builder builderOptions = BundlingOptions.builder()
                .command(functionPackagingInstructions)
                .image(Runtime.JAVA_11.getBundlingImage())
                .volumes(singletonList(
                        DockerVolume.builder()
                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                .containerPath("/root/.m2/")
                                .build()
                ))
                .user("root")
                .outputType(BundlingOutput.ARCHIVED);

        Function checkHealthFunction = new Function(
                this,
                "HealthCheck",
                FunctionProps.builder()
                        .code(Code.fromAsset("../lambda", AssetOptions.builder()
                                .bundling(builderOptions
                                        .command(functionPackagingInstructions)
                                        .build()
                                ).build()
                        ))
                        .handler("com.henry.App")
                        .runtime(Runtime.JAVA_11)
                        .timeout(Duration.seconds(30))
                        .memorySize(512)
                        .build()
        );

        RestApi api = new RestApi(this, "healthApi",
                RestApiProps.builder().restApiName("Server's Health Service").build());

        IResource items = api.getRoot().addResource("health");

        Integration healthIntegration = new LambdaIntegration(checkHealthFunction);
        items.addMethod("GET", healthIntegration);

        Map<String, String> env = new HashMap<>();
        env.put("privileged", "true");
        CodePipeline pipeline = CodePipeline.Builder.create(this, "pipeline")
                .pipelineName("MyPipeline")
                .synth(ShellStep.Builder.create("Synth")
                        .input(CodePipelineSource.gitHub("HieuMinh67/learn-cdk", "main"))
                        .commands(Arrays.asList("npm install -g aws-cdk", "cd cdk", "cdk synth"))
                        .build())
                .dockerEnabledForSynth(true)
                .build();
    }
}
