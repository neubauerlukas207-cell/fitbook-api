package cz.fitbook.api.demo;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cz.fitbook.api.lesson.Instructor;
import cz.fitbook.api.lesson.InstructorRepository;
import cz.fitbook.api.lesson.Lesson;
import cz.fitbook.api.lesson.LessonRepository;
import cz.fitbook.api.lesson.LessonType;
import cz.fitbook.api.lesson.Room;
import cz.fitbook.api.lesson.RoomRepository;
import cz.fitbook.api.member.Member;
import cz.fitbook.api.member.MemberRepository;
import cz.fitbook.api.reservation.ReservationService;

/** Vytvoří ukázkový rozvrh na 14 dní dopředu (relativně k dnešku), aby API mělo co ukázat. */
@Component
@ConditionalOnProperty(name = "fitbook.demo-data", havingValue = "true")
class DemoDataInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

	private record Slot(LocalTime time, LessonType type, int duration, int roomIndex, int instructorIndex) {
	}

	private static final List<Slot> WEEKDAY = List.of(
			new Slot(LocalTime.of(7, 0), LessonType.JOGA, 60, 1, 0),
			new Slot(LocalTime.of(12, 0), LessonType.PILATES, 50, 1, 2),
			new Slot(LocalTime.of(17, 30), LessonType.SPINNING, 55, 2, 1),
			new Slot(LocalTime.of(18, 30), LessonType.CROSSFIT, 60, 0, 1));
	private static final Map<DayOfWeek, List<Slot>> WEEKEND = Map.of(
			DayOfWeek.SATURDAY, List.of(new Slot(LocalTime.of(9, 0), LessonType.ZUMBA, 60, 0, 0),
					new Slot(LocalTime.of(10, 30), LessonType.BODY_PUMP, 60, 0, 2)),
			DayOfWeek.SUNDAY, List.of(new Slot(LocalTime.of(10, 0), LessonType.JOGA, 75, 1, 0)));

	private final LessonRepository lessons;
	private final RoomRepository rooms;
	private final InstructorRepository instructors;
	private final MemberRepository members;
	private final ReservationService reservations;
	private final Clock clock;

	DemoDataInitializer(LessonRepository lessons, RoomRepository rooms, InstructorRepository instructors,
			MemberRepository members, ReservationService reservations, Clock clock) {
		this.lessons = lessons;
		this.rooms = rooms;
		this.instructors = instructors;
		this.members = members;
		this.reservations = reservations;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (lessons.count() > 0) {
			return;
		}
		List<Room> roomList = rooms.findAll();
		List<Instructor> instructorList = instructors.findAll();
		LocalDate today = LocalDate.now(clock);

		for (int day = 0; day < 14; day++) {
			LocalDate date = today.plusDays(day);
			List<Slot> slots = WEEKEND.getOrDefault(date.getDayOfWeek(), WEEKDAY);
			for (Slot s : slots) {
				lessons.save(new Lesson(s.type(), date.atTime(s.time()), s.duration(), roomList.get(s.roomIndex()),
						instructorList.get(s.instructorIndex())));
			}
		}

		var now = today.atStartOfDay();
		List<Member> demoMembers = members.saveAll(List.of(
				new Member("Eva Nováková", "eva@example.com", now),
				new Member("Karel Horák", "karel@example.com", now),
				new Member("Lucie Černá", "lucie@example.com", now)));

		var upcoming = lessons.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAt(
				java.time.LocalDateTime.now(clock).plusHours(3), today.plusDays(7).atStartOfDay());
		for (int i = 0; i < upcoming.size(); i++) {
			for (int m = 0; m <= i % demoMembers.size(); m++) {
				reservations.book(demoMembers.get(m).getId(), upcoming.get(i).getId());
			}
		}
		log.info("Ukázková data: {} lekcí, {} členů", lessons.count(), demoMembers.size());
	}
}
