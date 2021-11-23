package com.severtrans.notification.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EventLog {
    
    public EventLog(String linkID) {
        this.linkID = linkID;
    }

    /**
     * kb_sost.id
     */
    String id;
    /**
     * Ссылка на заказ. <b>ID_OBSL</b>
     */
    String orderID;
    /**
     * <b>sv_hvoc.id</b>
     */
    String eventID;
    // String info;
    
    /**
     * Ссылка на документ СОХ id_du
     */
    String linkID;
    
}
