package cz.fitbook.api.lesson;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import cz.fitbook.api.common.BusinessRuleException;
import cz.fitbook.api.lesson.LessonDtos.CreateLessonRequest;
import cz.fitbook.api.lesson.LessonDtos.InstructorResponse;
import cz.fitbook.api.lesson.LessonDtos.LessonResponse;
import cz.fitbook.api.lesson.LessonDtos.RoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Lekce")
class LessonController {

	private final LessonService service;
	private final RoomRepository rooms;
	private final InstructorRepository instructors;
	private final Clock clock;

	LessonController(LessonService service, RoomRepository rooms, InstructorRepository instructors, Clock clock) {
		this.service = service;
		this.rooms = rooms;
		this.instructors = instructors;
		this.clock = clock;
	}

	@GetMapping("/lessons")
	@Operation(summary = "Rozvrh lekcí", description = "Výchozí interval: dnešek + 7 dní. Lze filtrovat podle typu.")
	List<LessonResponse> schedule(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) LessonType type) {
		LocalDate start = from != null ? from : LocalDate.now(clock);
		LocalDate end = to != null ? to : start.plusDays(7);
		if (end.isBefore(start)) {
			throw new BusinessRuleException("INVALID_RANGE", "Datum 'to' musí být po datu 'from'");
		}
		return service.schedule(start.atStartOfDay(), end.plusDays(1).atStartOfDay(), type);
	}

	@GetMapping("/lessons/{id}")
	@Operation(summary = "Detail lekce včetně volných míst")
	LessonResponse get(@PathVariable Long id) {
		return service.get(id);
	}

	@PostMapping("/lessons")
	@Operation(summary = "Vypsání nové lekce", description = "409 když je sál nebo instruktor v tu dobu obsazený")
	ResponseEntity<LessonResponse> create(@Valid @RequestBody CreateLessonRequest request) {
		LessonResponse created = service.create(request);
		var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(created.id());
		return ResponseEntity.created(location).body(created);
	}

	@DeleteMapping("/lessons/{id}")
	@Operation(summary = "Smazání lekce bez rezervací")
	ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/rooms")
	@Operation(summary = "Seznam sálů")
	List<RoomResponse> rooms() {
		return rooms.findAll().stream().map(r -> new RoomResponse(r.getId(), r.getName(), r.getCapacity())).toList();
	}

	@GetMapping("/instructors")
	@Operation(summary = "Seznam instruktorů")
	List<InstructorResponse> instructors() {
		return instructors.findAll().stream()
				.map(i -> new InstructorResponse(i.getId(), i.getName(), i.getRating())).toList();
	}
}
