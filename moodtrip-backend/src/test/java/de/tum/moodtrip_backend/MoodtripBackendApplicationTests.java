package de.tum.moodtrip_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.liquibase.enabled=false"})
class MoodtripBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
