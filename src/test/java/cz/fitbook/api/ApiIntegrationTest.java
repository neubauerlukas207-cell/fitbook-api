package cz.fitbook.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import cz.fitbook.api.forecast.ForecastUnavailableException;
import cz.fitbook.api.forecast.InsightsClient;
import cz.fitbook.api.forecast.InsightsClient.InsightsPrediction;

/**
 * Testy přes HTTP vrstvu proti skutečné databázi (H2 + Flyway migrace).
 * {@code @Transactional} – každý test se po skončení vrátí zpět, testy se navzájem neovlivňují.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiIntegrationTest {

	private static final AtomicInteger SEQ = new AtomicInteger();
	private static final long STUDIO = 3; // Spinning studio, kapacita 20
	private static final long SMALL_ROOM = 2; // Malý sál, kapacita 12

	@Autowired
	MockMvc mvc;

	@MockitoBean
	InsightsClient insights;

	// --- pomocné metody --------------------------------------------------------------------------

	private ResultActions postJson(String url, String json) throws Exception {
		return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private long createMember() throws Exception {
		int n = SEQ.incrementAndGet();
		String body = postJson("/api/members", """
				{"name": "Člen %d", "email": "clen%d@example.com"}""".formatted(n, n))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	private long createLesson(long roomId, long instructorId, LocalDateTime start) throws Exception {
		String body = postJson("/api/lessons", """
				{"type": "SPINNING", "startsAt": "%s", "durationMinutes": 60, "roomId": %d, "instructorId": %d}"""
				.formatted(start, roomId, instructorId))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	private ResultActions book(long memberId, long lessonId) throws Exception {
		return postJson("/api/reservations", """
				{"memberId": %d, "lessonId": %d}""".formatted(memberId, lessonId));
	}

	private static LocalDateTime inDays(int days, int hour) {
		return LocalDateTime.now().plusDays(days).withHour(hour).truncatedTo(ChronoUnit.HOURS);
	}

	// --- testy -----------------------------------------------------------------------------------

	@Test
	void celý_průběh_rezervace() throws Exception {
		long member = createMember();
		long lesson = createLesson(STUDIO, 1, inDays(5, 18));

		book(member, lesson)
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", containsString("/api/reservations/")))
				.andExpect(jsonPath("$.status").value("ACTIVE"));

		mvc.perform(get("/api/lessons/{id}", lesson))
				.andExpect(jsonPath("$.booked").value(1))
				.andExpect(jsonPath("$.freeSpots").value(19));

		String body = mvc.perform(get("/api/members/{id}/reservations", member))
				.andExpect(jsonPath("$", hasSize(1)))
				.andReturn().getResponse().getContentAsString();
		long reservation = ((Number) JsonPath.read(body, "$[0].id")).longValue();

		mvc.perform(delete("/api/reservations/{id}", reservation))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
		mvc.perform(get("/api/lessons/{id}", lesson)).andExpect(jsonPath("$.booked").value(0));
	}

	@Test
	void dvojitá_rezervace_vrátí_409_s_kódem_chyby() throws Exception {
		long member = createMember();
		long lesson = createLesson(STUDIO, 1, inDays(6, 18));
		book(member, lesson).andExpect(status().isCreated());

		book(member, lesson)
				.andExpect(status().isConflict())
				.andExpect(header().string("Content-Type", containsString("application/problem+json")))
				.andExpect(jsonPath("$.code").value("ALREADY_BOOKED"));
	}

	@Test
	void plná_lekce_vrátí_409() throws Exception {
		long lesson = createLesson(SMALL_ROOM, 2, inDays(7, 10));
		for (int i = 0; i < 12; i++) {
			book(createMember(), lesson).andExpect(status().isCreated());
		}
		book(createMember(), lesson)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("LESSON_FULL"));
	}

	@Test
	void obsazený_sál_vrátí_409() throws Exception {
		createLesson(STUDIO, 1, inDays(8, 18));
		postJson("/api/lessons", """
				{"type": "ZUMBA", "startsAt": "%s", "durationMinutes": 60, "roomId": 3, "instructorId": 2}"""
				.formatted(inDays(8, 18).plusMinutes(30)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ROOM_BUSY"));
	}

	@Test
	void neplatný_vstup_vrátí_400_se_seznamem_chyb() throws Exception {
		postJson("/api/members", """
				{"name": "", "email": "neni-email"}""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists())
				.andExpect(jsonPath("$.errors.email").value("Neplatný e-mail"));
	}

	@Test
	void stejný_email_nelze_registrovat_dvakrát() throws Exception {
		postJson("/api/members", """
				{"name": "Jan", "email": "jan@example.com"}""").andExpect(status().isCreated());
		postJson("/api/members", """
				{"name": "Jan 2", "email": "JAN@example.com"}""")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_EXISTS"));
	}

	@Test
	void neexistující_lekce_vrátí_404() throws Exception {
		mvc.perform(get("/api/lessons/{id}", 999_999))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Nenalezeno"));
	}

	@Test
	void rozvrh_filtruje_podle_typu_a_data() throws Exception {
		LocalDateTime day = inDays(9, 7);
		createLesson(STUDIO, 1, day);
		postJson("/api/lessons", """
				{"type": "JOGA", "startsAt": "%s", "durationMinutes": 60, "roomId": 2, "instructorId": 2}"""
				.formatted(day.plusHours(2))).andExpect(status().isCreated());

		String date = day.toLocalDate().toString();
		mvc.perform(get("/api/lessons").param("from", date).param("to", date))
				.andExpect(jsonPath("$", hasSize(2)));
		mvc.perform(get("/api/lessons").param("from", date).param("to", date).param("type", "JOGA"))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].type").value("JOGA"));
	}

	@Test
	void předpověď_použije_ML_službu() throws Exception {
		long lesson = createLesson(STUDIO, 1, inDays(10, 18));
		when(insights.predict(any())).thenReturn(new InsightsPrediction(0.85, 17, 20, "vysoká"));

		mvc.perform(get("/api/lessons/{id}/forecast", lesson))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.predictedBookings").value(17))
				.andExpect(jsonPath("$.level").value("vysoká"));
	}

	@Test
	void nedostupná_ML_služba_vrátí_503() throws Exception {
		long lesson = createLesson(STUDIO, 1, inDays(11, 18));
		when(insights.predict(any())).thenThrow(new ForecastUnavailableException("Služba neodpovídá"));

		mvc.perform(get("/api/lessons/{id}/forecast", lesson)).andExpect(status().isServiceUnavailable());
	}

	@Test
	void statistiky_obsazenosti() throws Exception {
		long lesson = createLesson(STUDIO, 1, inDays(12, 18));
		book(createMember(), lesson).andExpect(status().isCreated());

		mvc.perform(get("/api/stats/occupancy"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].type").value("SPINNING"))
				.andExpect(jsonPath("$[0].booked").value(1));
	}
}
