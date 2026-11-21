package cz.fitbook.api.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TimeConfig {

	/** Čas se bere přes Clock – v testech ho jde nahradit pevným časem. */
	@Bean
	Clock clock(FitbookProperties properties) {
		return Clock.system(ZoneId.of(properties.timezone()));
	}
}
