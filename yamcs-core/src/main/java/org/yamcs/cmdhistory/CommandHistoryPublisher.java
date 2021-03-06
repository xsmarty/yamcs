package org.yamcs.cmdhistory;

import org.yamcs.InvalidCommandId;
import org.yamcs.commanding.PreparedCommand;

import org.yamcs.protobuf.Commanding.CommandId;

/**
 * Used by the commanding applications to save commands and commands acknowledgements into a history.
 * @author nm
 *
 */
public interface CommandHistoryPublisher {
    public static String CommandComplete_KEY = "CommandComplete";
    public static String CommandFailed_KEY = "CommandFailed";
    public static String TransmissionContraints_KEY = "TransmissionConstraints";
    public static String Verifier_KEY_PREFIX = "Verifier";
    
    public abstract void updateStringKey(CommandId cmdId, String key, String value) throws InvalidCommandId;
    public abstract void updateTimeKey(CommandId cmdId, String key, long value) throws InvalidCommandId;	
    public abstract void addCommand(PreparedCommand pc);
}