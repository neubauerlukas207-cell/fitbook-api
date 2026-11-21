package cz.fitbook.api.lesson;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.fitbook.api.common.BusinessRuleException;
import cz.fitbook.api.common.NotFoundException;
import cz.fitbook.api.lesson.LessonDtos.CreateLessonRequest;
import cz.fitbook.api.lesson.LessonDtos.LessonResponse;
import cz.fitbook.api.reservation.ReservationRepository;
import cz.fitbook.api.reservation.ReservationStatus;

@Service
public class LessonService {

	/** Nejdelší možná lekce – podle ní hledáme kandidáty na překryv. */
	private static final int MAX_DURATION_MINUTES = 180;

	private final LessonRepository lessons;
	private final RoomRepository rooms;
	private final InstructorRepository instructors;
	private final ReservationRepository reservations;
	private final Clock clock;

	public LessonService(LessonRepository lessons, RoomRepository rooms, InstructorRepository instructors,
			ReservationRepository reservations, Clock clock) {
		this.lessons = lessons;
		this.rooms = rooms;
		this.instructors = instructors;
		this.reservations = reservations;
		this.clock = clock;
	}

	/** Rozvrh v intervalu – 2 dotazy celkem: lekce (se sálem a instruktorem) + počty rezervací. */
	@Transactional(readOnly = true)
	public List<LessonResponse> schedule(LocalDateTime from, LocalDateTime to, LessonType type) {
		List<Lesson> found = type == null
				? lessons.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAt(from, to)
				: lessons.findByTypeAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAt(type, from, to);
		Map<Long, Long> booked = reservations.activeCountsByLesson(found.stream().map(Lesson::getId).toList());
		return found.stream().map(l -> LessonResponse.of(l, booked.getOrDefault(l.getId(), 0L))).toList();
	}

	@Transactional(readOnly = true)
	public LessonResponse get(Long id) {
		Lesson lesson = find(id);
		return LessonResponse.of(lesson, reservations.countByLessonIdAndStatus(id, ReservationStatus.ACTIVE));
	}

	@Transactional(readOnly = true)
	public Lesson find(Long id) {
		return lessons.findWithDetailsById(id).orElseThrow(() -> new NotFoundException("Lekce", id));
	}

	@Transactional
	public LessonResponse create(CreateLessonRequest request) {
		if (!request.startsAt().isAfter(LocalDateTime.now(clock))) {
			throw new BusinessRuleException("LESSON_IN_PAST", "Lekci nelze vypsat do minulosti");
		}
		Room room = rooms.findById(request.roomId()).orElseThrow(() -> new NotFoundException("Sál", request.roomId()));
		Instructor instructor = instructors.findById(request.instructorId())
				.orElseThrow(() -> new NotFoundException("Instruktor", request.instructorId()));

		LocalDateTime start = request.startsAt();
		LocalDateTime end = start.plusMinutes(request.durationMinutes());
		LocalDateTime searchFrom = start.minusMinutes(MAX_DURATION_MINUTES);

		if (lessons.findByRoomIdAndStartsAtBetween(room.getId(), searchFrom, end).stream()
				.anyMatch(l -> l.overlaps(start, end))) {
			throw new BusinessRuleException("ROOM_BUSY", "Sál " + room.getName() + " je v tu dobu obsazený");
		}
		if (lessons.findByInstructorIdAndStartsAtBetween(instructor.getId(), searchFrom, end).stream()
				.anyMatch(l -> l.overlaps(start, end))) {
			throw new BusinessRuleException("INSTRUCTOR_BUSY", "Instruktor " + instructor.getName() + " v tu dobu vede jinou lekci");
		}

		Lesson saved = lessons.save(new Lesson(request.type(), start, request.durationMinutes(), room, instructor));
		return LessonResponse.of(saved, 0);
	}

	@Transactional
	public void delete(Long id) {
		Lesson lesson = find(id);
		if (reservations.countByLessonIdAndStatus(id, ReservationStatus.ACTIVE) > 0) {
			throw new BusinessRuleException("LESSON_HAS_RESERVATIONS", "Lekci s aktivními rezervacemi nelze smazat");
		}
		lessons.delete(lesson);
	}
}
