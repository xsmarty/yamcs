package org.yamcs.archive;



import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.SimpleString;
import org.yamcs.yarch.Stream;
import org.yamcs.yarch.TupleDefinition;
import org.yamcs.yarch.YarchDatabase;
import org.yamcs.hornetq.StreamAdapter;
import org.yamcs.yarch.streamsql.ParseException;
import org.yamcs.yarch.streamsql.StreamSqlException;

import com.google.common.util.concurrent.AbstractService;
import org.yamcs.api.YamcsApiException;
import org.yamcs.hornetq.EventTupleTranslator;

/**
 * Sets up the archiving of the events coming on events_realtime and events_dump streams
 *  into the yarch table events.
 * @author nm
 *
 */
public class EventRecorder extends AbstractService {
    static TupleDefinition eventTpdef; 
    static final public String TABLE_NAME="events";
    static final public String REALTIME_EVENT_STREAM_NAME="events_realtime";
    static final public String DUMP_EVENT_STREAM_NAME="events_dump";

    StreamAdapter rtStreamAdapter, dumpStreamAdapter;

    public EventRecorder(String instance) throws StreamSqlException, ParseException, HornetQException, YamcsApiException {
	YarchDatabase ydb=YarchDatabase.getInstance(instance);
	if(ydb.getTable(TABLE_NAME)==null) {
	    ydb.execute("create table "+TABLE_NAME+"(gentime timestamp, source enum, seqNum int, body PROTOBUF('org.yamcs.protobuf.Yamcs$Event'), primary key(gentime, source, seqNum)) histogram(source) partition by time(gentime) table_format=compressed");
	}
	eventTpdef=ydb.getTable("events").getTupleDefinition();

	ydb.execute("insert into "+TABLE_NAME+" select * from "+REALTIME_EVENT_STREAM_NAME);
	ydb.execute("insert into "+TABLE_NAME+" select * from "+DUMP_EVENT_STREAM_NAME);

	Stream realtimeEventStream=ydb.getStream(REALTIME_EVENT_STREAM_NAME);
	rtStreamAdapter = new StreamAdapter(realtimeEventStream, new SimpleString(instance+".events_realtime"), new EventTupleTranslator());

	Stream dumpEventStream=ydb.getStream(DUMP_EVENT_STREAM_NAME);
	dumpStreamAdapter = new StreamAdapter(dumpEventStream, new SimpleString(instance+".events_dump"), new EventTupleTranslator());
    }

    @Override
    protected void doStart() {
	notifyStarted();
    }

    @Override
    protected void doStop() {
	rtStreamAdapter.quit();
	dumpStreamAdapter.quit();
	notifyStopped();
    }

}
