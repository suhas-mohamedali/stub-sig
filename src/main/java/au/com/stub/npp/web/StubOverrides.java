package au.com.stub.npp.web;

/** Request headers testers can send to steer the stub's response. They are not part of the Cuscal spec. */
public final class StubOverrides {

    /** Force an HTTP status, e.g. 400, 401, 404, 429, 500, 503, 504. The body is an Error1 object. */
    public static final String RESPONSE_STATUS = "x-stub-response-status";

    /** RAPAIN only: reason_code to return instead of the configured default, e.g. AC05. */
    public static final String REASON_CODE = "x-stub-reason-code";

    /** Delay the response by this many milliseconds, to test client timeouts. */
    public static final String DELAY_MS = "x-stub-delay-ms";

    private StubOverrides() {
    }
}
