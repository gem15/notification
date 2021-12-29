create or replace procedure MSG_4101_test(p_msg CLOB :='',p_err out varchar2, p_info out varchar2) is
	v_n_avto    VARCHAR2(40); --!!! delete, order_rec make public
c_test CONSTANT VARCHAR2(20) := 'TEST_';--TEST_

	TYPE order_rectype IS RECORD (
		vn kb_zak.id_klient%TYPE, msgid VARCHAR2(36), number1 VARCHAR2(38), orderDate DATE, ordertype NUMBER(1,0), typeofdelivery
		VARCHAR2(100), plannedDate DATE, contrCode VARCHAR2(100), contrName VARCHAR2(1000), contrAddress
		VARCHAR2(1000), carrierCode VARCHAR2(100), carrierName VARCHAR2(1000), carrierTIN		VARCHAR2(13),
     numbercar VARCHAR2(100), driver VARCHAR2(100) DEFAULT NULL, docid VARCHAR2(36), action NUMBER(1), dopinfconsignee VARCHAR2
		(100), comment1 VARCHAR2(1000)
	);
	order_rec order_rectype;
	TYPE sost_rec IS RECORD (
		id sv_hvoc.val_id%TYPE, short sv_hvoc.val_short%TYPE, full sv_hvoc.val_full%TYPE
	);
	l_sost sost_rec;
  v_type_order_name varchar2(1000); -- FK

	 v_planned_Date	DATE;
	 v_id_tir_old     NUMBER;
	 v_inwork			BOOLEAN := FALSE; -- заказ в работе?
	 v_edit				BOOLEAN := FALSE;-- never used
--	 l_id_sost			VARCHAR2(38);
-- Test *****

	v_id_zak     kb_zak.id%TYPE; --v_id_zak, v_id_wms, v_id_svh, v_prfx, v_id_wms_zak, v_id_usr
    v_prfx       VARCHAR2(5 CHAR);
    v_id_wms_zak VARCHAR2(38);
    v_id_svh     VARCHAR2(38);
    v_id_usr     VARCHAR2(38);
    v_id_wms     kb_zak.id_wms%TYPE;
    v_new_rec    kb_zak.id%TYPE; --получатель
    v_id_obsl    kb_spros.id%TYPE;
    v_id_dog     VARCHAR2(50);
    v_id_tzs     kb_sost.id_tzs%TYPE;
    v_id_sost    kb_sost.id%TYPE;
    cnt_sku      NUMBER;
    v_id_tir     NUMBER;
    v_tmp        VARCHAR2(38);
    v_counter    NUMBER;
    v_rows       NUMBER;
    v_sost_doc   VARCHAR2(38); -- never used
    pack_err     VARCHAR2(3800);
    vn_not_found EXCEPTION;

--	 v_order_Date	DATE;
--	 v_OrderType	VARCHAR2(20);
   v_msg        CLOB;
--	v_one_char	VARCHAR2(1);
	 l_err_sku	VARCHAR2( 32000):= NULL;
	 l_err_qty	VARCHAR2( 32000):= NULL;
   v_id_info    kb_zak.id_info%type; --sol 22.12.2021
   v_id_carrier VARCHAR2(38):= NULL; -- перевозчик ПЗ 7382 29.12.2021 
