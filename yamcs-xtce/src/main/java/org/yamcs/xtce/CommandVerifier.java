package org.yamcs.xtce;

import java.io.Serializable;

/**
 *XTCE:
 * A command verifier is used to check that the command has been successfully executed. 
 * Command Verifiers may be either a Custom Algorithm or a Boolean Check or the presence of a Container for a relative change in the value of a Parameter.  
 * The CheckWindow is a time period where the verification must test true to pass.
 *
 * @author nm
 *
 */
public class CommandVerifier implements Serializable {
    private static final long serialVersionUID = 2L;
    
    /**
     * what can happen when the verification finishes
     * XTCE does not specify very well, just that each verifier returns true or false. 
     * 
     * We acknowledge the fact that the verifier can also timeout and define three TerminationAction for the three outcomes: true, false or timeout. 
     */
    public enum TerminationAction {
        SUCCESS, //the command is declared successful
        FAIL //the command is declard failed
    }      
    private TerminationAction onSuccess = null, onFail = null, onTimeout = null;
    
    
    /** 
     * differs from XTCE
     * 
     * Command verification stage. We use this to implement the different stages hardcoded in the XTCE: TransferredToRange, SentFromRange, etc
     * In XTCE some of those verifications have extra parameters. This can be implemented in the future by subclassing this class.
     *  
     */
    final private String stage;

    
    /**
     * XTCE: A time based check window
     */
    final private CheckWindow checkWindow;
    
    
    /**
     * When verification is a new instance of the referenced Container; this verifier return true when the referenced container has been received and processed.
     */
    SequenceContainer containerRef;
    //NOT implemented from XTCE
    /*       
     * comparisonList;
     * customAlgorithm;
     * ParameterValueChange
     * BooleanExpression
     */
    

    public CommandVerifier(String stage, CheckWindow checkWindow) {
        this.stage = stage;
        this.checkWindow = checkWindow;
    }

    public String getStage() {
        return stage;
    }

    public void setContainerRef(SequenceContainer containerRef) {
        this.containerRef = containerRef;
    }

    public SequenceContainer getContainerRef() {
        return containerRef;
    }


    public CheckWindow getCheckWindow() {
        return checkWindow;
    }
      
    public TerminationAction getOnTimeout() {
        return onTimeout;
    }

    public void setOnTimeout(TerminationAction onTimeout) {
        this.onTimeout = onTimeout;
    }

    public TerminationAction getOnFail() {
        return onFail;
    }

    public void setOnFail(TerminationAction onFail) {
        this.onFail = onFail;
    }

    public TerminationAction getOnSuccess() {
        return onSuccess;
    }

    public void setOnSuccess(TerminationAction onSuccess) {
        this.onSuccess = onSuccess;
    }

    
    public String toString() {
        return "{stage: "+stage+", containerRef: "+containerRef.getName()+", checkWindow: "+checkWindow+"}";
    }


}
