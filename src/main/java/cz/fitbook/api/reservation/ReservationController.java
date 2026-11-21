package cz.fitbook.api.reservation;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import cz.fitbook.api.reservation.ReservationDtos.CreateReservationRequest;
import cz.fitbook.api.reservation.ReservationDtos.ReservationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Rezervace")
class ReservationController {

	private final ReservationService service;

	ReservationController(ReservationService service) {
		this.service = service;
	}

	@PostMapping
	@Operation(summary = "Rezervace lekce", description = "409 když je lekce plná, už začala nebo už má člen rezervaci")
	ResponseEntity<ReservationResponse> book(@Valid @RequestBody CreateReservationRequest request) {
		ReservationResponse created = service.book(request.memberId(), request.lessonId());
		var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(created.id());
		return ResponseEntity.created(location).body(created);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Zrušení rezervace", description = "Nejpozději 2 hodiny před začátkem lekce")
	ReservationResponse cancel(@PathVariable Long id) {
		return service.cancel(id);
	}
}
