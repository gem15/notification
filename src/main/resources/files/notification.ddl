CREATE SEQUENCE seq_ftp;
CREATE SEQUENCE seq_Response_Extra;
CREATE SEQUENCE seq_Response_Ftp;
CREATE TABLE ftps (Id number(10) NOT NULL, Login varchar2(255) NOT NULL, Password varchar2(255) NOT NULL, Hostname varchar2(255) NOT NULL, Port number(10) NOT NULL, PRIMARY KEY (Id));
CREATE TABLE Response_Extra (Id number(10) NOT NULL, Query_Text varchar2(3000) NOT NULL, Alias_Text varchar2(255) NOT NULL, Voc varchar2(255) NOT NULL, Direction varchar2(255) NOT NULL, Order_Type varchar2(255), PRIMARY KEY (Id));
CREATE TABLE Response_Ftp (Id number(10) NOT NULL, Response_Extra_id number(10) NOT NULL, ftp_id number(10) NOT NULL, path varchar2(1000) DEFAULT '/' NOT NULL, Vn number(10) NOT NULL, PRIMARY KEY (Id));
ALTER TABLE Response_Ftp ADD CONSTRAINT FKResponse_F709292 FOREIGN KEY (ftp_id) REFERENCES ftps (Id);
ALTER TABLE Response_Ftp ADD CONSTRAINT FKResponse_F70255 FOREIGN KEY (Response_Extra_id) REFERENCES Response_Extra (Id);

CREATE OR REPLACE TRIGGER ftp_bir 
BEFORE INSERT ON ftps 
FOR EACH ROW

BEGIN
  SELECT seq_ftp.NEXTVAL
  INTO   :new.id
  FROM   dual;
END;
/

CREATE OR REPLACE TRIGGER Response_Extra_bir 
BEFORE INSERT ON Response_Extra 
FOR EACH ROW

BEGIN
  SELECT seq_Response_Extra.NEXTVAL
  INTO   :new.id
  FROM   dual;
END;
/

CREATE OR REPLACE TRIGGER Response_Ftp_bir 
BEFORE INSERT ON Response_Ftp 
FOR EACH ROW

BEGIN
  SELECT seq_Response_Ftp.NEXTVAL
  INTO   :new.id
  FROM   dual;
END;
/
