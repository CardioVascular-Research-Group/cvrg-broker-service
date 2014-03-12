package edu.jhu.cvrg.services.nodeConversionService.annotation.muse;

import org.jdom.Element;
import org.jdom.JDOMException;

import edu.jhu.cvrg.dbapi.dto.AnnotationDTO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * This object will contain the element values for the QRSTimesTypes tag in the MUSE XML.  The elements
 * stored here are based off of a DTD received long ago.  Values that are not found in any particular ECG will be
 * set to null.
 * 
 * These are annotations for the entire ECG
 * 
 * @author Brandon Benitez
 *
 */
public class QRSTimesTypes {
	
	private ArrayList<QRSTime> qrsTimes;
	private String globalRR;
	private String qtrGGR;

	public QRSTimesTypes() {
		qrsTimes = new ArrayList<QRSTime>();
	}
	
	public void parseQRSTimesTypes(Element qrsTimesTypesData) {
		// get each child element and store it in the appropriate variable
		List allChildren = qrsTimesTypesData.getChildren();
		
		if(!(allChildren.isEmpty())) {
			for (Object child : allChildren) {
				Element childElement = (Element) child;
			    if(childElement != null) {	
			    	// determine which tag it is and add it to the appropriate variable
					if(childElement.getName().equals("QRS")) {
						// TODO:  Capture the QRS data types
						List qrsDataMembers = childElement.getChildren();
						for(Object qrsData : qrsDataMembers) {
							Element qrsElement = (Element) qrsData;
							QRSTime newQRS = new QRSTime(qrsElement.getChildText("Number"), qrsElement.getChildText("Type"), qrsElement.getChildText("Time"));
							qrsTimes.add(newQRS);
						}
					}
					else if(childElement.getName().equals("GlobalRR")) {
						globalRR = childElement.getText();
					}
					else if(childElement.getName().equals("QTRGGR")) {
						qtrGGR = childElement.getText();
					}
			    }
			}
		}
	}
	
	public LinkedHashMap<String, String> getAllAnnotations() {
		LinkedHashMap<String, String> finalMap = new LinkedHashMap<String, String>();
		
		
		
		return finalMap;
	}

}
