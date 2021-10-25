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
	v_vn        VARCHAR2(6);
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

  PROCEDURE add_sku ( -- добавление по 1 файлу
		 p_msg IN CLOB, p_err OUT VARCHAR2 --p_vn IN NUMBER,
	) AS
	   --l_err        VARCHAR2(2048);
    v_id_obsl    VARCHAR2(38); --ИД суточного заказа
    l_id_zak     VARCHAR2(38);
    v_prf_wms    VARCHAR2(3);
    v_uof        VARCHAR2(38);
    v_id_usr     VARCHAR2(38);
    vn_not_found EXCEPTION;
  
    vn            VARCHAR2(2048);
    article       VARCHAR2(2048);
    upc           VARCHAR2(2048);
    art_name      VARCHAR2(2048);
    measure       VARCHAR2(2048);
    control_date  VARCHAR2(2048);
    storage_pos   VARCHAR2(2048);
    billing_class VARCHAR2(2048);
  BEGIN
    SELECT extractvalue(VALUE(t), '/AddingGoods/VN') vn, --
           extractvalue(VALUE(t), '/AddingGoods/ARTICLE') article, --
           extractvalue(VALUE(t), '/AddingGoods/UPC') upc, --
           extractvalue(VALUE(t), '/AddingGoods/NAME') art_name, --
           extractvalue(VALUE(t), '/AddingGoods/MEASURE') measure, --
           extractvalue(VALUE(t), '/AddingGoods/PRODUCT_LIFE') control_date, --
           extractvalue(VALUE(t), '/AddingGoods/STORAGE_POS') storage_pos, --
           extractvalue(VALUE(t), '/AddingGoods/BILLING_CLASS') billing_class --
      INTO vn, article, upc, art_name, measure, control_date, storage_pos, billing_class
      FROM TABLE(xmlsequence(extract(xmltype(p_msg),'/AddingGoods'))) t;
  
    IF vn IS NULL THEN
      p_err := 'отсутствует ВН';
      RAISE vn_not_found;
    END IF;
    --отмечаем, что взяли файл в работу
--    UPDATE KB_ICD_IN t SET t.message_status = 'R' WHERE t.message_id = p_id_file;
--    COMMIT;
  
    --разберёмся с клиентом, в переносном смысле или в прямом...
    BEGIN
      SELECT z.id, z.prf_wms, z.id_usr
        INTO l_id_zak, v_prf_wms, v_id_usr
        FROM kb_zak z
       WHERE z.id_klient = vn
             AND z.id_usr IN ('KB_USR92734', 'KB_USR99992');
    EXCEPTION
      WHEN OTHERS THEN
        p_err := 'не найден ВН';
        RAISE vn_not_found;
    END;
  
    --поиск/создание суточного заказа
    BEGIN
      SELECT sp.id
        INTO v_id_obsl
        FROM kb_spros sp, kb_zak z
       WHERE sp.n_gruz = 'SKU'
             AND trunc(sp.dt_zakaz) = trunc(SYSDATE)
             AND sp.id_zak = l_id_zak;
    EXCEPTION
      WHEN no_data_found THEN
        SELECT SV_UTILITIES.FORM_KEY(KB_SPROS_SEQ.NextVal) INTO v_id_obsl FROM dual;
      
        INSERT INTO kb_spros
          (id, dt_zakaz, id_zak, id_pok, n_gruz, usl)
        VALUES
          (v_id_obsl,
           trunc(SYSDATE),
           l_id_zak,
           l_id_zak,
           'SKU',
           'Суточный заказ по пакетам SKU');
      WHEN OTHERS THEN
        NULL;
    END;
  
    --- \/            -- передача номенклатуры
    DELETE FROM KB_T_ARTICLE;
    -- е.и. из справочника
    BEGIN
      SELECT h.val_id
        INTO v_uof
        FROM sv_hvoc h
       WHERE h.voc_id = 'KB_MEA'
             AND UPPER(h.val_short) = UPPER(measure);
    EXCEPTION
      WHEN OTHERS THEN
        v_uof := NULL;
    END;
  
    INSERT INTO KB_T_ARTICLE
      (id_sost, comments, measure, marker, str_sr_godn, storage_pos, tip_tov) -- str_mu_code,categ, MARKER)
    VALUES
      (article, art_name, v_uof, upc, control_date, storage_pos, billing_class);
  
    UPDATE KB_T_ARTICLE --- ???
       SET COMMENTS = REPLACE(REPLACE(RTRIM(LTRIM(REPLACE(TRANSLATE(COMMENTS, '?»«', '#""'), '°', ' град.'))),
                                      CHR(10)),
                              CHR(13)),
           id_sost  = REPLACE(REPLACE(RTRIM(LTRIM(TRANSLATE(id_sost, '~|?', '$@^'))), CHR(10)), CHR(13))
     WHERE COMMENTS <>
           REPLACE(REPLACE(RTRIM(LTRIM(REPLACE(TRANSLATE(COMMENTS, '?»«', '#""'), '°', ' град.'))), CHR(10)),
                   CHR(13))
           OR id_sost <> REPLACE(REPLACE(RTRIM(LTRIM(TRANSLATE(id_sost, '~|?', '$@^'))), CHR(10)), CHR(13));
  
    kb_pack.wms3_updt_sku(l_id_zak, v_prf_wms, p_err);
  
    --добавляем событие 4301 в заказ
    IF p_err IS NOT NULL AND p_err NOT LIKE 'Загружено записей:%' THEN
      INSERT INTO kb_sost
        (id_obsl, dt_sost, dt_sost_end, id_sost, sost_prm, id_isp) --sost_doc, 
      VALUES
        (v_id_obsl,
         SYSDATE,
         SYSDATE,
         'KB_USL99770',
--         p_id_file,
         'Артикул ' || article || ' не загруже по причине:' || p_err,
         '010277043');
    
