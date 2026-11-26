package kr.personal.historyjobs.core;
import java.time.*;
public final class Schedule {
    private Schedule(){}
    public static ZonedDateTime next(Instant instant,int minutes){
        if(minutes<0||minutes>=1440)throw new IllegalArgumentException("예약 시각 오류");
        ZonedDateTime now=instant.atZone(Classifier.KST);
        ZonedDateTime next=now.toLocalDate().atStartOfDay(Classifier.KST).plusMinutes(minutes);
        return next.isAfter(now.plusSeconds(3))?next:next.plusDays(1);
    }
}
