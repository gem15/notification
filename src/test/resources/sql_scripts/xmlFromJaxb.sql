SELECT
	1
FROM
	kb_sost st
	INNER JOIN kb_spros sp ON st.id_obsl = sp.id
WHERE
	sp.id_zak = '0102304213'
	AND st.id_sost = 'KB_USL99770'
	AND st.id_du = '965e4682-9ec3-11eb-80c0-00155d0c6c19';
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
	<msgID>89f81f05-9d1e-4319-9b9d-b6f4e34c7e77</msgID>
	<msgType>0</msgType>
	<order>
		<orderType>false</orderType>
		<orderNo>ОП-00000297</orderNo>
		<orderDate>2021-05-27T14:46:38</orderDate>
		<plannedDate>2021-10-01T23:59:59</plannedDate>
		<contrCode>БФ-005335</contrCode>
		<contrName>КРЦ ЭФКО - Каскад ООО</contrName>
		<orderLine>
			<lineNumber>1</lineNumber>
			<article>197015</article>
			<name>ЭФКО ФУД Professional Масло подсолнечное раф. дезод.  ПЭТ 92</name>
			<qty>21600</qty>
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
'
<Shell>
	<customerID>300185</customerID>
	<msgID>89f81f05-9d1e-4319-9b9d-b6f4e34c7e77</msgID>
	<msgType>0</msgType>
	<order>
		<orderType>false</orderType>
		<orderNo>ОП-00000297</orderNo>
		<orderDate>2021-05-27T14:46:38</orderDate>
		<plannedDate>2021-10-01T23:59:59</plannedDate>
		<contrCode>БФ-005335</contrCode>
		<contrName>КРЦ ЭФКО - Каскад ООО</contrName>
		<orderLine>
			<lineNumber>1</lineNumber>
			<article>197015</article>
			<name>ЭФКО ФУД Professional Масло подсолнечное раф. дезод.  ПЭТ 92</name>
			<qty>21600</qty>
		</orderLine>
	</order>
</Shell>
'
),'//Shell/order/orderLine'))) t; --//ReceiptOrderForGoods/Goods