  --процедура парсинга и передачи в солво заявок на ПО ICD--
  PROCEDURE HLM_4101(p_msg IN CLOB, p_err OUT VARCHAR2) IS
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
          EXIT;
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
        (c_test || 'FTP УП -> ' || rec.number1,
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
        (id_obsl, dt_sost, dt_sost_end, id_sost,  sost_prm, id_isp)--sost_doc,
      VALUES
        (v_id_obsl, SYSDATE, SYSDATE, 'KB_USL99770', 'ПО', '010277043'); --p_id_file, 
    
      FOR rec_det IN (SELECT extractvalue(VALUE(t), '/Goods/LineNumber') AS LineNumber, --номер строки
                             extractvalue(VALUE(t), '/Goods/Article') AS Article, --артикул товара
                             extractvalue(VALUE(t), '/Goods/Name') AS NAME, --имя товара
                             extractvalue(VALUE(t), '/Goods/Category') AS Category, --категория товара
                             extractvalue(VALUE(t), '/Goods/Mark2') AS Mark, --номер документа
                             extractvalue(VALUE(t), '/Goods/Mark2') AS Mark2, --номер документа
                             extractvalue(VALUE(t), '/Goods/Mark2') AS Mark3, --номер документа
                             extractvalue(VALUE(t), '/Goods/Count') AS Count, --кол-во
                             extractvalue(VALUE(t), '/Goods/Comment') AS Comment --Комментарий
                        FROM TABLE(xmlsequence(extract(xmltype(p_msg),'//ReceiptOrderForGoods/Goods'))) t)
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
          (id_obsl, n_tovar, kol_tovar, usl, ul_otpr, brak)
        VALUES
          (v_id_obsl, rec_det.article, rec_det.count1, rec_det.comment1, rec_det.mark2, rec_det.category);
      END LOOP;
    
    END LOOP;
    --разбор заявки завершен, заказ в АРМ сформирован
    IF p_err IS NULL THEN
      --передача в СОЛВО
      DELETE FROM kb_t_mdet;
      dbms_output.put_line(v_id_obsl);
      INSERT INTO kb_t_mdet
        (id_sost, id_obsl, f01, f02, f05 /*категория*/, f18 /*маркер 2*/)
        (SELECT 'KB_USL60173', v_id_obsl, n_tovar, KOL_TOVAR, brak, ul_otpr FROM kb_ttn WHERE id_obsl = v_id_obsl);
    
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
    
  END HLM_4101;
  --Конец разборки с ПО--
