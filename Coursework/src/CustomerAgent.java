import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPAException;

import java.lang.Math;
import ontology.PhoneOntology;
import ontology.elements.*;

public class CustomerAgent extends Agent {
	private Codec codec = new SLCodec();
	private Ontology ontology = PhoneOntology.getInstance();
	private int day = 0;
	private AID tickerAgent;
	private AID manufacturerAgent;

	@Override
	protected void setup() {
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

		// add to yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("customer-agent");
		sd.setName(getLocalName() + "-customer-agent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		addBehaviour(new DailyBehaviour(this));
	}

	@Override
	protected void takeDown() {
		// remove from yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	public class DailyBehaviour extends CyclicBehaviour {
		public DailyBehaviour(Agent a) {
			super(a);
		}
		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchContent("new day");
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				if (tickerAgent == null) {
					tickerAgent = msg.getSender();
				}
				day++;

				// Generate the order
				CustomerOrder customerOrder = new CustomerOrder();
				customerOrder.setOrderFrom(myAgent.getAID());
				customerOrder.setDayMade(day);
				customerOrder.setCompleted(false);
				// Determine the type of phone
				if (Math.random() < 0.5) {
					customerOrder.setType(0);
				} else {
					customerOrder.setType(1);
				}
				if (Math.random() < 0.5) {
					customerOrder.setRam(4);
				} else {
					customerOrder.setRam(8);
				}
				if (Math.random() < 0.5) {
					customerOrder.setStorage(64);
				} else {
					customerOrder.setStorage(256);
				}
				int quantity = (int) Math.floor(1 + 50 * Math.random());
				customerOrder.setQuantity(quantity);
				customerOrder.setPrice((int) Math.floor(100 + 500 * Math.random()));
				customerOrder.setDue(day + (int) Math.floor(1 + 10 * Math.random()));
				customerOrder.setPenalty((int) ((int) quantity * Math.floor(1 + 50 * Math.random())));

				// Send the order to manufacturer
				manufacturerAgent = new AID("Manufacturer", AID.ISLOCALNAME);
				ACLMessage order = new ACLMessage(ACLMessage.INFORM);
				order.addReceiver(manufacturerAgent);
				order.setLanguage(codec.getName());
				order.setOntology(ontology.getName());
				try {
					getContentManager().fillContent(order, customerOrder);
					send(order);
				} catch (CodecException e) {
					e.printStackTrace();
				} catch (OntologyException e) {
					e.printStackTrace();
				}
						ACLMessage dayDone = new ACLMessage(ACLMessage.INFORM);
						dayDone.addReceiver(tickerAgent);
						dayDone.setContent("done");
						myAgent.send(dayDone);

			} else {
				block();
			}
		}
	}
}