--      UPDATE KB_ICD_IN i
--         SET i.message_status          = 'E',
--             i.message_err_code        = p_err,
--             i.message_processing_date = SYSDATE
--       WHERE i.message_id = p_id_file;
--      send_notification(p_id_file, p_err, vn);
    ELSE
		P_ERR := null;
      INSERT INTO kb_sost
        (id_obsl, dt_sost, dt_sost_end, id_sost, sost_prm, id_isp)--sost_doc, 
      VALUES
        (v_id_obsl,
         SYSDATE,
         SYSDATE,
         'KB_USL99770',
--         p_id_file,
         'Артикул ' || article || ' отправлен в СОХ',
         '010277043');
    
      -- change status to S
--      UPDATE KB_ICD_IN t SET t.message_status = 'S' WHERE t.message_id = p_id_file;
    END IF;
    COMMIT;
    --END LOOP rec;
EXCEPTION
    WHEN vn_not_found THEN
--      ROLLBACK;
--      UPDATE KB_ICD_IN t
--         SET t.message_status   = 'E',
--             t.message_err_code = p_err
--       WHERE t.message_id = p_id_file;
--      COMMIT;
--      send_notification(p_id_file, p_err);
      dbms_output.put_line(p_err);
    
    WHEN OTHERS THEN
    
      p_err := SQLERRM;
		dbms_output.put_line(p_err);
