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
 * @param reasonCode        transaction_status_reason_code, sent only when transactionStatus is RJCT or PDNG
 * @param instructedAmount  instructed_amount used when the Make Payment body has no payment_information.instructed_amount
 * @param serviceLevel      service_level used when the Make Payment body has no payment_information.service_level
 * @param postingFlag       posting_flag: PSTG, PSTY or PSTD
 * @param settlementMethod  settlement_method_identification: OFFU, CCOM or ONUS
 */
@ConfigurationProperties(prefix = "stub.status-callback")
public record StatusCallbackProperties(Boolean enabled, String baseUrl, Long delayMs, String transactionStatus,
        String reasonCode, String instructedAmount, String serviceLevel, String postingFlag, String settlementMethod) {

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
        transactionStatus = orDefault(transactionStatus, "ACSC");
        reasonCode = orDefault(reasonCode, "AC07");
        instructedAmount = orDefault(instructedAmount, "6.36");
        serviceLevel = orDefault(serviceLevel, "sct.05");
        postingFlag = orDefault(postingFlag, "PSTY");
        settlementMethod = orDefault(settlementMethod, "OFFU");
    }

    private static String orDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }
}
