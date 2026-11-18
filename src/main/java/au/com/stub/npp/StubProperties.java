package au.com.stub.npp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Stub behaviour settings (prefix {@code stub}).
 *
 * @param alwaysOk         when true, every call returns 200 with the success body, whatever the headers say.
 *                         Set to false to enable the x-traceId check and the x-stub-response-status override
 * @param rapainReasonCode default reason_code returned by Receive Payment Initiation (RAPAIN)
 * @param requireTraceId   when true (and alwaysOk is false), a missing x-traceId header returns 400
 */
@ConfigurationProperties(prefix = "stub")
public record StubProperties(Boolean alwaysOk, String rapainReasonCode, Boolean requireTraceId) {

    public StubProperties {
        if (alwaysOk == null) {
            alwaysOk = Boolean.TRUE;
        }
        if (rapainReasonCode == null || rapainReasonCode.isBlank()) {
            rapainReasonCode = "ACCP";
        }
        if (requireTraceId == null) {
            requireTraceId = Boolean.TRUE;
        }
    }
}
