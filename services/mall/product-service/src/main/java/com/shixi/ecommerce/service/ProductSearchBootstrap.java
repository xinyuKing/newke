package com.shixi.ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ProductSearchBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ProductSearchBootstrap.class);

    private final ProductIndexService productIndexService;

    public ProductSearchBootstrap(ProductIndexService productIndexService) {
        this.productIndexService = productIndexService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            productIndexService.initializeIndexIfNeeded();
        } catch (Exception ex) {
            log.warn("Initialize product search index failed", ex);
        }
    }
}
