import java.util.ArrayList;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import ontology.PhoneOntology;
import ontology.elements.ManufacturerSupplierOrder;
import ontology.elements.SupplierManufacturerDelivery;

public class Supplier2Agent extends Agent {
	private Codec codec = new SLCodec();
	private Ontology ontology = PhoneOntology.getInstance();
	private AID tickerAgent;
	private AID manufacturerAgent;
	private int day = 0;
	private ArrayList<SupplierManufacturerDelivery> deliveries = new ArrayList<>();

	protected void setup() {
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

		// Get manufacturer AID
		manufacturerAgent = new AID("manufacturer", AID.ISLOCALNAME);
		// add to yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("supplier2-agent");
		sd.setName(getLocalName() + "-supplier2-agent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		addBehaviour(new DailyBehaviour());
	}

	protected void takedown() {
		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	public class DailyBehaviour extends CyclicBehaviour {

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchContent("new day");
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				if (tickerAgent == null) {
					tickerAgent = msg.getSender();
				}
				day++;

				CyclicBehaviour ois = new OrdersInServer();
				myAgent.addBehaviour(ois);
				CyclicBehaviour so = new shipOrder();
				myAgent.addBehaviour(so);
				myAgent.addBehaviour(new EndDayListener());
				// addBehaviour(new OrdersInServer());
			} else {
				block();
			}
		}
	}

	public class OrdersInServer extends CyclicBehaviour {

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchLanguage(codec.getName()),
					MessageTemplate.MatchOntology(ontology.getName()));
			ACLMessage msg = receive(mt);
			if (msg != null) {
				if (msg.getPerformative() == ACLMessage.INFORM) {
					try {
						ContentElement ce = getContentManager().extractContent(msg);
						if (ce instanceof ManufacturerSupplierOrder) {
							ManufacturerSupplierOrder order = (ManufacturerSupplierOrder) ce;
							SupplierManufacturerDelivery delivery = new SupplierManufacturerDelivery();
							int price = 0;
							delivery.setStorage64(order.getStorage64());
							price = price + (15 * order.getStorage64());
							delivery.setStorage256(order.getStorage256());
							price = price + (40 * order.getStorage256());
							delivery.setRam4(order.getRam4());
							price = price + (20 * order.getRam4());
							delivery.setRam8(order.getRam8());
							price = price + (35 * order.getRam8());
							delivery.setCost(price);
							delivery.setDayDue(day + 1);

							deliveries.add(delivery);

							System.out.println("Manufacturer Order received by Supplier1");
						}
					} catch (CodecException e) {
						e.printStackTrace();
					} catch (UngroundedException e) {
						e.printStackTrace();
					} catch (OntologyException e) {
						e.printStackTrace();
					}
				}
			} else {
				block();
			}
		}

	}

	public class shipOrder extends CyclicBehaviour {

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchContent("Ready for delivery");
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				System.out.println("Ready for delivery message received");
				for (SupplierManufacturerDelivery delivery : deliveries) {
					if (delivery.getDayDue() == day) {
						ACLMessage orderSend = new ACLMessage(ACLMessage.INFORM);
						orderSend.addReceiver(manufacturerAgent);
						orderSend.setLanguage(codec.getName());
						orderSend.setOntology(ontology.getName());
						try {
							getContentManager().fillContent(orderSend, delivery);
							send(orderSend);
							System.out.println("Delivery shipped to manufacturer");
						} catch (CodecException e) {
							e.printStackTrace();
						} catch (OntologyException e) {
							e.printStackTrace();
						}
					}
				}
			} else {
				block();
			}
		}
	}

	public class EndDayListener extends CyclicBehaviour {

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchContent("done");
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
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