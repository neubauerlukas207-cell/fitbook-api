package cz.fitbook.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FitbookApiApplicationTests {

	@Test
	void contextLoads() {
		// ověří, že se aplikace spustí, Flyway migrace projdou a JPA entity odpovídají schématu
	}
}
