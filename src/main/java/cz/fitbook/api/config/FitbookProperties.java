package cz.fitbook.api.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Nastavení aplikace z application.properties (prefix {@code fitbook.}). */
@ConfigurationProperties("fitbook")
public record FitbookProperties(String timezone, boolean demoData, Duration cancellationDeadline, Insights insights) {

	public record Insights(String url) {

		public boolean enabled() {
			return url != null && !url.isBlank();
		}
	}
}
