package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@UnitTest
class ProductServiceUnitTests {

    @Mock
    private NotificationService notificationService;
    @Mock
    private ProductRepository productRepository;
    @InjectMocks
    private ProductServiceImpl productService;

    // ── NORMAL ────────────────────────────────────────────────────────────────

    @Test
    void normalProduct_withStock_decrementsAvailable() {
        Product product = new Product(null, 15, 10, ProductType.NORMAL, "USB Cable", null, null, null);
        Mockito.when(productRepository.save(product)).thenReturn(product);

        productService.processProduct(product);

        assertEquals(9, product.getAvailable());
        Mockito.verify(productRepository).save(product);
        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    void normalProduct_outOfStock_withLeadTime_notifiesDelay() {
        Product product = new Product(null, 15, 0, ProductType.NORMAL, "RJ45 Cable", null, null, null);
        Mockito.when(productRepository.save(product)).thenReturn(product);

        productService.processProduct(product);

        assertEquals(15, product.getLeadTime());
        Mockito.verify(productRepository).save(product);
        Mockito.verify(notificationService).sendDelayNotification(15, "RJ45 Cable");
    }

    @Test
    void normalProduct_outOfStock_withoutLeadTime_doesNothing() {
        Product product = new Product(null, 0, 0, ProductType.NORMAL, "Discontinued Item", null, null, null);

        productService.processProduct(product);

        assertEquals(0, product.getAvailable());
        Mockito.verifyNoInteractions(productRepository);
        Mockito.verifyNoInteractions(notificationService);
    }

    // ── SEASONAL ──────────────────────────────────────────────────────────────

    @Test
    void seasonalProduct_inSeason_withStock_decrementsAvailable() {
        Product product = new Product(null, 5, 10, ProductType.SEASONAL, "Watermelon",
            null, LocalDate.now().minusDays(10), LocalDate.now().plusDays(50));
        Mockito.when(productRepository.save(product)).thenReturn(product);

        productService.processProduct(product);

        assertEquals(9, product.getAvailable());
        Mockito.verify(productRepository).save(product);
        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    void seasonalProduct_leadTimeExceedsSeason_notifiesOutOfStock() {
        Product product = new Product(null, 60, 0, ProductType.SEASONAL, "Strawberry",
            null, LocalDate.now().minusDays(5), LocalDate.now().plusDays(30));
        Mockito.when(productRepository.save(product)).thenReturn(product);

        productService.processProduct(product);

        assertEquals(0, product.getAvailable());
        Mockito.verify(notificationService).sendOutOfStockNotification("Strawberry");
        Mockito.verify(productRepository).save(product);
    }

    @Test
    void seasonalProduct_beforeSeason_notifiesOutOfStock() {
        Product product = new Product(null, 5, 0, ProductType.SEASONAL, "Grapes",
            null, LocalDate.now().plusDays(30), LocalDate.now().plusDays(90));
        Mockito.when(productRepository.save(product)).thenReturn(product);

        productService.processProduct(product);

        Mockito.verify(notificationService).sendOutOfStockNotification("Grapes");
        Mockito.verify(productRepository).save(product);
    }

    @Test
    void seasonalProduct_inSeason_outOfStock_notifiesDelay() {
        Product product = new Product(null, 5, 0, ProductType.SEASONAL, "Apricot",
            null, LocalDate.now().minusDays(10), LocalDate.now().plusDays(50));
        Mockito.when(productRepository.save(product)).thenReturn(product);

        productService.processProduct(product);

        Mockito.verify(notificationService).sendDelayNotification(5, "Apricot");
        Mockito.verify(productRepository).save(product);
    }

    // ── EXPIRABLE ─────────────────────────────────────────────────────────────

    @Test
    void expirableProduct_notExpired_withStock_decrementsAvailable() {
        Product product = new Product(null, 10, 5, ProductType.EXPIRABLE, "Butter",
            LocalDate.now().plusDays(30), null, null);
        Mockito.when(productRepository.save(product)).thenReturn(product);

        productService.processProduct(product);

        assertEquals(4, product.getAvailable());
        Mockito.verify(productRepository).save(product);
        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    void expirableProduct_expired_notifiesExpiration() {
        LocalDate expiryDate = LocalDate.now().minusDays(1);
        Product product = new Product(null, 10, 3, ProductType.EXPIRABLE, "Milk",
            expiryDate, null, null);
        Mockito.when(productRepository.save(product)).thenReturn(product);

        productService.processProduct(product);

        assertEquals(0, product.getAvailable());
        Mockito.verify(notificationService).sendExpirationNotification("Milk", expiryDate);
        Mockito.verify(productRepository).save(product);
    }
}
