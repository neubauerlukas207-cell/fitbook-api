package cz.fitbook.api.forecast;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import cz.fitbook.api.forecast.InsightsClient.InsightsPrediction;
import cz.fitbook.api.forecast.InsightsClient.InsightsRequest;
import cz.fitbook.api.lesson.Lesson;
import cz.fitbook.api.lesson.LessonService;
import cz.fitbook.api.reservation.ReservationRepository;
import cz.fitbook.api.reservation.ReservationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Předpovědi")
class ForecastController {

	record Forecast(Long lessonId, long currentBookings, int capacity, double predictedOccupancy,
			int predictedBookings, String level) {
	}

	private final LessonService lessons;
	private final ReservationRepository reservations;
	private final InsightsClient insights;

	ForecastController(LessonService lessons, ReservationRepository reservations, InsightsClient insights) {
		this.lessons = lessons;
		this.reservations = reservations;
		this.insights = insights;
	}

	@GetMapping("/api/lessons/{id}/forecast")
	@Operation(summary = "Předpověď obsazenosti lekce",
			description = "Zavolá ML službu FitBook Insights (Python). 503, když služba není dostupná.")
	Forecast forecast(@PathVariable Long id) {
		Lesson lesson = lessons.find(id);
		var start = lesson.getStartsAt();
		InsightsPrediction p = insights.predict(new InsightsRequest(
				lesson.getType().insightsName(),
				start.getDayOfWeek().getValue() - 1, // Insights: 0 = pondělí
				start.getHour(),
				start.getMonthValue(),
				lesson.capacity(),
				lesson.getInstructor().getRating().doubleValue(),
				false));
		long booked = reservations.countByLessonIdAndStatus(id, ReservationStatus.ACTIVE);
		return new Forecast(id, booked, lesson.capacity(), p.occupancy(), p.expectedBookings(), p.level());
	}
}
