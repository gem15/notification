  PROCEDURE msg_4101_(p_err OUT VARCHAR2) IS
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
	v_one_char	VARCHAR2(1);
	 l_err_sku	VARCHAR2( 32000);
	 l_err_qty	VARCHAR2( 32000);
	 
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

		--=== обработка типа поставки/отгрузки
		BEGIN
		  SELECT s.val_id INTO v_id_tzs FROM sv_hvoc s 
		  WHERE upper(s.val_full) = upper(rec.TypeOfDelivery) AND s.VOC_ID='SCH_NP';
		EXCEPTION
		  WHEN no_data_found THEN v_id_tzs := NULL;
		END;
	
		IF v_id_tzs IS NULL THEN
			BEGIN 
				SELECT z.type_cox	INTO v_id_tzs
				FROM kb_spros sp, kb_zak_type_conv z
				WHERE sp.id = v_id_obsl
				AND sp.id_pok = z.id_zak
				AND UPPER(z.type_cox_zak) = UPPER(rec.TypeOfDelivery);
				
				UPDATE kb_sost SET id_tzs = v_id_tzs WHERE id = v_id_sost;
			EXCEPTION
				WHEN no_data_found THEN 
				p_err := 'Тип заказа '|| rec.TypeOfDelivery || ' не найден.';
				RAISE vn_not_found;
			END;
		END IF;
		SELECT substr(UPPER(val_short), 1, 1) INTO v_one_char FROM sv_hvoc WHERE val_id = v_id_tzs;
		IF v_one_char != 'I' THEN -- !!!
			p_err := 'Неверное направление у типа заказа';
			RAISE vn_not_found;
		END IF;
		
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
			IF rec_det.Count1 IS NULL OR rec_det.Count1 = 0 THEN
				p_err := p_err || 'У номенклатуры '||rec_det.article||' не задано количество' || CHR(10);
			END IF;
        --поиск номенклатру в справочнике
        SELECT COUNT(1) INTO cnt_sku FROM sku s WHERE s.sku_id = v_prfx || rec_det.article;
        IF nvl(cnt_sku, 0) = 0 THEN
          p_err := p_err || 'Не найдена номенклатура ' || rec_det.article || CHR(10);
        END IF;
        --заполняем таблицу грузов
        INSERT INTO kb_ttn
          (id_obsl, n_tovar, kol_tovar, brak, pak_tovar, ul_otpr, usl)
        VALUES
          (v_id_obsl, rec_det.article, rec_det.count1, rec_det.category, rec_det.mark, rec_det.mark2, rec_det.mark3);
      END LOOP;
		IF p_err IS NOT NULL THEN
			RAISE vn_not_found;
		END IF;
    
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