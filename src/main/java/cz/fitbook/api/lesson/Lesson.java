package cz.fitbook.api.lesson;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Lesson {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	private LessonType type;

	private LocalDateTime startsAt;

	@Column(name = "duration_minutes")
	private int durationMinutes;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Room room;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Instructor instructor;

	protected Lesson() {
	}

	public Lesson(LessonType type, LocalDateTime startsAt, int durationMinutes, Room room, Instructor instructor) {
		this.type = type;
		this.startsAt = startsAt;
		this.durationMinutes = durationMinutes;
		this.room = room;
		this.instructor = instructor;
	}

	public LocalDateTime endsAt() {
		return startsAt.plusMinutes(durationMinutes);
	}

	/** Dva intervaly se překrývají, když jeden začne dřív, než druhý skončí (a naopak). */
	public boolean overlaps(LocalDateTime start, LocalDateTime end) {
		return startsAt.isBefore(end) && start.isBefore(endsAt());
	}

	public int capacity() {
		return room.getCapacity();
	}

	public Long getId() {
		return id;
	}

	public LessonType getType() {
		return type;
	}

	public LocalDateTime getStartsAt() {
		return startsAt;
	}

	public int getDurationMinutes() {
		return durationMinutes;
	}

	public Room getRoom() {
		return room;
	}

	public Instructor getInstructor() {
		return instructor;
	}
}
