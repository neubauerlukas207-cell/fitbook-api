package cz.fitbook.api.member;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class MemberDtos {

	private MemberDtos() {
	}

	public record CreateMemberRequest(
			@NotBlank(message = "Jméno je povinné") @Size(max = 100) String name,
			@NotBlank(message = "E-mail je povinný") @Email(message = "Neplatný e-mail") @Size(max = 150) String email) {
	}

	public record MemberResponse(Long id, String name, String email, LocalDateTime createdAt) {

		static MemberResponse of(Member m) {
			return new MemberResponse(m.getId(), m.getName(), m.getEmail(), m.getCreatedAt());
		}
	}
}
