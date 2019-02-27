package com.ca.iso8583.steps;

import java.io.PrintWriter;

import org.adelbs.iso8583.clientserver.CallbackAction;
import org.adelbs.iso8583.clientserver.ISOConnection;
import org.adelbs.iso8583.clientserver.SocketPayload;
import org.adelbs.iso8583.exception.ParseException;
import org.adelbs.iso8583.helper.PayloadMessageConfig;
import org.adelbs.iso8583.protocol.ISOMessage;
import org.w3c.dom.Element;

import com.ca.iso8583.test.TestExecConnectionManager;
import com.ca.iso8583.vo.ConnectionInfoVO;
import com.itko.lisa.test.TestCase;
import com.itko.lisa.test.TestDefException;
import com.itko.lisa.test.TestExec;
import com.itko.lisa.test.TestNode;
import com.itko.lisa.test.TestRunException;
import com.itko.util.XMLUtils;


public class ISO8583RequestNode extends TestNode implements GenericCreateConnectionNodeInterface {

	private String stepContent = "";
	private String connectionInfo = "";
	private String connectionName = "";
	
	@Override
	public String getTypeName() throws Exception {
		return "ISO8583 Request";
	}

	@Override
	protected void execute(TestExec testExec) throws TestRunException {
		TestExecConnectionManager connManager = null;
		try {
			connManager = new TestExecConnectionManager(testExec, this.getConnectionName());
			if (!connectionInfo.equals("")) {
				ConnectionInfoVO connInfo = new ConnectionInfoVO(connectionInfo);
				connManager = new TestExecConnectionManager(testExec, connInfo.getName());
				connManager.connectTo(connInfo);
			}		
			final ISOConnection isoConnection = connManager.getCurrentConnection();
			
			String parsedXML = testExec.parseInState(stepContent);
			
			final TestExec te = testExec;
			final PayloadMessageConfig payloadMessageConfig = new PayloadMessageConfig(parsedXML);
			ISOMessage isoMessage = new ISOMessage(payloadMessageConfig.getMessageVO());
			
			byte[] data = payloadMessageConfig.getIsoConfig().getDelimiter().preparePayload(isoMessage, payloadMessageConfig.getIsoConfig());
			
			isoConnection.setIsoConfig(payloadMessageConfig.getIsoConfig());
			isoConnection.setCallback(new CallbackAction() {

				@Override
				public void dataReceived(SocketPayload payload) throws ParseException {
					payloadMessageConfig.updateFromPayload(payload.getData());
					te.setLastResponse(payloadMessageConfig.getXML());
				}

				@Override
				public void log(String log) { }

				@Override
				public void end() {
					if (te.getLastResponse() == null || te.getLastResponse().equals(""))
						te.setLastResponse("");
				}
				
			});
			
			if (!isoConnection.isConnected()) isoConnection.connect();
			isoConnection.send(new SocketPayload(data, isoConnection.getClientSocket()));
			isoConnection.processNextPayload(true, 0);
		}
		catch (Exception x) {
			connManager.cleanUp();
			throw new TestRunException(x.getMessage(), x);
		}
	}

	@Override
	public void initialize(TestCase testCase, Element element) throws TestDefException {
		for (int i = 0; i < element.getChildNodes().getLength(); i++) {
			if (element.getChildNodes().item(i).getNodeName().equals("ISO8583RequestStepContent"))
				this.stepContent = element.getChildNodes().item(i).getTextContent();
			else if (element.getChildNodes().item(i).getNodeName().equals("ISO8583ConnectionInfo"))
				this.connectionInfo = element.getChildNodes().item(i).getTextContent();
			else if (element.getChildNodes().item(i).getNodeName().equals("ISO8583ConnectionName"))
				this.connectionName = element.getChildNodes().item(i).getTextContent();
		}
	}

	public String getStepContent() {
		return stepContent;
	}

	public String getConnectionName() {
		return this.connectionName;
	}

	@Override
	public String getSavedConnectionInfo() {
		return connectionInfo;
	}
	
	@Override
	public void writeSubXML(PrintWriter pw) {
		//XMLUtils.streamTagAndChild(pw, "ISO8583ConnectionInfo", (String) getAttribute("ISO8583ConnectionInfo"));
		XMLUtils.streamTagAndChild(pw, "ISO8583RequestStepContent", stepContent);
		XMLUtils.streamTagAndChild(pw, "ISO8583ConnectionInfo", connectionInfo);
		XMLUtils.streamTagAndChild(pw, "ISO8583ConnectionName", connectionName);
		pw.flush();
	}

	@Override
	public boolean canDeployToVSE() {
		return true;
	}

}