--      send_notification(p_id_file, p_err);
  END ADD_SKU;
  
    --процедура парсинга и передачи в солво заявок на ПО ICD--
  PROCEDURE msg_4101(p_msg IN CLOB, p_err OUT VARCHAR2) IS
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
    v_dt_rec     DATE;
    v_counter    NUMBER;
    v_rows       NUMBER;
    v_sost_doc   VARCHAR2(38);
    pack_err     VARCHAR2(3800);
    v_prfx       VARCHAR2(5 CHAR);
    v_id_wms_zak VARCHAR2(38);
    v_id_svh     VARCHAR2(38);
    v_id_usr     VARCHAR2(38);
    vn_not_found EXCEPTION;
  BEGIN
    --делаем для ПО
    SAVEPOINT s1;
    FOR rec IN (SELECT extractvalue(VALUE(t), '/ReceiptOrderForGoods/NumberDoc') AS number1, --Номер ПО
                       extractvalue(VALUE(t), '/ReceiptOrderForGoods/DateDoc') AS Date1, --Дата ПО
                       extractvalue(VALUE(t), '/ReceiptOrderForGoods/Customer') AS Customer, --Заказчик
                       extractvalue(VALUE(t), '/ReceiptOrderForGoods/OrderType') AS OrderType, --Тип заказа
                       extractvalue(VALUE(t), '/ReceiptOrderForGoods/TypeOfDelivery') AS TypeOfDelivery, --Тип поставки
                       extractvalue(VALUE(t), '/ReceiptOrderForGoods/PlannedDeliveryDate') AS PlannedDeliveryDate, --Планируемая дата поставки
                       extractvalue(VALUE(t), '/ReceiptOrderForGoods/IDSupplier') AS IDSupplier, --код поставщика
                       extractvalue(VALUE(t), '/ReceiptOrderForGoods/NameSupplier') AS NameSupplier, --имя поставщика
                       extractvalue(VALUE(t), '/ReceiptOrderForGoods/AdressSupplier') AS AdressSupplier, --адрес поставщика
                       extractvalue(VALUE(t), '/ReceiptOrderForGoods/VN') AS VN, --ВН клиента
                       extractvalue(VALUE(t), '/ReceiptOrderForGoods/IDCarrier') AS IDCarrier, --код перевозчика
                       extractvalue(VALUE(t), '/ReceiptOrderForGoods/TypeCar') AS TypeCar, --Тип машины
                       extractvalue(VALUE(t), '/ReceiptOrderForGoods/NumberCar') AS NumberCar, --Номер машины
                       extractvalue(VALUE(t), '/ReceiptOrderForGoods/Driver') Driver,
							  extractvalue(VALUE(t), '/ReceiptOrderForGoods/GUID') docID
                  FROM TABLE(xmlsequence(extract(xmltype(p_msg),'//ReceiptOrderForGoods'))) t)
    LOOP
    
      IF rec.vn IS NULL THEN
        p_err := 'отсутствует ВН';
        RAISE vn_not_found;
      END IF;
    
    --   UPDATE KB_ICD_IN t SET t.message_status = 'R' WHERE t.message_id = p_id_file;
    --   COMMIT;
      v_dt_rec := to_date(rec.planneddeliverydate, 'dd.mm.yyyy hh24:mi:ss');
      v_vn     := rec.vn;
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
        v_id_tir := Utility_Pkg.find_tir(v_n_avto, v_id_zak, to_date(rec.planneddeliverydate, 'dd.mm.rrrr hh24:mi'));
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
        SELECT s.val_id INTO v_id_tzs FROM sv_hvoc s WHERE upper(s.val_full) = upper(rec.OrderType);
      EXCEPTION
        WHEN no_data_found THEN
          p_err := 'Неправильный тип поставки.';
          EXIT;
      END;
      --наполнение заказа
      --4101
      INSERT INTO kb_sost
        (id_obsl, dt_sost, dt_sost_end, id_sost, id_dog, sost_doc, id_isp, id_tzs, dt_doc, sost_prm)
      VALUES
        (v_id_obsl,
         to_date(rec.planneddeliverydate, 'dd.mm.rrrr hh24:mi'),
         to_date(rec.planneddeliverydate, 'dd.mm.rrrr hh24:mi'),
         'KB_USL60173',
         substr(v_id_dog, 2),
         c_test || rec.Number1,
         '010277043',
         v_id_tzs,
         rec.date1,
         c_test)
      RETURNING id INTO v_id_sost;
      --добавляем событие 4301
      INSERT INTO kb_sost
        (id_obsl, dt_sost, dt_sost_end, id_sost,  sost_prm, id_isp,id_du)--sost_doc,
      VALUES
        (v_id_obsl, SYSDATE, SYSDATE, 'KB_USL99770', 'ПО', '010277043',rec.docID); --rec.guid p_id_file, 
    
      FOR rec_det IN (SELECT extractvalue(VALUE(t), '/Goods/LineNumber') AS LineNumber, --номер строки
                             extractvalue(VALUE(t), '/Goods/Article') AS Article, --артикул товара
                             extractvalue(VALUE(t), '/Goods/Name') AS NAME, --имя товара
                             extractvalue(VALUE(t), '/Goods/Category') AS Category, --категория товара
                             extractvalue(VALUE(t), '/Goods/Mark2') AS Mark, --номер документа
                             extractvalue(VALUE(t), '/Goods/Mark2') AS Mark2, --номер документа
                             extractvalue(VALUE(t), '/Goods/Mark2') AS Mark3, --номер документа
                             extractvalue(VALUE(t), '/Goods/Count') AS Count1, --кол-во
                             extractvalue(VALUE(t), '/Goods/Comment') AS Comment1 --Комментарий
                        FROM TABLE(xmlsequence(extract(xmltype(p_msg),'//ReceiptOrderForGoods/Goods'))) t)
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
         to_char(v_dt_rec, 'dd.mm.yyyy') --f09
        ,
         to_char(v_dt_rec, 'hh24:mi') --f10
        ,
         'нет',
         v_id_wms,
         nvl(v_id_wms_zak, v_id_wms));
      --проверка заказа перед отправкой      
      kb_pack.wms3_Check_OrderA(pack_err, 'INCOMING', v_id_sost, v_tmp, v_tmp, v_tmp);
    
      IF (pack_err IS NOT NULL) THEN
        p_err := pack_err;
      ELSE
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
    WHEN vn_not_found THEN
      ROLLBACK;
    --   UPDATE KB_ICD_IN t SET t.message_status   = 'E', t.message_err_code = p_err WHERE t.message_id = p_id_file;
    --   COMMIT;
    --   send_notification(p_id_file, p_err);
    
    WHEN OTHERS THEN
      ROLLBACK;
    --   UPDATE KB_ICD_IN t SET t.message_status   = 'E', t.message_err_code = 'Некорректное вложение' WHERE t.message_id = p_id_file;
    --   COMMIT;
      p_err := SQLERRM;
    --   send_notification(p_id_file, p_err);
    
  END MSG_4101;
  --Конец разборки с ПО--

    PROCEDURE MSG_4103(p_msg IN CLOB, p_err OUT VARCHAR2) IS
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
    v_dt_rec     DATE;
    v_counter    NUMBER;
    v_rows       NUMBER;
    v_sost_doc   VARCHAR2(38);
    pack_err     VARCHAR2(380);
    v_prfx       VARCHAR2(5 CHAR);
    v_id_wms_zak VARCHAR2(20);
    v_id_svh     VARCHAR2(38);
    v_id_usr     VARCHAR2(38);
    vn_not_found EXCEPTION;
  BEGIN
    --делаем для ПО
    SAVEPOINT s1;
    FOR rec IN (SELECT extractvalue(VALUE(t), '/ExpenditureOrderForGoods/NumberDoc') AS number1, --Номер рО
                       extractvalue(VALUE(t), '/ExpenditureOrderForGoods/DateDoc') AS Date1, --Дата РО
                       extractvalue(VALUE(t), '/ExpenditureOrderForGoods/Customer') AS Customer, --Отправитель
                       extractvalue(VALUE(t), '/ExpenditureOrderForGoods/OrderType') AS OrderType, --Тип заказа
                       extractvalue(VALUE(t), '/ExpenditureOrderForGoods/TypeOfDelivery') AS TypeOfDelivery, --Тип поставки
                       extractvalue(VALUE(t), '/ExpenditureOrderForGoods/PlannedShipmentDate') AS PlannedShipmentDate, --Планируемая дата поставки
                       extractvalue(VALUE(t), '/ExpenditureOrderForGoods/IDConsignee') AS IDConsignee, --код получателя
                       extractvalue(VALUE(t), '/ExpenditureOrderForGoods/NameConsignee') AS NameConsignee, --имя пполучателя
                       extractvalue(VALUE(t), '/ExpenditureOrderForGoods/VN') AS VN, --ВН клиента
                       extractvalue(VALUE(t), '/ExpenditureOrderForGoods/IDCarrier') AS IDCarrier, --код перевозчика
                       extractvalue(VALUE(t), '/ExpenditureOrderForGoods/TypeCar') AS TypeCar, --Тип машины
                       extractvalue(VALUE(t), '/ExpenditureOrderForGoods/NumberCar') AS NumberCar, --Номер машины
                       extractvalue(VALUE(t), '/ExpenditureOrderForGoods/Driver') Driver,
							  extractvalue(VALUE(t), '/ExpenditureOrderForGoods/GUID') docID,
							  extractvalue(VALUE(t), '/ExpenditureOrderForGoods/Email') Email,
							  extractvalue(VALUE(t), '/ExpenditureOrderForGoods/AdressConsignee') AdressConsignee, --адр получателя
							  extractvalue(VALUE(t), '/ExpenditureOrderForGoods/Comment') Comment1 --Комментарий
                  FROM TABLE(xmlsequence(extract(xmltype(p_msg),'//ExpenditureOrderForGoods'))) t)
    LOOP
    
      IF rec.vn IS NULL THEN
        p_err := 'отсутствует ВН';
        RAISE vn_not_found;
      END IF;
    
      -- изменить статус
