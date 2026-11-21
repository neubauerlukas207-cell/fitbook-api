package cz.fitbook.api.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import cz.fitbook.api.common.BusinessRuleException;
import cz.fitbook.api.common.NotFoundException;
import cz.fitbook.api.config.FitbookProperties;
import cz.fitbook.api.lesson.Instructor;
import cz.fitbook.api.lesson.Lesson;
import cz.fitbook.api.lesson.LessonRepository;
import cz.fitbook.api.lesson.LessonType;
import cz.fitbook.api.lesson.Room;
import cz.fitbook.api.member.Member;
import cz.fitbook.api.member.MemberRepository;

/** Jednotkové testy pravidel rezervací – bez Springu a databáze, s pevně daným „teď“. */
class ReservationServiceTest {

	private static final ZoneId ZONE = ZoneId.of("Europe/Prague");
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 21, 12, 0);

	private final ReservationRepository reservations = mock(ReservationRepository.class);
	private final LessonRepository lessons = mock(LessonRepository.class);
	private final MemberRepository members = mock(MemberRepository.class);
	private final Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
	private final ReservationService service = new ReservationService(reservations, lessons, members,
			new FitbookProperties("Europe/Prague", false, Duration.ofHours(2), null), clock);

	private final Member member = withId(new Member("Eva", "eva@example.com", NOW), 1L);

	@BeforeEach
	void setUp() {
		when(members.findById(1L)).thenReturn(Optional.of(member));
		when(reservations.save(any())).thenAnswer(inv -> inv.getArgument(0));
	}

	private Lesson lessonAt(LocalDateTime start, int capacity) {
		Lesson lesson = withId(new Lesson(LessonType.SPINNING, start, 60, new Room("Studio", capacity),
				new Instructor("Tomáš", BigDecimal.valueOf(4.3))), 10L);
		when(lessons.findByIdForUpdate(10L)).thenReturn(Optional.of(lesson));
		return lesson;
	}

	@Test
	void rezervuje_volnou_lekci() {
		lessonAt(NOW.plusDays(1), 20);
		when(reservations.countByLessonIdAndStatus(10L, ReservationStatus.ACTIVE)).thenReturn(5L);

		var result = service.book(1L, 10L);

		assertThat(result.status()).isEqualTo(ReservationStatus.ACTIVE);
		verify(reservations).save(any());
	}

	@Test
	void plna_lekce_vrati_konflikt() {
		lessonAt(NOW.plusDays(1), 20);
		when(reservations.countByLessonIdAndStatus(10L, ReservationStatus.ACTIVE)).thenReturn(20L);

		assertThatThrownBy(() -> service.book(1L, 10L))
				.isInstanceOf(BusinessRuleException.class)
				.extracting("code").isEqualTo("LESSON_FULL");
		verify(reservations, never()).save(any());
	}

	@Test
	void nelze_rezervovat_lekci_ktera_uz_zacala() {
		lessonAt(NOW.minusMinutes(5), 20);

		assertThatThrownBy(() -> service.book(1L, 10L)).extracting("code").isEqualTo("LESSON_STARTED");
	}

	@Test
	void druha_rezervace_stejne_lekce_vrati_konflikt() {
		Lesson lesson = lessonAt(NOW.plusDays(1), 20);
		when(reservations.findByMemberIdAndLessonId(1L, 10L))
				.thenReturn(Optional.of(new Reservation(member, lesson, NOW)));

		assertThatThrownBy(() -> service.book(1L, 10L)).extracting("code").isEqualTo("ALREADY_BOOKED");
	}

	@Test
	void po_zruseni_lze_rezervovat_znovu_bez_noveho_zaznamu() {
		Lesson lesson = lessonAt(NOW.plusDays(1), 20);
		Reservation cancelled = withId(new Reservation(member, lesson, NOW.minusDays(1)), 7L);
		cancelled.cancel();
		when(reservations.findByMemberIdAndLessonId(1L, 10L)).thenReturn(Optional.of(cancelled));

		var result = service.book(1L, 10L);

		assertThat(result.id()).isEqualTo(7L);
		assertThat(result.status()).isEqualTo(ReservationStatus.ACTIVE);
		assertThat(result.createdAt()).isEqualTo(NOW);
	}

	@Test
	void neexistujici_clen_vrati_404() {
		assertThatThrownBy(() -> service.book(99L, 10L)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void zruseni_vcas_projde() {
		Reservation r = withId(new Reservation(member, lessonAt(NOW.plusHours(3), 20), NOW), 5L);
		when(reservations.findWithLessonById(5L)).thenReturn(Optional.of(r));

		assertThat(service.cancel(5L).status()).isEqualTo(ReservationStatus.CANCELLED);
	}

	@Test
	void zruseni_mene_nez_2_hodiny_predem_vrati_konflikt() {
		Reservation r = withId(new Reservation(member, lessonAt(NOW.plusMinutes(90), 20), NOW), 5L);
		when(reservations.findWithLessonById(5L)).thenReturn(Optional.of(r));

		assertThatThrownBy(() -> service.cancel(5L)).extracting("code").isEqualTo("TOO_LATE_TO_CANCEL");
		assertThat(r.isActive()).isTrue();
	}

	@Test
	void opakovane_zruseni_vrati_konflikt() {
		Reservation r = withId(new Reservation(member, lessonAt(NOW.plusDays(1), 20), NOW), 5L);
		r.cancel();
		when(reservations.findWithLessonById(5L)).thenReturn(Optional.of(r));

		assertThatThrownBy(() -> service.cancel(5L)).extracting("code").isEqualTo("ALREADY_CANCELLED");
	}

	private static <T> T withId(T entity, Long id) {
		ReflectionTestUtils.setField(entity, "id", id);
		return entity;
	}
}
