package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.dto.product.ProcessOrderResponse;
import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.exceptions.OrderNotFoundException;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.services.ProductService;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@UnitTest
class OrderServiceImplUnitTests {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderServiceImpl orderServiceImpl;

    @Test
    void processOrder_withValidOrder_returnsResponseAndProcessesAllProducts() {
        Product p1 = new Product(null, 15, 10, ProductType.NORMAL, "USB Cable", null, null, null);
        Product p2 = new Product(null, 10, 5, ProductType.EXPIRABLE, "Butter", LocalDate.now().plusDays(10), null, null);
        Order order = new Order(1L, Set.of(p1, p2));

        Mockito.when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        ProcessOrderResponse response = orderServiceImpl.processOrder(1L);

        assertEquals(1L, response.id());
        Mockito.verify(productService).processProduct(p1);
        Mockito.verify(productService).processProduct(p2);
    }

    @Test
    void processOrder_withUnknownOrderId_throwsOrderNotFoundException() {
        Mockito.when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderServiceImpl.processOrder(999L));
        Mockito.verifyNoInteractions(productService);
    }
}
