package au.com.stub.npp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Stub behaviour settings (prefix {@code stub}).
 *
 * @param rapainReasonCode default reason_code returned by Receive Payment Initiation (RAPAIN)
 * @param requireTraceId   when true, a missing x-traceId header returns 400 as the spec marks it required
 */
@ConfigurationProperties(prefix = "stub")
public record StubProperties(String rapainReasonCode, Boolean requireTraceId) {

    public StubProperties {
        if (rapainReasonCode == null || rapainReasonCode.isBlank()) {
            rapainReasonCode = "ACCP";
        }
        if (requireTraceId == null) {
            requireTraceId = Boolean.TRUE;
        }
    }
}
