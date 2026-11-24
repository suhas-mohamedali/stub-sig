package au.com.stub.npp.callback;

import au.com.stub.npp.StatusCallbackProperties;
import jakarta.annotation.PreDestroy;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sends the delayed payment status callback, POST {baseUrl}/{orgId}/payments/{paymentId}/status,
 * with a PaymentStatusReportResponse1 body. Only the call and its response status are logged,
 * never the body.
 */
@Component
public class PaymentStatusCallback {

    private static final Logger log = LoggerFactory.getLogger("npp.calls");

    private final StatusCallbackProperties properties;
    private final RestClient client;
    private final JsonMapper json = JsonMapper.builder().build();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "status-callback");
        t.setDaemon(true);
        return t;
    });

    public PaymentStatusCallback(StatusCallbackProperties properties) {
        this.properties = properties;
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .proxy(HttpClient.Builder.NO_PROXY)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    /** Schedules the callback; returns at once. Does nothing when the callback is disabled. */
    public void schedule(String orgId, String paymentId, String traceId, String requestBody) {
        if (!properties.enabled()) {
            return;
        }
        String trace = (traceId == null || traceId.isBlank()) ? UUID.randomUUID().toString() : traceId;
        log.info("... SCHEDULED status callback for {}/{} in {} ms traceId={}",
                orgId, paymentId, properties.delayMs(), trace);
        Map<String, Object> body = body(paymentId, requestBody);
        scheduler.schedule(() -> send(orgId, paymentId, trace, body), properties.delayMs(), TimeUnit.MILLISECONDS);
    }

    String url(String orgId, String paymentId) {
        return UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path("/{orgId}/payments/{paymentId}/status")
                .buildAndExpand(orgId, paymentId)
                .encode()
                .toUriString();
    }

    private void send(String orgId, String paymentId, String traceId, Map<String, Object> body) {
        String url = url(orgId, paymentId);
        MDC.put("traceId", traceId);
        long start = System.nanoTime();
        try {
            log.info(">>> OUTGOING POST {} traceId={} (payment status callback)", url, traceId);
            HttpStatusCode status = client.post()
                    .uri(url)
                    .header("x-traceId", traceId)
                    .contentType(MediaType.APPLICATION_JSON)
                    // serialised up front so the request carries a Content-Length instead of chunked encoding
                    .body(json.writeValueAsString(withAcceptanceTime(body)))
                    .retrieve()
                    .onStatus(s -> true, (req, res) -> { })
                    .toBodilessEntity()
                    .getStatusCode();
            log.info("<<< OUTGOING RESPONSE POST {} status={} traceId={} durationMs={}",
                    url, status.value(), traceId, (System.nanoTime() - start) / 1_000_000);
        } catch (Exception e) {
            log.warn("<<< OUTGOING FAILED POST {} traceId={} durationMs={} error={}",
                    url, traceId, (System.nanoTime() - start) / 1_000_000, e.getMessage());
        } finally {
            MDC.remove("traceId");
        }
    }

    /**
     * Builds the PaymentStatusReportResponse1 body. instructed_amount and service_level are copied from the
     * Make Payment request's payment_information when present; the request body itself is never logged.
     */
    public Map<String, Object> body(String paymentId, String requestBody) {
        JsonNode paymentInformation = paymentInformation(requestBody);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("original_transaction_identification", paymentId);
        info.put("transaction_status", properties.transactionStatus());
        if ("RJCT".equals(properties.transactionStatus()) || "PDNG".equals(properties.transactionStatus())) {
            info.put("transaction_status_reason_code", properties.reasonCode());
        }
        info.put("acceptance_date_time", null);
        info.put("instructed_amount", text(paymentInformation, "instructed_amount", properties.instructedAmount()));
        info.put("posting_flag", properties.postingFlag());
        info.put("service_level", text(paymentInformation, "service_level", properties.serviceLevel()));
        info.put("settlement_method_identification", properties.settlementMethod());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transaction_status_information", info);
        return body;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> withAcceptanceTime(Map<String, Object> body) {
        Map<String, Object> info = (Map<String, Object>) body.get("transaction_status_information");
        info.put("acceptance_date_time", Instant.now().truncatedTo(ChronoUnit.MILLIS).toString());
        return body;
    }

    private JsonNode paymentInformation(String requestBody) {
        if (requestBody == null || requestBody.isBlank()) {
            return null;
        }
        try {
            JsonNode node = json.readTree(requestBody).path("payment_information");
            return node.isObject() ? node : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String text(JsonNode node, String field, String fallback) {
        if (node == null) {
            return fallback;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isValueNode() || value.asString().isBlank()) {
            return fallback;
        }
        return value.asString();
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }
}
