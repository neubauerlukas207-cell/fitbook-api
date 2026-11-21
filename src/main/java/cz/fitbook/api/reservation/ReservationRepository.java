package cz.fitbook.api.reservation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	Optional<Reservation> findByMemberIdAndLessonId(Long memberId, Long lessonId);

	long countByLessonIdAndStatus(Long lessonId, ReservationStatus status);

	@EntityGraph(attributePaths = {"lesson", "lesson.room", "lesson.instructor"})
	List<Reservation> findByMemberIdOrderByLessonStartsAt(Long memberId);

	@EntityGraph(attributePaths = {"lesson", "lesson.room", "lesson.instructor"})
	Optional<Reservation> findWithLessonById(Long id);

	@Query("""
			select r.lesson.id, count(r) from Reservation r
			where r.lesson.id in :lessonIds and r.status = :status
			group by r.lesson.id""")
	List<Object[]> countGroupedByLesson(@Param("lessonIds") Collection<Long> lessonIds,
			@Param("status") ReservationStatus status);

	/** Počty aktivních rezervací pro více lekcí najednou – jeden dotaz místo jednoho na každou lekci. */
	default Map<Long, Long> activeCountsByLesson(Collection<Long> lessonIds) {
		Map<Long, Long> counts = new HashMap<>();
		if (lessonIds.isEmpty()) {
			return counts;
		}
		for (Object[] row : countGroupedByLesson(lessonIds, ReservationStatus.ACTIVE)) {
			counts.put((Long) row[0], (Long) row[1]);
		}
		return counts;
	}
}
