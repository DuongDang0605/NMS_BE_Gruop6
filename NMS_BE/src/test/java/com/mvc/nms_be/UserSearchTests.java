package com.mvc.nms_be;

import com.mvc.nms_be.entity.SystemAccount;
import com.mvc.nms_be.repository.SystemAccountRepository;
import com.mvc.nms_be.security.CustomUserDetails;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.migrate-plaintext-passwords=false", "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
@Transactional
class UserSearchTests {
    @Autowired WebApplicationContext context;
    @Autowired SystemAccountRepository accounts;
    private MockMvc mvc;
    private String marker;

    @BeforeEach
    void setup() {
        marker = "search" + UUID.randomUUID().toString().replace("-", "");
        var actor = new SystemAccount();
        actor.setAccountId(-1);
        actor.setAccountRole(1);
        var principal = new CustomUserDetails(actor);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity())
                .defaultRequest(get("/").with(user(principal))).build();
    }

    @Test
    void searchRequiresAnAdmin() throws Exception {
        mvc.perform(get("/api/v1/users").with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous()))
                .andExpect(status().isUnauthorized());
        var staff = new SystemAccount();
        staff.setAccountId(-2);
        staff.setAccountRole(2);
        mvc.perform(get("/api/v1/users").with(user(new CustomUserDetails(staff))))
                .andExpect(status().isForbidden());
    }

    @Test
    void pageableFiltersBeforeCountingAndSortingWithoutExposingPasswords() throws Exception {
        account("Zulu", "zulu", 2);
        account("Alpha", "alpha", 2);
        account("Bravo", "bravo", 1);
        mvc.perform(get("/api/v1/users").param("keyword", marker.toUpperCase())
                        .param("role", "2").param("page", "1").param("size", "1")
                        .param("sort", "accountName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].accountName").value("Zulu"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.content[*].accountPassword").isEmpty());
    }

    @Test
    void combinesIdNameEmailAndRoleAndReturnsEmptyPages() throws Exception {
        var match = account("Nguyễn Văn An", "an", 1);
        mvc.perform(get("/api/v1/users").param("accountId", match.getAccountId().toString())
                        .param("name", "  NGUYỄN  ").param("email", marker.toUpperCase()).param("role", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].accountId").value(match.getAccountId()));
        mvc.perform(get("/api/v1/users").param("keyword", marker).param("role", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/users").param("keyword", marker).param("page", "99"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void wildcardCharactersAreSearchedLiterally() throws Exception {
        account("100%_![special", "literal", 2);
        account("100other", "other", 2);
        for (String literal : new String[]{"%", "_", "[", "!", "%_!["}) {
            mvc.perform(get("/api/v1/users").param("email", marker).param("name", literal))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].accountName").value("100%_![special"));
        }
    }

    @Test
    void stableIdTieBreakerAndMultipleSortsWork() throws Exception {
        var first = account("Same", "first", 2);
        var second = account("Same", "second", 1);
        mvc.perform(get("/api/v1/users").param("keyword", marker).param("sort", "accountName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].accountId").value(first.getAccountId()))
                .andExpect(jsonPath("$.content[1].accountId").value(second.getAccountId()));
        mvc.perform(get("/api/v1/users").param("keyword", marker)
                        .param("sort", "accountRole,asc", "accountId,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].accountRole").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "page=abc", "size=0", "size=101", "role=3", "accountId=0",
            "sort=accountPassword,asc", "sort=missing,desc", "sort=accountId,invalid"})
    void invalidQueryReturns400(String parameter) throws Exception {
        var pair = parameter.split("=", 2);
        mvc.perform(get("/api/v1/users").param(pair[0], pair[1]))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    private SystemAccount account(String name, String localPart, int role) {
        var account = new SystemAccount();
        account.setAccountName(name);
        account.setAccountEmail(localPart + "@" + marker + ".example.com");
        account.setAccountRole(role);
        account.setAccountPassword("test-hash-not-returned");
        return accounts.saveAndFlush(account);
    }
}
