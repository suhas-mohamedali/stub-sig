package au.com.stub.npp.web;

import au.com.stub.npp.StubProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catches any method or path that none of the spec operations match, for example a GET on
 * /{orgId}/payments/{paymentId} or a URL with an extra base path. With stub.always-ok on it
 * answers 200 {} so the caller is never refused; otherwise it answers 404 with an Error1 body.
 * Either way the call is flagged in the log as UNMAPPED.
 */
@RestController
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger("npp.calls");

    private final StubProperties properties;

    public FallbackController(StubProperties properties) {
        this.properties = properties;
    }

    @RequestMapping(path = "/**", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> anything(HttpServletRequest request) {
        log.warn("!!! UNMAPPED {} {} matches no operation in the NPP Payments Notification spec",
                request.getMethod(), request.getRequestURI());
        String traceId = request.getHeader("x-traceId");
        ResponseEntity.BodyBuilder builder = properties.alwaysOk()
                ? ResponseEntity.ok()
                : ResponseEntity.status(HttpStatus.NOT_FOUND);
        if (traceId != null && !traceId.isBlank()) {
            builder.header("x-traceId", traceId);
        }
        builder.contentType(MediaType.APPLICATION_JSON);
        return properties.alwaysOk()
                ? builder.body(Map.of())
                : builder.body(new ErrorResponse("CUS.API.404",
                        request.getMethod() + " " + request.getRequestURI() + " is not an operation in the spec"));
    }
}
