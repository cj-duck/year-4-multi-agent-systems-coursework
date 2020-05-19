import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import java.util.ArrayList;
import jade.core.AID;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPAException;

public class TickerAgent extends Agent {
	private AID supplier1Agent;
	private AID supplier2Agent;
	private AID manufacturerAgent;
	public static final int NUM_DAYS = 100;

	@Override
	protected void setup() {
		// add to yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("ticker-agent");
		sd.setName(getLocalName() + "-ticker-agent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException e) {
			e.printStackTrace();
		}

		manufacturerAgent = new AID("manufacturer", AID.ISLOCALNAME);
		supplier1Agent = new AID("supplier1", AID.ISLOCALNAME);
		supplier2Agent = new AID("supplier2", AID.ISLOCALNAME);
		// wait for other agents to start
		doWait(5000);
		addBehaviour(new SyncAgentsBehaviour(this));
	}

	@Override
	protected void takeDown() {
		// deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	public class SyncAgentsBehaviour extends Behaviour {

		private int day = 1;
		private int step = 0;
		private int numFinReceived = 0;
		private ArrayList<AID> simulationAgents = new ArrayList<>();

		public SyncAgentsBehaviour(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			switch (step) {
			case 0:
				// find all customer agents using directory service
				DFAgentDescription template1 = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("customer-agent");
				template1.addServices(sd);
				try {
					simulationAgents.clear();
					DFAgentDescription[] agentsType1 = DFService.search(myAgent, template1);
					for (int i = 0; i < agentsType1.length; i++) {
						simulationAgents.add(agentsType1[i].getName());
					}
				} catch (FIPAException e) {
					e.printStackTrace();
				}
				simulationAgents.add(supplier1Agent);
				simulationAgents.add(supplier2Agent);
				simulationAgents.add(manufacturerAgent);
				
				// send new day message to each agent
				ACLMessage tick = new ACLMessage(ACLMessage.INFORM);
				tick.setContent("new day");
				for (AID id : simulationAgents) {
					tick.addReceiver(id);
				}
				myAgent.send(tick);
				System.out.println("New day started. Day: " + day);
				step++;
				
				break;
			case 1:
				MessageTemplate mt = MessageTemplate.MatchContent("done");
				ACLMessage msg = myAgent.receive(mt);
				if (msg != null) {
					numFinReceived++;
					if (numFinReceived >= simulationAgents.size()) {
						step++;
					}
				} else {
					block();
				}
			}
		}

		@Override
		public boolean done() {
			return step == 2;
		}

		@Override
		public void reset() {
			super.reset();
			step = 0;
			numFinReceived = 0;
		}

		@Override
		public int onEnd() {
			System.out.println("End of Day " + day + "\r\n\r\n");		
			if (day == NUM_DAYS) {
				// send termination message to each agent
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.setContent("terminate");
				for (AID agent : simulationAgents) {
					msg.addReceiver(agent);
				}
				myAgent.send(msg);
			} else {
				reset();
				myAgent.addBehaviour(this);
			}
			day++;
			return 0;
		}
	}
}
