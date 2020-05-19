package ontology.elements;

public class CustomerOrder extends Order {
	
	private boolean completed;
	
	public boolean getCompleted() {
		return completed;
	}
	
	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	private int type;

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	private int ram;

	public int getRam() {
		return ram;
	}

	public void setRam(int ram) {
		this.ram = ram;
	}

	private int storage;

	public int getStorage() {
		return storage;
	}

	public void setStorage(int storage) {
		this.storage = storage;
	}

	private int quantity;

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	
	private int price;
	
	public int getPrice() {
		return price;
	}
	
	public void setPrice(int price) {
		this.price = price;
	}

	private int due;

	public int getDue() {
		return due;
	}

	public void setDue(int due) {
		this.due = due;
	}

	private int penalty;

	public int getPenalty() {
		return penalty;
	}

	public void setPenalty(int penalty) {
		this.penalty = penalty;
	}
	
	private int profit;
	
	public int getProfit() {
		return profit;
	}
	
	public void setProfit(int profit) {
		this.profit = profit;
	}
}
