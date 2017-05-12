package info.nightscout.androidaps.plugins.PumpDanaRKorean.comm;

import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Date;

import info.nightscout.androidaps.Config;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.db.TempBasal;
import info.nightscout.androidaps.events.EventTempBasalChange;
import info.nightscout.androidaps.interfaces.TreatmentsInterface;
import info.nightscout.androidaps.plugins.PumpDanaR.DanaRPump;
import info.nightscout.androidaps.plugins.PumpDanaR.comm.MessageBase;

public class MsgStatusBolusExtended_k extends MessageBase {
    private static Logger log = LoggerFactory.getLogger(MsgStatusBolusExtended_k.class);

    public MsgStatusBolusExtended_k() {
        SetCommand(0x0207);
    }

    public void handleMessage(byte[] bytes) {
        DanaRPump pump = DanaRPump.getInstance();
        boolean isExtendedInProgress = intFromBuff(bytes, 0, 1) == 1;
        int extendedBolusHalfHours = intFromBuff(bytes, 1, 1);
        int extendedBolusMinutes = extendedBolusHalfHours * 30;

        double extendedBolusAmount = intFromBuff(bytes, 2, 2) / 100d;
        int extendedBolusSoFarInSecs = intFromBuff(bytes, 4, 3);
        int extendedBolusDeliveryPulse = intFromBuff(bytes, 7, 2);
        int isEasyUIUserSleep = intFromBuff(bytes, 9, 1);

        int extendedBolusSoFarInMinutes = extendedBolusSoFarInSecs / 60;
        double extendedBolusAbsoluteRate = isExtendedInProgress ? extendedBolusAmount / extendedBolusMinutes * 60 : 0d;
        Date extendedBolusStart = isExtendedInProgress ? getDateFromSecAgo(extendedBolusSoFarInSecs) : new Date(0);
        int extendedBolusRemainingMinutes = extendedBolusMinutes - extendedBolusSoFarInMinutes;

        pump.isExtendedInProgress = isExtendedInProgress;
        pump.extendedBolusMinutes = extendedBolusMinutes;
        pump.extendedBolusAmount = extendedBolusAmount;
        pump.extendedBolusSoFarInMinutes = extendedBolusSoFarInMinutes;
        pump.extendedBolusAbsoluteRate = extendedBolusAbsoluteRate;
        pump.extendedBolusStart = extendedBolusStart;
        pump.extendedBolusRemainingMinutes = extendedBolusRemainingMinutes;

        updateExtendedBolusInDB();

        if (Config.logDanaMessageDetail) {
            log.debug("Is extended bolus running: " + isExtendedInProgress);
            log.debug("Extended bolus min: " + extendedBolusMinutes);
            log.debug("Extended bolus amount: " + extendedBolusAmount);
            log.debug("Extended bolus so far in minutes: " + extendedBolusSoFarInMinutes);
            log.debug("Extended bolus absolute rate: " + extendedBolusAbsoluteRate);
            log.debug("Extended bolus start: " + extendedBolusStart);
            log.debug("Extended bolus remaining minutes: " + extendedBolusRemainingMinutes);
        }
    }

    @NonNull
    private Date getDateFromSecAgo(int tempBasalAgoSecs) {
        return new Date((long) (Math.ceil(new Date().getTime() / 1000d) - tempBasalAgoSecs) * 1000);
    }

    public static void updateExtendedBolusInDB() {
        TreatmentsInterface treatmentsInterface = MainApp.getConfigBuilder();
        DanaRPump pump = DanaRPump.getInstance();
        Date now = new Date();

        try {

            if (treatmentsInterface.isExtendedBoluslInProgress()) {
                TempBasal extendedBolus = treatmentsInterface.getExtendedBolus(new Date().getTime());
                if (pump.isExtendedInProgress) {
                    if (extendedBolus.absolute != pump.extendedBolusAbsoluteRate) {
                        // Close current extended
                        extendedBolus.timeEnd = now;
                        MainApp.getDbHelper().getDaoTempBasals().update(extendedBolus);
                        // Create new
                        TempBasal newExtended = new TempBasal();
                        newExtended.timeStart = now;
                        newExtended.absolute = pump.extendedBolusAbsoluteRate;
                        newExtended.isAbsolute = true;
                        newExtended.duration = pump.extendedBolusMinutes;
                        newExtended.isExtended = true;
                        MainApp.getDbHelper().getDaoTempBasals().create(newExtended);
                        MainApp.bus().post(new EventTempBasalChange());
                    }
                } else {
                    // Close curent temp basal
                    extendedBolus.timeEnd = now;
                    MainApp.getDbHelper().getDaoTempBasals().update(extendedBolus);
                    MainApp.bus().post(new EventTempBasalChange());
                }
            } else {
                if (pump.isExtendedInProgress) {
                    // Create new
                    TempBasal newExtended = new TempBasal();
                    newExtended.timeStart = now;
                    newExtended.absolute = pump.extendedBolusAbsoluteRate;
                    newExtended.isAbsolute = true;
                    newExtended.duration = pump.extendedBolusMinutes;
                    newExtended.isExtended = true;
                    MainApp.getDbHelper().getDaoTempBasals().create(newExtended);
                    MainApp.bus().post(new EventTempBasalChange());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}