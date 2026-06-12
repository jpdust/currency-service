package com.unstampedpages.currencyservice;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * AWS Lambda entry point. Bridges HTTP API (v2) Gateway proxy events to the Spring Boot application.
 *
 * <p>Lambda handler string:
 * {@code com.unstampedpages.currencyservice.StreamLambdaHandler::handleRequest}
 *
 * <p>Uses the explicit {@link SpringBootProxyHandlerBuilder} with {@code .servletApplication()}
 * to ensure the correct {@code AnnotationConfigServletWebServerApplicationContext} is always
 * created. The legacy static shortcut {@code getHttpApiV2ProxyHandler()} allows Spring Boot to
 * infer the context type, which fails inside Lambda's classloader.
 *
 * <p>Deploy the shadow JAR produced by {@code ./gradlew shadowJar}, located at
 * {@code build/libs/currency-service-*-all.jar}.
 */
public class StreamLambdaHandler implements RequestStreamHandler {

    private static final SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> handler;

    static {
        try {
            handler = new SpringBootProxyHandlerBuilder<HttpApiV2ProxyRequest>()
                    .defaultHttpApiV2Proxy()
                    .servletApplication()
                    .springBootApplication(CurrencyServiceApplication.class)
                    .buildAndInitialize();
        } catch (ContainerInitializationException e) {
            throw new RuntimeException("Failed to initialize Spring Boot application", e);
        }
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        handler.proxyStream(inputStream, outputStream, context);
    }
}
