CREATE SEQUENCE seq_ftp;
CREATE SEQUENCE seq_Response_Extra;
CREATE SEQUENCE seq_Response_Ftp;
CREATE TABLE ftps (Id number(10) NOT NULL, Login varchar2(255) NOT NULL, Password varchar2(255) NOT NULL, Hostname varchar2(255) NOT NULL, Port number(10) NOT NULL, PRIMARY KEY (Id));
CREATE TABLE Response_Extra (Id number(10) NOT NULL, Query_Text varchar2(3000) NOT NULL, Alias_Text varchar2(255) NOT NULL, Voc varchar2(255) NOT NULL, Direction varchar2(255) NOT NULL, Order_Type varchar2(255), PRIMARY KEY (Id));
CREATE TABLE Response_Ftp (Id number(10) NOT NULL, Response_Extra_id number(10) NOT NULL, ftp_id number(10) NOT NULL, path varchar2(1000) DEFAULT '/' NOT NULL, Vn number(10) NOT NULL, PRIMARY KEY (Id));
ALTER TABLE Response_Ftp ADD CONSTRAINT FKResponse_F709292 FOREIGN KEY (ftp_id) REFERENCES ftps (Id);
ALTER TABLE Response_Ftp ADD CONSTRAINT FKResponse_F70255 FOREIGN KEY (Response_Extra_id) REFERENCES Response_Extra (Id);

CREATE OR REPLACE TRIGGER "SPRUT4"."RESPONSE_FTP_BIR" 
BEFORE INSERT ON Response_Ftp 
FOR EACH ROW

BEGIN
	IF :new.id IS NULL THEN
		SELECT seq_Response_Ftp.NEXTVAL INTO :new.id FROM dual;
	END IF;
END;

/
ALTER TRIGGER "SPRUT4"."RESPONSE_FTP_BIR" ENABLE;

CREATE OR REPLACE TRIGGER "SPRUT4"."RESPONSE_EXTRA_BIR" 
BEFORE INSERT ON Response_EXTRA 
FOR EACH ROW

BEGIN
	IF :new.id IS NULL THEN
		SELECT seq_Response_EXTRA.NEXTVAL INTO :new.id FROM dual;
	END IF;
END;

/
ALTER TRIGGER "SPRUT4"."RESPONSE_EXTRA_BIR" ENABLE;

  CREATE OR REPLACE TRIGGER "SPRUT4"."FTP_BIR" 
BEFORE INSERT ON ftps 
FOR EACH ROW

BEGIN
  	IF :new.id IS NULL THEN
		SELECT seq_ftp.NEXTVAL INTO :new.id FROM dual;
	END IF;
END;

/
ALTER TRIGGER "SPRUT4"."FTP_BIR" ENABLE;


------------------------------------------------------------------
SELECT * FROM kb_sost s
inner join kb_spros p on s.id_obsl = p.id
inner join kb_zak z on p.id_zak =z.id
where z.id_klient=300191
and s.sost_prm like 'OUT_%'
--and s.sost_prm='OUT_559_1625510865246.xml';
;
SELECT * FROM kb_spros where id='01023954359';
