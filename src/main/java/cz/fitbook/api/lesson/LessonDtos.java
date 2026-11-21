package cz.fitbook.api.lesson;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class LessonDtos {

	private LessonDtos() {
	}

	public record CreateLessonRequest(
			@NotNull(message = "Chybí typ lekce") LessonType type,
			@NotNull(message = "Chybí začátek lekce") LocalDateTime startsAt,
			@Min(value = 15, message = "Lekce musí trvat aspoň 15 minut")
			@Max(value = 180, message = "Lekce může trvat nejvýš 180 minut") int durationMinutes,
			@NotNull(message = "Chybí sál") Long roomId,
			@NotNull(message = "Chybí instruktor") Long instructorId) {
	}

	public record LessonResponse(Long id, LessonType type, LocalDateTime startsAt, LocalDateTime endsAt,
			int durationMinutes, String room, String instructor, int capacity, long booked, long freeSpots) {

		static LessonResponse of(Lesson l, long booked) {
			return new LessonResponse(l.getId(), l.getType(), l.getStartsAt(), l.endsAt(), l.getDurationMinutes(),
					l.getRoom().getName(), l.getInstructor().getName(), l.capacity(), booked,
					Math.max(0, l.capacity() - booked));
		}
	}

	public record RoomResponse(Long id, String name, int capacity) {
	}

	public record InstructorResponse(Long id, String name, BigDecimal rating) {
	}
}