--      UPDATE KB_HLM_IN t SET t.message_status = 'R' WHERE t.message_id = p_id_file;
--      COMMIT;
      v_dt_rec := to_date(rec.PlannedShipmentDate, 'dd.mm.yyyy hh24:mi:ss');
      v_vn     := rec.vn;
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
        v_id_tir := Utility_Pkg.find_tir(v_n_avto, v_id_zak, to_date(rec.plannedshipmentdate, 'dd.mm.rrrr hh24:mi:ss'));
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
    
--      SELECT SUBSTR(regexp_substr(t.message_name, '[^\]*$'), 1, INSTR(regexp_substr(t.message_name, '[^\]*$'), '.', -1) - 1)
--        INTO v_file_name
--        FROM Kb_Hlm_in t
--       WHERE t.message_id = p_id_file;
    
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
        SELECT s.val_id INTO v_id_tzs FROM sv_hvoc s WHERE upper(s.val_full) = upper(rec.OrderType);
      EXCEPTION
        WHEN no_data_found THEN
          p_err := 'Неправильный тип отгрузки.';
          EXIT;
      END;
      --создаем событие 4103 - плановая отгрузка товара точка входа в солво
      
      SELECT SV_UTILITIES.FORM_KEY(KB_KONTR_SEQ.NextVal)	INTO v_id_sost FROM dual;
      INSERT INTO kb_sost
        (id,id_obsl, dt_sost, dt_sost_end, id_sost, id_dog, sost_doc, id_isp /*мы*/, id_tzs, dt_doc, sost_prm)
      VALUES
        (v_id_sost,
        v_id_obsl,
         to_date(rec.PlannedShipmentDate, 'dd.mm.rrrr hh24:mi:ss'),
         to_date(rec.PlannedShipmentDate, 'dd.mm.rrrr hh24:mi:ss'),
         'KB_USL60175' /* 4103*/,
         substr(v_id_dog, 2),
         c_test || rec.number1,
         '010277043',
         v_id_tzs, --SCH_NP94574 Отгрузка
         rec.date1,
         rec.Comment1);