BEGIN
	--***** Test '20.12.2021 23:00:00' 20.12.21 23:00:00
  IF p_msg IS NULL THEN
    v_msg := TO_CLOB('<Shell>
      <customerID>300185</customerID>
      <msgID>7187c8a0-013f-476b-aa87-f65961d4631b</msgID>
      <msgType>1</msgType>
      <order>
        <guid>cf843545-9eb5-11eb-80c0-00155d0c6c0M</guid>
        <action>0</action>
        <orderType>true</orderType>
        <orderKind>test</orderKind>
        <orderNo>MK05</orderNo>
        <orderDate>2021-08-17T01:00:59</orderDate>
        <plannedDate>2021-12-21T05:00:05</plannedDate>
        <contrCode>T019597</contrCode>
        <contrName>ЦТО+ ООО</contrName>
        <contrAddress>117209, Москва г, Керченская ул., дом № 6, корпус 3, квартира 56</contrAddress>
        <licencePlate>K 333 MA05</licencePlate>
        <driver>Trump05</driver>
        <carrierCode></carrierCode>
        <carrierName>Тестовый перевозчик</carrierName>
        <carrierTIN>1234567890123</carrierTIN>
        <orderLine>
          <lineNumber>1</lineNumber>
          <article>00-01185415</article>
          <name>Офисное кресло EChair-685 TС ткань черный пластик</name>
          <qty>6</qty>
          <category>0</category>
        </orderLine>
      </order>
    </Shell>');
  ELSE
    v_msg := p_msg;
  END IF;       
--!!!***** 00-01185415 пресс-шайба 00-07064083 Офисное кресло EChair-685
--	v_msg := REPLACE(p_msg, ' xmlns="http://www.severtrans.com"');

  SELECT 
		extractvalue(VALUE(t), '/Shell/customerID') AS VN, --ВН клиента
		extractvalue(VALUE(t), '/Shell/msgID') AS msgID, 
		extractvalue(VALUE(t), '/Shell//order/orderNo') AS number1, --Номер ПО
		to_date(REPLACE(extractvalue(VALUE(t), '/Shell/order/orderDate'),'T',' '), 'yyyy-mm-dd hh24:mi:ss') AS orderDate, --Дата ПО
		CASE WHEN EXTRACTVALUE(VALUE(T), '/Shell/order/orderType') ='true' THEN 1 ELSE 0 END AS ORDERTYPE, --Тип заказа
		extractvalue(VALUE(t), '/Shell/order/orderKind') AS TypeOfDelivery, --Тип поставки
		to_date(REPLACE(extractvalue(VALUE(t), '/Shell/order/plannedDate'),'T',' '), 'yyyy-mm-dd hh24:mi:ss') AS plannedDate,
		extractvalue(VALUE(t), '/Shell/order/contrCode') AS contrCode, --код поставщика
		extractvalue(VALUE(t), '/Shell/order/contrName') AS contrName, --имя поставщика
		extractvalue(VALUE(t), '/Shell/order/contrAddress') AS contrAddress, --адрес поставщика
		extractvalue(VALUE(t), '/Shell/order/carrierCode') AS carrierCode, --код перевозчика
		extractvalue(VALUE(t), '/Shell/order/carrierName') AS carrierName, --имя перевозчика
		extractvalue(VALUE(t), '/Shell/order/carrierTIN') AS carrierTIN, --ИНН перевозчика
		extractvalue(VALUE(t), '/Shell/order/licencePlate') AS NumberCar, --Номер машины
		extractvalue(VALUE(t), '/Shell/order/driver') Driver,
		extractvalue(VALUE(t), '/Shell/order/guid') docID,
		extractvalue(VALUE(t), '/Shell/order/action') action,
		extractvalue(VALUE(t), '/Shell/order/dopInfConsignee') AS DopInfConsignee, --уточнение информации о специфике сборки/упаковки
		extractvalue(VALUE(t), '/Shell/order/comment') Comment1
		INTO order_rec
  FROM TABLE(xmlsequence(extract(xmltype(v_msg),'//Shell'))) t;

/*DBMS_OUTPUT.PUT_LINE( to_char(order_rec.plannedDate, 'yyyy-mm-dd hh24:mi:ss') );
RAISE vn_not_found;
*/
  --== init orderKind record
	SELECT val_id id, val_short   short, val_full full
	INTO l_sost	FROM sv_hvoc	WHERE val_id =
		CASE
			WHEN order_rec.ordertype = 0 THEN 'KB_USL60173'
			ELSE 'KB_USL60175'
		END;

	--=== разберёмся с клиентом, в переносном смысле или в прямом...
  BEGIN
   SELECT z.id, z.id_wms, z.id_svh, z.prf_wms, order_rec.contrCode, z.id_usr
   /*
   TODO: owner="It06" category="Optimize" priority="2 - Medium" created="14.12.2021"
   text="chnge to record"
   */
    INTO v_id_zak, v_id_wms, v_id_svh, v_prfx, v_id_wms_zak, v_id_usr
    FROM kb_zak z
   WHERE z.id_klient = order_rec.vn
       AND z.id_usr IN ('KB_USR92734', 'KB_USR99992');
  EXCEPTION
   WHEN OTHERS THEN
    p_err := 'неправильный ВН';
    RAISE vn_not_found;
  END;

    --== ищем заказ
	BEGIN
		SELECT sp.id, sp.id_tir, z.id_svh, st1.id
			INTO v_id_obsl, v_id_tir_old, v_id_svh, v_id_sost
			FROM kb_sost st
		 INNER JOIN kb_spros sp
				ON st.id_obsl = sp.ID
		 INNER JOIN kb_zak z
				ON z.ID = sp.id_zak
		 INNER JOIN kb_sost st1
				ON st.id_obsl = st1.id_obsl AND st1.id_sost = l_sost.id --IN ('KB_USL60173','KB_USL60175')
		 WHERE st.id_sost = 'KB_USL99770' AND st.id_du = order_rec.docID; --'965e4682-9ec3-11eb-80c0-00155d0c0000' ;
	EXCEPTION
		WHEN no_data_found THEN
			v_id_obsl := NULL;
	END;

  IF v_id_obsl IS NULL then --=== новый заказ

    if order_rec.action != 0 THEN
      DBMS_OUTPUT.PUT_LINE( 'not found' );
      p_err := 'Не найден заказ для action = '|| order_rec.action;
      return;
    end if;
	
	  --=== заделаем машину, почти как Генри Форд
	  IF order_rec.numbercar IS NOT NULL THEN
		 order_rec.numbercar := utility_pkg.String2AutoNumber(order_rec.numbercar);
		 v_id_tir := Utility_Pkg.find_tir(order_rec.numbercar, v_id_zak, order_rec.plannedDate);
		 IF v_id_tir IS NULL THEN
       SELECT SV_UTILITIES.FORM_KEY(KB_TIR_SEQ.NextVal) INTO v_id_tir FROM dual;
			INSERT INTO KB_TIR
			  (id,n_tir, id_iper, id_trans, n_avto, vodit, Id_Svh)
			VALUES
			  (v_id_tir,
        'Б/Н СОХ',
			  'KB_PER24667', -- Междугородняя
			  'KB_TRN24662', -- АвтоМобильный, 
			  order_rec.numbercar,
			  order_rec.driver,
			  v_id_svh);-- RETURNING id INTO v_id_tir;
		 END IF;
     --=== обработка перевозчика
     IF order_rec.carrierCode IS NOT NULL THEN
       -- ищем/создаём
      BEGIN
        SELECT z.id INTO v_id_carrier
        FROM kb_zak z
        WHERE z.id_wms = order_rec.carrierCode
        AND z.id_klient = order_rec.vn
        AND z.id_tip_zak = 'KB_TZK24516'; --	тип ПЕРЕВОЗЧИК
      EXCEPTION
        WHEN OTHERS THEN
        NULL;
      END;
      IF v_id_carrier IS NULL THEN
        --делаем нового перевозчика
         INSERT INTO kb_zak
          (id_klient, id_wms, naimen, inn_zak,id_tip_zak)
         VALUES
          (order_rec.vn,
          order_rec.carrierCode,
          order_rec.carrierName,
          order_rec.carrierTIN,
          'KB_TZK82894') RETURNING id INTO v_id_carrier;
      END IF;
      -- привязываем к ТС
      UPDATE kb_tir SET id_per = v_id_carrier WHERE id = v_id_tir;
     END IF;
	  ELSE
		 v_id_tir := NULL;
	  END IF;

	  --=== теперь сделаем или найдём контрагента
	  if order_rec.contrCode is null then
		 v_new_rec := v_id_zak;
	  else
		 BEGIN
			SELECT MIN(z.id)
			  INTO v_new_rec
			  FROM kb_zak z
			WHERE z.id_wms = order_rec.contrCode
					AND z.id_klient = order_rec.vn;
		 EXCEPTION
			WHEN OTHERS THEN
			  NULL;
		 END;
	  end if; 
	  --если не нашли
	  IF v_new_rec IS NULL THEN
		 --делаем нового контрагента 
		 INSERT INTO kb_zak
			(id_klient, id_wms, n_zak, id_tip_zak, naimen, ur_adr, id_info)
		 VALUES
			(order_rec.vn,
			order_rec.contrCode,
			order_rec.contrName,
			'KB_TZK82894',
			order_rec.contrName,
			order_rec.contrAddress,
      v_id_info) RETURNING id INTO v_new_rec;
      
      --sol 22.12.2021 копируем из клиента в контрагенты кастомные типы поставка/отгрузка
       insert into kb_zak_type_conv (id_zak, type_cox_zak, type_cox)
       select v_new_rec, k.type_cox_zak, k.type_cox from kb_zak_type_conv k where k.id_zak = v_id_zak;

	  END IF;
	
	  --=== создание заказа
	  INSERT INTO kb_spros
		 (n_gruz, dt_zakaz, id_zak, id_pok, is_postavka, id_spros, id_tir, id_kat)
	  VALUES
		 (c_test || 'FTP '||CASE WHEN order_rec.orderType = 0 THEN 'УП' ELSE 'РО' END||' --> ' || order_rec.number1,
		 to_char(SYSDATE, 'dd.mm.rr'),
		 v_id_zak,
		 v_new_rec,
		 order_rec.orderType,--'0''1',
		 NULL,
		 v_id_tir,
		 'KB_TGR98182') RETURNING id INTO v_id_obsl;

	  --=== поиск договора  
	  SELECT MIN(decode(nvl(a.id_dog, a.id_usl),
							  NULL,
							  decode(dg.id_tdoc /*Тип документа*/, 'KB_TDD39116', '2', 'KB_TDD39115', '1', NULL) || dg.id,
							  '0' || nvl(a.id_dog, a.id_usl)))
		 INTO v_id_dog
		 FROM kb_spros sp, kb_dog dg, --ДОГОВОРЫ
			  kb_spros_dog a --ДОГОВОРЫ ЗАКАЗА
	  WHERE sp.id = v_id_obsl
			  AND a.id_obsl(+) = sp.id
			  AND dg.id_zak = sp.id_zak(+)
			  AND dg.id_isp(+) = '010277043'
			  AND nvl(dg.dt_end(+), SYSDATE) >= SYSDATE
			  AND dg.id_tdoc IN ('KB_TDD39116', 'KB_TDD39115');
	  --привязка договора к заказу                     
	  INSERT INTO kb_spros_dog (id_obsl, id_vtu, id_dog) VALUES (v_id_obsl, 'KB_VTU50767', substr(v_id_dog, 2));

	  --===== наполнение заказа 4101\3
	  INSERT INTO kb_sost
		 (id_obsl, dt_sost, dt_sost_end, id_sost, id_dog, sost_doc, id_isp, id_tzs, dt_doc, sost_prm)
	  VALUES
		 (v_id_obsl,
		 order_rec.plannedDate,
		 order_rec.plannedDate,
		 l_sost.id,
		 substr(v_id_dog, 2),
		 c_test || order_rec.Number1,
		 '010277043',
		 v_id_tzs,
		 order_rec.orderDate,
		 order_rec.Comment1)
	  RETURNING id INTO v_id_sost;

--	  --=== обработка типа поставки/отгрузки (ждём)
    p_type_order(p_id_pol => v_new_rec,
      p_in_out => CASE WHEN order_rec.orderType = 0 THEN 'IN' ELSE 'OUT' END,
      p_type_zak => order_rec.typeofdelivery,
      p_dopinfconsignee => order_rec.DopInfConsignee,
      p_type_order_code => v_id_tzs,
      p_type_order_name => v_type_order_name,-- DUMMY
      p_err => pack_err);	 
			
    if pack_err <> 'OK' then
      p_err := pack_err;
      RAISE vn_not_found;
    end if;
	--RAISE vn_not_found;--!!! куьщму ьу
	  --=== добавляем событие 4301 получено входящее сообщение
		INSERT INTO kb_sost
			(id_obsl, dt_sost, dt_sost_end, id_sost, sost_prm, id_isp, id_du) --sost_doc,
		VALUES
			(v_id_obsl,
			 SYSDATE,
			 SYSDATE,
			 'KB_USL99770',
			 CASE WHEN order_rec.orderType = 0 THEN 'УП' ELSE 'РО' END,
			 '010277043',
			 order_rec.docID); --rec.guid p_id_file,
       
  ELSE --==== изменение заказа  ========================

      --== Заказ завершён
      SELECT COUNT(*) INTO cnt_sku FROM kb_sost st
       WHERE st.id_obsl = v_id_obsl AND st.id_sost IN ('KB_USL39027', 'KB_USL50541');
      IF cnt_sku >0 THEN 
        p_err := 'Заказ выполнен/отменён';
        RAISE vn_not_found;
      END IF;
        
      --== заказ уже в работе ?
      SELECT count(*) INTO cnt_sku
      FROM kb_sost st 
      WHERE  st.id_obsl= v_id_obsl and (st.id_sost = 'KB_USL60183' OR st.sost_prm like 'УП в статусе "G"%');
      v_inwork := cnt_sku > 0;

      --== блок дополнительных проверок
      IF order_rec.action = 0 AND v_inwork THEN 
        p_err := 'Заказ уже в работе.';
        RAISE vn_not_found;
      ELSIF order_rec.action = 1 AND NOT v_inwork THEN
        p_err := 'Заказ уже в работе. Удаление невозможно';
        RAISE vn_not_found;
      ELSIF order_rec.action > 2 THEN
        p_err := 'Непонятное значение action';
        RAISE vn_not_found;
      END IF;

      --== Удаление заказа
      if order_rec.action = 1 THEN --delete order not ready yet
        DBMS_OUTPUT.PUT_LINE( 'delete order' );
        return;
      end if;

      --== Изменение плановой даты PD
/*      select dt_sost into v_planned_Date from kb_sost where id_obsl=v_id_obsl AND id_sost = l_sost.id;
      IF v_planned_Date != order_rec.plannedDate THEN
    		p_info := p_info || 'Замена плановой даты'||CHR(10);
        UPDATE kb_sost	SET dt_sost = order_rec.planneddate, dt_sost_end = order_rec.planneddate
        WHERE id_obsl = v_id_obsl AND id_sost = l_sost.id;

        IF order_rec.action = 2 AND v_inwork THEN
          IF order_rec.orderType = 0 THEN
            UPDATE wms.incomings@wms3.kvt.local SET date_to_ship 
                   =to_number(order_rec.planneddate - to_date('01-01-1970','DD-mm-YYYY')) * 24 * 60 * 60;
          ELSE
            UPDATE wms.orders@wms3.kvt.local SET date_to_ship 
            =to_number(order_rec.planneddate - to_date('01-01-1970','DD-mm-YYYY')) * 24 * 60 * 60;
          END IF;
        END IF;  
      END IF;
*/	
      --== Изменение ТС VEH
      IF order_rec.numbercar IS NOT NULL THEN
       order_rec.numbercar := utility_pkg.String2AutoNumber(order_rec.numbercar);
       v_id_tir := Utility_Pkg.find_tir(order_rec.numbercar, v_id_zak, order_rec.plannedDate);
       IF v_id_tir IS NULL THEN
         SELECT SV_UTILITIES.FORM_KEY(KB_TIR_SEQ.NextVal) INTO v_id_tir FROM dual;
        INSERT INTO KB_TIR
          (id, n_tir, id_iper, id_trans, n_avto, vodit, Id_Svh)
        VALUES
          (v_id_tir,
          'Б/Н СОХ',
          'KB_PER24667',
          'KB_TRN24662',
          order_rec.numbercar,
          order_rec.driver,
          v_id_svh);-- RETURNING id INTO v_id_tir; fuck
       END IF;
       IF v_id_tir_old IS NULL OR v_id_tir != v_id_tir_old THEN
          p_info := 'Замена ТС'||CHR(10);
          UPDATE kb_spros SET id_tir = v_id_tir WHERE id = v_id_obsl;
         IF order_rec.action = 2 AND v_inwork AND order_rec.orderType = 1 THEN
           -- ТС в СОЛВО только для отгрузок
           UPDATE wms.orders@wms3.kvt.local	SET car_num = order_rec.numbercar, driver_fio = order_rec.driver;
         END IF;
       END IF;
      END IF;
      
      IF order_rec.action = 2 THEN
        RETURN; -- делать больше нечего
      ELSE
        DELETE kb_ttn WHERE id_obsl = v_id_obsl; -- чистим kb_ttn  
      END IF;

  end if; --v_id_obsl IS NULL

--========== общая часть ======================
  FOR rec_det IN (
   SELECT extractvalue(VALUE(t), '/orderLine/lineNumber') AS LineNumber, --номер строки
    extractvalue(VALUE(t), '/orderLine/article') AS Article, --артикул товара
    extractvalue(VALUE(t), '/orderLine/name') AS NAME, --имя товара
    extractvalue(VALUE(t), '/orderLine/category') AS Category, --категория товара
    TRIM(BOTH ' ' FROM extractvalue(VALUE(t), '/orderLine/mark')) AS Mark, --номер документаTRIM(BOTH ' ' FROM '  derby ')
    extractvalue(VALUE(t), '/orderLine/mark2') AS Mark2, --номер документа
    extractvalue(VALUE(t), '/orderLine/mark3') AS Mark3, --номер документа
    extractvalue(VALUE(t), '/orderLine/qty') AS Count1, --кол-во
    extractvalue(VALUE(t), '/orderLine/comment') AS Comment1, --Комментарий
    extractvalue(VALUE(t), '/orderLine/storageLife') storageLife -- годен до (дата)
   FROM TABLE(xmlsequence(extract(xmltype(v_msg),'//Shell/order/orderLine'))) t)
  LOOP
    -- проверяем кол-во
   IF rec_det.Count1 IS NULL OR rec_det.Count1 = 0 THEN
     l_err_qty := rec_det.article || NVL(l_err_qty, '');
    --p_err := p_err || 'У номенклатуры '||rec_det.article||' не задано количество' || CHR(10);
   END IF;
   --поиск номенклатру в справочнике
   SELECT COUNT(1) INTO cnt_sku FROM sku s WHERE s.sku_id = v_prfx || rec_det.article;
   IF nvl(cnt_sku, 0) = 0 THEN
     l_err_sku := rec_det.article || NVL(l_err_sku, '');
    --p_err := p_err || 'Не найдена номенклатура ' || rec_det.article || CHR(10);
   END IF;
     --заполняем таблицу грузов
     INSERT INTO kb_ttn
      (id_obsl, n_tovar, kol_tovar, brak, pak_tovar, ul_otpr, usl, srok_godn)
     VALUES
      (v_id_obsl, rec_det.article, rec_det.count1, rec_det.category, rec_det.mark, rec_det.mark2, rec_det.mark3, rec_det.storageLife);

  END LOOP rec_det;

  IF l_err_qty IS NOT NULL THEN
    p_err := 'Для номенклатур(ы) ' || l_err_qty || ' не задано кол-во.' || CHR(10);
  END IF;
  IF l_err_sku IS NOT NULL THEN
    p_err := NVL(p_err, '') || 'Не найдены номенклатура(ы): ' || l_err_sku;
  END IF;
  IF p_err IS NOT NULL THEN
    RAISE vn_not_found;
  END IF;

  --=== разбор заявки завершен, заказ в АРМ сформирован, передача в СОЛВО
  DELETE FROM kb_t_mdet;
  INSERT INTO kb_t_mdet
    (id_sost, id_obsl, f01, f02, f05 /*категория*/, f06 /*маркер*/, f18 /*маркер 2*/,f21/*маркер 3*/,f07)
    (SELECT l_sost.id, v_id_obsl, n_tovar, KOL_TOVAR, brak, pak_tovar, ul_otpr, usl, srok_godn FROM kb_ttn
  WHERE id_obsl = v_id_obsl);

  DELETE FROM kb_t_master;
  INSERT INTO kb_t_master
    ( f09, f10, f16, f18, f14) --f06,f07, f08, --> fuck
  VALUES
    (
    /*v_sost_doc, --f06
     'Поставка', --f07 Отгрузка
      'Поставка',  --f08
   */
      to_char(order_rec.plannedDate, 'dd.mm.yyyy'), --f09
      to_char(order_rec.plannedDate, 'hh24:mi'), --f10
      'нет',
      v_id_wms,
      nvl(v_id_wms_zak, v_id_wms));
  --проверка заказа перед отправкой      
  kb_pack.wms3_Check_OrderA(p_err, CASE WHEN order_rec.orderType = 0 THEN 'INCOMING' ELSE 'ORDER' END, v_id_sost, v_tmp, v_tmp, v_tmp);

  IF p_err IS NOT NULL THEN
    RAISE vn_not_found;
  END IF;
  --!!! Аспект А004
  KB_MONITOR.CRT_COEF(v_id_obsl, v_id_dog);

  --фактическая передача данных в СОЛВО
  kb_pack.wms3_export_io(pack_err, CASE WHEN order_rec.orderType = 0 THEN 'INCOMING' ELSE 'ORDER' END, v_id_sost);

  -- Фиксируем факт успешной отправки в ГС (событие 4113 Заказ направлен в СУС)
  SELECT COUNT(*), SUM(a.counter) INTO v_counter, v_rows
  FROM (SELECT f01, SUM(f02), f20, f07, COUNT(*) AS counter FROM kb_t_mdet GROUP BY f01, f20, f07) a;

  INSERT INTO kb_sost -- 4113
    (id_obsl,
      id_isp,
      dt_sost,
      dt_sost_end,
      ora_user_edit_row_lock,
      ora_user_edit_row_name,
      row_creator,
      id_sost,
      sost_prm)
    SELECT v_id_obsl, '010277043', SYSDATE, SYSDATE, 1, USER, 'GWI2', sl.val_id, 'Строк в заявке: ' ||
            TO_CHAR(v_rows) || CHR(13) ||
            CHR(10) || 'Передано строк: ' ||
            TO_CHAR(v_counter)
      FROM sv_hvoc sl
      WHERE sl.voc_id = 'KB_USL'
            AND sl.val_short = '4113';
	 
  COMMIT;

EXCEPTION
  WHEN vn_not_found THEN
    ROLLBACK;
  WHEN OTHERS THEN
    p_err := SQLERRM;
    ROLLBACK;
END MSG_4101_test;
