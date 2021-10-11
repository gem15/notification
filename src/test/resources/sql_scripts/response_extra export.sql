--======= MAIN Rq
SELECT
e.id as extraID,
vn, path_in, path_out,
--e.master, e.details,
alias_text alias, e.prefix,
e.order_type, t.inout_id, f.hostname, e.legacy,t.name as type_name, t.id as type_id
FROM response_ftp r INNER JOIN response_extra e ON r.response_extra_id = e.id
INNER JOIN ftps f ON r.ftp_id = f.id
INNER JOIN response_type T ON T.ID = e.response_type_id
--WHERE r.ftp_id = 4
;
select * from response_ftp;
select * from response_extra;
select * from response_ftp
--where ftp_id =4
where path_in is null
;
update response_ftp set vn=300259 where ftp_id = 4;
update response_ftp set path_in='IN' where path_in is null;

select * from ftps;
select * from response_type;

--FTP APP Папки IN, LOADED и OUT  - полный доступ
--Аккаунт для клиента :    app - sT458APP --Для нас  : svtapp - tVApp8842
/*
300261 КУХНИ-ТАЙПИТ
300262 КРЕСЛА-ТАЙПИТ
300263 КОРПУС-ТАЙПИТ
*/
REM INSERTING into FTPS_EXPORT
SET DEFINE OFF;
Insert into FTPS_EXPORT (ID,LOGIN,PASSWORD,HOSTNAME,PORT,DESCRIPTION) values ('2','severtrans_it','SevTraIt20XX','213.170.95.24','21','Тестовый');
Insert into FTPS_EXPORT (ID,LOGIN,PASSWORD,HOSTNAME,PORT,DESCRIPTION) values ('3','tpitsvt','DertnPR330','91.228.118.220','21','Тайпит');
Insert into FTPS_EXPORT (ID,LOGIN,PASSWORD,HOSTNAME,PORT,DESCRIPTION) values ('1','severtrans_it','SevTraIt20XX','176.118.31.139','21','IC Distribution');
DELETE FROM ftps WHERE id <>3;
select *  FROM ftps WHERE id <>3;

REM INSERTING into SPRUT4.RESPONSE_FTP
SET DEFINE OFF;
Insert into SPRUT4.RESPONSE_FTP (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,PATH_OUT,VN) values ('6','4','4',null,'OUT','300185');
Insert into SPRUT4.RESPONSE_FTP (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,PATH_OUT,VN) values ('4','5','4',null,'OUT','300185');
Insert into SPRUT4.RESPONSE_FTP (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,PATH_OUT,VN) values ('5','6','4',null,'OUT','300185');
Insert into SPRUT4.RESPONSE_FTP (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,PATH_OUT,VN) values ('62','7','4','IN','OUT','300185');
Insert into SPRUT4.RESPONSE_FTP (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,PATH_OUT,VN) values ('63','9','4','IN','LOADED','300185');
Insert into SPRUT4.RESPONSE_FTP (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,PATH_OUT,VN) values ('64','10','4','IN','LOADED','300185');


