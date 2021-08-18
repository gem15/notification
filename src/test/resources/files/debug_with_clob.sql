DECLARE
  P_MSG CLOB :=to_clob('<?xml version="1.0" encoding="UTF-8"?>
  <ReceiptOrderForGoods>
  <NumberDoc>MK00-010610</NumberDoc>
  <DateDoc>11.08.2021</DateDoc>
  <OrderType>Поставка</OrderType>
  <TypeOfDelivery>Поставка</TypeOfDelivery>
  <PlannedDeliveryDate>12.08.2021</PlannedDeliveryDate>
  <IDSupplier>019597</IDSupplier>
  <NameSupplier>ЦТО+ ООО</NameSupplier>
  <AdressSupplier>117209, Москва г, Керченская ул., дом № 6, корпус 3, квартира 56</AdressSupplier>
  <VN>300185</VN>
  <NumberCar></NumberCar>
  <Driver></Driver>
  <GUID>cf843545-9eb5-11eb-80c0-00155d0c6c19</GUID>
  <Goods>
    <LineNumber>0</LineNumber>
    <Article>00-07064083</Article>
    <Name>Винт М6*30 Россия пресшайба</Name>
    <Category>0</Category>
    <StorageLife/>
    <Marker/>
    <Marker2/>
    <Marker3/>
    <Lot/>
    <Count>50000</Count>
    <Comment/>
  </Goods>
</ReceiptOrderForGoods>
');
  P_ERR VARCHAR2(200);
BEGIN
  KB_MONITOR.MSG_4101(
    P_MSG => P_MSG,
    P_ERR => P_ERR
  );
  /* Legacy output: 
DBMS_OUTPUT.PUT_LINE('P_ERR = ' || P_ERR);
*/ 
  :P_ERR := P_ERR;
rollback; 
END;