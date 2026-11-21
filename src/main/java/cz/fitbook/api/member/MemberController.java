package cz.fitbook.api.member;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import cz.fitbook.api.common.BusinessRuleException;
import cz.fitbook.api.common.NotFoundException;
import cz.fitbook.api.member.MemberDtos.CreateMemberRequest;
import cz.fitbook.api.member.MemberDtos.MemberResponse;
import cz.fitbook.api.reservation.ReservationDtos.ReservationResponse;
import cz.fitbook.api.reservation.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/members")
@Tag(name = "Členové")
class MemberController {

	private final MemberRepository members;
	private final ReservationService reservations;
	private final Clock clock;

	MemberController(MemberRepository members, ReservationService reservations, Clock clock) {
		this.members = members;
		this.reservations = reservations;
		this.clock = clock;
	}

	@PostMapping
	@Transactional
	@Operation(summary = "Registrace nového člena")
	ResponseEntity<MemberResponse> create(@Valid @RequestBody CreateMemberRequest request) {
		String email = request.email().trim().toLowerCase();
		if (members.existsByEmailIgnoreCase(email)) {
			throw new BusinessRuleException("EMAIL_EXISTS", "Člen s tímto e-mailem už existuje");
		}
		Member saved = members.save(new Member(request.name().trim(), email, LocalDateTime.now(clock)));
		var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(saved.getId());
		return ResponseEntity.created(location).body(MemberResponse.of(saved));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Detail člena")
	MemberResponse get(@PathVariable Long id) {
		return members.findById(id).map(MemberResponse::of).orElseThrow(() -> new NotFoundException("Člen", id));
	}

	@GetMapping("/{id}/reservations")
	@Operation(summary = "Rezervace člena seřazené podle začátku lekce")
	List<ReservationResponse> reservations(@PathVariable Long id) {
		return reservations.ofMember(id);
	}
}
