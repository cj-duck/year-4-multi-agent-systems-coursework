package ontology.elements;

import jade.content.Predicate;
import jade.core.AID;

public class Order implements Predicate {
	private AID orderFrom;

	public AID getOrderFrom() {
		return orderFrom;
	}

	public void setOrderFrom(AID orderFrom) {
		this.orderFrom = orderFrom;
	}

	private int dayMade;

	public int getDayMade() {
		return dayMade;
	}

	public void setDayMade(int dayMade) {
		this.dayMade = dayMade;
	}
}
