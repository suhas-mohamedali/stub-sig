package au.com.stub.npp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Default settings: stub.always-ok=true, so every call gets 200. */
@SpringBootTest(properties = "logging.file.name=target/test-logs/npp-stub.log")
@AutoConfigureMockMvc
class AlwaysOkModeTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void missingTraceIdStillReturns200() throws Exception {
        mvc.perform(post("/CUSCAU2SXXX/payments/P1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
    }

    @Test
    void forcedErrorStatusIsIgnored() throws Exception {
        mvc.perform(post("/CUSCAU2SXXX/payments/P1").header("x-traceId", "t1")
                        .header("x-stub-response-status", "503")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void rapainStillReturnsReasonCode() throws Exception {
        mvc.perform(post("/CUSCAU2SXXX/instructions/INSTR0001").header("x-stub-reason-code", "AC05")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason_code").value("AC05"));
    }
}
