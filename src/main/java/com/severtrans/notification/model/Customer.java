package com.severtrans.notification.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;

/**
 * Клиент
 * Insert into KB_ZAK (ID,ID_SVH,ID_WMS,IS_HOLDER,ID_USR,N_ZAK,ID_KLIENT)
 * values ('0102315556','KB_SVH95476','10406','1','KB_USR99992','ТОРГОВЫЙ ДОМ
 * РУСЬ','300254');
 */
@Data
@Table("KB_ZAK")
public class Customer {
  /**
   * ID
   */
  @Id
  String id;
  /**
   * Транспортная площадка по умолчанию ID_SVH (ссылка на sv_hvoc)
   */
  @Column("ID_SVH")
  String defaultPlatform;
  /**
   * ID клиента в СОХ (ID_WMS)
   */
  @Column("ID_WMS")
  String holderID;
  /**
   * Шлюз ID_USR IN ('KB_USR92734', 'KB_USR99992');
   */
  @Column("ID_USR")
  String gateId;
  /**
   * Наименование клиента
   */
  @Column("N_ZAK")
  String customerName;
  /**
   * Внутренний номер клиента ВН
   */
  @Column("ID_KLIENT")
  int clientId;
  /**
   * Префикс
   */
  @Column("PRF_WMS")
  String prefix;
}
