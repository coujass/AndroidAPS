package com.gxwtech.roundtrip2.RoundtripService.medtronic.PumpData.records;

import com.gxwtech.roundtrip2.RoundtripService.medtronic.PumpModel;

/**
 * Created by geoff on 6/5/16.
 */
public class JournalEntryExerciseMarkerPumpEvent extends TimeStampedRecord {
    public JournalEntryExerciseMarkerPumpEvent(){}

    @Override
    public int getLength() { return 8; }

    @Override
    public String getShortTypeName() {
        return "Exercise Marker";
    }
}
