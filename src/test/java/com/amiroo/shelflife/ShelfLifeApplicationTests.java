package com.amiroo.shelflife;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "SHELFLIFE_POSTGRES_TESTS", matches = "true")
class ShelfLifeApplicationTests {

    @Test
    void contextLoads() {
    }
}