--      RETURNING id INTO v_id_sost; --- услуга
      --добавляем событие 4301 Получено входящее сообщение
      INSERT INTO kb_sost
        (id_obsl, dt_sost, dt_sost_end, id_sost, sost_prm, id_isp,id_du)
      VALUES
        (v_id_obsl, SYSDATE, SYSDATE, 'KB_USL99770', 'РО', '010277043',rec.docID);
    
      --v_sost_doc := rec.NumberImportInvoice;
      FOR rec_det IN (SELECT extractvalue(VALUE(t), '/Goods/LineNumber') AS LineNumber, --номер строки
                             extractvalue(VALUE(t), '/Goods/Article') AS Article, --артикул товара
                             extractvalue(VALUE(t), '/Goods/Name') AS NAME, --имя товара
                             extractvalue(VALUE(t), '/Goods/Category') AS Category, --категория товара
                              extractvalue(VALUE(t), '/Goods/Mark') AS Mark, --номер документа
                             extractvalue(VALUE(t), '/Goods/Mark2') AS Mark2, --номер документа
                             extractvalue(VALUE(t), '/Goods/Mark3') AS Mark3, --номер документа
                             extractvalue(VALUE(t), '/Goods/Count') AS Count1, --кол-во
                             extractvalue(VALUE(t), '/Goods/Comment') Comment1, --Комментарий
                             extractvalue(VALUE(t), '/Goods/StorageLife') StorageLife
                        FROM TABLE(xmlsequence(extract(xmltype(p_msg),'//ExpenditureOrderForGoods/Goods'))) t)
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
        ,to_char(v_dt_rec, 'dd.mm.yyyy') --f09
        ,to_char(v_dt_rec, 'hh24:mi') --f10
			,'нет',
         v_id_wms,
         nvl(v_id_wms_zak, v_id_wms));
      --проверка заказа перед отправкой      
      kb_pack.wms3_Check_OrderA(pack_err, 'ORDER', v_id_sost, v_tmp, v_tmp, v_tmp);

      IF (pack_err IS NOT NULL) THEN
        p_err := pack_err;
      ELSE
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
--      UPDATE KB_HLM_IN t
--         SET t.message_status   = 'E',
--             t.message_err_code = p_err
--       WHERE t.message_id = p_id_file;
--      send_notification(p_id_file, p_err, v_vn);
    ELSE
      -- change status to S
