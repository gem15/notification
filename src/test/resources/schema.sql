drop table if exists notif;
drop table if exists notifdet;
create table notif
(
    dt_sost     DATE default SYSDATE,
    dt_sost_end DATE,
    sost_doc    VARCHAR(100),
    sost_prm    VARCHAR(4000),
    id_du       VARCHAR(38),
    DT_VEH      DATE,
    ID_SUPPL    VARCHAR(20), --ID_WMS	VARCHAR(20 BYTE)	Yes		62	ID клиента в СОХ
    ID_KLIENT   INTEGER,      --VN
    N_ZAK       VARCHAR(255),
    UR_ADR      VARCHAR(255),
    N_AVTO      VARCHAR(255),
    VODIT       VARCHAR(255),
    ID_USR      VARCHAR(38),
    ID_OBSL     VARCHAR(38)
);

create table notifdet
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