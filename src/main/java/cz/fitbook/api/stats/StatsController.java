package cz.fitbook.api.stats;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.fitbook.api.lesson.LessonDtos.LessonResponse;
import cz.fitbook.api.lesson.LessonService;
import cz.fitbook.api.lesson.LessonType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/stats")
@Tag(name = "Statistiky")
class StatsController {

	record TypeOccupancy(LessonType type, int lessons, long capacity, long booked, double occupancy) {
	}

	private final LessonService lessons;
	private final Clock clock;

	StatsController(LessonService lessons, Clock clock) {
		this.lessons = lessons;
		this.clock = clock;
	}

	@GetMapping("/occupancy")
	@Operation(summary = "Obsazenost podle typu lekce", description = "Výchozí interval: posledních 30 a příštích 30 dní")
	List<TypeOccupancy> occupancy(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		LocalDate today = LocalDate.now(clock);
		LocalDate start = from != null ? from : today.minusDays(30);
		LocalDate end = to != null ? to : today.plusDays(30);

		Map<LessonType, long[]> totals = new TreeMap<>(); // [počet lekcí, kapacita, rezervace]
		for (LessonResponse l : lessons.schedule(start.atStartOfDay(), end.plusDays(1).atStartOfDay(), null)) {
			long[] t = totals.computeIfAbsent(l.type(), k -> new long[3]);
			t[0]++;
			t[1] += l.capacity();
			t[2] += l.booked();
		}
		return totals.entrySet().stream()
				.map(e -> new TypeOccupancy(e.getKey(), (int) e.getValue()[0], e.getValue()[1], e.getValue()[2],
						Math.round(1000.0 * e.getValue()[2] / e.getValue()[1]) / 1000.0))
				.sorted(Comparator.comparingDouble(TypeOccupancy::occupancy).reversed())
				.toList();
	}
}