--      UPDATE KB_HLM_IN t SET t.message_status = 'S' WHERE t.message_id = p_id_file;
      -- test
      IF LENGTH(c_test) != 0 THEN
        dbms_output.put_line('Order # ' || v_id_obsl);
      END IF;

    END IF;
    COMMIT;
  EXCEPTION
    WHEN vn_not_found THEN
      ROLLBACK;
--      UPDATE KB_HLM_IN t SET t.message_status   = 'E', t.message_err_code = p_err WHERE t.message_id = p_id_file;
--      COMMIT;
--      send_notification(p_id_file, p_err);
    
    WHEN OTHERS THEN
      ROLLBACK;
--      UPDATE KB_HLM_IN t SET t.message_status   = 'E', t.message_err_code = 'Некорректное вложение' WHERE t.message_id = p_id_file;
--      COMMIT;
      p_err := SQLERRM;
--      send_notification(p_id_file, p_err);
  END MSG_4103;
  --/////Конец разбора РО\\\\\\

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
    v_dt_rec     DATE;
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
		--                       extractvalue(VALUE(t), '/Shell/Customer') AS Customer, --Заказчик
			  extractvalue(VALUE(t), '/Shell/order/orderType') AS OrderType, --Тип заказа
		     extractvalue(VALUE(t), '/Shell/order/orderKind') AS TypeOfDelivery, --Тип поставки
			  extractvalue(VALUE(t), '/Shell/order/plannedDate') AS PlannedDeliveryDate,
			  extractvalue(VALUE(t), '/Shell/order/contrCode') AS IDSupplier, --код поставщика
			  extractvalue(VALUE(t), '/Shell/order/contrName') AS NameSupplier, --имя поставщика
			  extractvalue(VALUE(t), '/Shell/order/contrAddress') AS AdressSupplier, --адрес поставщика
		--                       extractvalue(VALUE(t), '/Shell/order/IDCarrier') AS IDCarrier, --код перевозчика
		--                       extractvalue(VALUE(t), '/Shell/order/TypeCar') AS TypeCar, --Тип машины
			  extractvalue(VALUE(t), '/Shell/order/licencePlate') AS NumberCar, --Номер машины
			  extractvalue(VALUE(t), '/Shell/order/driver') Driver,
			  extractvalue(VALUE(t), '/Shell/order/guid') docID
		FROM TABLE(xmlsequence(extract(xmltype(REPLACE(v_msg,' xmlns="http://www.severtrans.com"')),'//Shell'))) t)
   LOOP
	
		v_order_Date := to_date(REPLACE(rec.Date1,'T',' '), 'yyyy-mm-dd hh24:mi:ss'); --'2021-06-06T15:52:50'
		v_planned_Date := to_date(REPLACE(rec.PlannedDeliveryDate,'T',' '), 'yyyy-mm-dd hh24:mi:ss');
