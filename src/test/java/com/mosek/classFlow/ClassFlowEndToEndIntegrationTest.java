package com.mosek.classFlow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mosek.classFlow.entity.Role;
import com.mosek.classFlow.entity.User;
import com.mosek.classFlow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClassFlowEndToEndIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void setupAdmin() throws Exception {
        if (!userRepository.existsByEmailIgnoreCase("admin@classflow.edu")) {
            userRepository.save(User.builder()
                    .email("admin@classflow.edu")
                    .passwordHash(passwordEncoder.encode("AdminPass123!"))
                    .firstName("Super")
                    .lastName("Admin")
                    .role(Role.ADMIN)
                    .active(true)
                    .build());
        }
        adminToken = login("admin@classflow.edu", "AdminPass123!");
    }

    @Test
    @DisplayName("Full E2E flow across Phases 1 to 6 and clean error handling")
    void fullEndToEndWorkflow() throws Exception {
        // 0. Unauthenticated request returns clean 401 ApiErrorResponse
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        // 0b. Malformed JSON returns clean 400 without stack trace
        MvcResult badJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST_FORMAT"))
                .andReturn();
        assertThat(badJson.getResponse().getContentAsString()).doesNotContain("Jackson", "Exception");

        // 1. Admin creates teacher, delegate, class, subject and room
        String teacherId = createUser("prof@classflow.edu", "ProfPass123!", "Jean", "Dupont", Role.TEACHER, null);
        String delegateId = createUser("delegue@classflow.edu", "DelPass123!", "Awa", "Kone", Role.DELEGATE, null);

        JsonNode classJson = postJson("/api/v1/classes", adminToken, """
                {"name":"L3 INFO","track":"Informatique","level":"L3","academicYear":"2026-2027","expectedSize":30,"delegateId":"%s"}
                """.formatted(delegateId));
        String classId = classJson.get("id").asText();

        JsonNode subjectJson = postJson("/api/v1/classes/subjects", adminToken, """
                {"academicClassId":"%s","code":"JAVA","name":"Programmation Java","defaultCoefficient":3.0,"teacherId":"%s"}
                """.formatted(classId, teacherId));
        String subjectId = subjectJson.get("id").asText();

        JsonNode roomJson = postJson("/api/v1/rooms", adminToken, """
                {"code":"A204","name":"Salle A204","wifiSsid":"WIFI-A204","networkSecret":"SECRET-A204","operational":true}
                """);
        String roomId = roomJson.get("id").asText();
        String qrToken = roomJson.get("qrCodeToken").asText();
        assertThat(qrToken).startsWith("QR-A204-");

        // 1b. Delegate imports roster via CSV preview + confirm
        String delegateToken = login("delegue@classflow.edu", "DelPass123!");
        String csv = "Matricule;Nom;Prenoms;Email\nESATIC001;KOUASSI;Jean;jean@etu.edu\nESATIC002;TRAORE;Moussa;moussa@etu.edu\n";
        MockMultipartFile file = new MockMultipartFile("file", "liste.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        MvcResult previewRes = mockMvc.perform(multipart("/api/v1/classes/" + classId + "/roster/preview")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + delegateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validCount").value(2))
                .andReturn();
        JsonNode previewNode = objectMapper.readTree(previewRes.getResponse().getContentAsString());

        mockMvc.perform(post("/api/v1/classes/" + classId + "/roster/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + delegateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fileName":"liste.csv","rows":%s}
                                """.formatted(previewNode.get("validRows").toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // 1c. Student registration fails for unknown matricule, succeeds for ESATIC001
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"matricule":"UNKNOWN99","email":"ghost@etu.edu","password":"Password123!","firstName":"Ghost","lastName":"User"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("MATRICULE_NOT_IN_ROSTER"));

        JsonNode studentAuth = postJson("/api/v1/auth/register", null, """
                {"matricule":"ESATIC001","email":"jean@etu.edu","password":"Password123!","firstName":"Jean","lastName":"Kouassi"}
                """);
        String studentToken = studentAuth.get("accessToken").asText();

        // 2. Delegate creates timetable slot and generates session for today
        LocalDate today = LocalDate.now();
        LocalTime start = LocalTime.now().minusMinutes(5).withSecond(0).withNano(0);
        LocalTime end = start.plusHours(2);
        if (end.isBefore(start)) {
            start = LocalTime.of(8, 0);
            end = LocalTime.of(10, 0);
        }

        postJson("/api/v1/classes/" + classId + "/timetable/slots", delegateToken, """
                {"subjectId":"%s","teacherId":"%s","roomId":"%s","dayOfWeek":"%s","startTime":"%s","endTime":"%s"}
                """.formatted(subjectId, teacherId, roomId, today.getDayOfWeek().name(), start, end));

        JsonNode generated = postJson("/api/v1/classes/" + classId + "/sessions/generate", delegateToken, """
                {"fromDate":"%s","toDate":"%s"}
                """.formatted(today, today));
        assertThat(generated.isArray()).isTrue();
        assertThat(generated.size()).isEqualTo(1);
        String sessionId = generated.get(0).get("id").asText();

        // 3. Open session, verify Wi-Fi check, check-in student, prevent double check-in
        mockMvc.perform(post("/api/v1/sessions/" + sessionId + "/open")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + delegateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Wrong Wi-Fi rejected (RM06)
        mockMvc.perform(post("/api/v1/attendance/check-in")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseSessionId":"%s","qrCodeToken":"%s","wifiSsid":"WRONG-WIFI","networkProof":"SECRET-A204"}
                                """.formatted(sessionId, qrToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NETWORK_VERIFICATION_FAILED"));

        // Valid check-in
        JsonNode checkIn = postJson("/api/v1/attendance/check-in", studentToken, """
                {"courseSessionId":"%s","qrCodeToken":"%s","wifiSsid":"WIFI-A204","networkProof":"SECRET-A204"}
                """.formatted(sessionId, qrToken));
        String recordId = checkIn.get("id").asText();

        // Double check-in rejected (RM07)
        mockMvc.perform(post("/api/v1/attendance/check-in")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseSessionId":"%s","qrCodeToken":"%s","wifiSsid":"WIFI-A204","networkProof":"SECRET-A204"}
                                """.formatted(sessionId, qrToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));

        // Close session & export PDF
        mockMvc.perform(post("/api/v1/attendance/sessions/" + sessionId + "/close")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + delegateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionStatus").value("PENDING_VALIDATION"));

        MvcResult pdfRes = mockMvc.perform(get("/api/v1/attendance/sessions/" + sessionId + "/sheet/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + delegateToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();
        assertThat(new String(pdfRes.getResponse().getContentAsByteArray(), 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");

        // 4. Teacher disputes, delegate corrects (sheet v2), teacher validates v2
        String teacherToken = login("prof@classflow.edu", "ProfPass123!");
        mockMvc.perform(post("/api/v1/validations/sessions/" + sessionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"DISPUTED","comment":"Vérifier le retard de Jean"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("DISPUTED"));

        mockMvc.perform(patch("/api/v1/attendance/records/" + recordId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + delegateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newStatus":"LATE","reason":"Arrivé 20 min après le début"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LATE"));

        mockMvc.perform(post("/api/v1/validations/sessions/" + sessionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"VALIDATED","comment":"Feuille conforme"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sheetVersion").value(2));

        // 5. Personal grades & automatic averages (RM16-RM17)
        postJson("/api/v1/grades/evaluations", studentToken, """
                {"subjectName":"Java","evaluationType":"EXAM","title":"Partiel","score":16,"maxScore":20,"coefficient":2,"semester":"S1","academicYear":"2026-2027","evaluationDate":"2026-11-15"}
                """);
        postJson("/api/v1/grades/evaluations", studentToken, """
                {"subjectName":"Java","evaluationType":"HOMEWORK","title":"TP1","score":10,"maxScore":20,"coefficient":1,"semester":"S1","academicYear":"2026-2027","evaluationDate":"2026-11-16"}
                """);
        mockMvc.perform(get("/api/v1/grades/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallAverage").value(14.0))
                .andExpect(jsonPath("$.totalEvaluations").value(2));

        // 6. Offline sync idempotency (RM18-RM19)
        UUID opId = UUID.randomUUID();
        String syncBody = """
                {"operations":[{"clientOperationId":"%s","operationType":"OFFLINE_NOTE","payload":"{}"}]}
                """.formatted(opId);

        mockMvc.perform(post("/api/v1/sync/push")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(syncBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("APPLIED"));

        mockMvc.perform(post("/api/v1/sync/push")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(syncBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("DUPLICATE_IGNORED"));

        mockMvc.perform(get("/api/v1/sync/pull")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluations.length()").value(2));
    }

    private String login(String email, String password) throws Exception {
        JsonNode res = postJson("/api/v1/auth/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
        return res.get("accessToken").asText();
    }

    private String createUser(String email, String password, String firstName, String lastName, Role role, String classId) throws Exception {
        String body = """
                {"email":"%s","password":"%s","firstName":"%s","lastName":"%s","role":"%s"}
                """.formatted(email, password, firstName, lastName, role.name());
        return postJson("/api/v1/admin/users", adminToken, body).get("id").asText();
    }

    private JsonNode postJson(String url, String token, String body) throws Exception {
        var req = post(url).contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) {
            req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        MvcResult result = mockMvc.perform(req)
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
