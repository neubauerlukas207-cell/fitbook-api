package cz.fitbook.api.forecast;

import java.time.Duration;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonProperty;

import cz.fitbook.api.config.FitbookProperties;

/** HTTP klient pro Python službu FitBook Insights (POST /predict). */
@Component
public class InsightsClient {

	public record InsightsRequest(
			@JsonProperty("lesson_type") String lessonType,
			int weekday,
			int hour,
			int month,
			int capacity,
			@JsonProperty("instructor_rating") double instructorRating,
			@JsonProperty("is_holiday") boolean holiday) {
	}

	public record InsightsPrediction(
			double occupancy,
			@JsonProperty("expected_bookings") int expectedBookings,
			int capacity,
			String level) {
	}

	private final RestClient rest;
	private final boolean enabled;

	InsightsClient(FitbookProperties properties) {
		this.enabled = properties.insights() != null && properties.insights().enabled();
		var factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(2));
		factory.setReadTimeout(Duration.ofSeconds(3));
		this.rest = RestClient.builder()
				.baseUrl(enabled ? properties.insights().url() : "http://localhost")
				.requestFactory(factory)
				.build();
	}

	public InsightsPrediction predict(InsightsRequest request) {
		if (!enabled) {
			throw new ForecastUnavailableException("Služba FitBook Insights není nastavená (fitbook.insights.url)");
		}
		try {
			return rest.post().uri("/predict").body(request).retrieve().body(InsightsPrediction.class);
		}
		catch (RestClientException e) {
			throw new ForecastUnavailableException("Služba FitBook Insights neodpovídá");
		}
	}
}
