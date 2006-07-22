package org.contract4j5.configurator;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.contract4j5.aspects.Contract4J;
import org.contract4j5.aspects.ConstructorBoundaryConditions;
import org.contract4j5.aspects.MethodBoundaryConditions;
import org.contract4j5.aspects.InvariantConditions;
import org.contract4j5.interpreter.ExpressionInterpreter;
import org.contract4j5.ContractEnforcer;
import org.contract4j5.testexpression.DefaultTestExpressionMaker;
import org.contract4j5.testexpression.ParentTestExpressionFinder;
import org.contract4j5.util.reporter.Reporter;
import org.contract4j5.util.reporter.Severity;
import org.contract4j5.util.reporter.WriterReporter;

/**
 * Configure Contract4J using properties.  The System properties
 * will be read <em>first</em>, followed by an optional Properties object.
 * @author Dean Wampler <mailto:dean@aspectprogramming.com>
 */
public class PropertiesConfigurator extends AbstractConfigurator {
	public static String PROPERTY_PREFIX = "org.contract4j5.";
	public static final String[] enabledPropertyKeys = new String[] {
		"Contract", "Pre", "Post", "Invar",
	};
	public static enum KnownBeanKeys {
		GlobalReporter,
		GlobalReporterThreshold,
		GlobalWriterReporterWriter,
		GlobalWriterReporterOutputStream, 
		ContractEnforcer,
		ContractEnforcerIncludeStackTrace,
		ExpressionInterpreter,
		ExpressionInterpreterEmptyTestExpressionsValid,
		ExpressionInterpreterOptionalKeywordSubstitutions,
		DefaultFieldInvarTestExpressionMaker,
		DefaultFieldCtorInvarTestExpressionMaker,
		DefaultMethodInvarTestExpressionMaker,
		DefaultCtorInvarTestExpressionMaker,
		DefaultTypeInvarTestExpressionMaker,
		DefaultCtorPreTestExpressionMaker,
		DefaultCtorPostReturningVoidTestExpressionMaker,
		DefaultMethodPreTestExpressionMaker,
		DefaultMethodPostTestExpressionMaker,
		DefaultMethodPostReturningVoidTestExpressionMaker,
		CtorParentTestExpressionFinder,
		MethodParentTestExpressionFinder,
		MethodInvarParentTestExpressionFinder,
		CtorInvarParentTestExpressionFinder,
		TypeInvarParentTestExpressionFinder
	};
	
	private Properties properties = null;
	public Properties getProperties() { return properties; }
	public void       setProperties(Properties p) { properties = p; }
	
	/**
	 * Configure the system with a properties object. The System properties
	 * will be read <em>first</em>.
	 * @param properties
	 */
	public PropertiesConfigurator(Properties properties) {
		setProperties(properties);
	}
	
	/**
	 * Configure the system with the System properties only. 
	 */
	public PropertiesConfigurator() {}
	
	/**
	 * Configurator that reads the specification from the system properties.
	 * Provides only partial support for the configuration options.
	 * @see org.contract4j5.configurator.Configurator#configure()
	 */
	protected void doConfigure() {
		foundContractDisableProperty = false;  // starting over...
		errors.setLength(0);
		Properties properties = System.getProperties();
		initSystemProps(properties);
		properties = getProperties();
		if (properties != null) {
			initSystemProps(properties);
		}
	}

	private Reporter globalReporter = null;
	private Severity globalReporterThreshold = null;
	private java.io.Writer globalWriterReporterWriter = null;
	private java.io.OutputStream globalWriterReporterOutputStream = null;
	private boolean foundContractDisableProperty = false;
	private ContractEnforcer ce = null; 
	private boolean includeStackTrace = false;
	private boolean includeStackTraceWasSet = false;
	private ExpressionInterpreter ei = null; 
	private boolean emptyTestExprsValid = false;
	private boolean emptyTestExprsValidWasSet = false;
	private Map<String,String> optionalKeywordSubstitutions;

	private StringBuffer errors = new StringBuffer(1024);
	
