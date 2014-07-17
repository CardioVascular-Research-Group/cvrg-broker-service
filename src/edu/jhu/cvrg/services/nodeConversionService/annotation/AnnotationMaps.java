package edu.jhu.cvrg.services.nodeConversionService.annotation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class AnnotationMaps {
public static Map<String, Integer> dynamicLeadNames = new ConcurrentHashMap<String, Integer>(){
	private static final long serialVersionUID = 6205600584630757507L;
		{
			/*put("I", 0);
			put("II", 1);
			put("III", 2);
			put("aVR", 3);
			put("aVL", 4);
			put("aVF", 5);
			put("V1", 6);
			put("V2", 7);
			put("V3", 8);
			put("V4", 9);
			put("V5", 10);
			put("V6", 11);
			put("VX", 12);
			put("VY", 13);
			put("VZ", 14);*/
        }
    };
    
public static final Map<String, String> schilAnoMap = new HashMap<String, String>(){
	private static final long serialVersionUID = 6205600584630757507L;
		{
			put("AXIS_QRS", "QRS_Wave_Complex_Axis");
			put("QRS", "QRS_Wave_Duration");
			put("QT", "QT_Interval");
			put("QTC", "QT_Corrected");
			put("Q_DUR", "Q_Wave_Duration");
			put("Q_AMPL", "Q_Wave_Amplitude");
			put("R_DUR", "R_Wave_Duration");
			put("R_AMPL", "R_Wave_Amplitude");
			put("S_DUR", "S_Wave_Duration");
			put("S_AMPL", "S_Wave_Amplitude");
        }
    };

public static final Map<String, String> ecgOntoMap = new HashMap<String, String>(){
	private static final long serialVersionUID = 3239200211633843945L;
		{
			put("PR_Interval", "#ECG_000000341");
			put("QT_Interval", "#ECG_000000682");
			put("QT_Corrected", "#ECG_000000701");
			put("QT_Corrected_Fridericias_Formula", "#ECG_000000040");
			put("Q_Wave_Duration", "#ECG_000000551");
			put("Q_Wave_Amplitude", "#ECG_000000652");
			put("R_Wave_Amplitude", "#ECG_000000750");
			put("R_Wave_Duration", "#ECG_000000597");
			put("S_Wave_Amplitude", "#ECG_000000107");
			put("S_Wave_Duration", "#ECG_000000491");
			put("QRS_Wave_Duration", "#ECG_000000072");
			put("QRS_Wave_Complex_Axis", "#ECG_000000838");
        }
    };
    
}