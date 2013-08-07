package org.yamcs.derivedvalues;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.yamcs.DerivedValue;
import org.yamcs.DerivedValuesProvider;
import org.yamcs.MdbDerivedValue;
import org.yamcs.api.EventProducer;
import org.yamcs.api.EventProducerFactory;
import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.xtce.BaseDataType;
import org.yamcs.xtce.DataEncoding;
import org.yamcs.xtce.EnumeratedParameterType;
import org.yamcs.xtce.IntegerDataEncoding;
import org.yamcs.xtce.Parameter;
import org.yamcs.xtce.XtceDb;

public class DerivedValues_XTCE implements DerivedValuesProvider {
    private static final String KEY_ALGO_NAME = "algoName";
    private static final String KEY_UPDATED = "updated";
    private static final String KEY_YAMCS = "Yamcs";
	private static final Logger log = LoggerFactory.getLogger(DerivedValues_XTCE.class);
	ArrayList<DerivedValue> derivedValues = new ArrayList<DerivedValue>();
	int algoCount;
	String currentCode, currentOutputParam, currentAlgo;
	String[] currentInputParams;
	File currentXtceDir;
	ScriptEngineManager semgr;
	ScriptEngine currentEngine;
	HashMap<String,ScriptEngine> enginesByName = new HashMap<String,ScriptEngine>();
	HashMap<String,ScriptEngine> enginesByExtension = new HashMap<String,ScriptEngine>();
	HashMap<String,WindowBuffer> buffersByPname = new HashMap<String,WindowBuffer>();
	private XtceDb xtcedb;
	
	public DerivedValues_XTCE(XtceDb xtcedb) {
	    this.xtcedb=xtcedb;
		try {
			// load scripting engines
			semgr = new ScriptEngineManager();
			for (ScriptEngineFactory factory:semgr.getEngineFactories()) {
				log.debug(String.format("Loading scripting engine: %s/%s, language: %s/%s, names: %s, extensions: %s",
					factory.getEngineName(), factory.getEngineVersion(),
					factory.getLanguageName(), factory.getLanguageVersion(),
					factory.getNames(), factory.getExtensions()));
				final ScriptEngine engine = factory.getScriptEngine();
				
				EventProducer eventProducer = EventProducerFactory.getEventProducer();
				eventProducer.setSource("CustomAlgorithm");
				engine.put(KEY_YAMCS, new ScriptHelper(engine, eventProducer));
				for (String name:factory.getNames()) {
					enginesByName.put(name, engine);
				}
				for (String ext:factory.getExtensions()) {
					enginesByExtension.put(ext, engine);
				}
			}
			
			// Pass Java values as JavaScript primitives instead of objects
			// This is pretty dirty. We're depending on non-public Rhino API
			sun.org.mozilla.javascript.internal.Context ctx = sun.org.mozilla.javascript.internal.Context.enter();
			sun.org.mozilla.javascript.internal.WrapFactory wf = new sun.org.mozilla.javascript.internal.WrapFactory();
			wf.setJavaPrimitiveWrap(false);
			ctx.setWrapFactory(wf);

			// load XML files from classpath
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			String[] cp = ((String)System.getProperties().get("java.class.path")).split((String)System.getProperties().get("path.separator"));
			for (String dirname:cp) {
				File dir = new File(dirname);
				if (dir.isDirectory() && dir.exists()) {
					currentXtceDir = new File(dir, "xtce");
					if (currentXtceDir.isDirectory() && currentXtceDir.exists()) {
						for (File f:currentXtceDir.listFiles(new FilenameFilter() {
							public boolean accept(File f, String name) {
								return name.toLowerCase().endsWith(".xml");
							}
						})) {
							Element root = db.parse(f).getDocumentElement();
							if (root.getTagName().equalsIgnoreCase("SpaceSystem")) {
								loadTag_SpaceSystem(root);
							} else {
								log.warn(String.format("Cannot find tag SpaceSystem in %s", f.getName()));
							}
						}
					}
				}
			}

		} catch (IOException e) {
			System.err.println(e.getMessage());
		} catch (org.xml.sax.SAXException e) {
			System.err.println(e.getMessage());
		} catch (ParserConfigurationException e) {
			System.err.println(e.getMessage());
		}
	}

	public Collection<DerivedValue> getDerivedValues() {
		return derivedValues;
	}

	void loadTag_SpaceSystem(Element el) {
		algoCount = 0;
		NodeList nl = el.getElementsByTagName("TelemetryMetaData");
		for (int i = 0; i < nl.getLength(); ++i) {
			loadTag_TelemetryMetaData((Element)nl.item(i));
		}
		log.debug(String.format("Loaded %d algorithms from \"%s\"", algoCount, el.getAttribute("name")));
	}

	void loadTag_TelemetryMetaData(Element el) {
		NodeList nl = el.getElementsByTagName("AlgorithmSet");
		for (int i = 0; i < nl.getLength(); ++i) {
			loadTag_AlgorithmSet((Element)nl.item(i));
		}
	}

