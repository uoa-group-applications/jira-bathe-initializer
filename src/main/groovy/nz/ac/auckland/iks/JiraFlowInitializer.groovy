package nz.ac.auckland.iks

import bathe.BatheInitializer
import groovy.json.JsonOutput
import groovy.transform.CompileStatic

/**
 * @author Kefeng Deng (kden022, k.deng@auckland.ac.nz)
 */
@CompileStatic
public class JiraFlowInitializer implements BatheInitializer {

	private static final String JIRA_FLOW_D = "-Djira.flow="

	@Override
	public int getOrder() {
		return
	}

	@Override
	public String getName() {
		return "bathe-jira-flow"
	}

	@Override
	public String[] initialize(String[] args, String jumpClass) {
		for (String argument : args) {
			if (argument.startsWith(JIRA_FLOW_D)) {
				boolean doTransition = Boolean.parseBoolean(argument.replace(JIRA_FLOW_D, ""))
				if (doTransition) {
					transitJiraReleaseNotes()
				}
			}
		}
		return args
	}

	protected String getLocalServerName() throws UnknownHostException {
		return InetAddress.getLocalHost().getHostName()
	}

	protected void transitJiraReleaseNotes() {

		ReleaseNotes releaseNotes = new ReleaseNotes()
		releaseNotes.server = getLocalServerName()


	}

	protected void postMessageToRemote(String targetUrl, List<ReleaseNotes> releaseNotes) {
		try {
			URL host = new URL(targetUrl)
			HttpURLConnection connection = (HttpURLConnection) host.openConnection()
			connection.setDoOutput(true)
			connection.setRequestMethod("POST")
			connection.setRequestProperty("Accept", "application/json")
			connection.setRequestProperty("Content-Type", "application/json")

			OutputStreamWriter outputStream = new OutputStreamWriter(connection.outputStream)
			outputStream.write(JsonOutput.toJson(releaseNotes))
			outputStream.flush()
			outputStream.close()

			String text = connection.inputStream.text
			connection.inputStream.close()
			connection.disconnect()
		} catch (Exception ex) {

		}

	}

	public static final class ReleaseNotes {
		String server
		String version
		List<String> tickets
	}

}
