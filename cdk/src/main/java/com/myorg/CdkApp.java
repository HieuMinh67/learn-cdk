package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.util.Arrays;

public class CdkApp {
    public static void main(final String[] args) {
        App app = new App();

        new CdkStack(app, "CdkStack", StackProps.builder()
                .env(Environment.builder()
                        .account("639750771399")
                        .region("ap-southeast-1")
                        .build())
                .build());

        app.synth();
    }
}