	void loadTag_AlgorithmSet(Element el) {
		NodeList nl = el.getElementsByTagName("CustomAlgorithm");
		for (int i = 0; i < nl.getLength(); ++i) {
			loadTag_CustomAlgorithm((Element)nl.item(i));
		}
	}

	void loadTag_CustomAlgorithm(Element el) {
		currentCode = null;
		currentInputParams = null;
		currentOutputParam = null;
		currentAlgo = el.getAttribute("name");

		int i;
		NodeList nl = el.getElementsByTagName("AlgorithmText");
		for (i = 0; i < nl.getLength(); ++i) {
			loadTag_AlgorithmText((Element)nl.item(i));
		}
		nl = el.getElementsByTagName("ExternalAlgorithmSet");
		for (i = 0; i < nl.getLength(); ++i) {
			loadTag_ExternalAlgorithmSet((Element)nl.item(i));
		}
		nl = el.getElementsByTagName("InputSet");
		for (i = 0; i < nl.getLength(); ++i) {
			loadTag_InputSet((Element)nl.item(i));
		}
		nl = el.getElementsByTagName("OutputSet");
		for (i = 0; i < nl.getLength(); ++i) {
			loadTag_OutputSet((Element)nl.item(i));
		}

		if (currentCode == null) {
			log.warn(String.format("Algorithm %s: no code", currentAlgo));
		} else {
			derivedValues.add(new Algorithm(currentEngine, currentAlgo, currentCode, currentInputParams, currentOutputParam));
			++algoCount;
		}
	}

	void loadTag_AlgorithmText(Element el) {
		// utilise only the first tag
		if (currentCode == null) {
			final String lang = el.getAttribute("language");
			currentEngine = enginesByName.get(lang);
			if (currentEngine != null) {
				NodeList nl = el.getChildNodes();
				if (nl.getLength() > 0) {
					Text content = (Text)nl.item(0);
					currentCode = content.getWholeText();
				}
			} else {
				log.warn(String.format("Algorithm %s: unsupported language \"%s\"", currentAlgo, lang));
			}
		}
	}

	void loadTag_ExternalAlgorithmSet(Element el) {
		NodeList nl = el.getElementsByTagName("ExternalAlgorithm");
		for (int i = 0; i < nl.getLength(); ++i) {
			loadTag_ExternalAlgorithm((Element)nl.item(i));
		}
	}

	void loadTag_ExternalAlgorithm(Element el) {
		if (currentCode == null) {
			String loc = el.getAttribute("algorithmLocation");
			Pattern p = Pattern.compile(".*\\.([^.]+)");
			Matcher m = p.matcher(loc);
			if (m.matches()) {
				currentEngine = enginesByExtension.get(m.group(1));
				if (currentEngine != null) {
					File f = new File(currentXtceDir, loc);
					try {
						FileInputStream reader = new FileInputStream(f);
						long len = f.length();
						byte[] buf = new byte[(int)len];
						if (reader.read(buf) == len) {
							currentCode = new String(buf, "ISO-8859-1");
						}
						reader.close();
					} catch (IOException e) {
						log.warn("Cannot load external algorithm file: "+f.getPath());
					}
				} else {
					log.warn(String.format("Algorithm file name %s does not match any known scripting engine extension", loc));
				}
			} else {
				log.warn(String.format("Algorithm file name %s does not have an extension", loc));
			}
		}
	}

	void loadTag_InputSet(Element el) {
		NodeList nl = el.getElementsByTagName("ParameterInstanceRef");
		currentInputParams = new String[nl.getLength()];
		for (int i = 0; i < nl.getLength(); ++i) {
			currentInputParams[i] = ((Element)nl.item(i)).getAttribute("parameterName");
		}
	}

	void loadTag_OutputSet(Element el) {
		NodeList nl = el.getElementsByTagName("OutputParameterRef");
		// only one element expected
		for (int i = 0; i < nl.getLength(); ++i) {
			currentOutputParam = ((Element)nl.item(i)).getAttribute("parameterName");
		}
	}

	class Algorithm extends MdbDerivedValue {
	    String algoName;
	    String programCode;
		ScriptEngine engine;

		Algorithm(ScriptEngine engine, String algoName, String programCode, String[] inputParams, String outputParam) {
			super((outputParam != null ? outputParam : algoName), inputParams);
			this.algoName = algoName;
			this.engine = engine;
			this.programCode = programCode;
		}

