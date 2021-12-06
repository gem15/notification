create or replace PACKAGE BODY KB_MONITOR AS
/*
f01 -- p_sku_id
f02 -- p_qty
f05 -- p_category
f06 -- p_marker
f07 -- Срок годности
f08      -- Штрих код
f09 -- Тип
f10 -- Стратегия годности
f12 --p_weight
f15 -- Поставщик
f16 -- p_comments f16 -- Комментарий
f17 -- Рецепт
f18 -- p_marker2
f19 -p_shipping_address ??
f20 -- p_lot
f21 -- p_marker3
*/
	v_n_avto    VARCHAR2(40);

art_from varchar2(4) :='~|.№';
art_to varchar2(4) :='$@.^'; --'$@#^'; ПЗ6995

  /**********************************
  * Оповещение выбранного клиента об ошибках
  **********************************/
  PROCEDURE send_notification(p_id_file IN NUMBER, p_err IN VARCHAR2, p_vn IN VARCHAR2) IS
  BEGIN
    FOR rec IN (SELECT ssd.data --ssd.*, z.*
                  FROM kb_zak z, sc_srv_data ssd
                 WHERE z.id_klient = p_vn
                       AND z.id = ssd.id_zak
                       AND ssd.id_srv = '5'
                       AND ssd.id_type = 'SCSRVD100130'
                       AND REPLACE(ssd.data, ' ') IS NOT NULL)
    LOOP
      kb_mail3.send(rec.data, --адреса получателя через запятую
                    'Ошибка разбора файла ' || p_id_file || ' ICD', --тема письма
                    'При разборе файла ' || p_id_file || ' произошла ошибка: ' || p_err, --содержимое письма
                    ''); --вложение, в основном просто пустое значение
    END LOOP;
  END send_notification;

PROCEDURE wms3_updt_sku (p_id     IN     VARCHAR2, --'0102304213'
                             p_pref   IN     VARCHAR2,--"TSM"
                             p_err       OUT VARCHAR2)
    IS
        cursorid                 NUMBER;            -- The cursor we will use.
        v_id_rap                 VARCHAR2 (32) := '0102167386';     -- Код РАП
        v_info_record_id         kb_sost.id%TYPE;
        v_info_record_id_obsl    kb_sost.id_obsl%TYPE;
        v_info_record_sost_prm   kb_sost.sost_prm%TYPE;
        v_info_record_counter    NUMBER := -1;
        l_err                    VARCHAR2 (2048);
        l_counter                NUMBER := 0;
        l_tbl                    VARCHAR2 (1024) := 'SKU';
        l_pack_id                gwi3.xml_data_to_wms.pack_id%TYPE;
    BEGIN
        SELECT   svn_user_function.select_list('SELECT RTRIM(LTRIM(marker)) FROM KB_T_ARTICLE WHERE marker IS NOT NULL HAVING COUNT(*) > 1 GROUP BY RTRIM(LTRIM(marker))')
          INTO   l_err
          FROM   DUAL;

        IF (l_err IS NOT NULL)
        THEN
            p_err := 'Дубл. ? ' || l_err;
            RETURN;
        END IF;

        IF (p_err >= SYSDATE)
        THEN
            p_err := 'Дата вступления в силу договора> текущей! ' || l_err;
            RETURN;
        END IF;