REM INSERTING into RESPONSE_FTP_300185
SET DEFINE OFF;
Insert into RESPONSE_FTP_300185 (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('63','9','3','IN','300185','LOADED');
Insert into RESPONSE_FTP_300185 (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('64','10','3','IN','300185','LOADED');
Insert into RESPONSE_FTP_300185 (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('4','5','3',null,'300185','OUT');
Insert into RESPONSE_FTP_300185 (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('5','6','3',null,'300185','OUT');
Insert into RESPONSE_FTP_300185 (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('6','4','3',null,'300185','OUT');
Insert into RESPONSE_FTP_300185 (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('62','7','3','IN','300185','OUT');


--300261 КУХНИ-ТАЙПИТ 300262 КРЕСЛА-ТАЙПИТ 300263 КОРПУС-ТАЙПИТ
Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('9','3','IN','300261','LOADED');
Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('10','3','IN','300261','LOADED');
Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('5','3',null,'300261','OUT');
Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('6','3',null,'300261','OUT');
Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('4','3',null,'300261','OUT');
Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('7','3','IN','300261','OUT');

Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('9','3','IN','300262','LOADED');
Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('10','3','IN','300262','LOADED');
Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('5','3',null,'300262','OUT');
Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('6','3',null,'300262','OUT');
Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('4','3',null,'300262','OUT');
Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('7','3','IN','300262','OUT');

Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('9','3','IN','300263','LOADED');
Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('10','3','IN','300263','LOADED');
Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('5','3',null,'300263','OUT');
Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('6','3',null,'300263','OUT');
Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('4','3',null,'300263','OUT');
Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('7','3','IN','300263','OUT');



--DEPRICATED
Insert into RESPONSE_FTP_EXPORT (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('2','2','1',null,'300191','/Integration.V2/KZG/Outbound/Response');
Insert into RESPONSE_FTP_EXPORT (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('3','3','1',null,'300191','/Integration.V2/KZG/Outbound/Response');
Insert into RESPONSE_FTP_EXPORT (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('1','1','1',null,'300191','/Integration.V2/KZG/Outbound/Response');
Insert into RESPONSE_FTP_EXPORT (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('41','4','2',null,'300227','/Integration.V2/Toshiba/Outbound/Response');
Insert into RESPONSE_FTP_EXPORT (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('42','5','2',null,'300227','/Integration.V2/Toshiba/Outbound/Response');
Insert into RESPONSE_FTP_EXPORT (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('43','6','2',null,'300227','/Integration.V2/Toshiba/Outbound/Response');
delete from response_ftp
where ftp_id <>3;
--update response_ftp set legacy=0 where ftp_id = 3;

--st3.id_du AS order_id , -- guid
--
--kb_sost st3 ,
--
--AND sp.id = st3.id_obsl
--AND st3.id_sost = 'KB_USL99770' --4301 Получено входящее сообщение

-- заполнение
-- SKU xsd
Insert into response_ftp (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT,LEGACY)
values ('65','7','3','/IN','300185',null,'0');
-- ?
INSERT INTO "SPRUT4"."RESPONSE_EXTRA" (ID, PREFIX, VERSION, RESPONSE_TYPE_ID) VALUES ('9', 'IN', 'Тест входящего сообщения', '1');
INSERT INTO "SPRUT4"."RESPONSE_FTP" (ID, RESPONSE_EXTRA_ID, FTP_ID, PATH_IN, VN) VALUES ('63', '9', '3', '/IN', '300185');

SELECT 1 --4302 ещё не отправлено уведомление
 FROM kb_sost
 WHERE id_obsl = sp.id
		 AND id_sost = 'KB_USL99771' --4302 Отправлено исходящее сообщение
		 AND sost_prm LIKE 'OUT_%';

SELECT * FROM kb_sost s
inner join kb_spros p on s.id_obsl = p.id
inner join kb_zak z on p.id_zak =z.id
where z.id_klient=300191
and s.sost_prm like 'IN_%'
--and s.sost_prm='OUT_559_1625510865246.xml';
;
SELECT * FROM kb_spros where id='01023954359';

--TOSHIBA каталог, XPEL уведомления
Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH,VN)
values ('4','2','/Integration.V2/Toshiba/Outbound/Response','300227');
Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH,VN)
values ('5','2','/Integration.V2/Toshiba/Outbound/Response','300227');
Insert into RESPONSE_FTP (RESPONSE_EXTRA_ID,FTP_ID,PATH,VN)
values ('6','2','/Integration.V2/Toshiba/Outbound/Response','300227');

select t.* from kb_xpel_out t where instr(t.xml_data,'ЦБ-00003173',1)>0;--t.file_name like 'OUT_%'  ORDER BY 1 DESC;
select t.* from kb_xpel_out t where instr(file_name,'1023765',1)>0;--t.file_name like 'OUT_%'  ORDER BY 1 DESC;

SELECT vn, path, e.master, e.details, alias_text alias, e.direction, e.order_type
FROM response_ftp r INNER JOIN response_extra e ON r.response_extra_id = e.id
INNER JOIN ftps f ON r.ftp_id = f.id WHERE r.ftp_id = 2;

--FTP localhost 300255 - чай hellmann
Insert into RESPONSE_FTP (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('4','2','3',null,'300255','/Response');
Insert into RESPONSE_FTP (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('5','3','3',null,'300255','/Response');
Insert into RESPONSE_FTP (ID,RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT) values ('6','1','3',null,'300255','/Response');

Insert into RESPONSE_FTP -- SKU
(RESPONSE_EXTRA_ID,FTP_ID,PATH_IN,VN,PATH_OUT)
values ('1','7','/IN','300185','/Response');

--
REM INSERTING into SPRUT4.RESPONSE_EXTRA select * from RESPONSE_EXTRA;
Insert into RESPONSE_EXTRA (ID,RESPONSE_TYPE_ID,MASTER,ALIAS_TEXT,PREFIX,ORDER_TYPE,VERSION)
values               ('7','5',null ,null,'SKU',null, 'Тест входящего сообщения');
Insert into RESPONSE_EXTRA (ID,RESPONSE_TYPE_ID,MASTER,ALIAS_TEXT,PREFIX,ORDER_TYPE,VERSION)
values               ('8','6',null ,null,'PS',null, 'Тест входящего сообщения');

CREATE TABLE RESPONSE_TYPE(
ID INT,
CODE VARCHAR2(255),
NAME VARCHAR2(255),
INOUT_ID INT,
CONSTRAINT msq_type_PK PRIMARY KEY (ID)
);
select * from RESPONSE_TYPE;
INSERT into RESPONSE_TYPE (ID,NAME,INOUT_ID) VALUES (1,'Заказ на поставку',1);
INSERT into RESPONSE_TYPE (ID,NAME,INOUT_ID) VALUES (2,'Заказ на отгрузку',2);
INSERT into RESPONSE_TYPE (ID,NAME,INOUT_ID) VALUES (3,'Подтверждение поставки',3);
INSERT into RESPONSE_TYPE (ID,NAME,INOUT_ID) VALUES (4,'Подтверждение отгрузки',4);
INSERT into RESPONSE_TYPE (ID,NAME,INOUT_ID) VALUES (5,'Справочник SKU',5);
INSERT into RESPONSE_TYPE (ID,NAME,INOUT_ID) VALUES (6,'Справосник Part Stock',6);
INSERT into RESPONSE_TYPE (ID,NAME,INOUT_ID) VALUES (7,'Подтверждение отгрузки',7);
ALTER TABLE response_extra
ADD CONSTRAINT FKRESPONSE_TYPE
   FOREIGN KEY (RESPONSE_TYPE_ID)
   REFERENCES RESPONSE_TYPE (id);



Insert into SPRUT4.RESPONSE_EXTRA (ID,RESPONSE_TYPE_ID,MASTER,ALIAS_TEXT,PREFIX,ORDER_TYPE,DETAILS,VERSION) values ('2','4','SELECT DISTINCT st.dt_sost, -- Дата заявки
							  st2.dt_sost_end /*фактическая дата закрытия заказа*/, st.sost_doc, --Номер ПО 
							  sp.id AS id_obsl, st2.id_du, -- № объекта в солво для прихода: № УП для расхода № заказа в терминах солво
							  (SELECT MIN(st4.dt_sost_end)
									FROM kb_sost st4
									JOIN sv_hvoc hv
									  ON hv.val_id = st4.id_sost
								  WHERE hv.val_short = ''3021''
										  AND hv.voc_id = ''KB_USL''
										  AND tir.id = st4.id_tir) dt_veh, --Фактическое время прибытия машины 
							  z.id_wms id_suppl, --IDSupplier
							  z.id_klient, --VN
							  z.n_zak, -- name
							  z.ur_adr, tir.n_avto, tir.vodit
                  FROM kb_spros sp, kb_sost st, kb_sost st2, kb_zak z, kb_tir tir
						WHERE sp.id = st.id_obsl
                       AND st.id_sost = ''KB_USL60175'' --4103 
                       AND sp.id = st2.id_obsl
                       AND st2.id_sost = ''KB_USL60177'' --4104 отгружен
--                       AND st2.dt_sost_end > SYSDATE - 1 
                       AND NOT EXISTS (SELECT 1 --4302 ещё не отправлено уведомление
                          FROM kb_sost
                         WHERE id_obsl = sp.id
                               AND id_sost = ''KB_USL99771'' --4302 Отправлено исходящее сообщение
                               AND sost_prm LIKE ''OUT_%'') --sol 21122020
--and sp.n_zakaza=''1615472''
                       AND sp.id_zak IN (SELECT id
                                           FROM kb_zak z
                                          WHERE z.id_klient = :id
                                                AND z.id_usr IS NOT NULL)
                       AND sp.id_pok = z.id --поставщик заказа IDSupplier
                       AND sp.id_tir = tir.id --водитель и номер машин','IssueOrderForGoods','OUT','Отгрузка','WITH cte AS ( SELECT DISTINCT 0,s.sku_id, s.name, l.expiration_date, l.production_date, l.lot, l.marker, l.marker2, l.marker3
				, CASE WHEN sn.serial_num IS NULL THEN l.units ELSE 1 END AS units, l.comments, sn.serial_num
			  FROM wms.order_details@wms3 o
			  INNER JOIN wms.loads@wms3   l ON o.order_id = l.order_id
			  INNER JOIN wms.sku@wms3     s ON l.sku_id = s.id
			  LEFT JOIN wms.wms_serial_num@wms3   sn ON sn.sku_id = s.id AND l.id = sn.ship_load_id
			  WHERE o.order_id = :id AND o.sku_id = l.sku_id
			)
			SELECT
			rownum, sku_id, name, expiration_date, production_date, lot, marker, marker2, marker3, SUM(units) qty, comments, serial_num
			FROM cte
			GROUP BY sku_id, name, expiration_date, production_date, lot, marker, marker2, marker3, comments, serial_num, rownum
',null);
Insert into SPRUT4.RESPONSE_EXTRA (ID,RESPONSE_TYPE_ID,MASTER,ALIAS_TEXT,PREFIX,ORDER_TYPE,DETAILS,VERSION) values ('3','7','SELECT DISTINCT st.dt_sost, -- Дата заявки
	  st2.dt_sost_end /*фактическая дата закрытия заказа*/, st.sost_doc, --Номер ПО 
	  sp.id AS id_obsl, st2.id_du, -- № объекта в солво для прихода: № УП для расхода № заказа в терминах солво
	  (SELECT MIN(st4.dt_sost_end)
			FROM kb_sost st4
			JOIN sv_hvoc hv
			  ON hv.val_id = st4.id_sost
		  WHERE hv.val_short = ''3021''
				  AND hv.voc_id = ''KB_USL''
				  AND tir.id = st4.id_tir) dt_veh, --Фактическое время прибытия машины 
	  z.id_wms id_suppl, --IDSupplier
	  z.id_klient, --VN
	  z.n_zak, -- name
	  z.ur_adr, tir.n_avto, tir.vodit, z2.n_zak zak_name --Customer
FROM kb_spros sp, kb_sost st, kb_sost st2, kb_zak z2, kb_zak z -- supplier
		 , kb_tir tir
  WHERE sp.id = st.id_obsl
		  AND st.id_sost = ''KB_USL60175'' --4103 
		  AND sp.id = st2.id_obsl
		  AND st2.id_sost = ''KB_USL60189'' --4111 Заказ на СОХ готов к отгрузке
--                       AND st2.dt_sost_end > SYSDATE - 1
		 --AND sp.id = st3.id_obsl
		 --AND st3.id_sost = ''KB_USL99770''
		 --AND st3.sost_prm LIKE ''РО_ХЛМ%'' --4301---признак пришёл на FTP
		  AND NOT EXISTS (SELECT 1 --4302 ещё не отправлено уведомление
			  FROM kb_sost
			 WHERE id_obsl = sp.id
					 AND id_sost = ''KB_USL99771''
					 AND sost_prm LIKE ''PICK_%'') --sol 21122020
		  AND sp.id_zak = z2.id
		  AND z2.id_klient = :id
		  AND z2.id_usr IS NOT NULL
		  AND sp.id_pok = z.id --поставщик заказа IDSupplier
		  AND sp.id_tir = tir.id --водиьеля и номер машины','PickOrderForGoods','PICK','Отгрузка','WITH cte AS ( SELECT DISTINCT 0,s.sku_id, s.name, l.expiration_date, l.production_date, l.lot, l.marker, l.marker2, l.marker3
				, CASE WHEN sn.serial_num IS NULL THEN l.units ELSE 1 END AS units, l.comments, sn.serial_num
			  FROM wms.order_details@wms3 o
			  INNER JOIN wms.loads@wms3   l ON o.order_id = l.order_id
			  INNER JOIN wms.sku@wms3     s ON l.sku_id = s.id
			  LEFT JOIN wms.wms_serial_num@wms3   sn ON sn.sku_id = s.id AND l.id = sn.ship_load_id
			  WHERE o.order_id = :id AND o.sku_id = l.sku_id
			)
			SELECT
			rownum, sku_id, name, expiration_date, production_date, lot, marker, marker2, marker3, SUM(units) qty, comments, serial_num
			FROM cte
			GROUP BY sku_id, name, expiration_date, production_date, lot, marker, marker2, marker3, comments, serial_num, rownum
',null);
Insert into SPRUT4.RESPONSE_EXTRA (ID,RESPONSE_TYPE_ID,MASTER,ALIAS_TEXT,PREFIX,ORDER_TYPE,DETAILS,VERSION) values ('4','3','SELECT DISTINCT st.dt_sost,                           -- Дата заявки
                st2.dt_sost_end /*фактическая дата закрытия заказа*/,
                st.sost_doc,                          --Номер ПО
                sp.id AS                    id_obsl,
                st2.id_du,                            -- № объекта в солво для прихода: № УП для расхода № заказа в терминах солво
                (SELECT MIN(st4.dt_sost_end)
                 FROM kb_sost st4
                          JOIN sv_hvoc hv
                               ON hv.val_id = st4.id_sost
                 WHERE hv.val_short = ''3021''
                   AND hv.voc_id = ''KB_USL''
                   AND tir.id = st4.id_tir) dt_veh,   --Фактическое время прибытия машины
                z.id_wms                    id_suppl, --IDSupplier
                z.id_klient,                          --VN
                z.n_zak,                              -- name
                z.ur_adr,
                tir.n_avto,
                tir.vodit,
                z2.id_usr
FROM kb_spros sp
   , kb_sost st
   , kb_sost st2
   , kb_zak z2
   , kb_zak z -- supplier
   , kb_tir tir
WHERE sp.id = st.id_obsl
  AND st.id_sost = ''KB_USL60173''  --4101
  AND sp.id = st2.id_obsl
  AND st2.id_sost = ''KB_USL60174'' --4102
  AND sp.id_zak IN (SELECT id
                    FROM kb_zak z
                    WHERE z.id_klient =:id
                      AND z.id_usr IS NOT NULL /*''KB_USR92734''*/
)
  AND sp.id_pok = z.id            --поставщик заказа IDSupplier
  AND sp.id_tir = tir.id          --водиьеля и номер машины
  AND sp.id_zak = z2.id','IssueReceiptForGoods','IN','Поставка','WITH cte AS ( SELECT DISTINCT 0,s.sku_id, s.name, l.expiration_date, l.production_date, l.lot, l.marker, l.marker2, l.marker3
		, CASE WHEN sn.serial_num IS NULL THEN l.units ELSE 1 END AS units, l.comments, sn.serial_num
	  FROM wms.rcn_detail@wms3 r
	  INNER JOIN wms.loads@wms3   l ON r.rcn_id = l.rcn_id
	  INNER JOIN wms.sku@wms3     s ON l.sku_id = s.id
	  LEFT JOIN wms.wms_serial_num@wms3   sn ON sn.sku_id = s.id AND l.id = sn.receive_load_id
	  WHERE r.inc_id = :id AND r.sku_id = l.sku_id --УП
	)
	SELECT
	rownum,sku_id, name, expiration_date, production_date, lot, marker, marker2, marker3, SUM(units) qty, comments, serial_num
	FROM cte
	GROUP BY sku_id, name, expiration_date, production_date, lot, marker, marker2, marker3, comments, serial_num
, rownum
','TEST');
Insert into SPRUT4.RESPONSE_EXTRA (ID,RESPONSE_TYPE_ID,MASTER,ALIAS_TEXT,PREFIX,ORDER_TYPE,DETAILS,VERSION) values ('5','4','SELECT DISTINCT st.dt_sost, -- Дата заявки
							  st2.dt_sost_end /*фактическая дата закрытия заказа*/, st.sost_doc, --Номер ПО 
							  sp.id AS id_obsl, st2.id_du, -- № объекта в солво для прихода: № УП для расхода № заказа в терминах солво
							  (SELECT MIN(st4.dt_sost_end)
									FROM kb_sost st4
									JOIN sv_hvoc hv
									  ON hv.val_id = st4.id_sost
								  WHERE hv.val_short = ''3021''
										  AND hv.voc_id = ''KB_USL''
										  AND tir.id = st4.id_tir) dt_veh, --Фактическое время прибытия машины 
							  z.id_wms id_suppl, --IDSupplier
							  z.id_klient, --VN
							  z.n_zak, -- name
							  z.ur_adr, tir.n_avto, tir.vodit
                  FROM kb_spros sp, kb_sost st, kb_sost st2, kb_zak z, kb_tir tir
						WHERE sp.id = st.id_obsl
                       AND st.id_sost = ''KB_USL60175'' --4103 
                       AND sp.id = st2.id_obsl
                       AND st2.id_sost = ''KB_USL60177'' --4104 отгружен
                       AND sp.id_zak IN (SELECT id
                                           FROM kb_zak z
                                          WHERE z.id_klient = :id
                                                AND z.id_usr IS NOT NULL)
                       AND sp.id_pok = z.id --поставщик заказа IDSupplier
                       AND sp.id_tir = tir.id --водитель и номер машин','IssueOrderForGoods','OUT','Отгрузка','WITH cte AS ( SELECT DISTINCT 0,s.sku_id, s.name, l.expiration_date, l.production_date, l.lot, l.marker, l.marker2, l.marker3
				, CASE WHEN sn.serial_num IS NULL THEN l.units ELSE 1 END AS units, l.comments, sn.serial_num
			  FROM wms.order_details@wms3 o
			  INNER JOIN wms.loads@wms3   l ON o.order_id = l.order_id
			  INNER JOIN wms.sku@wms3     s ON l.sku_id = s.id
			  LEFT JOIN wms.wms_serial_num@wms3   sn ON sn.sku_id = s.id AND l.id = sn.ship_load_id
			  WHERE o.order_id = :id AND o.sku_id = l.sku_id
			)
			SELECT
			rownum, sku_id, name, expiration_date, production_date, lot, marker, marker2, marker3, SUM(units) qty, comments, serial_num
			FROM cte
			GROUP BY sku_id, name, expiration_date, production_date, lot, marker, marker2, marker3, comments, serial_num, rownum
','TEST');
Insert into SPRUT4.RESPONSE_EXTRA (ID,RESPONSE_TYPE_ID,MASTER,ALIAS_TEXT,PREFIX,ORDER_TYPE,DETAILS,VERSION) values ('6','7','SELECT DISTINCT st.dt_sost, -- Дата заявки
	  st2.dt_sost_end /*фактическая дата закрытия заказа*/, st.sost_doc, --Номер ПО 
	  sp.id AS id_obsl, st2.id_du, -- № объекта в солво для прихода: № УП для расхода № заказа в терминах солво
	  (SELECT MIN(st4.dt_sost_end)
			FROM kb_sost st4
			JOIN sv_hvoc hv
			  ON hv.val_id = st4.id_sost
		  WHERE hv.val_short = ''3021''
				  AND hv.voc_id = ''KB_USL''
				  AND tir.id = st4.id_tir) dt_veh, --Фактическое время прибытия машины 
	  z.id_wms id_suppl, --IDSupplier
	  z.id_klient, --VN
	  z.n_zak, -- name
	  z.ur_adr, tir.n_avto, tir.vodit, z2.n_zak zak_name --Customer
FROM kb_spros sp, kb_sost st, kb_sost st2, kb_zak z2, kb_zak z -- supplier
		 , kb_tir tir
  WHERE sp.id = st.id_obsl
		  AND st.id_sost = ''KB_USL60175'' --4103 
		  AND sp.id = st2.id_obsl
		  AND st2.id_sost = ''KB_USL60189'' --4111 Заказ на СОХ готов к отгрузке
		  AND sp.id_zak = z2.id
		  AND z2.id_klient = :id
		  AND z2.id_usr IS NOT NULL
		  AND sp.id_pok = z.id --поставщик заказа IDSupplier
		  AND sp.id_tir = tir.id --водиьеля и номер машины','PickOrderForGoods','PICK','Отгрузка','WITH cte AS ( SELECT DISTINCT 0,s.sku_id, s.name, l.expiration_date, l.production_date, l.lot, l.marker, l.marker2, l.marker3
				, CASE WHEN sn.serial_num IS NULL THEN l.units ELSE 1 END AS units, l.comments, sn.serial_num
			  FROM wms.order_details@wms3 o
			  INNER JOIN wms.loads@wms3   l ON o.order_id = l.order_id
			  INNER JOIN wms.sku@wms3     s ON l.sku_id = s.id
			  LEFT JOIN wms.wms_serial_num@wms3   sn ON sn.sku_id = s.id AND l.id = sn.ship_load_id
			  WHERE o.order_id = :id AND o.sku_id = l.sku_id
			)
			SELECT
			rownum, sku_id, name, expiration_date, production_date, lot, marker, marker2, marker3, SUM(units) qty, comments, serial_num
			FROM cte
			GROUP BY sku_id, name, expiration_date, production_date, lot, marker, marker2, marker3, comments, serial_num, rownum
','TEST');


-- TEST
Insert into SPRUT4.RESPONSE_EXTRA (ID,MASTER,ALIAS_TEXT,VOC,DIRECTION,ORDER_TYPE,DETAILS) values ('4','SELECT DISTINCT st.dt_sost,                           -- Дата заявки
                st2.dt_sost_end /*фактическая дата закрытия заказа*/,
                st.sost_doc,                          --Номер ПО
                sp.id AS                    id_obsl,
                st2.id_du,                            -- № объекта в солво для прихода: № УП для расхода № заказа в терминах солво
                (SELECT MIN(st4.dt_sost_end)
                 FROM kb_sost st4
                          JOIN sv_hvoc hv
                               ON hv.val_id = st4.id_sost
                 WHERE hv.val_short = ''3021''
                   AND hv.voc_id = ''KB_USL''
                   AND tir.id = st4.id_tir) dt_veh,   --Фактическое время прибытия машины
                z.id_wms                    id_suppl, --IDSupplier
                z.id_klient,                          --VN
                z.n_zak,                              -- name
                z.ur_adr,
                tir.n_avto,
                tir.vodit,
                z2.id_usr
FROM kb_spros sp
   , kb_sost st
   , kb_sost st2
   , kb_zak z2
   , kb_zak z -- supplier
   , kb_tir tir
WHERE sp.id = st.id_obsl
  AND st.id_sost = ''KB_USL60173''  --4101
  AND sp.id = st2.id_obsl
  AND st2.id_sost = ''KB_USL60174'' --4102
  AND sp.id_zak IN (SELECT id
                    FROM kb_zak z
                    WHERE z.id_klient =:id
                      AND z.id_usr IS NOT NULL /*''KB_USR92734''*/
)
  AND sp.id_pok = z.id            --поставщик заказа IDSupplier
  AND sp.id_tir = tir.id          --водиьеля и номер машины
  AND sp.id_zak = z2.id','IssueReceiptForGoods','KB_USL60174','IN','Поставка','WITH cte AS ( SELECT DISTINCT 0,s.sku_id, s.name, l.expiration_date, l.production_date, l.lot, l.marker, l.marker2, l.marker3
		, CASE WHEN sn.serial_num IS NULL THEN l.units ELSE 1 END AS units, l.comments, sn.serial_num
	  FROM wms.rcn_detail@wms3 r
	  INNER JOIN wms.loads@wms3   l ON r.rcn_id = l.rcn_id
	  INNER JOIN wms.sku@wms3     s ON l.sku_id = s.id
	  LEFT JOIN wms.wms_serial_num@wms3   sn ON sn.sku_id = s.id AND l.id = sn.receive_load_id
	  WHERE r.inc_id = :id AND r.sku_id = l.sku_id --УП
	)
	SELECT
	rownum,sku_id, name, expiration_date, production_date, lot, marker, marker2, marker3, SUM(units) qty, comments, serial_num
	FROM cte
	GROUP BY sku_id, name, expiration_date, production_date, lot, marker, marker2, marker3, comments, serial_num
, rownum
');
Insert into SPRUT4.RESPONSE_EXTRA (ID,MASTER,ALIAS_TEXT,VOC,DIRECTION,ORDER_TYPE,DETAILS) values ('5','SELECT DISTINCT st.dt_sost, -- Дата заявки
							  st2.dt_sost_end /*фактическая дата закрытия заказа*/, st.sost_doc, --Номер ПО 
							  sp.id AS id_obsl, st2.id_du, -- № объекта в солво для прихода: № УП для расхода № заказа в терминах солво
							  (SELECT MIN(st4.dt_sost_end)
									FROM kb_sost st4
									JOIN sv_hvoc hv
									  ON hv.val_id = st4.id_sost
								  WHERE hv.val_short = ''3021''
										  AND hv.voc_id = ''KB_USL''
										  AND tir.id = st4.id_tir) dt_veh, --Фактическое время прибытия машины 
							  z.id_wms id_suppl, --IDSupplier
							  z.id_klient, --VN
							  z.n_zak, -- name
							  z.ur_adr, tir.n_avto, tir.vodit
                  FROM kb_spros sp, kb_sost st, kb_sost st2, kb_zak z, kb_tir tir
						WHERE sp.id = st.id_obsl
                       AND st.id_sost = ''KB_USL60175'' --4103 
                       AND sp.id = st2.id_obsl
                       AND st2.id_sost = ''KB_USL60177'' --4104 отгружен
                       AND sp.id_zak IN (SELECT id
                                           FROM kb_zak z
                                          WHERE z.id_klient = :id
                                                AND z.id_usr IS NOT NULL)
                       AND sp.id_pok = z.id --поставщик заказа IDSupplier
                       AND sp.id_tir = tir.id --водитель и номер машин','IssueOrderForGoods','KB_USL60175','OUT','Отгрузка','WITH cte AS ( SELECT DISTINCT 0,s.sku_id, s.name, l.expiration_date, l.production_date, l.lot, l.marker, l.marker2, l.marker3
				, CASE WHEN sn.serial_num IS NULL THEN l.units ELSE 1 END AS units, l.comments, sn.serial_num
			  FROM wms.order_details@wms3 o
			  INNER JOIN wms.loads@wms3   l ON o.order_id = l.order_id
			  INNER JOIN wms.sku@wms3     s ON l.sku_id = s.id
			  LEFT JOIN wms.wms_serial_num@wms3   sn ON sn.sku_id = s.id AND l.id = sn.ship_load_id
			  WHERE o.order_id = :id AND o.sku_id = l.sku_id
			)
			SELECT
			rownum, sku_id, name, expiration_date, production_date, lot, marker, marker2, marker3, SUM(units) qty, comments, serial_num
			FROM cte
			GROUP BY sku_id, name, expiration_date, production_date, lot, marker, marker2, marker3, comments, serial_num, rownum
');
Insert into SPRUT4.RESPONSE_EXTRA (ID,MASTER,ALIAS_TEXT,VOC,DIRECTION,ORDER_TYPE,DETAILS) values ('6','SELECT DISTINCT st.dt_sost, -- Дата заявки
	  st2.dt_sost_end /*фактическая дата закрытия заказа*/, st.sost_doc, --Номер ПО 
	  sp.id AS id_obsl, st2.id_du, -- № объекта в солво для прихода: № УП для расхода № заказа в терминах солво
	  (SELECT MIN(st4.dt_sost_end)
			FROM kb_sost st4
			JOIN sv_hvoc hv
			  ON hv.val_id = st4.id_sost
		  WHERE hv.val_short = ''3021''
				  AND hv.voc_id = ''KB_USL''
				  AND tir.id = st4.id_tir) dt_veh, --Фактическое время прибытия машины 
	  z.id_wms id_suppl, --IDSupplier
	  z.id_klient, --VN
	  z.n_zak, -- name
	  z.ur_adr, tir.n_avto, tir.vodit, z2.n_zak zak_name --Customer
FROM kb_spros sp, kb_sost st, kb_sost st2, kb_zak z2, kb_zak z -- supplier
		 , kb_tir tir
  WHERE sp.id = st.id_obsl
		  AND st.id_sost = ''KB_USL60175'' --4103 
		  AND sp.id = st2.id_obsl
		  AND st2.id_sost = ''KB_USL60189'' --4111 Заказ на СОХ готов к отгрузке
		  AND sp.id_zak = z2.id
		  AND z2.id_klient = :id
		  AND z2.id_usr IS NOT NULL
		  AND sp.id_pok = z.id --поставщик заказа IDSupplier
		  AND sp.id_tir = tir.id --водиьеля и номер машины','PickOrderForGoods','KB_USL60189','PICK','Отгрузка','WITH cte AS ( SELECT DISTINCT 0,s.sku_id, s.name, l.expiration_date, l.production_date, l.lot, l.marker, l.marker2, l.marker3
				, CASE WHEN sn.serial_num IS NULL THEN l.units ELSE 1 END AS units, l.comments, sn.serial_num
			  FROM wms.order_details@wms3 o
			  INNER JOIN wms.loads@wms3   l ON o.order_id = l.order_id
			  INNER JOIN wms.sku@wms3     s ON l.sku_id = s.id
			  LEFT JOIN wms.wms_serial_num@wms3   sn ON sn.sku_id = s.id AND l.id = sn.ship_load_id
			  WHERE o.order_id = :id AND o.sku_id = l.sku_id
			)
			SELECT
			rownum, sku_id, name, expiration_date, production_date, lot, marker, marker2, marker3, SUM(units) qty, comments, serial_num
			FROM cte
			GROUP BY sku_id, name, expiration_date, production_date, lot, marker, marker2, marker3, comments, serial_num, rownum
');
