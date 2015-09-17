package org.apodhrad.jeclipse.manager;

import static org.apodhrad.jdownload.manager.JDownloadManager.getName;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apodhrad.jdownload.manager.JDownloadManager;
import org.apodhrad.jdownload.manager.hash.Hash;
import org.apodhrad.jeclipse.manager.matcher.IsJavaExecutable;
import org.apodhrad.jeclipse.manager.util.FileSearch;
import org.apodhrad.jeclipse.manager.util.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author apodhrad
 *
 */
public class JBDS extends Eclipse {

	private static Logger log = LoggerFactory.getLogger(JBDS.class);

	public JBDS(String path) {
		super(path);
	}

	public JBDS(File file) {
		super(file);
	}

	public static JBDS installJBDS(File target, String url, Hash hash) throws IOException {
		String installer = getName(url);
		JDownloadManager manager = new JDownloadManager();
		manager.download(url, target, installer, false, hash);

		// Install JBDS
		File jarFile = new File(target, installer);
		String installationFile = null;
		try {
			installationFile = createInstallationFile(target, getJBDSVersion(installer));
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new RuntimeException("Exception occured during creating installation file");
		}

		// Switch IzPack mode to privileged on Windows
		if (OS.isWindows()) {
			System.setProperty("izpack.mode", "privileged");
		}

		new JarRunner(jarFile.getAbsolutePath(), installationFile).run();

		return new JBDS(new File(target, "jbdevstudio"));
	}

	private static String createInstallationFile(File target, String jbdsVersion) throws IOException {
		String jre = getJreLocation();
		if (jre == null) {
			throw new IllegalStateException("Cannot find JRE location!");
		}

		String dest = new File(target, "jbdevstudio").getAbsolutePath();

		String tempFile = new File(target, "/install.xml").getAbsolutePath();
		String targetFile = new File(target, "/installation.xml").getAbsolutePath();

		String sourceFile = "/install.xml";
		if (jbdsVersion != null && jbdsVersion.startsWith("8")) {
			sourceFile = "/install-8.xml";
		}
		if (jbdsVersion != null && jbdsVersion.startsWith("9")) {
			sourceFile = "/install-9.xml";
		}
		URL url = JBDS.class.getResource(sourceFile);

		FileUtils.copyURLToFile(url, new File(tempFile));
		BufferedReader in = new BufferedReader(new FileReader(tempFile));
		BufferedWriter out = new BufferedWriter(new FileWriter(targetFile));
		String line = null;
		while ((line = in.readLine()) != null) {
			out.write(line.replace("@DEST@", dest).replace("@JRE@", jre));
			out.newLine();
		}
		out.flush();
		out.close();
		in.close();

		new File(tempFile).delete();
		return targetFile;
	}

	private static String getJreLocation() {
		String jreLoc = null;

		// find jre location from java home
		String javaHome = System.getProperty("java.home");
		log.info("JRE: " + javaHome);
		FileSearch fileSearch = new FileSearch();
		List<File> result = fileSearch.find(new File(javaHome), new IsJavaExecutable());
		if (!result.isEmpty()) {
			jreLoc = result.get(0).getAbsolutePath();
		}

		return jreLoc;
	}

	public static String getJBDSVersion(String installer) {
		String[] part = installer.split("-");
		for (int i = 0; i < part.length; i++) {
			if (part[i].contains(".")) {
				return part[i];
			}
		}
		return null;
	}
}
