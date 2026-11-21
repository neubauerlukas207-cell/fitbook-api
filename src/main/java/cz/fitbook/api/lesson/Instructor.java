package cz.fitbook.api.lesson;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Instructor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	private BigDecimal rating;

	protected Instructor() {
	}

	public Instructor(String name, BigDecimal rating) {
		this.name = name;
		this.rating = rating;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public BigDecimal getRating() {
		return rating;
	}
}
