package com.nimbleways.springboilerplate.services.implementations;

import java.time.LocalDate;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    @Override
    public void processProduct(Product product) {
        log.debug("Processing {} product: {}", product.getType(), product.getName());
        switch (product.getType()) {
            case NORMAL -> processNormalProduct(product);
            case SEASONAL -> processSeasonalProduct(product);
            case EXPIRABLE -> processExpirableProduct(product);
        }
    }

    public void notifyDelay(int leadTime, Product product) {
        log.info("Notifying delay of {} days for product '{}'", leadTime, product.getName());
        product.setLeadTime(leadTime);
        productRepository.save(product);
        notificationService.sendDelayNotification(leadTime, product.getName());
    }

    private void processNormalProduct(Product product) {
        if (product.getAvailable() > 0) {
            decrementAndSave(product);
        } else if (product.getLeadTime() > 0) {
            notifyDelay(product.getLeadTime(), product);
        } else {
            log.warn("Normal product '{}' is out of stock with no lead time", product.getName());
        }
    }

    private void processSeasonalProduct(Product product) {
        LocalDate today = LocalDate.now();
        boolean inSeason = today.isAfter(product.getSeasonStartDate())
            && today.isBefore(product.getSeasonEndDate());
        if (inSeason && product.getAvailable() > 0) {
            decrementAndSave(product);
        } else if (today.plusDays(product.getLeadTime()).isAfter(product.getSeasonEndDate())) {
            log.info("Seasonal product '{}' lead time exceeds season end — marking out of stock", product.getName());
            notificationService.sendOutOfStockNotification(product.getName());
            product.setAvailable(0);
            productRepository.save(product);
        } else if (product.getSeasonStartDate().isAfter(today)) {
            log.info("Seasonal product '{}' is not yet in season — notifying out of stock", product.getName());
            notificationService.sendOutOfStockNotification(product.getName());
            productRepository.save(product);
        } else {
            notifyDelay(product.getLeadTime(), product);
        }
    }

    private void processExpirableProduct(Product product) {
        if (product.getAvailable() > 0 && product.getExpiryDate().isAfter(LocalDate.now())) {
            decrementAndSave(product);
        } else {
            log.info("Expirable product '{}' has expired (expiry: {}) — marking out of stock", product.getName(), product.getExpiryDate());
            notificationService.sendExpirationNotification(product.getName(), product.getExpiryDate());
            product.setAvailable(0);
            productRepository.save(product);
        }
    }

    private void decrementAndSave(Product product) {
        log.debug("Decrementing stock for product '{}': {} -> {}", product.getName(), product.getAvailable(), product.getAvailable() - 1);
        product.setAvailable(product.getAvailable() - 1);
        productRepository.save(product);
    }
}
