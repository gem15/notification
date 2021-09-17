SELECT
	  extractvalue(VALUE(t), '/Shell/customerID/VN') AS VN, --ВН клиента
	  extractvalue(VALUE(t), '/Shell/order/orderNo') AS number1, --NumberDoc Номер рО
	  extractvalue(VALUE(t), '/Shell/order/orderDate') AS Date1, --DateDoc Дата РО
--	  extractvalue(VALUE(t), '/Shell/order/Customer') AS Customer, --Отправитель
	  extractvalue(VALUE(t), '/Shell/order/orderType') AS OrderType, --Тип заказа
--	  extractvalue(VALUE(t), '/Shell/order/TypeOfDelivery') AS TypeOfDelivery, --Тип поставки
	  extractvalue(VALUE(t), '/Shell/order/plannedDate') AS PlannedShipmentDate, --PlannedShipmentDate Планируемая дата поставки
	  extractvalue(VALUE(t), '/Shell/order/contrCode') AS IDConsignee, --IDConsignee код получателя
	  extractvalue(VALUE(t), '/Shell/order/contrName') AS NameConsignee, --NameConsignee имя пполучателя
	  extractvalue(VALUE(t), '/Shell/order/AdressConsignee') AdressConsignee, --адр получателя
--	  extractvalue(VALUE(t), '/Shell/order/IDCarrier') AS IDCarrier, --код перевозчика
--	  extractvalue(VALUE(t), '/Shell/order/TypeCar') AS TypeCar, --Тип машины
	  extractvalue(VALUE(t), '/Shell/order/licencePlate') AS NumberCar, --NumberCar Номер машины
	  extractvalue(VALUE(t), '/Shell/order/driver') Driver,
	  extractvalue(VALUE(t), '/Shell/order/guid') docID
--	  extractvalue(VALUE(t), '/Shell/order/Email') Email,
--	  extractvalue(VALUE(t), '/Shell/order/Comment') Comment1 --Комментарий
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
</Shell>'),'//Shell'))) t