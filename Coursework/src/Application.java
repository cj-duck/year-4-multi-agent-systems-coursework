import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.core.Runtime;

public class Application {

	public static void main(String[] args) {
		// Declaring variables
		int numberOfCustomers = 3;
		// Set up JADE environment
		Profile myProfile = new ProfileImpl();
		Runtime myRuntime = Runtime.instance();
		ContainerController myContainer = myRuntime.createMainContainer(myProfile);
		try {
			// Start rma
			AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
			rma.start();
			// Start ticker agent
			AgentController ticker = myContainer.createNewAgent("ticker", TickerAgent.class.getCanonicalName(), null);
			ticker.start();

			// Start manufacturer agent
			AgentController manufacturer = myContainer.createNewAgent("Manufacturer",
					ManufacturerAgent.class.getCanonicalName(), null);
			manufacturer.start();

			// Start customer agents
			for (int i = 1; i < (numberOfCustomers + 1); i++) {
				AgentController customer = myContainer.createNewAgent("Customer" + i,
						CustomerAgent.class.getCanonicalName(), null);
				customer.start();
			}

			// Start Supplier1 agent
			AgentController supplier1 = myContainer.createNewAgent("supplier1", Supplier1Agent.class.getCanonicalName(),
					null);
			supplier1.start();

			// Start Supplier2 agent
			AgentController supplier2 = myContainer.createNewAgent("supplier2", Supplier2Agent.class.getCanonicalName(),
					null);
			supplier2.start();

		} catch (Exception e) {
			System.out.println("Error starting agent: " + e.toString());
		}
	}
}
