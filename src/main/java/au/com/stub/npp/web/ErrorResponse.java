package au.com.stub.npp.web;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Error1 schema from the spec. */
public record ErrorResponse(
        @JsonProperty("error_code") String errorCode,
        @JsonProperty("error_message") String errorMessage) {
}
