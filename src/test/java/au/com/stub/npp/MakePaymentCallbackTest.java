package au.com.stub.npp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Make Payment answers 200 with no body, then POSTs the payment status to the callback URL. */
@SpringBootTest(properties = {"logging.file.name=target/test-logs/npp-stub.log", "stub.status-callback.delay-ms=300"})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class MakePaymentCallbackTest {

    record Received(String method, String path, String traceId, String contentType, String body) {
    }

    private static final BlockingQueue<Received> RECEIVED = new ArrayBlockingQueue<>(10);
    private static final HttpServer SERVER = startServer();

    private static HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                RECEIVED.add(new Received(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                        exchange.getRequestHeaders().getFirst("x-traceId"),
                        exchange.getRequestHeaders().getFirst("Content-Type"), body));
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void callbackUrl(DynamicPropertyRegistry registry) {
        registry.add("stub.status-callback.base-url", () -> "http://127.0.0.1:" + SERVER.getAddress().getPort());
    }

    @AfterAll
    static void stop() {
        SERVER.stop(0);
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void makePaymentReturns200WithoutBodyThenPostsStatus(CapturedOutput output) throws Exception {
        long start = System.currentTimeMillis();
        mvc.perform(post("/PCCNAU53XXX/payments/test1234").header("x-traceId", "trace-42")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"secret\":\"DO-NOT-LOG-ME\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("x-traceId", "trace-42"))
                .andExpect(content().string(""));
        assertThat(System.currentTimeMillis() - start).as("response is not held for the callback").isLessThan(300);

        Received r = RECEIVED.poll(10, TimeUnit.SECONDS);
        assertThat(r).as("status callback received").isNotNull();
        assertThat(System.currentTimeMillis() - start).isGreaterThanOrEqualTo(300);
        assertThat(r.method()).isEqualTo("POST");
        assertThat(r.path()).isEqualTo("/PCCNAU53XXX/payments/test1234/status");
        assertThat(r.traceId()).isEqualTo("trace-42");
        assertThat(r.contentType()).startsWith("application/json");
        assertThat(r.body()).contains("\"transaction_status\":\"ACSC\"")
                .contains("\"original_transaction_identification\":\"test1234\"");

        Thread.sleep(300);
        assertThat(output.getOut())
                .contains(">>> OUTGOING POST http://127.0.0.1:" + SERVER.getAddress().getPort()
                        + "/PCCNAU53XXX/payments/test1234/status traceId=trace-42")
                .contains("<<< OUTGOING RESPONSE POST")
                .doesNotContain("DO-NOT-LOG-ME")
                .doesNotContain("transaction_status");
    }
}
