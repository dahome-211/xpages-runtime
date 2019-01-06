package org.openntf.openliberty.xpages.platform;

import com.ibm.commons.platform.WebAppServerPlatform;
import com.ibm.commons.util.StringUtil;
import com.ibm.designer.runtime.Application;
import com.ibm.designer.runtime.ApplicationException;
import com.ibm.domino.napi.c.C;
import com.ibm.domino.napi.c.Os;
import com.ibm.domino.xsp.module.nsf.platform.AbstractNotesDominoPlatform;
import com.ibm.domino.xsp.module.nsf.platform.JSDebuggerRuntime;
import com.ibm.jscript.JSContext;
import com.ibm.xsp.context.DojoLibraryFactory;
import com.ibm.xsp.domino.context.DominoDojo;
import com.ibm.xsp.model.domino.DominoUtils;

import lotus.notes.NotesThread;

import javax.servlet.ServletContext;

import org.openntf.openliberty.xpages.LibertyAppExecutionContext;
import org.openntf.openliberty.xpages.LibertyApplication;

import java.io.File;
import java.util.Properties;

public class LibertyPlatform extends WebAppServerPlatform {
	private static ServletContext servletContext;

	public static void initContext(ServletContext servletContext) {
		LibertyPlatform.servletContext = servletContext;
	}

	public static final String DOMINO_ROOT_PREFIX = "domino";
	public static final String DOMINO_RESOURCE_ROOT = "/.ibmxspres/domino";
	private File installationDirectory;
	private File dataDirectory;
	private File sharedDataDirectory;
	private File userDataDirectory;
	private File propertiesDirectory;
	private File xspDirectory;
	private File nsfDirectory;
	private File styleKitsDirectory;
	private File serverDirectory;
	private File dominoDirectory;
	private File notesIconsDirectory;
	private File jsDirectory;
	private Properties xspProperties;

	public LibertyPlatform() {
		super();
		
		NotesThread.sinitThread();
		C.initLibrary(null);

		installationDirectory = new File(Os.OSGetExecutableDirectory());
		dataDirectory = new File(Os.OSGetDataDirectory());
		sharedDataDirectory = new File(Os.OSGetSharedDataDirectory());
		userDataDirectory = dataDirectory;
		propertiesDirectory = new File(dataDirectory, "properties");
		xspDirectory = new File(installationDirectory, "xsp");
		nsfDirectory = new File(xspDirectory, "nsf");
		styleKitsDirectory = new File(nsfDirectory, "themes");
		serverDirectory = new File(dataDirectory, StringUtil.replace(AbstractNotesDominoPlatform.DOMINO_ROOT_PREFIX + "/java/xsp", '/', File.separatorChar));
		dominoDirectory = new File(dataDirectory, StringUtil.replace(AbstractNotesDominoPlatform.DOMINO_ROOT_PREFIX + "/html", '/', File.separatorChar));
		notesIconsDirectory = new File(dataDirectory, StringUtil.replace(AbstractNotesDominoPlatform.DOMINO_ROOT_PREFIX + "/icons", '/', File.separatorChar));
		jsDirectory = new File(dataDirectory, AbstractNotesDominoPlatform.DOMINO_ROOT_PREFIX + "/js/");
		this.xspProperties = this.loadStaticProperties();
		DominoDojo.installDominoFactory(this.jsDirectory);
		DojoLibraryFactory.initializeLibraries();
		this.initJSEngine();
	}
	
	protected void initJSEngine() {
		JSContext.ENABLE_JSDEBUGGER = this.isJavaDebugEnabled() && this.isJavaScriptDebugEnabled();
		if (JSContext.ENABLE_JSDEBUGGER) {
			JSContext.setDebuggerRuntime(new JSDebuggerRuntime());
		}

	}

	protected boolean isJavaDebugEnabled() {
		String var1 = DominoUtils.getEnvironmentString(AbstractNotesDominoPlatform.PROP_JAVADEBUG);
		return var1 != null ? StringUtil.equals(var1.trim(), "1") : false;
	}

	protected boolean isJavaScriptDebugEnabled() {
		String var1 = DominoUtils.getEnvironmentString(AbstractNotesDominoPlatform.PROP_JAVASCRIPTDEBUG);
		return var1 != null ? StringUtil.equals(var1.trim(), "1") : false;
	}

	private Properties loadStaticProperties() {
		return new Properties();
	}

	@Override
	public Object getObject(String s) {
		switch (StringUtil.toString(s)) {
		case "com.ibm.xsp.designer.ApplicationFinder":
			return (Application.IApplicationFinder) () -> {
				try {
					LibertyAppExecutionContext ctx = new LibertyAppExecutionContext(servletContext);
					return new LibertyApplication(ctx);
				} catch (ApplicationException e) {
					throw new RuntimeException(e);
				}
			};
		default:
			return super.getObject(s);
		}
	}

	@Override
	public File getInstallationDirectory() {
		return installationDirectory;
	}

	@Override
	public File getResourcesDirectory() {
		return nsfDirectory;
	}

	public File getGlobalResourceFile(String path) {
		if(path.startsWith("/stylekits/")) {
			return new File(styleKitsDirectory, path.substring("/stylekits/".length()));
		}
		if(path.startsWith("/server/")) {
			return new File(serverDirectory, path.substring("/server/".length()));
		}
		if(path.startsWith("/domino/")) {
			return new File(dominoDirectory, path.substring("/domino/".length()));
		}
		if(path.startsWith("/global/")) {
			return new File(serverDirectory, path.substring("/global/".length()));
		}
		if(path.startsWith("/properties/")) {

			if (userDataDirectory != null) {
				File localFile = new File(userDataDirectory, "properties/" + path.substring("/properties/".length()));
				if (localFile.exists()) {
					return localFile;
				}
			}

			if (sharedDataDirectory != null) {
				File localFile = new File(sharedDataDirectory, "properties/" + path.substring("/properties/".length()));
				if (localFile.exists()) {
					return localFile;
				}
			}

			return new File(propertiesDirectory, path.substring("/properties/".length()));
		}
		if(path.startsWith("/icons/")) {
			return new File(notesIconsDirectory, path.substring("/icons/".length()));
		}
		return super.getGlobalResourceFile(path);
	}
	
	@Override
	public String getProperty(String prop) {
		String var2;
		if (this.xspProperties != null) {
			var2 = this.xspProperties.getProperty(prop);
			if (StringUtil.isNotEmpty(var2)) {
				return var2;
			}
		}
		return super.getProperty(prop);
	}
	
	@Override
	public boolean isPlatform(String name) {
		switch(StringUtil.toString(name)) {
		case "Domino":
			// Sure I am
			return true;
		default:
			return super.isPlatform(name);
		}
	}
	
}
