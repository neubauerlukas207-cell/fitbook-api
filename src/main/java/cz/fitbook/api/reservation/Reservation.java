package cz.fitbook.api.reservation;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import cz.fitbook.api.lesson.Lesson;
import cz.fitbook.api.member.Member;

@Entity
public class Reservation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Lesson lesson;

	@Enumerated(EnumType.STRING)
	private ReservationStatus status;

	private LocalDateTime createdAt;

	protected Reservation() {
	}

	public Reservation(Member member, Lesson lesson, LocalDateTime createdAt) {
		this.member = member;
		this.lesson = lesson;
		this.status = ReservationStatus.ACTIVE;
		this.createdAt = createdAt;
	}

	public boolean isActive() {
		return status == ReservationStatus.ACTIVE;
	}

	void cancel() {
		status = ReservationStatus.CANCELLED;
	}

	void reactivate(LocalDateTime now) {
		status = ReservationStatus.ACTIVE;
		createdAt = now;
	}

	public Long getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public Lesson getLesson() {
		return lesson;
	}

	public ReservationStatus getStatus() {
		return status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
