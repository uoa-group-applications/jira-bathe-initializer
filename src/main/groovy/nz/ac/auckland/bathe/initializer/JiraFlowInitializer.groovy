package nz.ac.auckland.bathe.initializer

import bathe.BatheInitializer
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyjarjarcommonscli.MissingArgumentException

import java.util.jar.Attributes
import java.util.jar.Manifest

/**
 * @author Kefeng Deng (kden022, k.deng@auckland.ac.nz)
 */
public class JiraFlowInitializer implements BatheInitializer {

	private static final String JIRA_FLOW = "jira.flow"

	private static final String JUMP_CLASS = "Jump-Class"

	private static final String RELEASE_NOTES = 'META-INF/release_notes.json'

	private static final String MANIFEST_MF = 'META-INF/MANIFEST.MF'

	private static final String DEFAULT_VERSION = '1.1-SNAPSHOT'

	@Override
	public int getOrder() {
		return 4 // Just after system property initializer (which is 3)
	}

	@Override
	public String getName() {
		return 'jira-flow-initializer'
	}

	@Override
	public String[] initialize(String[] args, String jumpClass) {
		boolean doTransition = Boolean.parseBoolean(System.getProperty(JIRA_FLOW))
		if (doTransition) {
			transitJiraReleaseNotes()
		}
		return args
	}

	protected void transitJiraReleaseNotes() {

		postMessageToRemote(getTransitionServerUrl(), [
				server : getLocalServerName(),
				version: getVersionFromManifest(),
				tickets: getReleaseNotesOfCurrentVersion()
		])

	}

	protected void postMessageToRemote(String targetUrl, Map releaseNotes) {
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

			// Don't care about the remote result
			//String text = connection.inputStream.text

			connection.inputStream.close()
			connection.disconnect()
		} catch (Exception ex) {

		}

	}

	public List<String> getReleaseNotesOfCurrentVersion() {
		List<String> releaseNotes = []
		try {
			URL releaseNoteResource = Thread.currentThread().contextClassLoader.getResource(RELEASE_NOTES)
			String version = getVersionFromManifest()
			def releaseNotesMap = new JsonSlurper().parse(releaseNoteResource)
			releaseNotesMap.each {
				if (version.equals(it.version)) {
					releaseNotes.add(it.issue)
				}
			}
		} catch (IOException ioe) {
			System.err.println("Cannot load release notes due to unexpected error : ${ioe}")
		} catch (NullPointerException npe) {
			System.err.println('Cannot found release notes in current artifact')
		}
		return releaseNotes
	}

	public String getVersionFromManifest() {
		try {
			URL resource = Thread.currentThread().getContextClassLoader().getResource(MANIFEST_MF)
			Manifest manifest = new Manifest(resource.openStream())
			Attributes attr = manifest.getMainAttributes()
			if (attr.getValue(JUMP_CLASS)) {
				return attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION)
			}
		} catch (Exception ex) {
			System.err.println("Cannot get current artifact version due to unexpected error: ${ex}")
		}
		return DEFAULT_VERSION
	}

	public String getLocalServerName() throws UnknownHostException {
		return InetAddress.getLocalHost().getHostName()
	}

	public String getTransitionServerUrl() {
		String transitionServerURl = System.getProperty('jira.transition.serverUrl', '')
		if (transitionServerURl) {
			return transitionServerURl
		}

		throw new MissingArgumentException('JIRA Transition Server URL is missing')
	}

}
