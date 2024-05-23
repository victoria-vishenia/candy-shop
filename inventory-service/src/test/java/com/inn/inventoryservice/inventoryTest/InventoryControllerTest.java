package com.inn.inventoryservice.inventoryTest;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.inn.inventoryservice.Utils.InventoryMapper;
import com.inn.inventoryservice.controller.InventoryController;
import com.inn.inventoryservice.repository.InventoryRepository;
import com.inn.inventoryservice.service.InventoryService;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static com.inn.inventoryservice.inventoryTest.InventoryTestData.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {"spring.cloud.config.uri=http://localhost:8888"})
@ActiveProfiles("test")
class InventoryControllerTest {
    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    String clientId;
    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
   String clientSecret;
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private InventoryService inventoryServiceMock;
    @MockBean
    private InventoryRepository inventoryRepository;
    ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    InventoryMapper inventoryMapper;
    @InjectMocks
    private InventoryController inventoryControllerMock;
    @LocalServerPort
    private Integer port;

    @Container
    protected static KeycloakContainer KEYCLOAK_CONTAINER = new KeycloakContainer
            ("quay.io/keycloak/keycloak:23.0.7")
            .withRealmImportFiles("/candy-shop-realm-realm.json");

    @DynamicPropertySource
    static void registerResourceServerIssuerProperty(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> KEYCLOAK_CONTAINER.getAuthServerUrl() +"/realms/candy-shop-realm");
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> POSTGRE_SQL_CONTAINER =
            new PostgreSQLContainer<>("postgres:16.0")
                    .withDatabaseName("candy_shop")
                    .withUsername("productusername")
                    .withPassword("productpassword")
                    .withInitScript("inventory_test.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRE_SQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRE_SQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRE_SQL_CONTAINER::getPassword);
    }
    @BeforeAll
    static void beforeAll() {
        POSTGRE_SQL_CONTAINER.start();
        KEYCLOAK_CONTAINER.start();
    }

