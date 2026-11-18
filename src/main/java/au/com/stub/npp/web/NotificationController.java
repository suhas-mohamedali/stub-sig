package au.com.stub.npp.web;

import au.com.stub.npp.StubProperties;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * The six webhook operations of the Cuscal NPP Payments Notification API.
 * Request bodies are accepted but never inspected or logged.
 */
@RestController
public class NotificationController {

    private static final String TRACE_ID = "x-traceId";

    /** Error statuses the spec defines for every operation. */
    private static final Set<Integer> SPEC_ERROR_STATUSES = Set.of(400, 401, 404, 429, 500, 503, 504);

    private final StubProperties properties;

    public NotificationController(StubProperties properties) {
        this.properties = properties;
    }

    /** Receive Payment and Receive Clearing Request (ReceivePaymentRequest1). */
    @PostMapping(path = "/{orgId}/payments/{paymentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> receivePayment(@PathVariable String orgId, @PathVariable String paymentId,
            @RequestHeader(value = TRACE_ID, required = false) String traceId,
            @RequestHeader(value = StubOverrides.RESPONSE_STATUS, required = false) String forcedStatus,
            @RequestHeader(value = StubOverrides.DELAY_MS, required = false) String delayMs,
            @RequestBody(required = false) String body) {
        return respond(traceId, forcedStatus, delayMs, Map.of());
    }

    /** Receive IFTI Payment and IFTI Clearing Requests (ReceivePaymentRequest2). */
    @PostMapping(path = "/{orgId}/payments/ifti/{paymentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> receiveIftiPayment(@PathVariable String orgId, @PathVariable String paymentId,
            @RequestHeader(value = TRACE_ID, required = false) String traceId,
            @RequestHeader(value = StubOverrides.RESPONSE_STATUS, required = false) String forcedStatus,
            @RequestHeader(value = StubOverrides.DELAY_MS, required = false) String delayMs,
            @RequestBody(required = false) String body) {
        return respond(traceId, forcedStatus, delayMs, Map.of());
    }

    /** Receive RTRF, Request to Return Funds (RTRFRequest1). */
    @PostMapping(path = "/{orgId}/requests/returns/{paymentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> receiveRtrf(@PathVariable String orgId, @PathVariable String paymentId,
            @RequestHeader(value = TRACE_ID, required = false) String traceId,
            @RequestHeader(value = StubOverrides.RESPONSE_STATUS, required = false) String forcedStatus,
            @RequestHeader(value = StubOverrides.DELAY_MS, required = false) String delayMs,
            @RequestBody(required = false) String body) {
        return respond(traceId, forcedStatus, delayMs, Map.of());
    }

    /** Receive Payment Status (PaymentStatusReportResponse1). */
    @PostMapping(path = "/{orgId}/payments/{paymentId}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> receivePaymentStatus(@PathVariable String orgId, @PathVariable String paymentId,
            @RequestHeader(value = TRACE_ID, required = false) String traceId,
            @RequestHeader(value = StubOverrides.RESPONSE_STATUS, required = false) String forcedStatus,
            @RequestHeader(value = StubOverrides.DELAY_MS, required = false) String delayMs,
            @RequestBody(required = false) String body) {
        return respond(traceId, forcedStatus, delayMs, Map.of());
    }

    /** Receive Payment Initiation Status, MAPAIN (MandatePaymentStatusReportRequest1). */
    @PostMapping(path = "/{orgId}/instructions/{pmtIdInstrId}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> receivePaymentInitiationStatus(@PathVariable String orgId,
            @PathVariable String pmtIdInstrId,
            @RequestHeader(value = TRACE_ID, required = false) String traceId,
            @RequestHeader(value = StubOverrides.RESPONSE_STATUS, required = false) String forcedStatus,
            @RequestHeader(value = StubOverrides.DELAY_MS, required = false) String delayMs,
            @RequestBody(required = false) String body) {
        return respond(traceId, forcedStatus, delayMs, Map.of());
    }

    /** Receive Payment Initiation, RAPAIN (MandatePaymentInitiationRequest1). Returns MandatePaymentInitiationResponse1. */
    @PostMapping(path = "/{orgId}/instructions/{pmtIdInstrId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> receivePaymentInitiation(@PathVariable String orgId,
            @PathVariable String pmtIdInstrId,
            @RequestHeader(value = TRACE_ID, required = false) String traceId,
            @RequestHeader(value = StubOverrides.RESPONSE_STATUS, required = false) String forcedStatus,
            @RequestHeader(value = StubOverrides.REASON_CODE, required = false) String reasonCode,
            @RequestHeader(value = StubOverrides.DELAY_MS, required = false) String delayMs,
            @RequestBody(required = false) String body) {
        String code = (reasonCode == null || reasonCode.isBlank()) ? properties.rapainReasonCode() : reasonCode.trim();
        return respond(traceId, forcedStatus, delayMs, Map.of("reason_code", code));
    }

    private ResponseEntity<Object> respond(String traceId, String forcedStatus, String delayMs, Object okBody) {
        sleep(delayMs);

        if (forcedStatus != null && !forcedStatus.isBlank()) {
            HttpStatus status = parseStatus(forcedStatus.trim());
            if (status == null) {
                return error(HttpStatus.BAD_REQUEST, traceId, "CUS.API.STUB01",
                        StubOverrides.RESPONSE_STATUS + " must be one of " + SPEC_ERROR_STATUSES + " or 200");
            }
            if (status != HttpStatus.OK) {
                return error(status, traceId, "CUS.API." + status.value(), status.getReasonPhrase());
            }
        }

        if (properties.requireTraceId() && (traceId == null || traceId.isBlank())) {
            return error(HttpStatus.BAD_REQUEST, traceId, "CUS.API.400", "Missing required header x-traceId");
        }

        return withTrace(ResponseEntity.ok(), traceId).body(okBody);
    }

    private static HttpStatus parseStatus(String value) {
        try {
            int code = Integer.parseInt(value);
            if (code == 200 || SPEC_ERROR_STATUSES.contains(code)) {
                return HttpStatus.valueOf(code);
            }
        } catch (NumberFormatException ignored) {
            // fall through
        }
        return null;
    }

    private static ResponseEntity<Object> error(HttpStatus status, String traceId, String code, String message) {
        return withTrace(ResponseEntity.status(status), traceId).body(new ErrorResponse(code, message));
    }

    private static ResponseEntity.BodyBuilder withTrace(ResponseEntity.BodyBuilder builder, String traceId) {
        if (traceId != null && !traceId.isBlank()) {
            builder.header(TRACE_ID, traceId);
        }
        return builder.contentType(MediaType.APPLICATION_JSON);
    }

    private static void sleep(String delayMs) {
        if (delayMs == null || delayMs.isBlank()) {
            return;
        }
        try {
            long ms = Math.min(Long.parseLong(delayMs.trim()), 120_000L);
            if (ms > 0) {
                Thread.sleep(ms);
            }
        } catch (NumberFormatException ignored) {
            // ignore a bad value
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
