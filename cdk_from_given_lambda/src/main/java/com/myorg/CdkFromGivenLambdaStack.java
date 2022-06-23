package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketAttributes;
import software.amazon.awscdk.services.s3.IBucket;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

public class CdkFromGivenLambdaStack extends Stack {
    public CdkFromGivenLambdaStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public CdkFromGivenLambdaStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        final String bucketName = "given-jar";
        IBucket bucket = Bucket.fromBucketAttributes(this,
                "givenJarBucket",
                BucketAttributes.builder()
                        .bucketName(bucketName)
                        .build()
        );

        Function checkHealthFunction = new Function(
                this,
                "HealthCheck",
                FunctionProps.builder()
                        .code(S3Code.fromBucket(bucket,"health-checker.jar"))
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
    }
}