	protected void initSystemProps(Properties properties) {
		for (Object key: properties.keySet()) {
			String k = (String) key;
			if (k.startsWith(PROPERTY_PREFIX)) {
				String value = properties.getProperty(k);
				if (! processEnableTestTypeProperty(k, value)) {
					if (! processBean(k, value)) {
						recordUnknownPropertyError(k, value);
					}
				}
			}
		}
		configureContractEnforcer();
		configureGlobalReporter();
		if (errors.length() > 0) {
			try {
				getReporter().report (Severity.ERROR, this.getClass(), errors.toString());
			} catch (NullPointerException npe) {
				System.err.println("No \"reporter\" was defined using the System Properties (See PropertiesConfigurator.java)");
				System.err.print(Severity.ERROR.name() + ": " + errors.toString());
			}
		}
	}

	protected boolean processEnableTestTypeProperty(String propKey, String propValue) {
		for (int i = 0; i < enabledPropertyKeys.length; i++) {
			if (propKey != null && 
				propKey.equals(PROPERTY_PREFIX+enabledPropertyKeys[i])) {
				try {
					boolean value = convertToBoolean(propValue);
					if (i == 0) {	// The overall "contract" key?
						Contract4J.setEnabled(Contract4J.TestType.Pre,   value);
						Contract4J.setEnabled(Contract4J.TestType.Post,  value);
						Contract4J.setEnabled(Contract4J.TestType.Invar, value);
						foundContractDisableProperty = !value;
					} else {
						if (! foundContractDisableProperty)
							Contract4J.setEnabled(Contract4J.TestType.values()[i-1], value);
					}
				} catch (IllegalArgumentException iae) {
					recordEnableTestTypeError(propKey, propValue);
				}
				return true; // found and processed one of these options.
			}
		}
		return false;
	}