FOR c_a IN   (SELECT REPLACE(REPLACE(RTRIM(LTRIM(TRANSLATE(a.id_sost, art_from, art_to))),CHR(10)),CHR(13)) AS id_sost
                    ,a.price
                    ,REPLACE(REPLACE(RTRIM(LTRIM(REPLACE(TRANSLATE(a.comments,'№»«','#""'),'°',' град.'))),CHR(10)),CHR(13)) AS comments
                    , a.marker,
                     a.num,
                     a.categ,
                     a.tip_tov,
                     TO_NUMBER (a.upc) AS nru,
                     a.code AS code,
                     TO_NUMBER (a.expiry_date) AS ex_d,
                     a.str_sr_godn,
                     a.str_sscc,
                     a.str_part,
                     a.str_sert,
                     a.str_mu_code,
                     a.n_mu_unit,
                     a.abc,
                     a.storage_pos,
                     a.producer,
                     a.vendor,
                     a.coo,
                     a.measure,
                     a.kit_type,
                     a.usage_state,
                     s.name,
                     s.id,
                    /* h3.police_code */ trim(h3.val_full) AS h_storage_pos,
                    a.DESCRIPTION
              FROM           KB_T_ARTICLE a
                         LEFT OUTER JOIN
                             wms.sku@wms3.kvt.local s
                         ON s.sku_id =
                                p_pref
                                || REPLACE (
                                       REPLACE (RTRIM (LTRIM (a.id_sost)),
                                                CHR (10)),
                                       CHR (13))
                     LEFT OUTER JOIN
                         sv_hvoc h3
                     ON h3.voc_id = 'USLXRN'
                        AND CASE
                               WHEN s.id IS NULL
                                    AND NVL (UPPER(a.storage_pos), 'NULL') =
                                           'NULL'
                                    OR s.id IS NOT NULL
                                      AND UPPER (a.storage_pos) = 'NULL'
                               THEN
                                   'НОРМ'
                               ELSE
                                   UPPER (a.storage_pos)
                           END LIKE
                               UPPER (h3.val_short)
             WHERE   TRIM (a.id_sost) IS NOT NULL
             --AND s.ID >= 2000000
             )--MV20042020
        LOOP
            l_counter := l_counter + 1;

            INSERT INTO gwi3.packets_to_wms (tag)
               VALUES   (l_tbl)
            RETURNING   pack_id
                 INTO   l_pack_id;

            INSERT INTO gwi3.xml_data_to_wms (pack_id,
                                              line,
                                              data)
              VALUES   (l_pack_id,
                        1,
                        kb_gateway.msg_prolog
                        || kb_gateway.elm_sku (
                               p_pref || RTRIM (LTRIM (--c_a.id_sost
                                 --добавил проверку на префикс, штрихкод должен браться только по терволине sol 20170822
                                 --nvl(c_a.marker, c_a.id_sost) --изменил артикул на штрихкод, но если он пустой, то все равно ставим артикул sol 20170724
                                 decode(p_pref, 'TVL', nvl(c_a.marker, c_a.id_sost) ,c_a.id_sost)
                               )) -- p_sku_id
                                                                    ,
                               NVL (c_a.comments, c_a.name)          -- p_name
                               ,c_a.id_sost                        -- p_article
                               ,TO_NUMBER (c_a.price)        -- p_storage_price
                               ,c_a.DESCRIPTION/*nvl(c_a.marker, c_a.id_sost)--* /NULL  */       -- p_description
                               ,NVL (c_a.marker,'нет-' /*|| RTRIM (LTRIM (c_a.id_sost))*/) -- p_upc
                               ,c_a.abc                                -- p_abc
                                      --                               ,DECODE(c_a.id, NULL,NVL(c_a.h_storage_pos,'Нормальный'),DECODE(c_a.h_storage_pos, NULL, NULL, c_a.h_storage_pos)) -- p_storage_pos
                               ,trim(c_a.h_storage_pos)
										 ,TO_NUMBER (c_a.categ)       -- p_control_method
                               ,TO_NUMBER (c_a.str_sr_godn)   -- p_control_date
                               ,c_a.producer                      -- p_producer
                               ,c_a.vendor                          -- p_vendor
                               ,c_a.coo                                -- p_coo
                               ,p_id                             -- p_holder_id
                               ,c_a.measure                        -- p_measure
                               ,DECODE (
                                   c_a.id,
                                   NULL,
                                   NVL (c_a.tip_tov, 'НЗ'),
                                   DECODE (RTRIM (LTRIM (c_a.tip_tov)),
                                           NULL, NULL,
                                           c_a.tip_tov))    -- p_billing_class
                               ,DECODE (
                                   c_a.id,
                                   NULL,
                                   DECODE (RTRIM (LTRIM (c_a.str_part)),
                                           '1', 't',
                                           'f'),
                                   DECODE (RTRIM (LTRIM (c_a.str_part)),
                                           NULL, NULL,
                                           '1', 't',
                                           'f'))                  -- p_lot_req
                               ,TO_NUMBER (c_a.code)      -- p_serial_num_track
                               ,DECODE (
                                   c_a.id,
                                   NULL,
                                   DECODE (c_a.kit_type,
                                           'A', 't',
                                           'K', 't',
                                           'f'),
                                   DECODE (c_a.kit_type,
                                           NULL, NULL,
                                           'A', 't',
                                           'K', 't',
                                           'f'))                   -- p_is_kit
                               ,c_a.kit_type                      -- p_kit_type
                               ,DECODE (RTRIM (LTRIM (c_a.str_sert)),
                                           NULL, NULL,
                                           '1', 'СК',
                                           'NULL', 'NULL',
                                           RTRIM (LTRIM (c_a.str_sert)) --null
                                           )/*)*/                 -- p_sertificat
                               ,c_a.str_mu_code                 -- p_mu_barcode
                               ,TO_NUMBER (c_a.n_mu_unit)         -- p_mu_units
                               ,TO_NUMBER (c_a.num)           -- p_warning_time
                               ,c_a.nru                           -- p_bulk_qty
                               ,DECODE (
                                   c_a.id,
                                   NULL,
                                   DECODE (RTRIM (LTRIM (c_a.str_sscc)),
                                           '1', 't',
                                           'f'),
                                   DECODE (RTRIM (LTRIM (c_a.str_sscc)),
                                           NULL, NULL,
                                           '1', 't',
                                           'f'))                 -- p_sscc_req
                               ,c_a.usage_state                -- p_usage_state
                                              )
                        || kb_gateway.msg_footer_empty);
        END LOOP;

        IF (l_counter <= 0)
        THEN
            p_err := 'Ничего не загружено!';
        ELSE
--            p_err := 'Загружено записей: ' || TO_CHAR (l_counter);
            COMMIT WORK;
        END IF;
EXCEPTION
    WHEN OTHERS THEN
      ROLLBACK;
      p_err := SQLERRM;
