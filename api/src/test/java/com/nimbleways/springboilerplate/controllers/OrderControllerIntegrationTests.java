package com.nimbleways.springboilerplate.controllers;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import com.nimbleways.springboilerplate.utils.StaticLogbackAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "logging.level.com.nimbleways.springboilerplate=INFO")
class OrderControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void clearLogs() {
        StaticLogbackAppender.clearEvents();
    }

    @Test
    void processOrder_withValidOrder_returnsOk() throws Exception {
        List<Product> products = List.of(
            new Product(null, 15, 30, ProductType.NORMAL, "USB Cable", null, null, null),
            new Product(null, 10, 0, ProductType.NORMAL, "USB Dongle", null, null, null),
            new Product(null, 15, 30, ProductType.EXPIRABLE, "Butter", LocalDate.now().plusDays(26), null, null),
            new Product(null, 90, 6, ProductType.EXPIRABLE, "Milk", LocalDate.now().minusDays(2), null, null),
            new Product(null, 15, 30, ProductType.SEASONAL, "Watermelon", null, LocalDate.now().minusDays(2), LocalDate.now().plusDays(58)),
            new Product(null, 15, 30, ProductType.SEASONAL, "Grapes", null, LocalDate.now().plusDays(180), LocalDate.now().plusDays(240))
        );
        productRepository.saveAll(products);
        Order order = orderRepository.save(buildOrder(Set.copyOf(products)));

        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId())
                .contentType("application/json"))
                .andExpect(status().isOk());
    }

    @Test
    void processOrder_normalProductWithStock_decrementsAvailable() throws Exception {
        Product product = new Product(null, 15, 10, ProductType.NORMAL, "USB Cable", null, null, null);
        productRepository.save(product);
        Order order = orderRepository.save(buildOrder(Set.of(product)));

        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId())
                .contentType("application/json"))
                .andExpect(status().isOk());

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(9, updated.getAvailable());
    }

    @Test
    void processOrder_expirableExpiredProduct_setsAvailableToZero() throws Exception {
        Product product = new Product(null, 90, 6, ProductType.EXPIRABLE, "Milk", LocalDate.now().minusDays(2), null, null);
        productRepository.save(product);
        Order order = orderRepository.save(buildOrder(Set.of(product)));

        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId())
                .contentType("application/json"))
                .andExpect(status().isOk());

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(0, updated.getAvailable());
    }

    @Test
    void processOrder_seasonalProductInSeason_decrementsAvailable() throws Exception {
        Product product = new Product(null, 15, 30, ProductType.SEASONAL, "Watermelon",
                null, LocalDate.now().minusDays(2), LocalDate.now().plusDays(58));
        productRepository.save(product);
        Order order = orderRepository.save(buildOrder(Set.of(product)));

        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId())
                .contentType("application/json"))
                .andExpect(status().isOk());

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(29, updated.getAvailable());
    }

    @Test
    void processOrder_withUnknownOrderId_returnsNotFound() throws Exception {
        mockMvc.perform(post("/orders/{orderId}/processOrder", 999999L)
                .contentType("application/json"))
                .andExpect(status().isNotFound());

        boolean warnLogged = StaticLogbackAppender.getEvents().stream()
                .anyMatch(e -> e.getFormattedMessage().contains("Order not found"));
        assertTrue(warnLogged, "Expected a WARN log for missing order");
    }

    @Test
    void processOrder_withNegativeOrderId_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/orders/{orderId}/processOrder", -1L)
                .contentType("application/json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processOrder_logsSuccessMessage() throws Exception {
        Product product = new Product(null, 15, 10, ProductType.NORMAL, "Cable", null, null, null);
        productRepository.save(product);
        Order order = orderRepository.save(buildOrder(Set.of(product)));

        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId())
                .contentType("application/json"))
                .andExpect(status().isOk());

        boolean successLogged = StaticLogbackAppender.getEvents().stream()
                .anyMatch(e -> e.getFormattedMessage().contains("processed successfully"));
        assertTrue(successLogged, "Expected an INFO log for successful processing");
    }

    private static Order buildOrder(Set<Product> products) {
        Order order = new Order();
        order.setItems(products);

        return order;
    }
}