	protected boolean processBean(String beanName, String propValue) {
		if (propValue == null || propValue.trim().length() == 0) {
			recordEnableTestTypeError(beanName, propValue); 
			return true;  // we handled it here...
		}
		for (KnownBeanKeys beanKey: KnownBeanKeys.values()) {
			if (beanName != null &&	beanName.equals(PROPERTY_PREFIX+beanKey.name())) {
				try {
					switch (beanKey) {
					case GlobalReporter:
					{
						globalReporter = (Reporter) propertyToObject(propValue);
					}
					break;
					case GlobalReporterThreshold:
					{
						globalReporterThreshold = propertyToReporterThreshold(propValue);
					}
					break;
					case GlobalWriterReporterWriter:
					{
						globalWriterReporterWriter = (java.io.Writer) propertyToObject(propValue);						
					}
					break;
					case GlobalWriterReporterOutputStream:
					{
						globalWriterReporterOutputStream = (java.io.OutputStream) propertyToObject(propValue);
					}
					break;
					case ContractEnforcer:
					{
						ce = (ContractEnforcer) propertyToObject(propValue);
					}
					break;
					case ContractEnforcerIncludeStackTrace:
					{
						includeStackTrace = convertToBoolean(propValue);
						includeStackTraceWasSet = true;
					}
					break;
					case ExpressionInterpreter:
					{
						ei = (ExpressionInterpreter) propertyToObject(propValue);
					}
					break;
					case ExpressionInterpreterEmptyTestExpressionsValid:
					{
						emptyTestExprsValid = convertToBoolean(propValue);
						emptyTestExprsValidWasSet = true;
					}
					break;
					case ExpressionInterpreterOptionalKeywordSubstitutions:
					{
						processOptionalKeywordSubstitutions(propValue);
					}
					break;
					case DefaultFieldInvarTestExpressionMaker:
					{
						DefaultTestExpressionMaker dtem = 
							(DefaultTestExpressionMaker) propertyToObject(propValue);
								InvariantConditions.InvariantFieldConditions.setDefaultFieldInvarTestExpressionMaker(dtem);
					}	
					break;
					case DefaultFieldCtorInvarTestExpressionMaker:
					{
						DefaultTestExpressionMaker dtem = 
							(DefaultTestExpressionMaker) propertyToObject(propValue);
						InvariantConditions.InvariantFieldCtorConditions.setDefaultFieldInvarTestExpressionMaker(dtem);
					}
					break;
					case DefaultMethodInvarTestExpressionMaker:
					{
						DefaultTestExpressionMaker dtem = 
							(DefaultTestExpressionMaker) propertyToObject(propValue);
						InvariantConditions.InvariantMethodConditions.setDefaultMethodInvarTestExpressionMaker(dtem);
					}
					break;
					case DefaultCtorInvarTestExpressionMaker:
					{
						DefaultTestExpressionMaker dtem = 
							(DefaultTestExpressionMaker) propertyToObject(propValue);
						InvariantConditions.InvariantCtorConditions.setDefaultCtorInvarTestExpressionMaker(dtem);
					}
					break;
					case DefaultTypeInvarTestExpressionMaker:
					{
						DefaultTestExpressionMaker dtem = 
							(DefaultTestExpressionMaker) propertyToObject(propValue);
						InvariantConditions.InvariantTypeConditions.setDefaultTypeInvarTestExpressionMaker(dtem);
					}
					break;
					case DefaultCtorPreTestExpressionMaker:
					{
						DefaultTestExpressionMaker dtem = 
							(DefaultTestExpressionMaker) propertyToObject(propValue);
						ConstructorBoundaryConditions.setDefaultPreTestExpressionMaker(dtem);
					}
					break;
					case DefaultCtorPostReturningVoidTestExpressionMaker:
					{
						DefaultTestExpressionMaker dtem = 
							(DefaultTestExpressionMaker) propertyToObject(propValue);
						ConstructorBoundaryConditions.setDefaultPostReturningVoidTestExpressionMaker(dtem);
					}
					break;
					case DefaultMethodPreTestExpressionMaker:
					{
						DefaultTestExpressionMaker dtem = 
							(DefaultTestExpressionMaker) propertyToObject(propValue);
						MethodBoundaryConditions.setDefaultPreTestExpressionMaker(dtem);
					}
					break;
					case DefaultMethodPostTestExpressionMaker:
					{
						DefaultTestExpressionMaker dtem = 
							(DefaultTestExpressionMaker) propertyToObject(propValue);
						MethodBoundaryConditions.setDefaultPostTestExpressionMaker(dtem);
					}
					break;
					case DefaultMethodPostReturningVoidTestExpressionMaker:
					{
						DefaultTestExpressionMaker dtem = 
							(DefaultTestExpressionMaker) propertyToObject(propValue);
						MethodBoundaryConditions.setDefaultPostReturningVoidTestExpressionMaker(dtem);
					}
					break;
					case CtorParentTestExpressionFinder:
					{
						ParentTestExpressionFinder ptef = (ParentTestExpressionFinder) propertyToObject(propValue);
						ConstructorBoundaryConditions.setParentTestExpressionFinder(ptef);
					}
					break;
					case MethodParentTestExpressionFinder:
					{
						ParentTestExpressionFinder ptef = (ParentTestExpressionFinder) propertyToObject(propValue);
						MethodBoundaryConditions.setParentTestExpressionFinder(ptef);
					}
					break;
					case MethodInvarParentTestExpressionFinder:
					{
						ParentTestExpressionFinder ptef = (ParentTestExpressionFinder) propertyToObject(propValue);
						InvariantConditions.InvariantMethodConditions.setParentTestExpressionFinder(ptef);
					}
					break;
					case CtorInvarParentTestExpressionFinder:
					{
						ParentTestExpressionFinder ptef = (ParentTestExpressionFinder) propertyToObject(propValue);
						InvariantConditions.InvariantCtorConditions.setParentTestExpressionFinder(ptef);
					}
					break;
					case TypeInvarParentTestExpressionFinder:
					{
						ParentTestExpressionFinder ptef = (ParentTestExpressionFinder) propertyToObject(propValue);
						InvariantConditions.InvariantTypeConditions.setParentTestExpressionFinder(ptef);
					}
					break;
					default:
						throw new UnsupportedOperationException("Forgot to support bean type \""+beanKey+"\"!");
					}
				} catch (Exception ex) {
					recordBeanPropertyError(beanName, propValue, ex);
				}
				return true; // found and processed one of these options.
			}
		}
		return false;		
	}

	private Severity propertyToReporterThreshold(String propValue) {
		Severity s = Severity.parse(propValue);
		if (s == null) {
			errors.append("No Reporter Severity matching string \"");
			errors.append(propValue);
			errors.append("\". Ignored.\n");
		}
		return s;
	}

