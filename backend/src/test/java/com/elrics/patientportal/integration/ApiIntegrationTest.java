package com.elrics.patientportal.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.elrics.patientportal.repository.PatientRepository;
import com.elrics.patientportal.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import java.sql.Connection;
import java.sql.DriverManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end API test against a real MySQL 8 schema (Flyway migrations + Hibernate validation).
 * Automatically skipped when MySQL is not reachable (see TEST_DB_* in .env.example).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIf("mysqlAvailable")
class ApiIntegrationTest {

    static boolean mysqlAvailable() {
        String host = System.getenv().getOrDefault("TEST_DB_HOST", "localhost");
        String port = System.getenv().getOrDefault("TEST_DB_PORT", "3306");
        String user = System.getenv().getOrDefault("TEST_DB_USERNAME", "root");
        String pass = System.getenv().getOrDefault("TEST_DB_PASSWORD", "");
        String url = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=3000";
        try (Connection ignored = DriverManager.getConnection(url, user, pass)) {
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static final String REGISTER_JSON =
            "{\"fullName\":\"Dr. Admin\",\"email\":\"admin@example.com\",\"username\":\"admin\",\"password\":\"Secret123\"}";
    private static final String PATIENT_JSON =
            "{\"fullName\":\"Jane Smith\",\"age\":34,\"gender\":\"Female\",\"phone\":\"+1 555-0101\","
                    + "\"email\":\"jane@example.com\",\"address\":\"12 Main St\",\"bloodGroup\":\"O+\","
                    + "\"status\":\"Admitted\",\"emergencyContact\":\"John - 555-0102\"}";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PatientRepository patientRepository;

    private String token;
    private String patientId;

    @BeforeAll
    void cleanDatabase() {
        patientRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String bearer() {
        return "Bearer " + token;
    }

    @Test
    @Order(1)
    void firstRegistrationCreatesAdministratorWithoutLeakingPassword() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @Order(2)
    void secondRegistrationIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON.replace("admin@example.com", "other@example.com").replace("\"admin\"", "\"other\"")))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    void invalidRegistrationInputIsRejectedWith400() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"nope\",\"username\":\"a\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @Order(4)
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Wrong123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    void loginSucceedsAndReturnsToken() throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin@example.com\",\"password\":\"Secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        token = JsonPath.read(body, "$.token");
    }

    @Test
    @Order(6)
    void protectedEndpointsRejectMissingOrInvalidToken() throws Exception {
        mvc.perform(get("/api/patients")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dashboard/stats")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/patients").header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    void meReturnsCurrentAdministrator() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    @Order(8)
    void createPatient() throws Exception {
        String body = mvc.perform(post("/api/patients").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(PATIENT_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(startsWith("PT-")))
                .andExpect(jsonPath("$.fullName").value("Jane Smith"))
                .andReturn().getResponse().getContentAsString();
        patientId = JsonPath.read(body, "$.id");
    }

    @Test
    @Order(9)
    void duplicatePatientAndInvalidInputAreRejected() throws Exception {
        mvc.perform(post("/api/patients").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(PATIENT_JSON))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/patients").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"age\":500,\"gender\":\"x\",\"phone\":\"1\",\"bloodGroup\":\"Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName").exists());
    }

    @Test
    @Order(10)
    void listAndGetPatient() throws Exception {
        mvc.perform(get("/api/patients").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(patientId));
        mvc.perform(get("/api/patients/" + patientId).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloodGroup").value("O+"));
        mvc.perform(get("/api/patients/PT-999999").header("Authorization", bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(11)
    void searchPatients() throws Exception {
        mvc.perform(get("/api/patients/search").param("query", "jane").header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/patients/search").param("query", patientId).header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/patients/search").param("query", "nobody-here").header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Order(12)
    void dashboardStatsComeFromDatabase() throws Exception {
        mvc.perform(get("/api/dashboard/stats").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPatients").value(1))
                .andExpect(jsonPath("$.todaysAdmissions").value(1))
                .andExpect(jsonPath("$.currentlyAdmitted").value(1))
                .andExpect(jsonPath("$.recentActivity", hasSize(1)));
    }

    @Test
    @Order(13)
    void updatePatient() throws Exception {
        mvc.perform(put("/api/patients/" + patientId).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PATIENT_JSON.replace("Admitted", "Discharged").replace("\"age\":34", "\"age\":35")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Discharged"))
                .andExpect(jsonPath("$.age").value(35));
        mvc.perform(get("/api/dashboard/stats").header("Authorization", bearer()))
                .andExpect(jsonPath("$.dischargedToday").value(1));
    }

    @Test
    @Order(14)
    void deletePatient() throws Exception {
        mvc.perform(delete("/api/patients/" + patientId).header("Authorization", bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/patients/" + patientId).header("Authorization", bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/dashboard/stats").header("Authorization", bearer()))
                .andExpect(jsonPath("$.totalPatients").value(0));
    }
}
