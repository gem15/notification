  CREATE TABLE "MONITOR_LOG" 
   ("ID" VARCHAR2(36) NOT NULL, 
	"STATUS" VARCHAR2(20), --DEFAULT 'R' NOT NULL, 
	"MSG_TYPE" NUMBER(1,0), 
	"FILE_NAME" VARCHAR2(120), 
	"START_DATE" DATE, 
	"END_DATE" DATE, 
	"MSG" VARCHAR2(120), --CLOB, 
	"VN" NUMBER 
	 --CONSTRAINT "MONITOR_LOG_PK" PRIMARY KEY ("ID")
     );


CREATE TABLE KB_ZAK
(ID VARCHAR2(38),
 ID_SVH VARCHAR2(38),
 ID_WMS VARCHAR2(20),
 IS_HOLDER NUMBER(1,0),
 ID_USR VARCHAR2(38),
 N_ZAK VARCHAR2(256),
 ID_KLIENT NUMBER(6,0),
 PRF_WMS VARCHAR2(3),
     CONSTRAINT KB_ZAKID_PK PRIMARY KEY (ID)
);

CREATE TABLE SV_HVOC
(
    VAL_ID        VARCHAR2(38),
    HVOC_VAL_ID   VARCHAR2(38),
    VOC_ID        VARCHAR2(6),
    VAL_FULL      VARCHAR2(1000),
    VAL_SHORT     VARCHAR2(30),
    VAL_STATE     VARCHAR2(20),
    VAL_CHANGES   VARCHAR2(20),
    TOOLS         VARCHAR2(4000),
    DATA_BEGIN    DATE ,
    DATA_END      DATE,
    MASTER_VAL_ID VARCHAR2(38),
    POLICE_CODE   VARCHAR2(38)
--     ,CONSTRAINT HVOC_PK PRIMARY KEY (VAL_ID)
);


CREATE TABLE KB_T_ARTICLE
(	"ID_SOST" VARCHAR2(128),
     "MARKER" VARCHAR2(128),
     "PRICE" VARCHAR2(128),
     "COMMENTS" VARCHAR2(1024),
     "TIP_TOV" VARCHAR2(128),
     "NUM" VARCHAR2(128),
     "CATEG" VARCHAR2(128),
     "UPC" VARCHAR2(128),
     "CODE" VARCHAR2(128),
     "EXPIRY_DATE" VARCHAR2(128),
     "STR_SR_GODN" VARCHAR2(128),
     "STR_PART" VARCHAR2(128),
     "STR_SSCC" VARCHAR2(128),
     "STR_SERT" VARCHAR2(128),
     "STR_MU_CODE" VARCHAR2(128),
     "N_MU_UNIT" VARCHAR2(128),
     "ABC" VARCHAR2(128),
     "STORAGE_POS" VARCHAR2(128),
     "PRODUCER" VARCHAR2(128),
     "VENDOR" VARCHAR2(128),
     "COO" VARCHAR2(128),
     "MEASURE" VARCHAR2(128),
     "KIT_TYPE" VARCHAR2(128),
     "USAGE_STATE" VARCHAR2(128),
     "DESCRIPTION" VARCHAR2(1024)
);


/*create table unit
(
    id   VARCHAR(38),
    code VARCHAR(255),
    name VARCHAR(255)
);*/

CREATE TABLE master
(
    order_id VARCHAR(38),
    dt_sost     DATE default SYSDATE,
    dt_sost_end DATE,
    sost_doc    VARCHAR(100),
    sost_prm    VARCHAR(4000),
    id_du       VARCHAR(38),
    DT_VEH      DATE,
    ID_SUPPL    VARCHAR(20), --ID_WMS	VARCHAR(20)	Yes		62	ID клиента в СОХ
    ID_KLIENT   INTEGER,     --VN
    N_ZAK       VARCHAR(255),
    UR_ADR      VARCHAR(255),
    N_AVTO      VARCHAR(255),
    VODIT       VARCHAR(255),
    ID_USR      VARCHAR(38),
    ID_OBSL     VARCHAR(38)
);

CREATE TABLE detail
(
    "ROWNUM"          INTEGER,
    IDDU            VARCHAR(20),
    SKU_ID          VARCHAR(100),
    NAME            VARCHAR(255),
    EXPIRATION_DATE DATE,
    PRODUCTION_DATE DATE,
    LOT             VARCHAR(38),
    MARKER          VARCHAR(38),
    MARKER2         VARCHAR(38),
    MARKER3         VARCHAR(38),
    QTY             INTEGER,
    COMMENTS        VARCHAR(255),
    SERIAL_NUM      VARCHAR(255)
);

CREATE TABLE notif -- removeme
(
    dt_sost     DATE default SYSDATE,
    dt_sost_end DATE,
    sost_doc    VARCHAR(100),
    sost_prm    VARCHAR(4000),
    id_du       VARCHAR(38),
    DT_VEH      DATE,
    ID_SUPPL    VARCHAR(20), --ID_WMS	VARCHAR(20)	Yes		62	ID клиента в СОХ
    ID_KLIENT   INTEGER,     --VN
    N_ZAK       VARCHAR(255),
    UR_ADR      VARCHAR(255),
    N_AVTO      VARCHAR(255),
    VODIT       VARCHAR(255),
    ID_USR      VARCHAR(38),
    ID_OBSL     VARCHAR(38)
);

CREATE TABLE notifdet -- removeme
(
    IDDU            VARCHAR(20),
    SKU_ID          VARCHAR(100),
    NAME            VARCHAR(255),
    EXPIRATION_DATE DATE,
    PRODUCTION_DATE DATE,
    LOT             VARCHAR(38),
    MARKER          VARCHAR(38),
    MARKER2         VARCHAR(38),
    MARKER3         VARCHAR(38),
    QTY             INTEGER,
    COMMENTS        VARCHAR(255),
    SERIAL_NUM      VARCHAR(255)
);