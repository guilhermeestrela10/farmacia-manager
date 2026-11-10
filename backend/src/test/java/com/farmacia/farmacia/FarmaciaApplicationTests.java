package com.farmacia.farmacia;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "farmacia.test.mongo-uri", matches = "mongodb://127\\.0\\.0\\.1:55433/\\?replicaSet=farmacia-teste")
class FarmaciaApplicationTests {

	@Test
	void contextLoads() {
	}

}
