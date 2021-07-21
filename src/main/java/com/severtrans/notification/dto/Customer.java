package com.severtrans.notification.dto;

import lombok.Data;

@Data
public class Customer {
    /**
     * Код клиента
     */
   String id;
    /**
     * Транспортная площадка по умолчанию  ID_SVH (ссылка на sv_hvoc)
     */
   String defaultPlatform;
    /**
     * ID клиента в СОХ  (ID_WMS)
     */
   int holderID;
    /**
     * Шлюз ID_USR IN ('KB_USR92734', 'KB_USR99992');
     */
   String gateId;
    /**
     * Наименование клиента
     */
   String customerName;
    /**
     * Внутренний номер клиента
     */
   int clientId;
}
/*
Insert into KB_ZAK (ID,ID_SVH,ID_WMS,IS_HOLDER,ID_USR,N_ZAK,ID_KLIENT)
 values ('0102315556','KB_SVH95476','10406','1','KB_USR99992','ТОРГОВЫЙ ДОМ РУСЬ','300254');
I
* */