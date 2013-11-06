package edu.jhu.cvrg.services.nodeRegistrationService.data;

/**
 * @author wgirten1
 *
 */
public class NodeBrokerData {
	
	private String userId;
	private String emailAddress;
	private String nodeHost;
	private String nodeIPaddress;
	private boolean isComputable;
	private String service;
	private long loginDateTime;

	
	/**
	 * @return String
	 */
	public String getUserId() {return userId;}
	
	/**
	 * @param userId
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	/**
	 * @return String
	 */
	public String getEmailAddress() {return emailAddress;}
	
	/**
	 * @param emailAddress
	 */
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	/**
	 * @param host
	 */
	public void setNodeHost(String host) {nodeHost = host;}
	/**
	 * @return String
	 */
	public String getNodeHost() {return nodeHost;}
	
	/**
	 * @param ipAddress
	 */
	public void setNodeIPaddress(String ipAddress) {nodeIPaddress = ipAddress;}
	/**
	 * @return String
	 */
	public String getNodeIPaddress() {return nodeIPaddress;}
	
	/**
	 * @return boolean
	 */
	public boolean isComputable() {return isComputable;}
	/**
	 * @param isComputable
	 */
	public void setComputable(boolean isComputable) {
		this.isComputable = isComputable;
	}

	/**
	 * @return String
	 */
	public String getService() {return service;}
	/**
	 * @param service
	 */
	public void setService(String service) {
		this.service = service;
	}
	
	/**
	 * @return long
	 */
	public long getLoginDateTime() {return loginDateTime;}
	
	/**
	 * @param loginDateTime
	 */
	public void setLoginDateTime(long loginDateTime) {
		this.loginDateTime = loginDateTime;
	}
    
}