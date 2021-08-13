--https://codebeautify.org/sqlformatter
--                (SELECT MIN(st4.dt_sost_end)
--                 FROM kb_sost st4
--                          JOIN sv_hvoc hv
--                               ON hv.val_id = st4.id_sost
--                 WHERE hv.val_short = '3021'
--                   AND hv.voc_id = 'KB_USL'
--                   AND tir.id = st4.id_tir) dt_veh,   --Фактическое время прибытия машины
--                tir.n_avto,
--                tir.vodit,
--  AND sp.id_tir = tir.id          --водиьеля и номер машины

SELECT DISTINCT st3.id_du,
                st.dt_sost,
                st2.dt_sost_end,
                st.sost_doc,
                sp.id AS id_obsl,
                st2.id_du,
                z.id_wms id_suppl,
                z.id_klient,
                z.n_zak,
                z.ur_adr,
                z2.id_usr
FROM kb_spros sp ,
     kb_sost st ,
     kb_sost st2 ,
     kb_sost st3 ,
     kb_zak z2 ,
     kb_zak z -- supplier
--   , kb_tir tir

WHERE sp.id = st.id_obsl
  AND st.id_sost = 'KB_USL60173'
  AND sp.id = st2.id_obsl
  AND st2.id_sost = 'KB_USL60174'
  AND sp.id = st3.id_obsl
  AND st3.id_sost = 'KB_USL99770'
  AND sp.id_zak IN
    (SELECT id
     FROM kb_zak z
     WHERE z.id_klient =300185
       AND z.id_usr IS NOT NULL )
  AND sp.id_pok = z.id
  AND sp.id_zak = z2.id;