    @AfterAll
    static void afterAll() {
        POSTGRE_SQL_CONTAINER.stop();
        KEYCLOAK_CONTAINER.stop();
    }

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost:" + port;
        inventoryRepository.deleteAll();
    }


    public String getToken(String username, String password) {

        return given().contentType("application/x-www-form-urlencoded")
                .formParams(Map.of(
                        "username", username,
                        "password", password,
                        "grant_type", "password",
                        "client_id", clientId,
                        "client_secret", clientSecret))
                .post(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/candy-shop-realm/protocol/openid-connect/token")
                .then()
                .extract().path("access_token");
    }

    @Test
    @DisplayName("Keycloak container test")
    public void testKc() throws Exception {
        assertTrue(KEYCLOAK_CONTAINER.isCreated());
        assertTrue(KEYCLOAK_CONTAINER.isRunning());
    }

    @Test
    @DisplayName("PostgresQl container test")
    public void testPGConnection() {
        System.out.println(POSTGRE_SQL_CONTAINER.getJdbcUrl());
        assertTrue(POSTGRE_SQL_CONTAINER.isCreated());
        assertTrue(POSTGRE_SQL_CONTAINER.isRunning());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Get all inventory by admin- isOk")
    public void testGetAllInventorySuccess() throws Exception {
        String accessToken = getToken("viktoria.vi", "password");
        mockMvc.perform(get("/inventory/all")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "princ.di", password = "password2")
    @DisplayName("Get all inventory by user- forbidden")
    public void testGetAllInventoryForbidden() throws Exception {
        String accessToken = getToken("princ.di", "password2");
        mockMvc.perform(get("/inventory/all")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "viktoria.vi", roles = "ADMIN")
    @DisplayName("Get inventory by admin")
    public void testGetByInvCodeSuccess() throws Exception {
        String invCode = "code4";
        when(inventoryServiceMock.getByInvCode(invCode)).thenReturn(inventoryDto);
        when(inventoryRepository.findInventoryByInvCode(invCode)).thenReturn(existingInventory);
        when(inventoryMapper.convertToInventory(inventoryDto)).thenReturn(existingInventory);
        when(inventoryMapper.convertToDto(existingInventory)).thenReturn(inventoryDto);

        String accessToken = getToken("viktoria.vi", "password");
        mockMvc.perform(get("/inventory/find/" + invCode)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.invCode").value("code4"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.price").value(2.33));
    }

    @Test
    @WithMockUser(username = "princ.di", password="password2")
    @DisplayName("Get inventory by user")
    public void testGetByInvCodeForbidden() throws Exception {
        String invCode = "code4";
        when(inventoryServiceMock.getByInvCode(invCode)).thenReturn(inventoryDto);
        when(inventoryRepository.findInventoryByInvCode(invCode)).thenReturn(existingInventory);
        when(inventoryMapper.convertToInventory(inventoryDto)).thenReturn(existingInventory);
        when(inventoryMapper.convertToDto(existingInventory)).thenReturn(inventoryDto);

        String accessToken = getToken("princ.di", "password2");
        mockMvc.perform(get("/inventory/find/" + invCode)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(username = "viktoria.vi", roles = "ADMIN")
    @DisplayName("Creation inventory by admin")
    public void testCreateNewInventorySuccess() throws Exception {
        String accessToken = getToken("viktoria.vi", "password");
        mockMvc.perform(post("/inventory/create")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(newInventoryDto))))
                .andExpect(status().isCreated())
                .andExpect(content().string("Inventories created successfully"));
    }

    @Test
    @WithMockUser(username = "princ.di", password = "password2")
    @DisplayName("Creation inventory by user")
    public void testCreateOrderForbidden() throws Exception {

        String accessToken = getToken("princ.di", "password2");

        mockMvc.perform(post("/inventory/create")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newInventoryDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "viktoria.vi")
    @DisplayName("Delete inventory by admin")
    public void testDeleteInventorySuccess() throws Exception {
        String invCode = "code4";
        when(inventoryServiceMock.getByInvCode(invCode)).thenReturn(inventoryDto);
        String accessToken = getToken("viktoria.vi", "password");
        mockMvc.perform(delete("/inventory/delete")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(inventoryDto))))
                .andExpect(status().isOk())
                .andExpect(content().string("Inventories deleted successfully"));
    }

    @Test
    @WithMockUser(username = "princ.di", password = "password2")
    @DisplayName("Delete inventory by user")
    public void testDeleteInventoryForbidden() throws Exception {
        String invCode = "code4";
        String accessToken = getToken("princ.di", "password2");

        when(inventoryServiceMock.getByInvCode(invCode)).thenReturn(inventoryDto);
        mockMvc.perform(delete("/inventory/delete/" + invCode)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "viktoria.vi", roles = "ADMIN")
    @DisplayName("Update inventory by admin")
    public void testUpdateInventorySuccess() throws Exception {
        String invCode = "code4";
        when(inventoryServiceMock.getByInvCode(invCode))
                .thenReturn(inventoryDto);
        when(inventoryRepository.findInventoryByInvCode(invCode)).thenReturn(existingInventory);

        String accessToken = getToken("viktoria.vi", "password");
        mockMvc.perform(patch("/inventory/update")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(newInventoryDto))))
                .andExpect(status().isCreated())
                .andExpect(content().string("Inventory updated successfully"));
    }

    @Test
    @WithMockUser(username = "yan.be", password = "password1")
    @DisplayName("Update inventory by user")
    public void testUpdateInventoryForbidden() throws Exception {
        String invCode = "code4";
        when(inventoryServiceMock.getByInvCode(invCode))
                .thenReturn(inventoryDto);
        when(inventoryRepository.findInventoryByInvCode(invCode)).thenReturn(existingInventory);

        String accessToken = getToken("yan.be", "password1");
        mockMvc.perform(patch("/inventory/update")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(newInventoryDto))))
                .andExpect(status().isForbidden());
    }
}