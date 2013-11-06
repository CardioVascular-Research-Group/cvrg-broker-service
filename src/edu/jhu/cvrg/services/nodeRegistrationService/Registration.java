package edu.jhu.cvrg.services.nodeRegistrationService;


import java.util.HashMap;
import java.util.Iterator;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;

import edu.jhu.cvrg.services.nodeRegistrationService.data.NodeBrokerData;



/**
 * @author mtoerpe1
 *
 */
public class Registration {
	
	static HashMap nodesMap = new HashMap();

	/**
	 * @param param0
	 * @return OMElement
	 */
	public org.apache.axiom.om.OMElement nodeRegistration(org.apache.axiom.om.OMElement param0) {
		OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://www.cvrgrid.org/nodeRegistrationService/", "nodeRegistrationService");
        OMElement nodeRegistrationStatus = fac.createOMElement("nodeRegistrationStatus", omNs);
        Iterator iterator = param0.getChildren();
        String userId = ((OMElement)iterator.next()).getText();
        String emailAddress = ((OMElement)iterator.next()).getText();
        String hostName = ((OMElement)iterator.next()).getText();
        String ipAddress = ((OMElement)iterator.next()).getText();
        String computable = ((OMElement)iterator.next()).getText();
        String service = ((OMElement)iterator.next()).getText();
        String loginDateTime = ((OMElement)iterator.next()).getText();
        System.out.println("hello from node reg");
        try {
        if(!nodesMap.containsKey(ipAddress)) {
        	NodeBrokerData nodeBrokerData = new NodeBrokerData();
        	nodeBrokerData.setUserId(userId);
        	nodeBrokerData.setEmailAddress(emailAddress);
        	nodeBrokerData.setNodeHost(hostName);
        	nodeBrokerData.setNodeIPaddress(ipAddress);
        	nodeBrokerData.setComputable(new Boolean(computable).booleanValue());
        	nodeBrokerData.setService(service);
        	nodeBrokerData.setLoginDateTime(new Long(loginDateTime).longValue());
        	nodesMap.put(ipAddress, nodeBrokerData);
        }
        } catch (Exception e) { System.out.println(e.toString()); }
        nodeRegistrationStatus.addChild(fac.createOMText("" + "SUCCESS"));
		return nodeRegistrationStatus;
	}

	/**
	 * @param param0
	 * @return OMElement
	 */
	public org.apache.axiom.om.OMElement nodeUnregistration(org.apache.axiom.om.OMElement param0) {
		OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://www.cvrgrid.org/nodeRegistrationService/", "nodeRegistrationService");
        OMElement nodeRegistrationStatus = fac.createOMElement("nodeRegistrationStatus", omNs);
        Iterator iterator = param0.getChildren();
        String ipAddress = ((OMElement)iterator.next()).getText();
        if(nodesMap.containsKey(ipAddress)) {
        	nodesMap.remove(ipAddress);
        }
        nodeRegistrationStatus.addChild(fac.createOMText("" + "SUCCESS"));
		return nodeRegistrationStatus;
	}

	/**
	 * @param param0
	 * @return OMElement
	 */
	public org.apache.axiom.om.OMElement listRegisteredRegionalNodes(
			org.apache.axiom.om.OMElement param0) {
		OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://www.cvrgrid.org/nodeRegistrationService/", "nodeRegistrationService");
		OMElement nodes = fac.createOMElement("nodes", omNs);

		OMElement node = fac.createOMElement("node", omNs);
		node.addChild(fac.createOMText(""));
		try {
	        Iterator iterator = param0.getChildren();
	        String scope = ((OMElement)iterator.next()).getText();
	        if(scope.equalsIgnoreCase("all")) {
	        	NodeBrokerData nodeBrokerData = null;
	        	Iterator keyIterator = nodesMap.keySet().iterator();
	        	while(keyIterator.hasNext()) {
	            	nodeBrokerData = (NodeBrokerData)nodesMap.get((String)keyIterator.next());
	    			OMElement userId = fac.createOMElement("userid", omNs);
	    			userId.addChild(fac.createOMText(nodeBrokerData.getUserId()));
	    			node.addChild(userId);
	    			OMElement email = fac.createOMElement("email", omNs);
	    			email.addChild(fac.createOMText(nodeBrokerData.getEmailAddress()));
	    			node.addChild(email);
					OMElement host = fac.createOMElement("host", omNs);
					host.addChild(fac.createOMText(nodeBrokerData.getNodeHost()));
					node.addChild(host);
					OMElement address = fac.createOMElement("address", omNs);
					address.addChild(fac.createOMText(nodeBrokerData.getNodeIPaddress()));
					node.addChild(address);
					OMElement computable = fac.createOMElement("computable", omNs);
					computable.addChild(fac.createOMText(new Boolean(nodeBrokerData.isComputable()).toString()));
					node.addChild(computable);
					OMElement service = fac.createOMElement("service", omNs);
					service.addChild(fac.createOMText(nodeBrokerData.getService()));
					node.addChild(service);
					OMElement loginDateTime = fac.createOMElement("logindatetime", omNs);
					loginDateTime.addChild(fac.createOMText(new Long(nodeBrokerData.getLoginDateTime()).toString()));
					node.addChild(loginDateTime);
					nodes.addChild(node);
	        	}
	        }
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return nodes;
	}

}