package cz.fitbook.api.lesson;

public enum LessonType {
	SPINNING("spinning"),
	CROSSFIT("crossfit"),
	BODY_PUMP("body_pump"),
	ZUMBA("zumba"),
	KRUHOVY_TRENINK("kruhovy_trenink"),
	JOGA("joga"),
	PILATES("pilates");

	private final String insightsName;

	LessonType(String insightsName) {
		this.insightsName = insightsName;
	}

	/** Název, pod kterým typ lekce zná služba FitBook Insights. */
	public String insightsName() {
		return insightsName;
	}
}