		@Override
        public void updateValue() {
			for (int i = 0; i < args.length; ++i) {
				final String pname = args[i].getParameter().getName();
				Value v = args[i].getEngValue();
				
				// Store current engine values for parameters used in an algorithm window
				if (buffersByPname.containsKey(pname)) {
				    buffersByPname.get(pname).append((Number) engine.get(pname));
				    log.debug("Adding pname of type {}", engine.get(pname).getClass());
                    log.debug("Buffer: {}", buffersByPname.get(pname));
				}
				
				// Now, update the engine
				switch(v.getType()) {
				    case BINARY:
				        engine.put(pname, v.getBinaryValue().toByteArray());
				        break;
				    case DOUBLE:
				        engine.put(pname, v.getDoubleValue());
				        break;
				    case FLOAT:
				        engine.put(pname, v.getFloatValue());
                        break;
				    case SINT32:
				        engine.put(pname, v.getSint32Value());
                        break;
				    case STRING:
				        engine.put(pname, v.getStringValue());
				        break;
				    case UINT32:
				        engine.put(pname, v.getUint32Value()&0xFFFFFFFFL);
				        break;
				    case SINT64:
				        engine.put(pname, v.getSint64Value());
				        break;
				    case UINT64:
				        engine.put(pname, v.getUint64Value()&0xFFFFFFFFFFFFFFFFL);
				        break;
				    default:
				        log.warn("Ignoring update of unexpected value type {}", v.getType());
				}
			}
			updated = true;
			engine.put(KEY_UPDATED, updated);
			engine.put(KEY_ALGO_NAME, algoName);
			
			try {
				//long ts = System.currentTimeMillis();
				engine.eval(programCode);
				//System.out.println("script time "+(System.currentTimeMillis() - ts));
				updated = (Boolean) engine.get(KEY_UPDATED);
				if (updated) {
					Object res = engine.get(getParameter().getName());
					if (res == null) {
					    if (!getParameter().getName().equals(algoName)) {
					        log.warn("script variable "+getParameter().getName()+" was not set");
					    }
						updated = false;
					} else {
						if (res instanceof Double) {
							Double dres = (Double)res;
							if (dres.longValue() == dres.doubleValue()) {
							    setUnsignedIntegerValue(dres.intValue());
							} else {
							    setDoubleValue(dres.doubleValue());
							}
						} else if (res instanceof Boolean) {
						    setBinaryValue((((Boolean)res).booleanValue() ? "YES" : "NO").getBytes());
						} else {
						    setBinaryValue(res.toString().getBytes());
						}
					}
				}
			} catch (ScriptException e) {
				log.warn("script error for "+getParameter().getName()+": "+e.getMessage());
				updated = false;
			}
		}
	}

	public class ScriptHelper {
	    private ScriptEngine engine;
	    private EventProducer eventProducer;

        private ScriptHelper(ScriptEngine engine, EventProducer eventProducer) {
	        this.engine = engine;
	        this.eventProducer = eventProducer;
	    }
        
		public Object calibrate(int raw, String parameter) {
		// calibrate raw value according to the calibration rule of the given parameter
		// returns a Float or String object
			Parameter p = xtcedb.getParameter(parameter);
			if (p != null) {
				if (p.getParameterType() instanceof EnumeratedParameterType) {
					EnumeratedParameterType ptype = (EnumeratedParameterType)p.getParameterType();
					return ptype.calibrate(raw);
				} else {
					DataEncoding encoding = ((BaseDataType)p.getParameterType()).getEncoding();
					if(encoding instanceof IntegerDataEncoding) {
						return ((IntegerDataEncoding) encoding).getDefaultCalibrator().calibrate(raw);
					}
				}
			} else {
				log.warn(String.format("Cannot find parameter %s to calibrate %d", parameter, raw));
			}
			return null;
		}
		
		public void info(String msg) {
		    info((String) engine.get(KEY_ALGO_NAME), msg);
		}
		
		public void info(String type, String msg) {
		    eventProducer.sendInfo(type, msg);
		}
		
        public void warning(String msg) {
            warning((String) engine.get(KEY_ALGO_NAME), msg);
        }
        
        public void warning(String type, String msg) {
            eventProducer.sendWarning(type, msg);
        }
        
        public void error(String msg) {
            error((String) engine.get(KEY_ALGO_NAME), msg);
        }
        
        public void error(String type, String msg) {
            eventProducer.sendError(type, msg);
        }
		
		public Object prev(String parameter, int index) {
		    if (index < 1) { throw new IllegalArgumentException("Index into previous values should be >= 1"); }
		    if (!buffersByPname.containsKey(parameter)) {
		        buffersByPname.put(parameter, new WindowBuffer(index));
		    }
		    Object historicValue = buffersByPname.get(parameter).getHistoricValue(index);
		    if (historicValue == null) {
		        engine.put(KEY_UPDATED, Boolean.FALSE);
		    }
		    return historicValue;
		}

		public long letohl(int value) {
		// little endian to host long
			return ((value>>24)&0xff) + ((value>>8)&0xff00) + ((value&0xff00)<<8) + ((value&0xff)<<24);
		}
	}
	
	private static class WindowBuffer {
	    private Object[] historicValues; // LTR: least-recent to most-recent
	    
	    /**
	     * @param size Initial estimate of size
	     */
	    WindowBuffer(int size) {
	        historicValues = new Object[size];
	    }
	    
	    public Object getHistoricValue(int index) {
	        if (index > historicValues.length) {
	            historicValues = Arrays.copyOf(historicValues, index);
	            return null;
	        } else {
	            return historicValues[historicValues.length - index];
	        }
	    }
	    
	    void append(Object value) {
	        int i = 0;
	        for (; i < historicValues.length - 1; i++) {
	            historicValues[i] = historicValues[i+1];
	        }
	        historicValues[i] = value;
	    }
	    
	    @Override
	    public String toString() {
	        return Arrays.toString(historicValues);
	    }
	}
}
