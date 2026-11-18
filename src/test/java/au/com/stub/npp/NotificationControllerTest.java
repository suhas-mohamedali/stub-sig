package au.com.stub.npp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "logging.file.name=target/test-logs/npp-stub.log")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class NotificationControllerTest {

    private static final String TRACE = "5323b85f-a7cb-49ba-acfb-03abc7e07fb0";
    private static final String BODY = "{\"secret_field\":\"DO-NOT-LOG-ME\"}";

    @Autowired
    private MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {
            "/CUSCAU2SXXX/payments/BANKNTSTXXXN20180501000000000000010",
            "/CUSCAU2SXXX/payments/ifti/BANKNTSTXXXN20180501000000000000010",
            "/CUSCAU2SXXX/requests/returns/BANKNTSTXXXN20180501000000000000010",
            "/CUSCAU2SXXX/payments/BANKNTSTXXXN20180501000000000000010/status",
            "/CUSCAU2SXXX/instructions/INSTR0001/status"})
    void objectEndpointsReturnEmptyJson(String path) throws Exception {
        mvc.perform(post(path).header("x-traceId", TRACE).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(header().string("x-traceId", TRACE))
                .andExpect(content().json("{}"));
    }

    @Test
    void rapainReturnsDefaultReasonCode() throws Exception {
        mvc.perform(post("/CUSCAU2SXXX/instructions/INSTR0001").header("x-traceId", TRACE)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason_code").value("ACCP"));
    }

    @Test
    void rapainReasonCodeCanBeOverridden() throws Exception {
        mvc.perform(post("/CUSCAU2SXXX/instructions/INSTR0001").header("x-traceId", TRACE)
                        .header("x-stub-reason-code", "AC05")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason_code").value("AC05"));
    }

    @Test
    void missingTraceIdReturns400Error1() throws Exception {
        mvc.perform(post("/CUSCAU2SXXX/payments/P1").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("CUS.API.400"))
                .andExpect(jsonPath("$.error_message").exists());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 404, 429, 500, 503, 504})
    void forcedErrorStatusReturnsError1(int code) throws Exception {
        mvc.perform(post("/CUSCAU2SXXX/payments/P1").header("x-traceId", TRACE)
                        .header("x-stub-response-status", code)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().is(code))
                .andExpect(jsonPath("$.error_code").value("CUS.API." + code));
    }

    @Test
    void logsCallAndResponseWithoutBody(CapturedOutput output) throws Exception {
        mvc.perform(post("/CUSCAU2SXXX/payments/P1").header("x-traceId", TRACE)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());

        assertThat(output.getOut())
                .contains(">>> INCOMING POST /CUSCAU2SXXX/payments/P1 traceId=" + TRACE)
                .contains("<<< RESPONSE POST /CUSCAU2SXXX/payments/P1 status=200 traceId=" + TRACE)
                .doesNotContain("DO-NOT-LOG-ME");
    }
}
