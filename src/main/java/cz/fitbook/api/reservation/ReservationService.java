package cz.fitbook.api.reservation;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.fitbook.api.common.BusinessRuleException;
import cz.fitbook.api.common.NotFoundException;
import cz.fitbook.api.config.FitbookProperties;
import cz.fitbook.api.lesson.Lesson;
import cz.fitbook.api.lesson.LessonRepository;
import cz.fitbook.api.member.Member;
import cz.fitbook.api.member.MemberRepository;
import cz.fitbook.api.reservation.ReservationDtos.ReservationResponse;

/** Pravidla rezervací: kapacita sálu, žádné rezervace do minulosti, storno nejpozději X hodin předem. */
@Service
public class ReservationService {

	private final ReservationRepository reservations;
	private final LessonRepository lessons;
	private final MemberRepository members;
	private final FitbookProperties properties;
	private final Clock clock;

	public ReservationService(ReservationRepository reservations, LessonRepository lessons,
			MemberRepository members, FitbookProperties properties, Clock clock) {
		this.reservations = reservations;
		this.lessons = lessons;
		this.members = members;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional
	public ReservationResponse book(Long memberId, Long lessonId) {
		Member member = members.findById(memberId).orElseThrow(() -> new NotFoundException("Člen", memberId));
		// zámek na řádku lekce: souběžné rezervace téže lekce čekají, než tahle transakce skončí
		Lesson lesson = lessons.findByIdForUpdate(lessonId).orElseThrow(() -> new NotFoundException("Lekce", lessonId));
		LocalDateTime now = LocalDateTime.now(clock);

		if (!lesson.getStartsAt().isAfter(now)) {
			throw new BusinessRuleException("LESSON_STARTED", "Na lekci, která už začala, se nelze přihlásit");
		}
		var existing = reservations.findByMemberIdAndLessonId(memberId, lessonId);
		if (existing.filter(Reservation::isActive).isPresent()) {
			throw new BusinessRuleException("ALREADY_BOOKED", "Na tuto lekci už máte rezervaci");
		}
		long booked = reservations.countByLessonIdAndStatus(lessonId, ReservationStatus.ACTIVE);
		if (booked >= lesson.capacity()) {
			throw new BusinessRuleException("LESSON_FULL", "Lekce je plná");
		}

		Reservation reservation = existing.orElseGet(() -> new Reservation(member, lesson, now));
		reservation.reactivate(now);
		return ReservationResponse.of(reservations.save(reservation));
	}

	@Transactional
	public ReservationResponse cancel(Long reservationId) {
		Reservation reservation = reservations.findWithLessonById(reservationId)
				.orElseThrow(() -> new NotFoundException("Rezervace", reservationId));
		if (!reservation.isActive()) {
			throw new BusinessRuleException("ALREADY_CANCELLED", "Rezervace už byla zrušena");
		}
		LocalDateTime deadline = reservation.getLesson().getStartsAt().minus(properties.cancellationDeadline());
		if (LocalDateTime.now(clock).isAfter(deadline)) {
			throw new BusinessRuleException("TOO_LATE_TO_CANCEL",
					"Rezervaci lze zrušit nejpozději " + properties.cancellationDeadline().toHours()
							+ " h před začátkem lekce");
		}
		reservation.cancel();
		return ReservationResponse.of(reservation);
	}

	@Transactional(readOnly = true)
	public List<ReservationResponse> ofMember(Long memberId) {
		if (!members.existsById(memberId)) {
			throw new NotFoundException("Člen", memberId);
		}
		return reservations.findByMemberIdOrderByLessonStartsAt(memberId).stream().map(ReservationResponse::of).toList();
	}
}
