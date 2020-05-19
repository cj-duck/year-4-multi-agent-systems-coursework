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
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import ontology.PhoneOntology;
import ontology.elements.CustomerOrder;
import ontology.elements.ManufacturerSupplierOrder;
import ontology.elements.SupplierManufacturerDelivery;

public class ManufacturerAgent extends Agent {
	private Codec codec = new SLCodec();
	private Ontology ontology = PhoneOntology.getInstance();
	private ArrayList<CustomerOrder> customerOrders = new ArrayList<>();
	private ArrayList<CustomerOrder> acceptedOrders = new ArrayList<>();
	private AID supplier1Agent;
	private AID supplier2Agent;
	private AID tickerAgent;
	private int day = 0;
	private int cumulativeProfit;
	private int dailyProfit;
	private int dailyPartsCost = 0;

	// Warehouse Stock
	private int WarehouseScreen5;
	private int WarehouseScreen7;
	private int WarehouseBattery2000;
	private int WarehouseBattery3000;
	private int WarehouseStorage64;
	private int WarehouseStorage256;
	private int WarehouseRam4;
	private int WarehouseRam8;

	@Override
	protected void setup() {
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		// get supplier agent's AID
		supplier1Agent = new AID("supplier1", AID.ISLOCALNAME);
		supplier2Agent = new AID("supplier2", AID.ISLOCALNAME);
		// add to yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("manufacturer-agent");
		sd.setName(getLocalName() + "-manufacturer-agent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		//
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

				SequentialBehaviour seq = new SequentialBehaviour();
				seq.addSubBehaviour(new ReceiveCustomerMessages());
				seq.addSubBehaviour(new readyToReceive());
				seq.addSubBehaviour(new receiveDelivery());
				seq.addSubBehaviour(new assembleAndShip());
				seq.addSubBehaviour(new orderSorting());
				seq.addSubBehaviour(new makeSupplierOrders());
				seq.addSubBehaviour(new EndDay());

				addBehaviour(seq);
			} else {
				block();
			}
		}
	}

	public class ReceiveCustomerMessages extends OneShotBehaviour {

		private ArrayList<AID> customerAgents = new ArrayList<>();
		int numberOfCustomers;
		CustomerOrder newOrder;

		@Override
		public void action() {
			// find number of customers
			DFAgentDescription template1 = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("customer-agent");
			template1.addServices(sd);
			try {
				customerAgents.clear();
				// search for agents of type customer-agent
				DFAgentDescription[] agentsType1 = DFService.search(myAgent, template1);
				numberOfCustomers = agentsType1.length;
				System.out.println("Number of Customers found by Manufacturer: " + numberOfCustomers);
			} catch (FIPAException e) {
				e.printStackTrace();
			}
			// receive daily order from each customer
			for (int i = 0; i < numberOfCustomers; i++) {
				MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchLanguage(codec.getName()),
						MessageTemplate.MatchOntology(ontology.getName()));
				ACLMessage msg = blockingReceive(mt);
				if (msg.getPerformative() == ACLMessage.INFORM) {
					try {
						ContentElement ce = getContentManager().extractContent(msg);
						if (ce instanceof CustomerOrder) {
							CustomerOrder order = (CustomerOrder) ce;
							order.setProfit(getProfit(order));
							customerOrders.add(order);
							System.out.println("Received Customer Order from Customer" + (i + 1));
						}
					} catch (CodecException e) {
						e.printStackTrace();
					} catch (UngroundedException e) {
						e.printStackTrace();
					} catch (OntologyException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public class orderSorting extends OneShotBehaviour {

		@Override
		public void action() {

			CustomerOrder multi1 = null;
			CustomerOrder multi2 = null;
			CustomerOrder single = null;
			int multiValue = 0;
			int singleValue = 0;
			// search for suitable pair of profitable orders
			for (CustomerOrder order : customerOrders) {
				for (CustomerOrder orderComp : customerOrders) {
					if (order != orderComp) {
						if ((order.getQuantity() + orderComp.getQuantity()) <= 50) {
							int orderValue = order.getProfit();
							int orderCompValue = orderComp.getProfit();
							if ((orderValue > 0) && (orderCompValue > 0)) {
								if ((orderValue + orderCompValue) > multiValue) {
									multiValue = orderValue + orderCompValue;
									multi1 = order;
									multi2 = orderComp;
								}
							}
						}
					}
				}
			}
			// check for a single order that would be more profitable
			for (CustomerOrder order : customerOrders) {
				if (order.getQuantity() <= 50) {
					if (order.getProfit() > multiValue) {
						if (order.getProfit() > singleValue) {
							singleValue = order.getProfit();
							single = order;
						}
					}
				}
			}
			if (multiValue > singleValue) {
				acceptedOrders.add(multi1);
				acceptedOrders.add(multi2);
				System.out.println("Two orders accepted");
			} else if (singleValue > multiValue) {
				acceptedOrders.add(single);
				System.out.println("Single Order Accepted");
			} else {
				System.out.println("No orders accepted");
			}
			customerOrders.clear();
		}
	}

	public class readyToReceive extends OneShotBehaviour {

		@Override
		public void action() {
			// tell supplier that it's ready to receive deliveries
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setContent("Ready for delivery");
			msg.addReceiver(supplier1Agent);
			myAgent.send(msg);
			System.out.println("Ready for delivery message sent to supplier1");
			doWait(1000);
		}

	}

	public class receiveDelivery extends OneShotBehaviour {

		@Override
		public void action() {
			dailyPartsCost = 0;
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchLanguage(codec.getName()),
					MessageTemplate.MatchOntology(ontology.getName()));
			ACLMessage delivery = receive(mt);
			if (delivery != null) {
				System.out.println("Delivery Received from Supplier1");
				try {
					ContentElement ce = getContentManager().extractContent(delivery);
					if (ce instanceof SupplierManufacturerDelivery) {
						// Add delivery stock to warehouse stock
						SupplierManufacturerDelivery newDelivery = (SupplierManufacturerDelivery) ce;
						WarehouseScreen5 = WarehouseScreen5 + newDelivery.getScreen5();
						WarehouseScreen7 = WarehouseScreen7 + newDelivery.getScreen7();
						WarehouseBattery2000 = WarehouseBattery2000 + newDelivery.getBattery2000();
						WarehouseBattery3000 = WarehouseBattery3000 + newDelivery.getBattery3000();
						WarehouseStorage64 = WarehouseStorage64 + newDelivery.getStorage64();
						WarehouseStorage256 = WarehouseStorage256 + newDelivery.getStorage256();
						WarehouseRam4 = WarehouseRam4 + newDelivery.getRam4();
						WarehouseRam8 = WarehouseRam8 + newDelivery.getRam8();
						dailyPartsCost = dailyPartsCost + newDelivery.getCost();
					}
				} catch (CodecException e) {
					e.printStackTrace();
				} catch (UngroundedException e) {
					e.printStackTrace();
				} catch (OntologyException e) {
					e.printStackTrace();
				}

			}
		}
	}

	public class assembleAndShip extends OneShotBehaviour {

		@Override
		public void action() {
			int count = 0;
			int devicesAssembled = 0;
			for (CustomerOrder order : acceptedOrders) {
				int quantity = order.getQuantity();
				int value = order.getPrice() * quantity;
				int penalty = order.getPenalty();
				int totalPenalty;
				int partsValue = 0;
				// check warehouse to see if there's enough components to assemble order
				if (order.getType() == 0) {
					if ((quantity <= WarehouseScreen5) && (quantity <= WarehouseBattery2000)) {
						count++;
					}
				} else if ((quantity <= WarehouseScreen7) && (quantity <= WarehouseBattery3000)) {
					count++;
				}
				if (order.getStorage() == 64) {
					if (quantity <= WarehouseStorage64) {
						count++;
					}
				} else if (quantity <= WarehouseStorage256) {
					count++;
				}
				if (order.getRam() == 4) {
					if (quantity <= WarehouseRam4) {
						count++;
					}
				} else if (quantity <= WarehouseRam8) {
					count++;
				}
				// assemble if there's enough stock in warehouse and assembly wouldnt result in over 50 phones being made that day
				if ((count == 3) && ((50 - devicesAssembled) >= quantity)) {
					// Check phone type and use appropriate components to assemble
					if (order.getType() == 0) {
						WarehouseScreen5 = WarehouseScreen5 - quantity;
						WarehouseBattery2000 = WarehouseBattery2000 - quantity;
					} else {
						WarehouseScreen7 = WarehouseScreen7 - quantity;
						WarehouseBattery3000 = WarehouseBattery3000 - quantity;
					}
					if (order.getStorage() == 64) {
						WarehouseStorage64 = WarehouseStorage64 - quantity;
					} else {
						WarehouseStorage256 = WarehouseStorage256 - quantity;
					}
					if (order.getRam() == 4) {
						WarehouseRam4 = WarehouseRam4 - quantity;
					} else {
						WarehouseRam8 = WarehouseRam8 - quantity;
					}
					if (order.getDue() < day) {
						totalPenalty = penalty * (day - order.getDue());
					} else {
						totalPenalty = 0;
					}
					value = value - totalPenalty;
					dailyProfit = dailyProfit + value;
					System.out.println("Order completed! Value: £" + value);
					count = 0;
				}
				order.setCompleted(true);
				devicesAssembled = devicesAssembled + quantity;

			}
			devicesAssembled = 0;
			acceptedOrders.clear();
		}
	}

	public class makeSupplierOrders extends OneShotBehaviour {

		private int screen5 = 0;
		private int screen7 = 0;
		private int storage64 = 0;
		private int storage256 = 0;
		private int ram4 = 0;
		private int ram8 = 0;
		private int battery2000 = 0;
		private int battery3000 = 0;
		private int quantity;
		private int numberOfAcceptedOrders = 0;

		@Override
		public void action() {
			for (CustomerOrder order : acceptedOrders) {
				numberOfAcceptedOrders++;
			}
			// check stock needed for each accepted order
			if (numberOfAcceptedOrders > 0) {
				for (CustomerOrder order : acceptedOrders) {
					quantity = order.getQuantity();
					if (order.getType() == 0) {
						screen5 = screen5 + quantity;
						battery2000 = battery2000 + quantity;

					} else {
						screen7 = screen7 + quantity;
						battery3000 = battery3000 + quantity;
					}
					if (order.getRam() == 4) {
						ram4 = ram4 + quantity;
					} else {
						ram8 = ram8 + quantity;
					}
					if (order.getStorage() == 64) {
						storage64 = storage64 + quantity;
					} else {
						storage256 = storage256 + quantity;
					}
				}
				// make order
				ManufacturerSupplierOrder manufacturerOrder = new ManufacturerSupplierOrder();
				manufacturerOrder.setOrderFrom(myAgent.getAID());
				manufacturerOrder.setDayMade(day);
				manufacturerOrder.setScreen5(screen5);
				manufacturerOrder.setScreen7(screen7);
				manufacturerOrder.setBattery2000(battery2000);
				manufacturerOrder.setBattery3000(battery3000);
				manufacturerOrder.setStorage64(storage64);
				manufacturerOrder.setStorage256(storage256);
				manufacturerOrder.setRam4(ram4);
				manufacturerOrder.setRam8(ram8);

				// Send order to supplier
				ACLMessage order = new ACLMessage(ACLMessage.REQUEST);
				order.addReceiver(supplier1Agent);
				order.setLanguage(codec.getName());
				order.setOntology(ontology.getName());
				try {
					getContentManager().fillContent(order, manufacturerOrder);
					send(order);
					System.out.println("Manufacturer sent order to Supplier1");
					doWait(1000);
				} catch (CodecException e) {
					e.printStackTrace();
				} catch (OntologyException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public class EndDay extends OneShotBehaviour {
		@Override
		public void action() {
			// send a message to each seller that we have finished
			ACLMessage manufacturerDone = new ACLMessage(ACLMessage.INFORM);
			manufacturerDone.setContent("done");
			manufacturerDone.addReceiver(supplier1Agent);
			myAgent.send(manufacturerDone);
			ACLMessage manufacturerDone2 = new ACLMessage(ACLMessage.INFORM);
			manufacturerDone2.setContent("done");
			manufacturerDone2.addReceiver(supplier2Agent);
			myAgent.send(manufacturerDone2);
			// End of day notifications and profit calculations
			int warehouseStorageCosts = 5 * (WarehouseScreen5 + WarehouseScreen7 + WarehouseBattery2000
					+ WarehouseBattery3000 + WarehouseStorage64 + WarehouseStorage256 + WarehouseRam4 + WarehouseRam8);
			dailyProfit = dailyProfit - warehouseStorageCosts - dailyPartsCost;
			cumulativeProfit = cumulativeProfit + dailyProfit;
			System.out.println("Day's profit: £" + dailyProfit);
			dailyProfit = 0;
			System.out.println("Cumulative profit: £" + cumulativeProfit);
			// Send ticker end of day message
			ACLMessage dayDone = new ACLMessage(ACLMessage.INFORM);
			dayDone.addReceiver(tickerAgent);
			dayDone.setContent("done");
			myAgent.send(dayDone);
		}

	}

	public int getProfit(CustomerOrder order) {
		int partsPrice = 0;
		int orderValue = 0;
		int orderPartsPrice = 0;
		int orderProfit = 0;
		if (order.getType() == 0) {
			partsPrice = partsPrice + 170;
		} else {
			partsPrice = partsPrice + 250;
		}
		if (order.getRam() == 4) {
			partsPrice = partsPrice + 30;
		} else {
			partsPrice = partsPrice + 60;
		}
		if (order.getStorage() == 64) {
			partsPrice = partsPrice + 25;
		} else {
			partsPrice = partsPrice + 50;
		}
		orderValue = order.getQuantity() * order.getPrice();
		orderPartsPrice = order.getQuantity() * partsPrice;
		orderProfit = orderValue - orderPartsPrice;
		return orderProfit;
	}

}