	private void processOptionalKeywordSubstitutions(String propValue) {
		optionalKeywordSubstitutions = new HashMap<String,String>();
		String s = propValue.trim();
		for (String nvPair: s.split("\\s*,\\s*")) {
			String[] pair = nvPair.split("\\s*=\\s*");
			if (pair[0].length() == 0) {
				errors.append("keyword substitution format error: name empty in a name value pair.");
				errors.append("Map definition string is \"");
				errors.append(propValue);
				errors.append("\". Format should be \"name1=value1, name2=value2, ...\"\n");
			} else {
				optionalKeywordSubstitutions.put(pair[0], pair[1]);
			}
		}
	}

	private Object propertyToObject(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class clazz = Class.forName(className);
		Object object = clazz.newInstance();
		return object;
	}

	private void configureContractEnforcer() {
		if (ce == null) {
			ce = Contract4J.getContractEnforcer();
		} else {
			Contract4J.setContractEnforcer(ce);
		}
		if (ce != null) {
			if (includeStackTraceWasSet) {
				ce.setIncludeStackTrace(includeStackTrace);
			}
			if (ei == null) {
				ei = ce.getExpressionInterpreter();
			} else {
				ce.setExpressionInterpreter(ei);
			}
			if (ei != null) {
				if (emptyTestExprsValidWasSet) {
					ei.setTreatEmptyTestExpressionAsValidTest(emptyTestExprsValid);
				}
				if (optionalKeywordSubstitutions != null) {
					ei.setOptionalKeywordSubstitutions(optionalKeywordSubstitutions);
				}
			}
		}
	}

	private void configureGlobalReporter() {
		setReporter(globalReporter);
		if (globalReporterThreshold != null)
			globalReporter.setThreshold(globalReporterThreshold);
		if ((globalWriterReporterWriter != null ||
			globalWriterReporterOutputStream != null) && 
			!(globalReporter instanceof WriterReporter)) {
			errors.append("The \"global\" reporter is not a \"WriterReporter\", so the value for the java.io.Writer or the java.io.OutputStream is ignored.\n");
		} else {
			if (globalWriterReporterWriter != null) {
				WriterReporter wr = (WriterReporter) globalReporter;
				wr.setWriters(globalWriterReporterWriter);
			}
			if (globalWriterReporterOutputStream != null) {
				WriterReporter wr = (WriterReporter) globalReporter;
				wr.setStreams(globalWriterReporterOutputStream);
				if (globalWriterReporterWriter != null) {
					errors.append("Both a global java.io.OutputStream and java.io.Writer specified for the global \"Reporter\". The OutputStream will be used.\n");
				}
			}
		}
	}
	
	protected void recordUnknownPropertyError(String k, String value) {
		errors.append("Unrecognized property key \"");
		errors.append(k);
		errors.append("\" (value = \"");
		errors.append(value);
		errors.append("\") ignored.\n");
	}

	private void recordEnableTestTypeError(String propKey, String propValue) {
		errors.append("Invalid value \"");
		errors.append(propValue);
		errors.append("\" for property \"");
		errors.append(propKey);
		errors.append("\" ignored.\n");
	}
	
	private void recordBeanPropertyError(String beanName, Object object, Exception ex) {
		errors.append("Invalid value (type?) \"");
		errors.append(object);
		errors.append("\" for property \"");
		errors.append(beanName);
		errors.append("\" ignored. (");
		errors.append(ex.toString());
		errors.append(")\n");
	}
	
	/**
	 * @param s string that should start with "t", "f", "y", "n", or equals
	 * "on", or "off", case ignored. We assume the string is not null or empty.
	 * @return true or false corresponding to input string
	 * @throws IllegalArgumentException if the input string doesn't match an expected value.
	 */
	protected static boolean convertToBoolean (String s) throws IllegalArgumentException {
		if (s == null && s.length() == 0)
			throw new IllegalArgumentException("Boolean value string actually null or empty.");
		s = s.trim();
		char c = s.charAt(0);
		switch (c) {
		case 't':
		case 'T':
		case 'y':
		case 'Y':
			return true;
		case 'f':
		case 'F':
		case 'n':
		case 'N':
			return false;
			default:
		}
		if (s.equalsIgnoreCase("on")) {
			return true;
		}
		if (s.equalsIgnoreCase("off")) {
			return false;
		}
		throw new IllegalArgumentException("Boolean value string unrecognized: \""+s+"\".");
	}
}
