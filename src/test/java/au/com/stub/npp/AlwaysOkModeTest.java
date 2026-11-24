package au.com.stub.npp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
@SpringBootTest(properties = {"logging.file.name=target/test-logs/npp-stub.log", "stub.status-callback.enabled=false"})
@AutoConfigureMockMvc
class AlwaysOkModeTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void missingTraceIdStillReturns200() throws Exception {
        mvc.perform(post("/CUSCAU2SXXX/payments/ifti/P1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
    }

    @Test
    void forcedErrorStatusIsIgnored() throws Exception {
        mvc.perform(post("/CUSCAU2SXXX/payments/P1").header("x-traceId", "t1")
                        .header("x-stub-response-status", "503")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void rapainStillReturnsReasonCode() throws Exception {
        mvc.perform(post("/CUSCAU2SXXX/instructions/INSTR0001").header("x-stub-reason-code", "AC05")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason_code").value("AC05"));
    }

    @Test
    void getOnPaymentPathReturns200() throws Exception {
        mvc.perform(get("/PCCNAU53XXX/payments/test1234"))
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
    }

    @Test
    void unknownPathAndMethodReturn200() throws Exception {
        mvc.perform(put("/b2b/npp/payments/v4/PCCNAU53XXX/payments/test1234").content("{}"))
                .andExpect(status().isOk());
        mvc.perform(post("/anything/else"))
                .andExpect(status().isOk());
    }

    @Test
    void healthStillWorks() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
