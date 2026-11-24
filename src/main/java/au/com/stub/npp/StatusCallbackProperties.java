package au.com.stub.npp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the payment status callback (prefix {@code stub.status-callback}).
 * After a Make Payment POST to /{orgId}/payments/{paymentId}, the stub POSTs a payment status
 * to {baseUrl}/{orgId}/payments/{paymentId}/status once delayMs has passed.
 *
 * @param enabled           turn the callback on or off
 * @param baseUrl           where the status is sent, e.g. http://localhost:4002
 * @param delayMs           wait before sending, in milliseconds
 * @param transactionStatus transaction_status sent in the callback body, e.g. ACSC or RJCT
 */
@ConfigurationProperties(prefix = "stub.status-callback")
public record StatusCallbackProperties(Boolean enabled, String baseUrl, Long delayMs, String transactionStatus) {

    public StatusCallbackProperties {
        if (enabled == null) {
            enabled = Boolean.TRUE;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:4002";
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (delayMs == null || delayMs < 0) {
            delayMs = 3000L;
        }
        if (transactionStatus == null || transactionStatus.isBlank()) {
            transactionStatus = "ACSC";
        }
    }
}
