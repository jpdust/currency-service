package com.unstampedpages.currencyservice;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link StreamLambdaHandler}.
 *
 * <p>The static {@code handler} field is replaced with a mock via reflection so that
 * no Spring context is started during these tests. The original value is restored
 * after each test.
 */
class StreamLambdaHandlerTest {

    private Field handlerField;
    private SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> originalHandler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        handlerField = StreamLambdaHandler.class.getDeclaredField("handler");
        handlerField.setAccessible(true);
        originalHandler = (SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse>)
                handlerField.get(null);
    }

    @AfterEach
    void tearDown() throws Exception {
        handlerField.set(null, originalHandler);
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleRequest_delegatesToHandlerProxyStream() throws Exception {
        var mockHandler = (SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse>)
                mock(SpringBootLambdaContainerHandler.class);
        handlerField.set(null, mockHandler);

        var in = new ByteArrayInputStream(new byte[0]);
        var out = new ByteArrayOutputStream();
        var ctx = mock(Context.class);

        new StreamLambdaHandler().handleRequest(in, out, ctx);

        verify(mockHandler).proxyStream(in, out, ctx);
    }

    @Test
    void initHandler_onContainerInitializationException_throwsRuntimeException() {
        var cause = new ContainerInitializationException("init failed", null);

        assertThatThrownBy(() -> StreamLambdaHandler.initHandler(() -> { throw cause; }))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to initialize Spring Boot application")
                .hasCause(cause);
    }
}
