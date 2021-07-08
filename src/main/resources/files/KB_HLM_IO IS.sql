create or replace PACKAGE BODY KB_HLM_IO IS

  v_file_name VARCHAR2(100);
  v_txt       VARCHAR2(32000);
  g_clob      CLOB;
  v_vn        VARCHAR2(6);
  v_n_avto    VARCHAR2(40);
  --v_id_user VARCHAR2(38);
  v_error VARCHAR2(1000);

  --Функция для перевода из блоба в клобыыы, вдруг кому пригодится, здесь не пригодилась...
  FUNCTION F2B(B IN BLOB) RETURN CLOB IS
    c CLOB;
    n NUMBER;
  BEGIN
    --t := bin_files.retrieve(b);
    IF (b IS NULL) THEN
      RETURN NULL;
    END IF;
    IF (length(b) = 0) THEN
      RETURN empty_clob();
    END IF;
    dbms_lob.createtemporary(c, TRUE);
    n := 1;
    WHILE (n + 32767 <= length(b))
    LOOP
      dbms_lob.writeappend(c, 32767, utl_raw.cast_to_varchar2(dbms_lob.substr(b, 32767, n)));
      n := n + 32767;
    END LOOP;
    dbms_lob.writeappend(c, length(b) - n + 1, utl_raw.cast_to_varchar2(dbms_lob.substr(b, length(b) - n + 1, n)));
    --return convert(c,'UTF8','CL8MSWIN1251');
    RETURN c;
  END;
  /**********************************
  * Оповещение клиентов об ошибках
  **********************************/
  PROCEDURE send_notification(p_id_file IN NUMBER, p_err OUT VARCHAR2) IS
  BEGIN
    FOR rec IN (SELECT ssd.data --ssd.*, z.*
                  FROM kb_zak z, sc_srv_data ssd
                 WHERE z.id_klient IN (300223, 300228, 300217, 300237, 300255)
                       AND z.id = ssd.id_zak
                       AND ssd.id_srv = '5'
                       AND ssd.id_type = 'SCSRVD100130'
                       AND REPLACE(ssd.data, ' ') IS NOT NULL)
    LOOP
      kb_mail3.send(rec.data, --адреса получателя через запятую
                    'Ошибка разбора файла ' || p_id_file || ' HELLMANN', --тема письма
                    'При разборе файла ' || p_id_file || ' произошла ошибка: ' || p_err, --содержимое письма
                    ''); --вложение, в основном просто пустое значение
    END LOOP;
  END send_notification;

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
                    'Ошибка разбора файла ' || p_id_file || ' HELLMANN', --тема письма
                    'При разборе файла ' || p_id_file || ' произошла ошибка: ' || p_err, --содержимое письма
                    ''); --вложение, в основном просто пустое значение
    END LOOP;
  END send_notification;

  --процедура парсинга и передачи в солво заявок на ПО Хеллманн--
  PROCEDURE HLM_4101(p_id_file IN NUMBER, p_err OUT VARCHAR2) IS
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
    FOR rec IN (SELECT extractvalue(VALUE(t), '/ReceiptOrderForGoods/Number') AS number1, --Номер ПО
                       extractvalue(VALUE(t), '/ReceiptOrderForGoods/Date') AS Date1, --Дата ПО
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
                       extractvalue(VALUE(t), '/ReceiptOrderForGoods/Driver') Driver, extractvalue(VALUE(t),
                                     '/ReceiptOrderForGoods/NumberImportInvoice') NumberImportInvoice, --это нужнО для события 4151
                       extractvalue(VALUE(t), '/ReceiptOrderForGoods/DateImportInvoice') DateImportInvoice --это нужнО для события 4151
                  FROM TABLE(xmlsequence(extract((SELECT xmltype(f.message_data)
                                                    FROM kb_hlm_in f
                                                   WHERE f.message_id = p_id_file),
                                                  '//ReceiptOrderForGoods'))) t)
    LOOP
    
      IF rec.vn IS NULL THEN
        p_err := 'отсутствует ВН';
        RAISE vn_not_found;
      END IF;
    
      UPDATE KB_HLM_IN t SET t.message_status = 'R' WHERE t.message_id = p_id_file;
      COMMIT;
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
          EXIT;
      END;
    
      --заделаем машину, почти как Генри Форд
      IF rec.numbercar IS NOT NULL THEN
        v_n_avto := utility_pkg.String2AutoNumber(rec.numbercar);
        v_id_tir := Utility_Pkg.find_tir(v_n_avto, v_id_zak, to_date(rec.planneddeliverydate, 'dd.mm.rrrr hh24:mi:ss'));
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
    
      -- имя файла без расширения
      SELECT SUBSTR(regexp_substr(t.message_name, '[^\]*$'),
                     1,
                     INSTR(regexp_substr(t.message_name, '[^\]*$'), '.', -1) - 1)
        INTO v_file_name
        FROM Kb_Hlm_In t
       WHERE t.message_id = p_id_file;
    
      INSERT INTO kb_spros
        (n_gruz, dt_zakaz, id_zak, id_pok, is_postavka, id, id_spros, id_tir, id_kat)
      VALUES
        (c_test || 'FTP ПОСТАВКА -> ' || rec.number1, --- || ' -> ' || v_file_name
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
        SELECT s.val_id INTO v_id_tzs FROM sv_hvoc s WHERE upper(s.val_full) = upper(rec.typeofdelivery);
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
         to_date(rec.planneddeliverydate, 'dd.mm.rrrr hh24:mi:ss'),
         to_date(rec.planneddeliverydate, 'dd.mm.rrrr hh24:mi:ss'),
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
        (id_obsl, dt_sost, dt_sost_end, id_sost, sost_doc, sost_prm, id_isp)
      VALUES
        (v_id_obsl, SYSDATE, SYSDATE, 'KB_USL99770', p_id_file, 'ПО_ХЛМ ' || v_file_name, '010277043');
    --kb_ttn adr_otpr | kb_t_mdet f20/*партия*/
      FOR rec_det IN (SELECT extractvalue(VALUE(t), '/Goods/LineNumber') AS LineNumber, --номер строки
                             extractvalue(VALUE(t), '/Goods/Article') AS Article, --артикул товара
                             extractvalue(VALUE(t), '/Goods/Name') AS NAME, --имя товара
                             extractvalue(VALUE(t), '/Goods/Category') AS Category, --категория товара
                             extractvalue(VALUE(t), '/Goods/Mark') AS Mark, --номер документа
                             extractvalue(VALUE(t), '/Goods/Mark2') AS Mark2, --номер документа
                             extractvalue(VALUE(t), '/Goods/Mark3') AS Mark3, --номер документа
                             extractvalue(VALUE(t), '/Goods/Count') AS Count1, --кол-во
                             extractvalue(VALUE(t), '/Goods/Comment') Comment1 --Комментарий
                        FROM TABLE(xmlsequence(extract((SELECT xmltype(f.message_data)
                                                          FROM kb_hlm_in f
                                                         WHERE f.message_id = p_id_file),
                                                        '//ReceiptOrderForGoods/Goods'))) t)
      LOOP
        --поиск номенклатру в справочнике
        SELECT COUNT(1) INTO cnt_sku FROM sku s WHERE s.sku_id = v_prfx || rec_det.article;
      
        IF nvl(cnt_sku, 0) = 0 THEN
          p_err := 'Не найдена номенклатура ' || rec_det.article;
        END IF;
        IF p_err IS NOT NULL THEN
          ROLLBACK;
          EXIT;
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
      dbms_output.put_line(v_id_obsl);
      INSERT INTO kb_t_mdet
        (id_sost, id_obsl, f01, f02, f05 /*категория*/, f06 /*маркер*/, f18 /*маркер 2*/,f21/*маркер 3*/)
        (SELECT 'KB_USL60173', v_id_obsl, n_tovar, KOL_TOVAR, brak,
		  pak_tovar, ul_otpr, usl FROM kb_ttn WHERE id_obsl = v_id_obsl);
    
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
      --IF v_id_usr = 'KB_USR99992' THEN
        ---!!!тоже по 4103
        kb_pack.wms3_Check_OrderA(pack_err, 'INCOMING', v_id_sost, v_tmp, v_tmp, v_tmp);
      /*ELSIF v_id_usr = 'KB_USR92734' THEN
        kb_pack.wms2_Check_OrderA(pack_err, 'INCOMING', v_id_sost, v_tmp, v_tmp, v_tmp);
      ELSE
        v_error := 'Не правильно указан шлюз!';
      END IF;*/
    
      IF (pack_err IS NOT NULL) THEN
        p_err := pack_err;
      ELSE
        --фактическая передача данных в СОЛВО ---!!!
        --IF v_id_usr = 'KB_USR99992' THEN
          kb_pack.wms3_export_io(pack_err, 'INCOMING', v_id_sost);
        /*ELSIF v_id_usr = 'KB_USR92734' THEN
          kb_pack.wms2_export_io(pack_err, 'INCOMING', v_id_sost);
        ELSE
          v_error := 'Не правильно указан шлюз!';
        END IF;*/
      
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
      UPDATE KB_HLM_IN t
         SET t.message_status   = 'E',
             t.message_err_code = p_err
       WHERE t.message_id = p_id_file;
      send_notification(p_id_file, p_err, v_vn);
    ELSE
      -- change status to S
      UPDATE KB_HLM_IN t SET t.message_status = 'S' WHERE t.message_id = p_id_file;
      -- test
      IF LENGTH(c_test) != 0 THEN
        dbms_output.put_line('Order # ' || v_id_obsl);
      END IF;
    
    END IF;
    COMMIT;
  EXCEPTION
    WHEN vn_not_found THEN
      ROLLBACK;
      UPDATE KB_HLM_IN t
         SET t.message_status   = 'E',
             t.message_err_code = p_err
       WHERE t.message_id = p_id_file;
      COMMIT;
      send_notification(p_id_file, p_err);
      dbms_output.put_line(p_err);
    
    WHEN OTHERS THEN
      dbms_output.put_line(SQLERRM);
      ROLLBACK;
    
      UPDATE KB_HLM_IN t
         SET t.message_status   = 'E',
             t.message_err_code = 'Некорректное вложение'
       WHERE t.message_id = p_id_file;
      COMMIT;
      p_err := SQLERRM;
      send_notification(p_id_file, p_err);
    
  END HLM_4101;
  --Конец разборки с ПО--

  --/////////////Разбор РО(отгрузок)\\\\\\\\\\\\\\\\\\
  
  PROCEDURE HLM_4103(p_id_file IN NUMBER, p_err OUT VARCHAR2) IS
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
    FOR rec IN (SELECT extractvalue(VALUE(t), '/ExpenditureOrderForGoods/Number') AS number1, --Номер рО
                       extractvalue(VALUE(t), '/ExpenditureOrderForGoods/Date') AS Date1, --Дата РО
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
                       extractvalue(VALUE(t), '/ExpenditureOrderForGoods/Driver') Driver, extractvalue(VALUE(t),
                                     '/ExpenditureOrderForGoods/Email') Email, extractvalue(VALUE(t),
                                     '/ExpenditureOrderForGoods/AdressConsignee') AdressConsignee --адр получателя
                  FROM TABLE(xmlsequence(extract((SELECT xmltype(f.message_data)
                                                    FROM kb_hlm_in f
                                                   WHERE f.message_id = p_id_file),
                                                  '//ExpenditureOrderForGoods'))) t)
    LOOP
    
      IF rec.vn IS NULL THEN
        p_err := 'отсутствует ВН';
        RAISE vn_not_found;
      END IF;
    
      -- изменить статус
      UPDATE KB_HLM_IN t SET t.message_status = 'R' WHERE t.message_id = p_id_file;
      COMMIT;
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
    
      SELECT SUBSTR(regexp_substr(t.message_name, '[^\]*$'), 1, INSTR(regexp_substr(t.message_name, '[^\]*$'), '.', -1) - 1)
        INTO v_file_name
        FROM Kb_Hlm_in t
       WHERE t.message_id = p_id_file;
    
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
        (c_test || 'FTP Отгрузка --> ' || rec.number1, --- || ' --> ' || v_file_name
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
        SELECT s.val_id INTO v_id_tzs FROM sv_hvoc s WHERE upper(s.val_full) = upper(rec.typeofdelivery);
      EXCEPTION
        WHEN no_data_found THEN
          --rollback to s1;
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
         c_test); -- примечание?
--      RETURNING id INTO v_id_sost; --- услуга
      --добавляем событие 4301 Получено входящее сообщение
      INSERT INTO kb_sost
        (id_obsl, dt_sost, dt_sost_end, id_sost, sost_doc, sost_prm, id_isp)
      VALUES
        (v_id_obsl, SYSDATE, SYSDATE, 'KB_USL99770', p_id_file, 'РО_ХЛМ ' || v_file_name, '010277043');
    
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
                        FROM TABLE(xmlsequence(extract((SELECT xmltype(f.message_data)
                                                          FROM kb_hlm_in f
                                                         WHERE f.message_id = p_id_file),
                                                        '//ExpenditureOrderForGoods/Goods'))) t)
      LOOP
        --проверка номенклатуры
        SELECT COUNT(1) INTO cnt_sku FROM sku s WHERE s.sku_id = v_prfx || rec_det.article;
        IF nvl(cnt_sku, 0) = 0 THEN
          p_err := 'Не найдена номенклатура ' || rec_det.article;
        END IF;
        IF p_err IS NOT NULL THEN
          ROLLBACK;
          EXIT;
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
--      IF v_id_usr = 'KB_USR99992' THEN
        kb_pack.wms3_Check_OrderA(pack_err, 'ORDER', v_id_sost, v_tmp, v_tmp, v_tmp);