END wms3_updt_sku;
	
   --new xml
  PROCEDURE msg_4101_(p_msg IN CLOB, p_err OUT VARCHAR2) IS
    v_id_zak     kb_zak.id%TYPE;
    v_new_rec    kb_zak.id%TYPE; --получатель
    v_id_wms     kb_zak.id_wms%TYPE;
    v_id_obsl    kb_spros.id%TYPE;
    v_id_dog     VARCHAR2(50);
    v_id_tzs     kb_sost.id_tzs%TYPE;
    v_id_sost    kb_sost.id%TYPE;
    cnt_sku      NUMBER;
    v_id_tir     NUMBER;
    v_tmp        VARCHAR2(38);
    v_counter    NUMBER;
    v_rows       NUMBER;
    v_sost_doc   VARCHAR2(38);
    pack_err     VARCHAR2(3800);
    v_prfx       VARCHAR2(5 CHAR);
    v_id_wms_zak VARCHAR2(38);
    v_id_svh     VARCHAR2(38);
    v_id_usr     VARCHAR2(38);
    vn_not_found EXCEPTION;

	 v_order_Date	DATE;
	 v_planned_Date	DATE;
	 v_OrderType	VARCHAR2(20);
   v_msg        CLOB;
	 
  BEGIN
    --делаем для ПО
    SAVEPOINT s1;
    v_msg := REPLACE(p_msg, ' xmlns="http://www.severtrans.com"');
    FOR rec IN (
		SELECT 
			  extractvalue(VALUE(t), '/Shell/customerID') AS VN, --ВН клиента
			  extractvalue(VALUE(t), '/Shell/msgID') AS msgID, 
			  extractvalue(VALUE(t), '/Shell//order/orderNo') AS number1, --Номер ПО
			  extractvalue(VALUE(t), '/Shell/order/orderDate') AS Date1, --Дата ПО
			  extractvalue(VALUE(t), '/Shell/order/orderType') AS OrderType, --Тип заказа
		     extractvalue(VALUE(t), '/Shell/order/orderKind') AS TypeOfDelivery, --Тип поставки
			  extractvalue(VALUE(t), '/Shell/order/plannedDate') AS PlannedDeliveryDate,
			  extractvalue(VALUE(t), '/Shell/order/contrCode') AS IDSupplier, --код поставщика
			  extractvalue(VALUE(t), '/Shell/order/contrName') AS NameSupplier, --имя поставщика
			  extractvalue(VALUE(t), '/Shell/order/contrAddress') AS AdressSupplier, --адрес поставщика
			  extractvalue(VALUE(t), '/Shell/order/licencePlate') AS NumberCar, --Номер машины
			  extractvalue(VALUE(t), '/Shell/order/driver') Driver,
			  extractvalue(VALUE(t), '/Shell/order/guid') docID
		FROM TABLE(xmlsequence(extract(xmltype(REPLACE(v_msg,' xmlns="http://www.severtrans.com"')),'//Shell'))) t)
   LOOP
	
		v_order_Date := to_date(REPLACE(rec.Date1,'T',' '), 'yyyy-mm-dd hh24:mi:ss'); --'2021-06-06T15:52:50'
		v_planned_Date := to_date(REPLACE(rec.PlannedDeliveryDate,'T',' '), 'yyyy-mm-dd hh24:mi:ss');
    
      --разберёмся с клиентом, в переносном смысле или в прямом...
      BEGIN
        SELECT z.id, z.id_wms, z.id_svh, z.prf_wms, rec.IDSupplier, z.id_usr
          INTO v_id_zak, v_id_wms, v_id_svh, v_prfx, v_id_wms_zak, v_id_usr
          FROM kb_zak z
         WHERE z.id_klient = rec.vn
               AND z.id_usr IN ('KB_USR92734', 'KB_USR99992');
      EXCEPTION
        WHEN OTHERS THEN
          p_err := 'неправильный ВН';
          RAISE vn_not_found;--EXIT;
      END;
    
      --заделаем машину, почти как Генри Форд
      IF rec.numbercar IS NOT NULL THEN
        v_n_avto := utility_pkg.String2AutoNumber(rec.numbercar);
        v_id_tir := Utility_Pkg.find_tir(v_n_avto, v_id_zak, v_planned_Date);
        IF v_id_tir IS NULL THEN
          SELECT SV_UTILITIES.FORM_KEY(KB_TIR_SEQ.NextVal) INTO v_id_tir FROM dual;
          INSERT INTO KB_TIR
            (n_tir, id_iper, id_trans, /*id_svh, */ n_avto, id, vodit, Id_Svh)
          VALUES
            ('Б/Н СОХ',
             'KB_PER24667', -- Междугородняя
             'KB_TRN24662', -- АвтоМобильный, 
             v_n_avto,
             v_id_tir,
             rec.driver,
             v_id_svh);
        END IF;
      ELSE
        v_id_tir := NULL;
      END IF;
      --теперь сделаем или найдём поставщика
      if rec.IDSupplier is null then
        v_new_rec := v_id_zak;
      else
        BEGIN
          SELECT MIN(z.id)
            INTO v_new_rec
            FROM kb_zak z
           WHERE z.id_wms = rec.IDSupplier
                 AND z.id_klient = rec.vn;
        EXCEPTION
          WHEN OTHERS THEN
            NULL;
        END;
      end if; 
      --если не нашли
      IF v_new_rec IS NULL THEN
        --делаем нового контрагента 
        SELECT SV_UTILITIES.FORM_KEY(kb_zak_seq.NextVal) INTO v_new_rec FROM dual;
        INSERT INTO kb_zak
          (id, id_klient, id_wms, n_zak /*наименование*/, id_tip_zak, naimen, ur_adr)
        VALUES
          (v_new_rec,
           rec.vn /*'300160'-ВН*/,
           rec.idsupplier,
           rec.namesupplier /*наименование*/,
           'KB_TZK82894', --Поставщик/ Получатель СОХ
           rec.namesupplier,
           rec.adresssupplier);
      END IF;
    
      --=== создание заказа
      SELECT SV_UTILITIES.FORM_KEY(KB_SPROS_SEQ.NextVal) INTO v_id_obsl FROM dual;
    
      INSERT INTO kb_spros
        (n_gruz, dt_zakaz, id_zak, id_pok, is_postavka, id, id_spros, id_tir, id_kat)
      VALUES
        (c_test || 'FTP УП --> ' || rec.number1,
         to_char(SYSDATE, 'dd.mm.rr'),
         v_id_zak,
         v_new_rec,
         '1',
         v_id_obsl,
         NULL,
         v_id_tir,
         'KB_TGR98182');
      --поиск договора  
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
      -- определение типа поставки/отгрузки
      BEGIN
        SELECT s.val_id INTO v_id_tzs FROM sv_hvoc s 
        WHERE upper(s.val_full) = upper(rec.TypeOfDelivery) AND s.VOC_ID='SCH_NP';
      EXCEPTION
        WHEN no_data_found THEN
			v_id_tzs := 'SCH_NP94607';
