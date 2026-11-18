package au.com.stub.npp.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Logs one line when a call arrives and one line when its response goes out.
 * Only metadata is logged (method, path, trace id, content type and length, client address,
 * status, duration). Request and response bodies are never read or written to the log.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CallLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("npp.calls");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith(request.getContextPath() + "/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        String traceId = orDash(request.getHeader("x-traceId"));
        MDC.put("traceId", traceId);
        try {
            log.info(">>> INCOMING {} {} traceId={} contentType={} contentLength={} from={}",
                    request.getMethod(), request.getRequestURI(), traceId,
                    orDash(request.getContentType()), request.getContentLengthLong(), request.getRemoteAddr());
            chain.doFilter(request, response);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            log.info("<<< RESPONSE {} {} status={} traceId={} durationMs={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), traceId, ms);
            MDC.remove("traceId");
        }
    }

    private static String orDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }
}