/*      ELSIF v_id_usr = 'KB_USR92734' THEN
        kb_pack.wms2_Check_OrderA(pack_err, 'ORDER', v_id_sost, v_tmp, v_tmp, v_tmp);
      ELSE
        pack_err := 'Не правильно указан шлюз!';
      END IF;
*/    
      IF (pack_err IS NOT NULL) THEN
        p_err := pack_err;
      ELSE
--        IF v_id_usr = 'KB_USR99992' THEN
          kb_pack.wms3_export_io(pack_err, 'ORDER', v_id_sost);
/*        ELSIF v_id_usr = 'KB_USR92734' THEN
          kb_pack.wms2_export_io(pack_err, 'ORDER', v_id_sost);
        ELSE
          p_err := 'Не правильно указан шлюз!';
        END IF;*/
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
      UPDATE KB_HLM_IN t
         SET t.message_status   = 'E',
             t.message_err_code = p_err
       WHERE t.message_id = p_id_file;
      send_notification(p_id_file, p_err, v_vn);
    ELSE
      -- change status to S
      UPDATE KB_HLM_IN t SET t.message_status = 'S' WHERE t.message_id = p_id_file;
    END IF;
    COMMIT;
  EXCEPTION
    WHEN vn_not_found THEN
      ROLLBACK;
      UPDATE KB_HLM_IN t
         SET t.message_status   = 'E',
             t.message_err_code = p_err
       WHERE t.message_id = p_id_file;
      COMMIT;
      send_notification(p_id_file, p_err);
      dbms_output.put_line(p_err);
    
    WHEN OTHERS THEN
      ROLLBACK;
    
      UPDATE KB_HLM_IN t
         SET t.message_status   = 'E',
             t.message_err_code = 'Некорректное вложение'
       WHERE t.message_id = p_id_file;
      COMMIT;
    
      p_err := SQLERRM;
      --добавил SOL 25112020
      send_notification(p_id_file, p_err);
  END HLM_4103;
  --/////Конец разбора РО\\\\\\

  --функция находит все фактически отгруженные ПО, обрабатывает их и формирует клобы,
  --которые можно выгрузить в файлы...
  PROCEDURE HLM_4102 IS
    i NUMBER;
  BEGIN
    FOR rec IN (SELECT DISTINCT st.dt_sost, -- Дата заявки
                                st2.dt_sost_end /*фактическая дата закрытия заказа*/, st.sost_doc, --Номер ПО 
                                sp.id AS id_obsl, st2.id_du, -- № объекта в солво для прихода: № УП для расхода № заказа в терминах солво
                                (SELECT MIN(st4.dt_sost_end)
                                    FROM kb_sost st4
                                    JOIN sv_hvoc hv
                                      ON hv.val_id = st4.id_sost
                                   WHERE hv.val_short = '3021'
                                         AND hv.voc_id = 'KB_USL'
                                         AND tir.id = st4.id_tir) dt_veh, --Фактическое время прибытия машины 
                                z.id_wms id_suppl, --IDSupplier
                                z.id_klient, --VN
                                z.n_zak, -- name
                                z.ur_adr, tir.n_avto, tir.vodit, z2.id_usr
                  FROM kb_spros sp, kb_sost st, kb_sost st2, kb_sost st3, kb_zak z2, kb_zak z -- supplier
                      , kb_tir tir
                 WHERE sp.id = st.id_obsl
                       AND st.id_sost = 'KB_USL60173' --4101 
                       AND sp.id = st2.id_obsl
                       AND st2.id_sost = 'KB_USL60174' --4102
                       AND sp.id = st3.id_obsl
                       AND st3.id_sost = 'KB_USL99770'
                       AND st3.sost_prm LIKE 'ПО_ХЛМ%' --4301---признак пришёл на FTP
                       AND NOT EXISTS (SELECT 1--4302 ещё не отправлено уведомление
                          FROM kb_sost
                         WHERE id_obsl = sp.id
                               AND id_sost = 'KB_USL99771'
                               and sost_prm like 'IN_%') --sol 21122020
                       AND sp.id_zak IN (SELECT id
                                           FROM kb_zak z
                                          WHERE z.id_klient IN (300228, 300223, 300217, 300237, 300255)
                                                AND z.id_usr IS NOT NULL /*'KB_USR92734'*/
                                         )
                       AND sp.id_pok = z.id --поставщик заказа IDSupplier
                       AND sp.id_tir = tir.id --водиьеля и номер машины
                       AND sp.id_zak = z2.id
                
                )
    LOOP
      i := 0;
      clob_prepare;
      clob_append_row('<?xml version="1.0" encoding="windows-1251"?>');
      clob_append_row('<IssueOrderForGoods>');
      clob_append_row('<Date>' || to_date(rec.dt_sost, 'dd.mm.yyyy') || '</Date>');
      clob_append_row('<VehicleFactlArrivalTime>' || to_date(rec.dt_sost_end, 'dd.mm.yyyy  hh24:mi:ss') ||
                      '</VehicleFactlArrivalTime>'); --Фактическое время разгрузки
      clob_append_row('<FactDeliveryDate>' || to_date(rec.dt_veh, 'dd.mm.yyyy  hh24:mi:ss') || '</FactDeliveryDate>'); --Фактическое время прибытия машины
      clob_append_row('<Number>' || rec.sost_doc || '</Number>'); --Номер ПО
      -- Константы
      clob_append_row('<Customer>Хеллманн</Customer>');
      clob_append_row('<OrderType>Поставка</OrderType>');
      clob_append_row('<TypeOfDelivery>Поставка</TypeOfDelivery>');
      -- IDSupplier - от кого пришёл товар (с какого завода хеламну) сотри 4101 куда схранялтсь данные теги
      clob_append_row('<IDSupplier>' || rec.id_suppl || '</IDSupplier>'); -- Код поставщика
      clob_append_row('<NameSupplier>' || rec.n_zak || '</NameSupplier>');
      clob_append_row('<AdressSupplier>' || rec.ur_adr || '</AdressSupplier>');
      clob_append_row('<VN>' || rec.id_klient || '</VN>');
      -- Ford
      clob_append_row('<NumberCar>' || rec.n_avto || '</NumberCar>');
      clob_append_row('<Driver>' || rec.vodit || '</Driver>');
    
      /*
      wms.rcn_detail@wms3   состав "приходного ордера Солво"
      wms.loads@wms3  грузы
      wms.sku@wms3  справочник номенкл
      kb_ttn
      */
      FOR rec_dt IN (/*SELECT s.sku_id, s.name, l.expiration_date, l.production_date, l.lot, l.marker, l.marker2, SUM(l.units) qty, l.comments
                       FROM wms.rcn_detail@wms 2 r, wms.loads@wms 2 l, wms.sku@wms 2 s
                      WHERE r.inc_id = rec.id_du --УП
                            AND r.rcn_id = l.rcn_id --грузы данного ПО
                            AND r.sku_id = l.sku_id
                            AND l.sku_id = s.id -- привязка к справ номенклатуры
                            AND rec.id_usr = 'KB_USR92734'
                      GROUP BY s.sku_id, s.name, l.expiration_date, l.production_date, l.lot, l.marker, l.marker2, l.comments
                     UNION ALL*/
                     SELECT s.sku_id, s.name, l.expiration_date, l.production_date, l.lot, l.marker, l.marker2, SUM(l.units) qty, l.comments
                       FROM wms.rcn_detail@wms3 r, wms.loads@wms3 l, wms.sku@wms3 s
                      WHERE r.inc_id = rec.id_du --УП
                            AND r.rcn_id = l.rcn_id --грузы данного ПО
                            AND r.sku_id = l.sku_id
                            AND l.sku_id = s.id -- привязка к справ номенклатуры
                            AND rec.id_usr = 'KB_USR99992'
                      GROUP BY s.sku_id, s.name, l.expiration_date, l.production_date, l.lot, l.marker, l.marker2, l.comments)
      LOOP
      
        i := i + 1;
        clob_append_row('<Goods>');
        clob_append_row('<LineNumber>' || TO_CHAR(i) || '</LineNumber>');
        clob_append_row('<Article>' || substr(rec_dt.sku_id, 4) || '</Article>');
        clob_append_row('<Name>' || rec_dt.name || '</Name>');
        clob_append_row('<ExpirationDate>' || TO_CHAR(rec_dt.expiration_date, 'DDMMYYYY') || '</ExpirationDate>');
        clob_append_row('<ProductionDate>' || TO_CHAR(rec_dt.production_date, 'DDMMYYYY') || '</ProductionDate>');
        clob_append_row('<Lot>' || rec_dt.lot || '</Lot>');
        clob_append_row('<Marker>' || rec_dt.marker || '</Marker>');
        clob_append_row('<Marker2>' || rec_dt.marker2 || '</Marker2>');
        clob_append_row('<Count>' || rec_dt.qty || '</Count>');
        clob_append_row('<Comment>' || rec_dt.comments || '</Comment>');
        clob_append_row('</Goods>');
      END LOOP;
      clob_append_row('</IssueOrderForGoods>');
    
      send_xml('IN', rec.id_obsl);
      --clob_close; ---???
      COMMIT;
    END LOOP;
  END hlm_4102;

  PROCEDURE HLM_4104 IS
    i NUMBER := 0;
  
  BEGIN
    FOR rec IN (SELECT DISTINCT st.dt_sost, -- Дата заявки
                                st2.dt_sost_end /*фактическая дата закрытия заказа*/, st.sost_doc, --Номер ПО 
                                sp.id AS id_obsl, st2.id_du, -- № объекта в солво для прихода: № УП для расхода № заказа в терминах солво
                                (SELECT MIN(st4.dt_sost_end)
                                    FROM kb_sost st4
                                    JOIN sv_hvoc hv
                                      ON hv.val_id = st4.id_sost
                                   WHERE hv.val_short = '3021'
                                         AND hv.voc_id = 'KB_USL'
                                         AND tir.id = st4.id_tir) dt_veh, --Фактическое время прибытия машины 
                                z.id_wms id_suppl, --IDSupplier
                                z.id_klient, --VN
                                z.n_zak, -- name
                                z.ur_adr, tir.n_avto, tir.vodit, z2.id_usr
                  FROM kb_spros sp, kb_sost st, kb_sost st2, kb_sost st3, kb_zak z2, kb_zak z -- supplier
                      , kb_tir tir
                 WHERE sp.id = st.id_obsl
                       AND st.id_sost = 'KB_USL60175' --4103 
                       AND sp.id = st2.id_obsl
                       AND st2.id_sost = 'KB_USL60177' --4104 отгружен
                       AND sp.id = st3.id_obsl
                       AND st3.id_sost = 'KB_USL99770'
                       AND st3.sost_prm LIKE 'РО_ХЛМ%' --4301---признак пришёл на FTP
                       AND NOT EXISTS (SELECT 1--4302 ещё не отправлено уведомление
                          FROM kb_sost
                         WHERE id_obsl = sp.id
                               AND id_sost = 'KB_USL99771'
                               and sost_prm like 'OUT_%') --sol 21122020
                       AND sp.id_zak IN (SELECT id
                                           FROM kb_zak z
                                          WHERE z.id_klient IN (300228, 300223, 300217, 300237, 300255)
                                                AND z.id_usr IS NOT NULL)
                       AND sp.id_pok = z.id --поставщик заказа IDSupplier
                       AND sp.id_tir = tir.id --водиьеля и номер машины
                       AND sp.id_zak = z2.id
                )
    LOOP
      i := 0;
      clob_prepare;
      v_txt := '<?xml version="1.0" encoding="windows-1251"?>';
      clob_append_row(v_txt);
      v_txt := '<IssueOrderForGoods>';
      clob_append_row(v_txt);
      v_txt := '<Date>' || to_date(rec.dt_sost, 'dd.mm.yyyy') || '</Date>';
      clob_append_row(v_txt);
      v_txt := '<VehicleFactlArrivalTime>' || to_date(rec.dt_sost_end, 'dd.mm.yyyy  hh24:mi:ss') ||
               '</VehicleFactlArrivalTime>';
      clob_append_row(v_txt); --Фактическое время разгрузки
      v_txt := '<FactDeliveryDate>' || to_date(rec.dt_veh, 'dd.mm.yyyy  hh24:mi:ss') || '</FactDeliveryDate>';
      clob_append_row(v_txt); --Фактическое время прибытия машины
      v_txt := '<Number>' || rec.sost_doc || '</Number>';
      clob_append_row(v_txt); --Номер ПО
      -- Константы
      v_txt := '<Customer>Хеллманн</Customer>';
      clob_append_row(v_txt);
      v_txt := '<OrderType>Отгрузка</OrderType>';
      clob_append_row(v_txt);
      v_txt := '<TypeOfDelivery>Отгрузка</TypeOfDelivery>';
      clob_append_row(v_txt);
      -- IDSupplier - от кого пришёл товар (с какого завода хеламну) сотри 4101 куда схранялтсь данные теги
      v_txt := '<IDSupplier>' || rec.id_suppl || '</IDSupplier>';
      clob_append_row(v_txt); -- Код поставщика
      v_txt := '<NameSupplier>' || rec.n_zak || '</NameSupplier>';
      clob_append_row(v_txt);
      v_txt := '<AdressSupplier>' || rec.ur_adr || '</AdressSupplier>';
      clob_append_row(v_txt);
      v_txt := '<VN>' || rec.id_klient || '</VN>';
      clob_append_row(v_txt);
      -- Ford
      v_txt := '<NumberCar>' || rec.n_avto || '</NumberCar>';
      clob_append_row(v_txt);
      v_txt := '<Driver>' || rec.vodit || '</Driver>';
      clob_append_row(v_txt);
    
      /*
      wms.order_details@wms3   состав заказов Солво(отгрузка)
      wms.loads@wms3  грузы
      wms.sku@wms3  справочник номенкл
      kb_ttn
      */
      FOR rec_dt IN (SELECT s.sku_id, s.name, l.expiration_date, l.production_date, l.lot, l.marker, l.marker2, SUM(l.units) qty, l.comments
                       FROM wms.order_details@wms3 o, wms.loads@wms3 l, wms.sku@wms3 s
                     
                      WHERE o.order_id = rec.id_du --заказ
                            AND o.order_id = l.order_id --грузы данного заказа
                            AND o.sku_id = l.sku_id
                            AND l.sku_id = s.id -- привязка к справ номенклатуры
                      GROUP BY s.sku_id, s.name, l.expiration_date, l.production_date, l.lot, l.marker, l.marker2, l.comments)
      LOOP
      
        i := i + 1;
      
        v_txt := '<Goods>';
        clob_append_row(v_txt);
        v_txt := '<LineNumber>' || TO_CHAR(i) || '</LineNumber>';
        clob_append_row(v_txt);
        v_txt := '<Article>' || substr(rec_dt.sku_id, 4) || '</Article>';
        clob_append_row(v_txt);
        v_txt := '<Name>' || rec_dt.name || '</Name>';
        clob_append_row(v_txt);
        v_txt := '<ExpirationDate>' || TO_CHAR(rec_dt.expiration_date, 'DDMMYYYY') || '</ExpirationDate>';
        clob_append_row(v_txt);
        v_txt := '<ProductionDate>' || TO_CHAR(rec_dt.production_date, 'DDMMYYYY') || '</ProductionDate>';
        clob_append_row(v_txt);
        v_txt := '<Lot>' || rec_dt.lot || '</Lot>';
        clob_append_row(v_txt);
      
        v_txt := '<Marker>' || rec_dt.marker || '</Marker>';
        clob_append_row(v_txt);
        v_txt := '<Marker2>' || rec_dt.marker2 || '</Marker2>';
        clob_append_row(v_txt);
        v_txt := '<Count>' || rec_dt.qty || '</Count>';
        clob_append_row(v_txt);
        v_txt := '<Comment>' || rec_dt.comments || '</Comment>';
        clob_append_row(v_txt);
        v_txt := '</Goods>';
        clob_append_row(v_txt);
      END LOOP;
      v_txt := '</IssueOrderForGoods>';
      clob_append_row(v_txt);
    
      send_xml('OUT', rec.id_obsl);
      --clob_close; ---???
      COMMIT;
    END LOOP;
  
  END hlm_4104;

  PROCEDURE HLM_MSG(p_err OUT VARCHAR2) IS
    v_err VARCHAR2(380); -- in out; возврат ошибки?
  
  BEGIN
  
    FOR rec IN (SELECT t.message_id, substr(t.message_name, instr(t.message_name, 'BOX') + 7, 1) AS inout
                  FROM KB_HLM_IN t
                 WHERE t.message_status = 'N'
                 
                                                         AND t.message_delivery_date < SYSDATE - 5 / 60 / 24
                --and t.message_id=122
                 ORDER BY t.message_id) -- remove me!!!
    LOOP
      -- как err обрабатывать?
      IF rec.inout = 'I' THEN
        --IN
        KB_HLM_IO.HLM_4101(rec.message_id, v_err);
      ELSIF rec.inout = 'O' THEN
        --OUT
        KB_HLM_IO.HLM_4103(rec.message_id, v_err);
      ELSIF rec.inout = 'S' THEN
        --SKU
        KB_HLM_IO.HLM_SKU(rec.message_id, v_err);
      ELSIF rec.inout = 'P' THEN
        --PS
        KB_HLM_IO.PART_STOCK(rec.message_id, v_err);
      ELSE
        dbms_output.put_line('unknown prefix');
        --raise exception unknown flag 
      END IF;
    
      IF (v_err IS NOT NULL) THEN
        p_err := v_err;
        dbms_output.put_line(p_err);
      END IF;
    
    END LOOP;
    
    HLM_4102;--sol 21122020
    HLM_4104;--sol 21122020
    
  EXCEPTION
    WHEN OTHERS THEN
      kb_mail3.send('oleg.soskin@gksvt.ru, gennadiy.mann@gksvt.ru', --адреса получателя через запятую
                    'Ошибка выполнения monitor HELLMANN', --тема письма
                    SQLERRM, --содержимое письма
                    ''); --вложение, в основном просто пустое значение
  END HLM_MSG;

  PROCEDURE clob_prepare
  -- =============================================================================================================================================================
    -- Подготавливает g_clob к получению новых данных
    -- =============================================================================================================================================================
   IS
  BEGIN
    BEGIN
      dbms_lob.freetemporary(g_clob);
      dbms_lob.close(g_clob);
    EXCEPTION
      WHEN OTHERS THEN
        NULL;
    END;
    dbms_lob.createtemporary(g_clob, TRUE);
    dbms_lob.open(g_clob, dbms_lob.lob_readwrite);
  
  END;

  PROCEDURE clob_append_row(p_text IN VARCHAR2)
  -- =============================================================================================================================================================
    -- Дописывает данные из p_text в g_clob
    -- =============================================================================================================================================================
   IS
  BEGIN
    dbms_lob.writeappend(g_clob, length(p_text || chr(10)), p_text || chr(10));
    /*    dbms_output.put_line(length(p_text));
        dbms_output.put_line(g_clob);
    */
  END;

  PROCEDURE clob_close
  -- =============================================================================================================================================================
    -- Закрывает и очищает g_clob
    -- =============================================================================================================================================================
   IS
  BEGIN
    dbms_lob.close(g_clob);
    dbms_lob.freetemporary(g_clob);
  END;

  PROCEDURE send_xml(p_direction IN VARCHAR2, p_id_obsl IN VARCHAR2 := NULL) IS
    v_id        NUMBER;
    v_file_name VARCHAR2(255);
    v_tmp       VARCHAR2(32000);
  BEGIN
  
    SELECT SV_UTILITIES.FORM_KEY(SEQ_KB_HLM_OUT.NextVal) INTO v_id FROM dual;
    SELECT p_direction || '_' || LPAD(TO_CHAR(v_id), 11, '0') || '_' || to_char(SYSDATE, 'DDMMYYYY') || '.xml'
      INTO v_file_name
      FROM DUAL;
  
    INSERT INTO kb_hlm_out
      (ID, FILE_NAME, STATUS, XML_DATA, CREATE_DATE) --org, даты добавить?
    VALUES
      (v_id, v_file_name, 'N', g_clob, SYSDATE); --p_org,
  
    dbms_lob.close(g_clob);
    dbms_lob.freetemporary(g_clob);
  
    --добавляем 4302 подтверждение что по данному заказу мы отправили уведомление  
    IF p_id_obsl IS NOT NULL THEN
      INSERT INTO kb_sost
        (id_obsl, id_sost, dt_sost, dt_sost_end, sost_doc, sost_prm)
      VALUES
        (p_id_obsl, 'KB_USL99771', SYSDATE, SYSDATE, v_id, v_file_name);
    END IF;
    --------------------------------------------------------------------------------------------------------------------->
  
  EXCEPTION
    WHEN OTHERS THEN
      v_tmp := SQLERRM;
      dbms_output.put_line(substr(v_tmp, 250));
      NULL;
  END;

  --обработка пакета ID_SALES_EAN

  PROCEDURE HLM_SKU(p_id_file IN NUMBER, p_err OUT VARCHAR2) IS
  
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
           extractvalue(VALUE(t), '/AddingGoods/ARTICLE')article, --
           extractvalue(VALUE(t), '/AddingGoods/UPC') upc, --
           extractvalue(VALUE(t), '/AddingGoods/NAME') art_name, --
           extractvalue(VALUE(t), '/AddingGoods/MEASURE') measure, --
           extractvalue(VALUE(t), '/AddingGoods/PRODUCT_LIFE') control_date, --
           extractvalue(VALUE(t), '/AddingGoods/STORAGE_POS') storage_pos, --
           extractvalue(VALUE(t), '/AddingGoods/BILLING_CLASS') billing_class --
      INTO vn, article, upc, art_name, measure, control_date, storage_pos, billing_class
      FROM TABLE(xmlsequence(extract((SELECT xmltype(f.message_data) FROM kb_hlm_in f WHERE f.message_id = p_id_file),
                                      '/AddingGoods'))) t;
  
    IF vn IS NULL THEN
      p_err := 'отсутствует ВН';
      RAISE vn_not_found;
    END IF;
    --отмечаем, что взяли файл в работу
    UPDATE KB_HLM_IN t SET t.message_status = 'R' WHERE t.message_id = p_id_file;
    COMMIT;
  
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
       WHERE sp.n_gruz = 'HELLMAN_SKU'
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
           'HELLMAN_SKU',
           'Суточный заказ Хеллманн по пакетам SKU');
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
  
    --IF v_id_usr = 'KB_USR99992' THEN
      kb_pack.wms3_updt_sku(l_id_zak, v_prf_wms, p_err);
    /*ELSIF v_id_usr = 'KB_USR92734' THEN
      kb_pack.wms2_updt_sku(l_id_zak, v_prf_wms, p_err);
    ELSE
      v_error := 'Не правильно указан шлюз!';
    END IF;*/
  
    --добавляем событие 4301 в заказ
    IF p_err IS NOT NULL AND p_err NOT LIKE 'Загружено записей:%' THEN
      INSERT INTO kb_sost
        (id_obsl, dt_sost, dt_sost_end, id_sost, sost_doc, sost_prm, id_isp)
      VALUES
        (v_id_obsl,
         SYSDATE,
         SYSDATE,
         'KB_USL99770',
         p_id_file,
         'Артикул ' || article || ' не загруже по причине:' || p_err,
         '010277043');
    
      UPDATE KB_HLM_IN i
         SET i.message_status          = 'E',
             i.message_err_code        = p_err,
             i.message_processing_date = SYSDATE
       WHERE i.message_id = p_id_file;
      send_notification(p_id_file, p_err, vn);
    ELSE
      INSERT INTO kb_sost
        (id_obsl, dt_sost, dt_sost_end, id_sost, sost_doc, sost_prm, id_isp)
      VALUES
        (v_id_obsl,
         SYSDATE,
         SYSDATE,
         'KB_USL99770',
         p_id_file,
         'Артикул ' || article || ' отправлен в СОХ',
         '010277043');
    
      -- change status to S
      UPDATE KB_HLM_IN t SET t.message_status = 'S' WHERE t.message_id = p_id_file;
      -- test
      IF LENGTH(c_test) != 0 THEN
        dbms_output.put_line('Order # ' || v_id_obsl);
      END IF;
    END IF;
    COMMIT;
    --END LOOP rec;
  EXCEPTION
    WHEN vn_not_found THEN
      ROLLBACK;
      UPDATE KB_HLM_IN t
         SET t.message_status   = 'E',
             t.message_err_code = p_err
       WHERE t.message_id = p_id_file;
      COMMIT;
      send_notification(p_id_file, p_err);
      dbms_output.put_line(p_err);
    
    WHEN OTHERS THEN
    
      UPDATE KB_HLM_IN t
         SET t.message_status   = 'E',
             t.message_err_code = 'Некорректное вложение'
       WHERE t.message_id = p_id_file;
      COMMIT;
      p_err := SQLERRM;
      --добавил SOL 25112020
      send_notification(p_id_file, p_err);
  END HLM_SKU;

  /**********************************
  * Остаки
  **********************************/
  PROCEDURE PART_STOCK(p_id_file IN NUMBER, p_err OUT VARCHAR2) IS
  
    v_id_wms     VARCHAR2(38);
    l_id_zak     VARCHAR2(38);
    v_id_obsl    VARCHAR2(38);
    vn_not_found EXCEPTION;
    v_id_usr     VARCHAR2(38);
  
  BEGIN
    --отмечаем, что взяли файл в работу
    UPDATE KB_HLM_IN t SET t.message_status = 'R' WHERE t.message_id = p_id_file;
    COMMIT;
  
    /*    От вас ждем файл с префиксом «PS»
    <?xml version="1.0" encoding="utf-8"?>
    <PART_STOCK>
          <TIME_STAMP>2020-11-21 00:35:34</TIME_STAMP>
          <VN>300223</VN>
    </PART_STOCK> 
    */
    -- получить kb_zak.id_wms по ВН входящему
    SELECT extractvalue(VALUE(t), '/PART_STOCK/VN')
      INTO v_vn
      FROM TABLE(xmlsequence(extract((SELECT xmltype(f.message_data) FROM kb_hlm_in f WHERE f.message_id = p_id_file),
                                      '/PART_STOCK'))) t;
    IF v_vn IS NULL THEN
      p_err := 'отсутствует ВН';
      RAISE vn_not_found;
    END IF;
    BEGIN
      SELECT z.id_wms, z.id, z.id_usr
        INTO v_id_wms, l_id_zak, v_id_usr
        FROM kb_zak z
       WHERE z.id_klient = v_vn
             AND z.id_usr IN ('KB_USR92734', 'KB_USR99992');
    EXCEPTION
      WHEN OTHERS THEN
        p_err := 'Неправильный ВН';
        RAISE vn_not_found;
    END;
  
    /*<?xml version="1.0" encoding="UTF-8"?>
    <PART_STOCK>
        <TIME_STAMP>2020-11-21 00:35:34</TIME_STAMP>
        <VN>300223</VN>
        <PART_STOCK_LINE_LIST>
            <PART_STOCK_LINE_TEMPL>
                <LINE_NO>1</LINE_NO>
                <ARTICLE>COGENT1</ARTICLE>
                <UPC>2000001720875</UPC>
                <NAME>Матрас</NAME>
                <QTY>12</QTY>
            </PART_STOCK_LINE_TEMPL>
    */
    clob_prepare;
    ---шапка
    clob_append_row('<?xml version="1.0" encoding="windows-1251"?><PART_STOCK><TIME_STAMP>' || --' || chr(10) || '
                    to_date(SYSDATE, 'dd.mm.yyyy  hh24:mi:ss') || '</TIME_STAMP><VN>' || v_vn ||
                    '</VN><PART_STOCK_LINE_LIST>'); -- || chr(10)
    ---содержимое
    /*    <?xml version="1.0" encoding="UTF-8"?>
        <PART_STOCK>
            <TIME_STAMP>2020-11-21 00:35:34</TIME_STAMP>
            <VN>300223</VN>
            <PART_STOCK_LINE_LIST>
                <PART_STOCK_LINE_TEMPL>
                    <LINE_NO>1</LINE_NO>
                    <ARTICLE>COGENT1</ARTICLE>
                    <UPC>2000001720875</UPC>
                    <NAME>Матрас</NAME>
                    <QTY>12</QTY>
                </PART_STOCK_LINE_TEMPL>
                <PART_STOCK_LINE_TEMPL>
                    <LINE_NO>2</LINE_NO>
                    <ARTICLE>S100</ARTICLE>
                    <UPC>2000001695586</UPC>
                    <NAME>Реагент S100</NAME>
                    <QTY>1</QTY>
                </PART_STOCK_LINE_TEMPL>
            </PART_STOCK_LINE_LIST>
        </PART_STOCK>
    */
    FOR rec IN (SELECT '
                          <LINE_NO>' || rownum || '</LINE_NO>
                          <ARTICLE>' || i.article || '</ARTICLE>
                          <UPC>' || i.upc || '</UPC>
                          <NAME>' || i.name || '</NAME>
                          <QTY>' || i.qty || '</QTY>' txt
                  FROM (/*SELECT substr(s.sku_id, 4) article, s.upc, s.name NAME, SUM(l.units) qty
                           FROM wms.loads@wms 2.kvt.local l, wms.sku@wms 2.kvt.local s
                          WHERE l.holder_id = v_id_wms --10369
                                AND l.sku_id = s.id
                                AND l.status NOT IN ('L', 'J', '+')
                                AND v_id_usr = 'KB_USR92734'
                          GROUP BY s.id, s.name, substr(s.sku_id, 4), s.upc
                         UNION ALL*/
                         SELECT substr(s.sku_id, 4) article, s.upc, s.name NAME, SUM(l.units) qty
                           FROM wms.loads@wms3.kvt.local l, wms.sku@wms3.kvt.local s
                          WHERE l.holder_id = v_id_wms --10369
                                AND l.sku_id = s.id
                                AND l.status NOT IN ('L', 'J', '+')
                                AND v_id_usr = 'KB_USR99992'
                          GROUP BY s.id, s.name, substr(s.sku_id, 4), s.upc) i)
    LOOP
      clob_append_row('<PART_STOCK_LINE_TEMPL>' || rec.txt || '</PART_STOCK_LINE_TEMPL>');
    END LOOP rec;
    clob_append_row('</PART_STOCK_LINE_LIST></PART_STOCK>');
  
    --поиск/создание суточного заказа
    BEGIN
      SELECT sp.id
        INTO v_id_obsl
        FROM kb_spros sp --, kb_zak z
       WHERE sp.n_gruz = 'HELLMAN_STOCK'
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
           'HELLMAN_STOCK',
           'Суточный заказ Хеллманн по пакетам PS');
      WHEN OTHERS THEN
        NULL;
    END;
  
    --добавить send_xml
    send_xml('PS', v_id_obsl);
    --clob_close; ---???
  
    --добавляем событие 4301 в заказ
    INSERT INTO kb_sost
      (id_obsl, dt_sost, dt_sost_end, id_sost, sost_doc, sost_prm, id_isp)
    VALUES
      (v_id_obsl,
       SYSDATE,
       SYSDATE,
       'KB_USL99770',
       p_id_file,
       'Получен запрос PART_STOCK ' || p_id_file,
       '010277043');
  
    -- change status to S
    UPDATE KB_HLM_IN t SET t.message_status = 'S' WHERE t.message_id = p_id_file;
    COMMIT;
    -- test
    IF LENGTH(c_test) != 0 THEN
      dbms_output.put_line('Order # ' || v_id_obsl);
    END IF;
  
  EXCEPTION
    WHEN vn_not_found THEN
      ROLLBACK;
      UPDATE KB_HLM_IN t
         SET t.message_status   = 'E',
             t.message_err_code = p_err
       WHERE t.message_id = p_id_file;
      COMMIT;
      send_notification(p_id_file, p_err);
    
    WHEN OTHERS THEN
      ROLLBACK;
      dbms_output.put_line(SQLERRM);
    
      UPDATE KB_HLM_IN t
         SET t.message_status   = 'E',
             t.message_err_code = 'Некорректное вложение'
       WHERE t.message_id = p_id_file;
      COMMIT;
      p_err := SQLERRM;
      send_notification(p_id_file, p_err);
  END PART_STOCK;

END KB_HLM_IO;