--          p_err := 'Неправильный тип поставки.';
--          EXIT;
      END;
      --наполнение заказа
      --4101
      INSERT INTO kb_sost
        (id_obsl, dt_sost, dt_sost_end, id_sost, id_dog, sost_doc, id_isp, id_tzs, dt_doc, sost_prm)
      VALUES
        (v_id_obsl,
         v_planned_Date,
         v_planned_Date,
         'KB_USL60173',
         substr(v_id_dog, 2),
         c_test || rec.Number1,
         '010277043',
         v_id_tzs,
         v_order_Date,
         c_test)
      RETURNING id INTO v_id_sost;
      --добавляем событие 4301
      INSERT INTO kb_sost
        (id_obsl, dt_sost, dt_sost_end, id_sost,  sost_prm, id_isp,id_du)--sost_doc,
      VALUES
        (v_id_obsl, SYSDATE, SYSDATE, 'KB_USL99770', 'ПО', '010277043',rec.docID); --rec.guid p_id_file, 
    
      FOR rec_det IN (
			SELECT extractvalue(VALUE(t), '/orderLine/lineNumber') AS LineNumber, --номер строки
			  extractvalue(VALUE(t), '/orderLine/article') AS Article, --артикул товара
			  extractvalue(VALUE(t), '/orderLine/name') AS NAME, --имя товара
			  extractvalue(VALUE(t), '/orderLine/category') AS Category, --категория товара
			  TRIM(BOTH ' ' FROM extractvalue(VALUE(t), '/orderLine/mark')) AS Mark, --номер документаTRIM(BOTH ' ' FROM '  derby ')
			  extractvalue(VALUE(t), '/orderLine/mark2') AS Mark2, --номер документа
			  extractvalue(VALUE(t), '/orderLine/mark3') AS Mark3, --номер документа
			  extractvalue(VALUE(t), '/orderLine/qty') AS Count1, --кол-во
			  extractvalue(VALUE(t), '/orderLine/comment') AS Comment1 --Комментарий
			FROM TABLE(xmlsequence(extract(xmltype(v_msg),'//Shell/order/orderLine'))) t)
      LOOP
        --поиск номенклатру в справочнике
        SELECT COUNT(1) INTO cnt_sku FROM sku s WHERE s.sku_id = v_prfx || rec_det.article;
        IF nvl(cnt_sku, 0) = 0 THEN
          p_err := 'Не найдена номенклатура ' || rec_det.article;
			 RAISE vn_not_found;
        END IF;
        --заполняем таблицу грузов
        INSERT INTO kb_ttn
          (id_obsl, n_tovar, kol_tovar, brak, pak_tovar, ul_otpr, usl)
        VALUES
          (v_id_obsl, rec_det.article, rec_det.count1, rec_det.category, rec_det.mark, rec_det.mark2, rec_det.mark3);
      END LOOP;
    
    END LOOP;
    --разбор заявки завершен, заказ в АРМ сформирован
    IF p_err IS NULL THEN
      --передача в СОЛВО
      DELETE FROM kb_t_mdet;
      INSERT INTO kb_t_mdet
        (id_sost, id_obsl, f01, f02, f05 /*категория*/, f06 /*маркер*/, f18 /*маркер 2*/,f21/*маркер 3*/)
        (SELECT 'KB_USL60173', v_id_obsl, n_tovar, KOL_TOVAR, brak, pak_tovar, ul_otpr, usl FROM kb_ttn
		  WHERE id_obsl = v_id_obsl);
    
      DELETE FROM kb_t_master;
      INSERT INTO kb_t_master
        (f06, f07, f08, f09, f10, f16, f18, f14)
      VALUES
        (v_sost_doc --f06
        ,
         'Поставка' --f07
        ,
         'Поставка',
         to_char(v_planned_Date, 'dd.mm.yyyy') --f09
        ,
         to_char(v_planned_Date, 'hh24:mi') --f10
        ,
         'нет',
         v_id_wms,
         nvl(v_id_wms_zak, v_id_wms));
      --проверка заказа перед отправкой      
      kb_pack.wms3_Check_OrderA(pack_err, 'INCOMING', v_id_sost, v_tmp, v_tmp, v_tmp);
    
      IF (pack_err IS NOT NULL) THEN
        p_err := pack_err;
      ELSE
		
			CRT_COEF(v_id_obsl, v_id_dog); --!!!
			
        --фактическая передача данных в СОЛВО ---!!!
        kb_pack.wms3_export_io(pack_err, 'INCOMING', v_id_sost);
      
        -- Фиксируем факт успешной отправки в ГС (событие 4113 Заказ направлен в СУС)
        SELECT COUNT(*), SUM(a.counter)
          INTO v_counter, v_rows
          FROM (SELECT f01, SUM(f02), f20, f07, COUNT(*) AS counter FROM kb_t_mdet GROUP BY f01, f20, f07) a;
      
        INSERT INTO kb_sost
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
      END IF;
    END IF;
	 
    IF p_err IS NOT NULL THEN
      ROLLBACK;
      -- status E сохранить p_err в message_err_code
    --   UPDATE KB_ICD_IN t  SET t.message_status   = 'E', t.message_err_code = p_err WHERE t.message_id = p_id_file;
    --   send_notification(p_id_file, p_err, v_vn);
    ELSE
      -- change status to S
    --   UPDATE KB_ICD_IN t SET t.message_status = 'S' WHERE t.message_id = p_id_file;
      -- test
      IF LENGTH(c_test) != 0 THEN
        dbms_output.put_line('Order # ' || v_id_obsl);
      END IF;
    END IF;
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      ROLLBACK;
      p_err := SQLERRM;
    
  END MSG_4101_;
  --Конец разборки с ПО--

    PROCEDURE MSG_4103_(p_msg IN CLOB, p_err OUT VARCHAR2) IS
    v_id_zak     kb_zak.id%TYPE;
    v_new_rec    kb_zak.id%TYPE; --получатель
    v_id_wms     kb_zak.id_wms%TYPE;
    v_id_obsl    kb_spros.id%TYPE;
    v_id_dog     VARCHAR2(50);
    v_id_tzs     kb_sost.id_tzs%TYPE;
    v_id_sost    kb_sost.id%TYPE;
    cnt_sku      NUMBER;
    v_id_tir     NUMBER;
    v_tmp        VARCHAR2(38);
    v_counter    NUMBER;
    v_rows       NUMBER;
    v_sost_doc   VARCHAR2(38);
    pack_err     VARCHAR2(380);
    v_prfx       VARCHAR2(5 CHAR);
    v_id_wms_zak VARCHAR2(20);
    v_id_svh     VARCHAR2(38);
    v_id_usr     VARCHAR2(38);
    vn_not_found EXCEPTION;

	 v_order_Date	DATE;
	 v_planned_Date	DATE;
	 v_OrderType	VARCHAR2(20);
   v_msg        CLOB;
	 
  BEGIN
    --делаем для ПО
    SAVEPOINT s1;
    v_msg := REPLACE(p_msg, ' xmlns="http://www.severtrans.com"');
    FOR rec IN (
		SELECT
			  extractvalue(VALUE(t), '/Shell/customerID') AS VN,
			  extractvalue(VALUE(t), '/Shell/msgID') AS msgID, 
			  extractvalue(VALUE(t), '/Shell/order/orderNo') AS number1, --NumberDoc ????? ??
			  extractvalue(VALUE(t), '/Shell/order/orderDate') AS Date1, --DateDoc ???? ??
		--	  extractvalue(VALUE(t), '/Shell/order/Customer') AS Customer, --???????????
			  extractvalue(VALUE(t), '/Shell/order/orderType') AS OrderType, --??? ??????
			  extractvalue(VALUE(t), '/Shell/order/orderKind') AS TypeOfDelivery, --??? ????????
			  extractvalue(VALUE(t), '/Shell/order/plannedDate') AS PlannedShipmentDate, --PlannedShipmentDate ??????????? ???? ????????
			  extractvalue(VALUE(t), '/Shell/order/contrCode') AS IDConsignee, --IDConsignee ??? ??????????
			  extractvalue(VALUE(t), '/Shell/order/contrName') AS NameConsignee, --NameConsignee ??? ???????????
			  extractvalue(VALUE(t), '/Shell/order/AdressConsignee') AdressConsignee, --??? ??????????
		--	  extractvalue(VALUE(t), '/Shell/order/IDCarrier') AS IDCarrier, --??? ???????????
		--	  extractvalue(VALUE(t), '/Shell/order/TypeCar') AS TypeCar, --??? ??????
			  extractvalue(VALUE(t), '/Shell/order/licencePlate') AS NumberCar, --NumberCar ????? ??????
			  extractvalue(VALUE(t), '/Shell/order/driver') Driver,
			  extractvalue(VALUE(t), '/Shell/order/guid') docID,
		--	  extractvalue(VALUE(t), '/Shell/order/Email') Email,
			  extractvalue(VALUE(t), '/Shell/order/comment') Comment1 --???????????
		FROM TABLE(xmlsequence(extract(xmltype(REPLACE(v_msg,' xmlns="http://www.severtrans.com"')),'//Shell'))) t)
    LOOP
    
		v_order_Date := to_date(REPLACE(rec.Date1,'T',' '), 'yyyy-mm-dd hh24:mi:ss'); --'2021-06-06T15:52:50'
		v_planned_Date := to_date(REPLACE(rec.PlannedShipmentDate,'T',' '), 'yyyy-mm-dd hh24:mi:ss');
	   --разберёмся с клиентом, в переносном смысле или в прямом...
      BEGIN
        SELECT z.id, z.id_wms, z.id_svh, z.prf_wms, rec.IDConsignee, z.id_usr ---!!! проверить
          INTO v_id_zak, v_id_wms, v_id_svh, v_prfx, v_id_wms_zak, v_id_usr
          FROM kb_zak z
         WHERE z.id_klient = rec.vn
               AND z.id_usr IN ('KB_USR92734', 'KB_USR99992'); --найдём клиента
      EXCEPTION
        WHEN OTHERS THEN
          p_err := 'неправильный ВН';
			 RAISE vn_not_found;
      END;
      --заделаем машину, почти как Генри Форд
      IF rec.numbercar IS NOT NULL THEN
        v_n_avto := utility_pkg.String2AutoNumber(rec.numbercar);
        v_id_tir := Utility_Pkg.find_tir(v_n_avto, v_id_zak, v_planned_Date);
        IF v_id_tir IS NULL THEN
          SELECT SV_UTILITIES.FORM_KEY(KB_TIR_SEQ.NextVal) INTO v_id_tir FROM dual;
          INSERT INTO KB_TIR
            (n_tir, id_iper, id_trans, /*id_svh, */ n_avto, id, vodit, Id_Svh)
          VALUES
            ('Б/Н СОХ',
             'KB_PER24667', -- Междугородняя
             'KB_TRN24662', -- АвтоМобильный, 
             v_n_avto,
             v_id_tir,
             rec.driver,
             v_id_svh);
        END IF;
      ELSE
        v_id_tir := NULL;
      END IF;
      --теперь сделаем или найдём получателя
      if rec.IDConsignee is null then
        v_new_rec := v_id_zak;
      else
        BEGIN
          SELECT MIN(z.id)
            INTO v_new_rec
            FROM kb_zak z
           WHERE z.id_wms = rec.idconsignee
                 AND z.id_klient = rec.vn;
        EXCEPTION
          WHEN OTHERS THEN
            NULL;
        END;
      end if;
      --если не нашли
      IF v_new_rec IS NULL THEN
        --делаем нового контрагента
        SELECT SV_UTILITIES.FORM_KEY(kb_zak_seq.NextVal) INTO v_new_rec FROM dual;
        INSERT INTO kb_zak
          (id, id_klient, id_wms, n_zak /*наименование*/, id_tip_zak, naimen, ur_adr)
        VALUES
          (v_new_rec,
           rec.vn /*'300160'-ВН*/,
           rec.idConsignee,
           rec.nameConsignee /*наименование*/,
           'KB_TZK82894', -- Поставщик/ Получатель СОХ
           rec.nameConsignee,
           rec.adressconsignee);
      END IF;
      -- до этого места одинаково 4101
    
      ---создание заказа
      SELECT SV_UTILITIES.FORM_KEY(KB_SPROS_SEQ.NextVal) INTO v_id_obsl FROM dual;
    
      INSERT INTO kb_spros
        (n_gruz,
         dt_zakaz,
         id_zak,
         id_pok, --idconsignee
         is_postavka /* флаг поставка/отгрузка*/,
         id,
         id_spros,
         id_tir,
         id_kat)
      VALUES
        (c_test || 'FTP РО --> ' || rec.number1, --- || ' --> ' || v_file_name
         to_char(SYSDATE, 'dd.mm.rr'),
         v_id_zak,
         v_new_rec,
         '0',
         v_id_obsl,
         NULL,
         v_id_tir,
         'KB_TGR98182'); --FTP - сервер
      --работа с договором
      -- ищется действующий договор
      SELECT MIN(decode(nvl(a.id_dog, a.id_usl),
                         NULL,
                         decode(dg.id_tdoc, 'KB_TDD39116', '2', 'KB_TDD39115', '1', NULL) || dg.id,
                         '0' || nvl(a.id_dog, a.id_usl)))
        INTO v_id_dog
        FROM kb_spros sp, kb_dog dg, kb_spros_dog a
       WHERE sp.id = v_id_obsl
             AND a.id_obsl(+) = sp.id
             AND dg.id_zak = sp.id_zak(+)
             AND dg.id_isp(+) = '010277043' --ООО "ГК "СЕВЕРТРАНС"
             AND nvl(dg.dt_end(+), SYSDATE) >= SYSDATE
             AND dg.id_tdoc IN ('KB_TDD39116', 'KB_TDD39115');
      -- привязка договора к заказу                     
      INSERT INTO kb_spros_dog (id_obsl, id_vtu, id_dog) VALUES (v_id_obsl, 'KB_VTU50767', substr(v_id_dog, 2));

      -- определение типа поставки/отгрузки
      BEGIN
        SELECT s.val_id INTO v_id_tzs FROM sv_hvoc s 
        WHERE upper(s.val_full) = upper(rec.TypeOfDelivery) AND s.VOC_ID='SCH_NP';
      EXCEPTION
        WHEN no_data_found THEN
		  v_id_tzs := 'SCH_NP94574'; -- костыль
