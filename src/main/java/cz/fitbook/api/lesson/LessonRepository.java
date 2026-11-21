package cz.fitbook.api.lesson;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

	/** {@code @EntityGraph} načte sál a instruktora jedním dotazem (JOIN) – žádný problém N+1. */
	@EntityGraph(attributePaths = {"room", "instructor"})
	List<Lesson> findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAt(LocalDateTime from,
			LocalDateTime to);

	@EntityGraph(attributePaths = {"room", "instructor"})
	List<Lesson> findByTypeAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAt(LessonType type,
			LocalDateTime from, LocalDateTime to);

	@EntityGraph(attributePaths = {"room", "instructor"})
	Optional<Lesson> findWithDetailsById(Long id);

	/** Lekce v sále, které mohou kolidovat s intervalem (lekce trvá max. 180 min). */
	List<Lesson> findByRoomIdAndStartsAtBetween(Long roomId, LocalDateTime from, LocalDateTime to);

	List<Lesson> findByInstructorIdAndStartsAtBetween(Long instructorId, LocalDateTime from, LocalDateTime to);

	/**
	 * Zamkne řádek lekce (SELECT … FOR UPDATE) do konce transakce. Dvě souběžné rezervace na poslední
	 * volné místo se tak seřadí za sebe a kapacita se nepřekročí.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select l from Lesson l join fetch l.room where l.id = :id")
	Optional<Lesson> findByIdForUpdate(@Param("id") Long id);
}
