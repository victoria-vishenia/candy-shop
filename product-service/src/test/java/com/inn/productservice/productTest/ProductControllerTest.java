package com.inn.productservice.productTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inn.productservice.Utils.ProductMapper;
import com.inn.productservice.controller.ProductController;
import com.inn.productservice.model.Product;
import com.inn.productservice.repository.ProductRepository;
import com.inn.productservice.service.ProductService;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
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

import static com.inn.productservice.productTest.ProductTestData.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@AutoConfigureMockMvc()
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {"spring.cloud.config.uri=http://localhost:8888"})
@ActiveProfiles("test")
class ProductControllerTest {
    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
     String clientId;
    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    String clientSecret;
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ProductService productServiceMock;
    @MockBean
    private ProductRepository productRepository;
    @Mock
    private ProductMapper mapper;
    @InjectMocks
    private ProductController productControllerMock;
    @LocalServerPort
    private Integer port;
    ObjectMapper objectMapper=new ObjectMapper();
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
                    .withInitScript("product_test.sql");

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
        productRepository.deleteAll();
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
    @DisplayName("Get all products - isOk")
    public void testGetAllProductsSuccess() throws Exception {

        given().when().get("http://localhost:8083/product/all").then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", isA(List.class));
    }

    @Test
    @DisplayName("Get product by name")
    @WithMockUser
    public void testGetByInvCodeSuccess() throws Exception {

        String invCode = "Product1";
        when(productServiceMock.getByName(invCode)).thenReturn(productDto1);
        when(productRepository.findByName(invCode)).thenReturn(product1);
        when(mapper.mapToProduct(productDto1)).thenReturn(product1);
        when(mapper.mapToProductDto(product1)).thenReturn(productDto1);
        Product product = productRepository.findByName(invCode);

        mockMvc.perform(get("http://localhost:8083/product/candy-name/" + invCode))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Product1"))
                .andExpect(jsonPath("$.compound").value("Choco, nuts"))
                .andExpect(jsonPath("$.price").value(10.99F));
    }


    @Test
    @WithMockUser(username = "viktoria.vi", roles = "ADMIN")
    @DisplayName("Creation product by admin")
    public void testCreateNewProductSuccess() throws Exception {

        String accessToken = getToken("viktoria.vi","password");

        mockMvc.perform(post("/product/create")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(testProduct))))
                .andExpect(status().isCreated())
                .andExpect(content().string("Products created successfully"));
    }

    @Test
    @WithMockUser(username = "princ.di", password = "password2")
    @DisplayName("Creation product by user")
    public void testCreateProductForbidden() throws Exception {

        String accessToken = getToken("princ.di","password2");

        mockMvc.perform(post("/product/create")
                       .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(testProduct))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "viktoria.vi", roles = "ADMIN")
    @DisplayName("Delete product by admin")
    public void testDeleteProductSuccess() throws Exception {
        String invCode = "Product1";
        when(productServiceMock.getByName(invCode)).thenReturn(productDto1);
        when(mapper.convertToInventoryDto(productDto1)).thenReturn(inventoryDto1);

        String accessToken = getToken("viktoria.vi", "password");

        mockMvc.perform(delete("/product/delete")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(inventoryDto1))))
                .andExpect(status().isOk())
                .andExpect(content().string("Products deleted successfully"));
    }

    @Test
    @WithMockUser(username = "princ.di", password = "password2")
    @DisplayName("Delete product by user")
    public void testDeleteProductForbidden() throws Exception {
        String invCode = "Product1";
        String accessToken = getToken("princ.di", "password2");

        when(productServiceMock.getByName(invCode)).thenReturn(productDto1);
        when(productRepository.findByName(invCode)).thenReturn(product1);
        when(mapper.mapToProductDto(product1)).thenReturn(productDto1);
        when(mapper.mapToProduct(productDto1)).thenReturn(product1);

        mockMvc.perform(delete("/product/delete")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(productDto1))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "viktoria.vi", roles = "ADMIN")
    @DisplayName("Update product by admin")
    public void testUpdateProductSuccess() throws Exception {
        String invCode = "Product1";
        when(productServiceMock.getByName(invCode))
                .thenReturn(productDto1);
        when(productRepository.findByName(invCode)).thenReturn(product1);

        String accessToken = getToken("viktoria.vi", "password");

        mockMvc.perform(patch("/product/update")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(updatedProductDto1))))
                .andExpect(status().isOk())
                .andExpect(content().string("Products updated successfully"));
    }

    @Test
    @WithMockUser(username = "yan.be", password="password1")
    @DisplayName("Update product by user")
    public void testUpdateProductForbidden() throws Exception {
        String invCode = "Product1";
        when(productServiceMock.getByName(invCode))
                .thenReturn(productDto1);
        when(productRepository.findByName(invCode)).thenReturn(product1);

        String accessToken = getToken("yan.be", "password1");

        mockMvc.perform(patch("/product/update")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(updatedProductDto1))))
                .andExpect(status().isForbidden());
    }
}

