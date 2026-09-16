package com.msbatchproducer.msbatchproducer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"app.outbox.enabled=false", "spring.kafka.admin.auto-create=false"})
class MsBatchProducerApplicationTests {

    @Test
    void contextLoads() {
    }

}
