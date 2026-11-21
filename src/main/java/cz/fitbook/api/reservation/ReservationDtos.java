package cz.fitbook.api.reservation;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

import cz.fitbook.api.lesson.LessonType;

public final class ReservationDtos {

	private ReservationDtos() {
	}

	public record CreateReservationRequest(
			@NotNull(message = "Chybí člen") Long memberId,
			@NotNull(message = "Chybí lekce") Long lessonId) {
	}

	public record ReservationResponse(Long id, Long memberId, Long lessonId, LessonType lessonType,
			LocalDateTime lessonStartsAt, String room, ReservationStatus status, LocalDateTime createdAt) {

		static ReservationResponse of(Reservation r) {
			var lesson = r.getLesson();
			return new ReservationResponse(r.getId(), r.getMember().getId(), lesson.getId(), lesson.getType(),
					lesson.getStartsAt(), lesson.getRoom().getName(), r.getStatus(), r.getCreatedAt());
		}
	}
}