--          p_err := 'Неправильный тип отгрузки.';
--          EXIT;
      END;

      --создаем событие 4103 - плановая отгрузка товара точка входа в солво
      
      SELECT SV_UTILITIES.FORM_KEY(KB_KONTR_SEQ.NextVal)	INTO v_id_sost FROM dual;
      INSERT INTO kb_sost
        (id,id_obsl, dt_sost, dt_sost_end, id_sost, id_dog, sost_doc, id_isp /*мы*/, id_tzs, dt_doc, sost_prm)
      VALUES
        (v_id_sost,
        v_id_obsl,
         v_planned_Date,
         v_planned_Date,
         'KB_USL60175' /* 4103*/,
         substr(v_id_dog, 2),
         c_test || rec.number1,
         '010277043',
         v_id_tzs, --SCH_NP94574 Отгрузка
         v_order_Date,
         rec.Comment1);
--      RETURNING id INTO v_id_sost; --- услуга
      --добавляем событие 4301 Получено входящее сообщение
      INSERT INTO kb_sost
        (id_obsl, dt_sost, dt_sost_end, id_sost, sost_prm, id_isp,id_du)
      VALUES
        (v_id_obsl, SYSDATE, SYSDATE, 'KB_USL99770', 'РО', '010277043',rec.docID);
    
      --v_sost_doc := rec.NumberImportInvoice;
      FOR rec_det IN (
			SELECT extractvalue(VALUE(t), '/orderLine/lineNumber') AS LineNumber, --номер строки
			  extractvalue(VALUE(t), '/orderLine/article') AS Article, --артикул товара
			  extractvalue(VALUE(t), '/orderLine/name') AS NAME, --имя товара
			  extractvalue(VALUE(t), '/orderLine/category') AS Category, --категория товара
			  TRIM(BOTH ' ' FROM extractvalue(VALUE(t), '/orderLine/mark')) AS Mark, --номер документа
			  extractvalue(VALUE(t), '/orderLine/mark2') AS Mark2, --номер документа
			  extractvalue(VALUE(t), '/orderLine/mark3') AS Mark3, --номер документа
			  extractvalue(VALUE(t), '/orderLine/qty') AS Count1, --кол-во
			  extractvalue(VALUE(t), '/orderLine/comment') AS Comment1, --Комментарий
			  extractvalue(VALUE(t), '/orderLine/storageLife') StorageLife
         FROM TABLE(xmlsequence(extract(xmltype(v_msg),'//Shell/order/orderLine'))) t)
      LOOP
        --проверка номенклатуры
        SELECT COUNT(1) INTO cnt_sku FROM sku s WHERE s.sku_id = v_prfx || rec_det.article;
        IF nvl(cnt_sku, 0) = 0 THEN
          p_err := 'Не найдена номенклатура ' || rec_det.article;
			 RAISE vn_not_found;
        END IF;
        --заполняем таблицу грузов
        INSERT INTO kb_ttn
          (id_obsl, n_tovar, kol_tovar, brak, srok_godn, pak_tovar, ul_otpr, usl)
        VALUES
          (v_id_obsl,
           rec_det.article,
           rec_det.count1,
           rec_det.category,
           rec_det.storagelife,
           rec_det.mark, rec_det.mark2, rec_det.mark3);
      END LOOP;
    END LOOP;
    --начинается передача в солво
    IF p_err IS NULL THEN
      DELETE FROM kb_t_mdet;
      INSERT INTO kb_t_mdet
        (id_sost, id_obsl, f01, f02, f05 /*категория*/,  f07, f06 /*маркер*/, f18 /*маркер 2*/,f21/*маркер 3*/)
        (SELECT 'KB_USL60175', v_id_obsl, n_tovar, KOL_TOVAR, brak, srok_godn, pak_tovar, ul_otpr, usl
           FROM kb_ttn WHERE id_obsl = v_id_obsl);
    
      DELETE FROM kb_t_master;
      INSERT INTO kb_t_master
        (f06, f07, f09, f10, f16, f18, f14)
      VALUES
        (v_sost_doc --f06
        ,'Отгрузка' --f07
        ,to_char(v_planned_Date, 'dd.mm.yyyy') --f09
        ,to_char(v_planned_Date, 'hh24:mi') --f10
			,'нет',
         v_id_wms,
         nvl(v_id_wms_zak, v_id_wms));
      --проверка заказа перед отправкой      
      kb_pack.wms3_Check_OrderA(pack_err, 'ORDER', v_id_sost, v_tmp, v_tmp, v_tmp);

      IF (pack_err IS NOT NULL) THEN
        p_err := pack_err;
      ELSE

			CRT_COEF(v_id_obsl, v_id_dog); --!!!
		
        kb_pack.wms3_export_io(pack_err, 'ORDER', v_id_sost);
        -- Фиксируем факт успешной отправки в ГС (событие 4113 Заказ направлен в СУС)
        SELECT COUNT(*), SUM(a.counter)
          INTO v_counter, v_rows
          FROM (SELECT f01, SUM(f02), f20, f07, COUNT(*) AS counter FROM kb_t_mdet GROUP BY f01, f20, f07) a;
      
        INSERT INTO kb_sost
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
      END IF;
    END IF;
    IF p_err IS NOT NULL THEN
      ROLLBACK;
    END IF;
    COMMIT;
  EXCEPTION
    WHEN vn_not_found THEN
      ROLLBACK;
    WHEN OTHERS THEN
      ROLLBACK;
      p_err := SQLERRM;
  END MSG_4103_;
  --/////Конец разбора РО\\\\\\
	
	PROCEDURE CRT_COEF(p_id_obsl IN varchar2, p_id_dog IN varchar2) IS --, k out varchar2
		v_coef 						number := 0;
	  text_var  				VARCHAR2(256);
	  v_edit_row_date 	date;
	  v_dt_sost 				date;
	  v_dt_sost_end 		date;
	  v_id_sost					varchar2(38);
	  k1								varchar2(100);
	  v_cnt number;
	BEGIN
		select count(1) into v_cnt
						  from kb_dog_usl du
						 where du.id_dog = substr(p_id_dog, 2)--:kb_sost.id_dog
							and du.id_usl = 'KB_USL60440';--Ведение времени поступления заявок	A004
		if v_cnt > 0 then		 
				begin
				  select s.ora_user_edit_row_date, s.dt_sost, s.dt_sost_end, s.id_sost 
					 into v_edit_row_date, v_dt_sost, v_dt_sost_end, v_id_sost
					  from kb_sost s, kb_spros sp
					where sp.id = p_id_obsl --:kb_sost.id_obsl
						and sp.id = s.id_obsl
						 and s.id_sost in ('KB_USL60173','KB_USL60175');
				exception
					 	when no_data_found then return;
					 	when too_many_rows then null;
				end;
			
				if (v_id_sost = 'KB_USL60173' or v_id_sost = 'KB_USL60175')
							and v_dt_sost_end >= sysdate then
						 if trunc(v_edit_row_date,'ddd') < trunc(v_dt_sost_end,'ddd')
						 then
						  begin
								select (decode(nvl(da.coef,0),0,0,da.coef)) into v_coef
								 from kb_dog_usl du, kb_det_aspects da, kb_sost s
								where s.id_dog = du.id_dog
									 and du.id = da.id_aspect
									 and s.id_sost in ('KB_USL60173','KB_USL60175')
									 and s.id_obsl = p_id_obsl -- :kb_sost.id_obsl
									 and da.TIP_POD_ZAIAV = 'TIP_AS94383'--до
									 and du.id_usl = 'KB_USL60440'
									 and da.coef is not null
									 and (to_char(nvl(v_dt_sost_end, v_dt_sost),'hh24.mi.ss') 
											between to_char(da.vrem_rej_s,'hh24.mi.ss') and to_char(da.vrem_rej_do,'hh24.mi.ss'))
									 and v_edit_row_date <= 
										  to_date( to_char( trunc(nvl(v_dt_sost_end,v_dt_sost))-1,'dd.mm.yyyy') || to_char(da.VREM_POD_ZAIAV_DO,'hh24.mi.ss')
														 ,'dd.mm.yyyy hh24.mi.ss')
									 and ((v_id_sost = 'KB_USL60173' and da.napravl_ts in ('TIP_AS94379','TIP_AS94381'))
									  or (v_id_sost = 'KB_USL60175' and da.napravl_ts in ('TIP_AS94380','TIP_AS94381')));
						  exception
								  when no_data_found then v_coef := 0;
						  end;
				 end if;
						 
				 if trunc(v_edit_row_date,'ddd') <= trunc(v_dt_sost_end,'ddd') 
						and (v_coef = 0	or v_coef is null)
				 then
					--alert_note('1');
						select da.coef into v_coef
						 from kb_dog_usl du, kb_det_aspects da, kb_sost s
						where s.id_dog = du.id_dog
							  and du.id = da.id_aspect
							  and s.id_sost in ('KB_USL60173','KB_USL60175')
							  and s.id_obsl = p_id_obsl -- :kb_sost.id_obsl
							  and da.TIP_POD_ZAIAV = 'TIP_AS94384'--за
							  and du.id_usl = 'KB_USL60440'
							  and da.coef is not null
							  and (nvl(to_char(v_dt_sost_end,'hh24.mi.ss'),to_char(v_dt_sost,'hh24.mi.ss')) 
									between to_char(da.vrem_rej_s,'hh24.mi.ss') and to_char(da.vrem_rej_do,'hh24.mi.ss'))
							  and ((da.napravl_ts in ('TIP_AS94379','TIP_AS94381') and s.id_sost = 'KB_USL60173') or
							 (da.napravl_ts in ('TIP_AS94380','TIP_AS94381') and s.id_sost = 'KB_USL60175'))
							  and da.VREM_POD_ZAIAV_ZA = (select max(ss.VREM_POD_ZAIAV_ZA) 
																						from kb_det_aspects ss
																					 where ss.id_aspect = du.id
																						and (to_char(nvl(v_dt_sost_end, v_dt_sost),'hh24.mi.ss') 
																	 between to_char(ss.vrem_rej_s,'hh24.mi.ss') and to_char(ss.vrem_rej_do,'hh24.mi.ss'))
																and ((ss.napravl_ts in ('TIP_AS94379','TIP_AS94381') and s.id_sost = 'KB_USL60173') or
																	  (ss.napravl_ts in ('TIP_AS94380','TIP_AS94381') and s.id_sost = 'KB_USL60175'))
																						 and ss.VREM_POD_ZAIAV_ZA*60 <= (v_dt_sost_end - v_edit_row_date)*24*60);
				 --alert_note(v_coef);
			     end if;
					insert into kb_sost 
								 (id_obsl
								 ,id_isp
								 ,dt_sost
								 ,dt_sost_end
								 ,id_sost
								 ,kpogr
								 )
						select p_id_obsl--:kb_sost.id_obsl
								,'010277043' --:kb_sost.id_isp --ООО "ГК "СЕВЕРТРАНС"
								,sysdate
								,sysdate
								,sl.val_id
								,nvl(v_coef,1)
							from sv_hvoc sl
						 where sl.voc_id    = 'KB_USL' 
							and sl.val_short = '4150';
				 --commit;
				 end if;
		end if; -- v_cnt
		exception
				when no_data_found then 
					insert into kb_sost 
									 (id_obsl
									 ,id_isp
									 ,dt_sost
									 ,dt_sost_end
									 ,id_sost
									 ,kpogr
									 )
								select p_id_obsl --:kb_sost.id_obsl
										,'010277043' --:kb_sost.id_isp --ООО "ГК "СЕВЕРТРАНС"
										,sysdate
										,sysdate
										,sl.val_id
										,1
									from sv_hvoc sl
								 where sl.voc_id    = 'KB_USL' 
									and sl.val_short = '4150';
											
					   FOR I IN (
				             select min(z.n_zak) n_z --клиент
								 ,min(sp.n_zakaza) v_n_z --номер заказа
								 ,stragg(sd.data) v_d --почта
			               from kb_spros sp, kb_zak z, sc_srv_data sd
	   		              where sp.id = p_id_obsl
				              	and sp.id_zak = z.id
				              	and sd.id_zak = z.id
				                and sd.id_type in ('SCSRVD99631', 'SCSRVD82176','SCSRVD100130')
				              ) LOOP
							if i.v_d is not null then
				           kb_mail3.send(i.v_d,
				                      'Клиент: '||i.n_z||' заказ ГС №'|| i.v_n_z||'',
				                      'В заказе '||i.v_n_z||' невозможно установить коэффициент срочности без участия сотрудника. '||CHR(10)||CHR(13)||
				                      'Необходимо выполнить ручную фиксацию коэффициента в событии 4150.', '');				                      
							end if;
						 end loop;
              	return;
			
				when others then
				      kb_mail3.send('oleg.soskin@gksvt.ru, gennadiy.mann@gksvt.ru', --адреса получателя через запятую
                    'Ошибка выполнения KB_MONITOR.CRT_COEF', --тема письма
                    SQLERRM, --содержимое письма
                    ''); --вложение, в основном просто пустое значение

	END CRT_COEF;	
	
END KB_MONITOR;
