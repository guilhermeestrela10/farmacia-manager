package com.farmacia.farmacia;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "farmacia.test.url", matches = "jdbc:postgresql://127\\.0\\.0\\.1:55432/postgres")
class FarmaciaApplicationTests {

	@Test
	void contextLoads() {
	}

}