--		IF UPPER(rec.OrderType) = 'FALSE' THEN
--			v_OrderType := 'ПОСТАВКА';
--		ELSE
--			v_OrderType := 'ОТГРУЗКА';
--		END IF;
--костыль 
--      IF rec.msgID IS NOT NULL THEN
--			rec.docID := rec.msgID;
--      END IF;
		
      IF rec.vn IS NULL THEN
        p_err := 'отсутствует ВН';
        RAISE vn_not_found;
      END IF;
    
      v_dt_rec := v_planned_Date;--to_date(rec.planneddeliverydate, 'dd.mm.yyyy hh24:mi:ss');
      v_vn := rec.vn;

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
        WHERE upper(s.val_full) = upper(rec.TypeOfDelivery) AND VOC_ID='SCH_NP';
      EXCEPTION
        WHEN no_data_found THEN
          p_err := 'Неправильный тип поставки.';
          EXIT;
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
			  extractvalue(VALUE(t), '/orderLine/mark') AS Mark, --номер документа
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
    WHEN vn_not_found THEN
      ROLLBACK;
    --   UPDATE KB_ICD_IN t SET t.message_status   = 'E', t.message_err_code = p_err WHERE t.message_id = p_id_file;
    --   COMMIT;
    --   send_notification(p_id_file, p_err);
    
    WHEN OTHERS THEN
      ROLLBACK;
    --   UPDATE KB_ICD_IN t SET t.message_status   = 'E', t.message_err_code = 'Некорректное вложение' WHERE t.message_id = p_id_file;
    --   COMMIT;
      p_err := SQLERRM;
    --   send_notification(p_id_file, p_err);
    
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
    v_dt_rec     DATE;
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
    
      v_dt_rec := v_planned_Date;--to_date(rec.PlannedShipmentDate, 'dd.mm.yyyy hh24:mi:ss');
      v_vn     := rec.vn;
   
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
        WHERE upper(s.val_full) = upper(rec.TypeOfDelivery) AND VOC_ID='SCH_NP';
      EXCEPTION
        WHEN no_data_found THEN
          p_err := 'Неправильный тип отгрузки.';
          EXIT;
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
			  extractvalue(VALUE(t), '/orderLine/mark') AS Mark, --номер документа
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
--      UPDATE KB_HLM_IN t
--         SET t.message_status   = 'E',
--             t.message_err_code = p_err
--       WHERE t.message_id = p_id_file;
--      send_notification(p_id_file, p_err, v_vn);
    ELSE
      -- change status to S
--      UPDATE KB_HLM_IN t SET t.message_status = 'S' WHERE t.message_id = p_id_file;
      -- test
      IF LENGTH(c_test) != 0 THEN
        dbms_output.put_line('Order # ' || v_id_obsl);
      END IF;

    END IF;
    COMMIT;
  EXCEPTION
    WHEN vn_not_found THEN
      ROLLBACK;
--      UPDATE KB_HLM_IN t SET t.message_status   = 'E', t.message_err_code = p_err WHERE t.message_id = p_id_file;
--      COMMIT;
--      send_notification(p_id_file, p_err);
    
    WHEN OTHERS THEN
      ROLLBACK;
--      UPDATE KB_HLM_IN t SET t.message_status   = 'E', t.message_err_code = 'Некорректное вложение' WHERE t.message_id = p_id_file;
--      COMMIT;
      p_err := SQLERRM;
--      send_notification(p_id_file, p_err);
  END MSG_4103_;
  --/////Конец разбора РО\\\\\\
	
END KB_MONITOR;
