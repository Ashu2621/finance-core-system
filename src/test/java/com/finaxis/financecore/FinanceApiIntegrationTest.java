package com.finaxis.financecore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finaxis.financecore.auth.JwtService;
import com.finaxis.financecore.audit.AuditEventRepository;
import com.finaxis.financecore.record.IdempotencyRecordRepository;
import com.finaxis.financecore.record.FinancialRecordRepository;
import com.finaxis.financecore.user.UserAccount;
import com.finaxis.financecore.user.UserRepository;
import com.finaxis.financecore.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FinanceApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FinancialRecordRepository recordRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        auditEventRepository.deleteAll();
        idempotencyRepository.deleteAll();
        recordRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void publicRegistrationCannotEscalateRoleAndViewerCannotWrite() throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Demo Viewer",
                                  "email": "viewer@example.com",
                                  "password": "Secure123",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("VIEWER"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        String token = json.get("token").asText();

        UserAccount user = userRepository.findByEmailIgnoreCase("viewer@example.com").orElseThrow();
        assertThat(user.getRole()).isEqualTo(UserRole.VIEWER);
        assertThat(user.getPassword()).doesNotContain("Secure123");

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0));

        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "viewer-create-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void analystCanCreateAndReadOwnFinancialRecords() throws Exception {
        UserAccount analyst = saveUser("Analyst", "analyst@example.com", UserRole.ANALYST);
        String token = jwtService.generateToken(analyst).token();

        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "analyst-create-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("Engineering"))
                .andExpect(jsonPath("$.amount").value(2500));

        mockMvc.perform(get("/api/records")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].note").value("Cloud tools"));

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(2500))
                .andExpect(jsonPath("$.transactionCount").value(1));

        assertThat(auditEventRepository.count()).isEqualTo(1);
    }

    @Test
    void repeatedIdempotencyKeyReturnsOriginalRecordWithoutDuplicate() throws Exception {
        UserAccount analyst = saveUser("Analyst", "retry@example.com", UserRole.ANALYST);
        String token = jwtService.generateToken(analyst).token();

        String first = mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "payment-attempt-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String repeated = mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "payment-attempt-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readTree(repeated).get("id"))
                .isEqualTo(objectMapper.readTree(first).get("id"));
        assertThat(recordRepository.count()).isEqualTo(1);
        assertThat(auditEventRepository.count()).isEqualTo(1);
    }

    @Test
    void adminCanReadImmutableAuditTrail() throws Exception {
        UserAccount admin = saveUser("Admin", "audit-admin@example.com", UserRole.ADMIN);
        UserAccount analyst = saveUser("Analyst", "audited@example.com", UserRole.ANALYST);

        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + jwtService.generateToken(analyst).token())
                        .header("Idempotency-Key", "audited-create-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/audit-events")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin).token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].action").value("RECORD_CREATED"))
                .andExpect(jsonPath("$.content[0].actorId").value(analyst.getId()));
    }

    @Test
    void adminUserListingNeverExposesPasswordHashes() throws Exception {
        UserAccount admin = saveUser("Admin", "admin@example.com", UserRole.ADMIN);
        saveUser("Analyst", "analyst@example.com", UserRole.ANALYST);
        String token = jwtService.generateToken(admin).token();

        String body = mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("password");
        assertThat(body).doesNotContain("$2a$");
    }

    @Test
    void protectedEndpointRejectsMissingOrInvalidToken() throws Exception {
        mockMvc.perform(get("/api/records"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/records")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void repeatedPasswordFailuresTemporarilyLockAccount() throws Exception {
        saveUser("Target", "lockout@example.com", UserRole.VIEWER);
        String invalidLogin = """
                {
                  "email": "lockout@example.com",
                  "password": "definitely-wrong"
                }
                """;

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidLogin))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "lockout@example.com",
                                  "password": "Secure123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        UserAccount locked = userRepository
                .findByEmailIgnoreCase("lockout@example.com")
                .orElseThrow();
        assertThat(locked.getLockedUntil()).isNotNull();
    }

    private UserAccount saveUser(String name, String email, UserRole role) {
        UserAccount user = new UserAccount();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Secure123"));
        user.setRole(role);
        user.setActive(true);
        return userRepository.save(user);
    }

    private String recordJson() {
        return """
                {
                  "amount": 2500.00,
                  "type": "EXPENSE",
                  "category": "Engineering",
                  "date": "%s",
                  "note": "Cloud tools"
                }
                """.formatted(LocalDate.now());
    }
}
