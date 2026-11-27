package kr.personal.historyjobs.core;

import java.util.*;
import static kr.personal.historyjobs.core.Model.*;

public final class NoticeOrder {
    private NoticeOrder(){}
    public static Comparator<Notice> comparator(boolean newest,long now){
        Comparator<Notice> discovered=Comparator.comparingLong((Notice n)->n.firstSeen).reversed().thenComparing(n->n.id);
        Comparator<Notice> published=Comparator.comparing((Notice n)->Selection.date(n.published),Comparator.reverseOrder()).thenComparing(discovered);
        return newest?published:Comparator.comparingLong((Notice n)->n.deadline>=now&&n.deadline>0?n.deadline:Long.MAX_VALUE).thenComparing(discovered);
    }
    public static String elapsed(long millis){long seconds=Math.max(0,millis)/1000;return "소요 시간 · "+(seconds<60?seconds+"초":seconds/60+"분 "+seconds%60+"초");}
}
