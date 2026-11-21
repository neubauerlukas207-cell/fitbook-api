package cz.fitbook.api.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LessonTest {

	private static final LocalDateTime BASE = LocalDateTime.of(2026, 12, 1, 0, 0);

	// lekce 18:00–19:00
	private final Lesson lesson = new Lesson(LessonType.SPINNING, BASE.withHour(18), 60, new Room("Sál", 20),
			new Instructor("Test", BigDecimal.valueOf(4.5)));

	@ParameterizedTest(name = "{0}–{1} překryv = {2}")
	@CsvSource({
			"17:00, 18:00, false", // končí přesně když lekce začíná
			"19:00, 20:00, false", // začíná přesně když lekce končí
			"17:30, 18:30, true",
			"18:30, 19:30, true",
			"18:15, 18:45, true",  // celé uvnitř
			"17:00, 20:00, true",  // přes celou lekci
	})
	void overlaps(String from, String to, boolean expected) {
		var start = BASE.with(java.time.LocalTime.parse(from));
		var end = BASE.with(java.time.LocalTime.parse(to));
		assertThat(lesson.overlaps(start, end)).isEqualTo(expected);
	}
}
