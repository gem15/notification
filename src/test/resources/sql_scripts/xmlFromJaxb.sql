<?xml version="1.0" encoding="utf-8"?>
<confirmation>
  <customerID>300227</customerID>
  <msgType>0</msgType>
  <status>ERROR</status>
  <docNo>ML09-4201 </docNo>
  <info>При разборе файла 1505 произошла ошибка: Ошибка wms3_Check_OrderA: Дата заказа [09.09.2021 09:00] устарела!</info>
</confirmation>

    <xs:complexType name="Confirmation">
        <xs:sequence>
            <xs:element name="customerID" type="xs:int"/>
            <xs:element name="msgType" type="xs:int"/>
            <xs:element name="status">
                <xs:simpleType>
                    <xs:restriction base="xs:string">
                        <xs:pattern value="SUCCESS|ERROR"/>
                    </xs:restriction>
                </xs:simpleType>
            </xs:element>
            <xs:element name="docNo" type="xs:string" minOccurs="0"/>
            <xs:element name="info" type="xs:string" minOccurs="0"/>
        </xs:sequence>
    </xs:complexType>
SELECT 
	  extractvalue(VALUE(t), '/Shell/customerID') AS VN, --ВН клиента
	  extractvalue(VALUE(t), '/Shell//order/orderNo') AS number1, --Номер ПО
	  extractvalue(VALUE(t), '/Shell/order/orderDate') AS Date1, --Дата ПО
--                       extractvalue(VALUE(t), '/Shell/Customer') AS Customer, --Заказчик
	  extractvalue(VALUE(t), '/Shell/order/orderType') AS OrderType, --Тип заказа
-- поставка/отгрузка                      extractvalue(VALUE(t), '/Shell/order/TypeOfDelivery') AS TypeOfDelivery, --Тип поставки
	  extractvalue(VALUE(t), '/Shell/order/plannedDate') AS PlannedDeliveryDate,
	  extractvalue(VALUE(t), '/Shell/order/contrCode') AS IDSupplier, --код поставщика
	  extractvalue(VALUE(t), '/Shell/order/contrName') AS NameSupplier, --имя поставщика
	  extractvalue(VALUE(t), '/Shell/order/contrAddress') AS AdressSupplier, --адрес поставщика
--                       extractvalue(VALUE(t), '/Shell/order/IDCarrier') AS IDCarrier, --код перевозчика
--                       extractvalue(VALUE(t), '/Shell/order/TypeCar') AS TypeCar, --Тип машины
	  extractvalue(VALUE(t), '/Shell/order/licencePlate') AS NumberCar, --Номер машины
	  extractvalue(VALUE(t), '/Shell/order/driver') Driver,
	  extractvalue(VALUE(t), '/Shell/order/guid') docID
FROM TABLE(xmlsequence(extract(xmltype(
'<Shell>
	<customerID>300185</customerID>
	<order>
		<guid>48570797-9e99-11eb-80c5-00155d0c1603</guid>
		<orderType>false</orderType>
		<orderKind>Перемещение товаров</orderKind>
		<orderNo>MK00-068113</orderNo>
		<orderDate>2021-06-06T15:52:50</orderDate>
		<plannedDate>2021-04-23T00:00:00</plannedDate>
		<contrCode>000002</contrCode>
		<contrName>ЛСП ОХ - Склад отгрузки</contrName>
		<contrAddress>141150, МО, г. Лосино-Петровский, ул. Первомайская, д.1., стр. 27</contrAddress>
		<licencePlate>KK110</licencePlate>
		<driver>Фамилия</driver>
		<orderLine>
			<lineNumber>1</lineNumber>
			<article>00-01118298</article>
			<name>Офисное кресло Chairman    685    Россия     10-356 черный </name>
			<qty>4</qty>
			<category>91</category>
		</orderLine>
	</order>
</Shell>'
),'//Shell'))) t;

select to_date(REPLACE('2021-06-06T15:52:50','T',' '), 'yyyy-mm-dd hh24:mi:ss')from dual;

SELECT extractvalue(VALUE(t), '/orderLine/lineNumber') AS LineNumber, --номер строки
		  extractvalue(VALUE(t), '/orderLine/article') AS Article, --артикул товара
		  extractvalue(VALUE(t), '/orderLine/name') AS NAME, --имя товара
		  extractvalue(VALUE(t), '/orderLine/category') AS Category, --категория товара
		  extractvalue(VALUE(t), '/orderLine/mark2') AS Mark, --номер документа
		  extractvalue(VALUE(t), '/orderLine/mark2') AS Mark2, --номер документа
		  extractvalue(VALUE(t), '/orderLine/mark2') AS Mark3, --номер документа
		  extractvalue(VALUE(t), '/orderLine/qty') AS Count1, --кол-во
		  extractvalue(VALUE(t), '/orderLine/comment') AS Comment1 --Комментарий
FROM TABLE(xmlsequence(extract(xmltype(
'<Shell>
	<customerID>300185</customerID>
	<order>
		<guid>48570797-9e99-11eb-80c5-00155d0c1603</guid>
		<orderType>false</orderType>
		<orderKind>Перемещение товаров</orderKind>
		<orderNo>MK00-068113</orderNo>
		<orderDate>2021-06-06T15:52:50</orderDate>
		<plannedDate>2021-04-23T00:00:00</plannedDate>
		<contrCode>000002</contrCode>
		<contrName>ЛСП ОХ - Склад отгрузки</contrName>
		<contrAddress>141150, МО, г. Лосино-Петровский, ул. Первомайская, д.1., стр. 27</contrAddress>
		<licencePlate>KK110</licencePlate>
		<driver>Фамилия</driver>
		<orderLine>
			<lineNumber>1</lineNumber>
			<article>00-01118298</article>
			<name>Офисное кресло Chairman    685    Россия     10-356 черный </name>
			<qty>4</qty>
			<category>91</category>
		</orderLine>
		<orderLine>
			<lineNumber>2</lineNumber>
			<article>00-01118298</article>
			<name>Офисное кресло Chairman    685    Россия     10-356 черный </name>
			<qty>4</qty>
			<mark>m</mark>
			<mark2>m2</mark2>
			<mark3>m3</mark3>
			<lot>lot</lot>
			<category>91</category>
		</orderLine>
	</order>
</Shell>'
),'//Shell/order/orderLine'))) t; --//ReceiptOrderForGoods/Goods