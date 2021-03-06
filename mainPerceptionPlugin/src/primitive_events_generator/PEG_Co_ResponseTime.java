package primitive_events_generator;

import events.primitive_events.PE_ResponseTime;
import graph.CloudResource;
import graph.CloudResourceType;
import graph.Co;
import perception.events.PrimitiveEvent;
import perception.primitive_events_generator.PrimitiveEventGenerator;

import java.util.Optional;

public class PEG_Co_ResponseTime extends PrimitiveEventGenerator {

    public PEG_Co_ResponseTime(String name, long msRefreshingRate) {
        super(name, msRefreshingRate);
    }

    public PEG_Co_ResponseTime(long msRefreshingRate) {
        super("PEG_Co_ResponseTime", msRefreshingRate);
    }

    @Override
    protected Optional<PrimitiveEvent> processResource(CloudResource cr) {
        if(cr.getType() == CloudResourceType.CO) {
            PE_ResponseTime pe_responseTime = new PE_ResponseTime(cr.getName(), cr.getType(), cr.getScore(), ((Co)cr).getResponseTime());
            return Optional.of(pe_responseTime);
        }
        return Optional.empty();
    }

}
