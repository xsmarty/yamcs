package org.yamcs.parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamcs.YProcessor;
import org.yamcs.ConfigurationException;
import org.yamcs.InvalidIdentification;
import org.yamcs.ParameterValue;

import com.google.common.util.concurrent.AbstractService;

import org.yamcs.protobuf.Yamcs.NamedObjectId;
import org.yamcs.utils.TimeEncoding;
import org.yamcs.xtce.Parameter;
import org.yamcs.xtce.SystemParameter;
import org.yamcs.xtce.XtceDb;
import org.yamcs.xtceproc.XtceDbFactory;
import org.yamcs.yarch.Stream;
import org.yamcs.yarch.StreamSubscriber;
import org.yamcs.yarch.Tuple;
import org.yamcs.yarch.YarchDatabase;

/**
 * Provides system variables from the sys_var stream to ParameterRequestManager
 * 
 * It also provides some info about the current yprocessor
 * 
 * For the moment all variables starting with "/yamcs" are accepted and nothing else.
 * Aliases are not supported.
 * 
 */
public class SystemParametersProvider extends AbstractService implements StreamSubscriber, ParameterProvider {
    private ParameterRequestManager parameterListener;

    volatile private Map<String, SystemParameter> variables = new HashMap<String, SystemParameter>(); 
    Logger log;
    Stream stream;
    XtceDb xtceDb;
    YProcessor yproc;
    ArrayList<ParameterValue> yprocParams = new ArrayList<ParameterValue>();
    ScheduledThreadPoolExecutor timer=new ScheduledThreadPoolExecutor(1);
    ParameterValue yprocModePv;
    
    public SystemParametersProvider(String yamcsInstance) throws ConfigurationException {
        xtceDb = XtceDbFactory.getInstance(yamcsInstance);
    }
    
    public void init(YProcessor yproc) throws ConfigurationException {
        String instance = yproc.getInstance();
        log=LoggerFactory.getLogger(this.getClass().getName()+"["+yproc.getName()+"]");
        YarchDatabase ydb=YarchDatabase.getInstance(instance);
        stream=ydb.getStream(SystemParametersCollector.STREAM_NAME);
        if(stream==null) throw new ConfigurationException("Cannot find a stream named "+SystemParametersCollector.STREAM_NAME);

        this.yproc = yproc;
        setupYProcParameters();
      
    }

    
    @Override
    public void startProviding(Parameter paramDef) {
        //TODO
    }

    @Override
    public void startProvidingAll() {
        //TOOD
    }

    @Override
    public void stopProviding(Parameter paramDef) {
        //TODO
    }

    @Override
    public void onTuple(Stream s, Tuple tuple) {//the definition of the tuple is in PpProviderAdapter
        List<ParameterValue> params=new ArrayList<ParameterValue>();
        for(int i=4;i<tuple.size();i++) {
            org.yamcs.protobuf.Pvalue.ParameterValue gpv=(org.yamcs.protobuf.Pvalue.ParameterValue)tuple.getColumn(i);
            String name=tuple.getColumnDefinition(i).getName();
            SystemParameter sv = variables.get(name);
            if(sv==null) {
                sv = (SystemParameter) xtceDb.getParameter(name);
                if(sv==null) {
                    sv = createParameterForName(name);
                }
            } 

            ParameterValue pv=ParameterValue.fromGpb(sv, gpv);
            params.add(pv);
        }
        //    System.out.println("-------------- updating parameters "+params);
        parameterListener.update(params);
    }

    @Override
    public void streamClosed(Stream s) {
        notifyStopped();
    }


    private synchronized SystemParameter createParameterForName(String fqname) {
        SystemParameter sv = variables.get(fqname);
        if(sv==null) {
            log.info("Creating new SystemParameter for fqname={}", fqname);
            sv = SystemParameter.getForFullyQualifiedName(fqname);
            variables.put(fqname, sv);
        }

        return sv;
    }
    /**
     * return true if parameter starts with "/YAMCS" or the namespace is null and the name starts with "/YAMCS" 
     * 
     */
    @Override
    public boolean canProvide(NamedObjectId paraId) {
        boolean result;
        if(!paraId.hasNamespace()) {
            result = paraId.getName().startsWith(XtceDbFactory.YAMCS_SPACESYSTEM_NAME);
        } else {
            result = paraId.getNamespace().startsWith(XtceDbFactory.YAMCS_SPACESYSTEM_NAME);
        }
        return result;
        
    }

    @Override
    public boolean canProvide(Parameter para) {        
	return para.getQualifiedName().startsWith(XtceDbFactory.YAMCS_SPACESYSTEM_NAME);
    }

    
    @Override
    public Parameter getParameter(NamedObjectId paraId)  throws InvalidIdentification {
        String name = paraId.getName();
        if(paraId.hasNamespace())  {
            name = paraId.getNamespace()+"/"+name;
        }
        return getSystemParameter(name);
       
    }
    
    /**
     * Get (and possibly create) system parameter. 
     * The parameter may be in the xtcedb in case it is referred to by an algorithm. Otherwise is created on the fly and not added to XtceDB.
     */
    public SystemParameter getSystemParameter(String fqname) {
        SystemParameter sv = variables.get(fqname);
        if(sv==null) {
            sv = (SystemParameter) xtceDb.getParameter(fqname);
            if(sv==null) {
                sv = createParameterForName(fqname);
            }
        }
        return sv;
    }

    @Override
    public void setParameterListener(ParameterRequestManager parameterRequestManager) {
        this.parameterListener = parameterRequestManager; 

    }


    @Override
    protected void doStart() {
        stream.addSubscriber(this);
        timer.scheduleAtFixedRate(new Runnable() {
            
            @Override
            public void run() {
                updateYProcParameters();  
            }
        }, 0, 1, TimeUnit.SECONDS);
        notifyStarted();
    }

    @Override
    protected void doStop() {
        stream.removeSubscriber(this);
        timer.shutdown();
        notifyStopped();
    }
    
    private ParameterValue getYProcPV(String name, String value) {
        ParameterValue pv = new ParameterValue(getSystemParameter(XtceDbFactory.YAMCS_SPACESYSTEM_NAME+"/yprocessor/"+name));
        pv.setAcquisitionTime(TimeEncoding.currentInstant());
        pv.setGenerationTime(TimeEncoding.currentInstant());
        pv.setStringValue(value);
        return pv;
    }
    private void setupYProcParameters() {
        ParameterValue yprocNamePv = getYProcPV("name", yproc.getName());
        yprocParams.add(yprocNamePv);
        
        ParameterValue yprocCreatorPv = getYProcPV("creator", yproc.getCreator());
        yprocParams.add(yprocCreatorPv);
        
        String mode =  yproc.isReplay()? "replay":"realtime";            
        yprocModePv = getYProcPV("mode", mode);
        yprocParams.add(yprocModePv); 
    }
    
    private void updateYProcParameters() {
        yprocModePv.setGenerationTime(TimeEncoding.currentInstant());
        parameterListener.update(yprocParams);
    }
}