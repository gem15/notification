<!--<?xml version="1.0" encoding="UTF-8"?>-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" exclude-result-prefixes="xs"
                xmlns:st="http://www.severtrans.com" version="2.0">
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:function name="st:myDateTime">
        <xsl:param name="dt"/>
        <xsl:value-of select="concat(
            substring($dt, 7, 4),'-',
            substring($dt, 4, 2),'-',
            substring($dt, 1, 2),'T',
            substring($dt, 12, 2),':',
            substring($dt, 15, 2),':00'
            )"/>
    </xsl:function>
    <!--    <xsl:template match="Shell">
        <xsl:element name="msgType">ZZZZ</xsl:element>
        <xsl:copy><xsl:apply-templates select="@*|node()" /></xsl:copy>
<!-\-        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
            <!-\\-Do something special for Node766, like add a certain string-\\->
            <xsl:text> add some text </xsl:text>
        </xsl:copy>
-\->
    </xsl:template>
-->

    <xsl:template match="/">
        <Shell xmlns="http://www.severtrans.com">
            <xsl:choose>
                <xsl:when test="ReceiptOrderForGoods != ''">
                    <xsl:element name="msgType">delivery</xsl:element>
                    <xsl:element name="customerID">
                        <xsl:value-of select="current()/ReceiptOrderForGoods/VN"/>
                    </xsl:element>
                    <xsl:element name="customer">
                        <xsl:value-of select="current()/ReceiptOrderForGoods/Customer"/>
                    </xsl:element>
                    <xsl:apply-templates mode="delivery" select="current()"/>
                </xsl:when>

                <xsl:when test="ExpenditureOrderForGoods != ''">
                    <xsl:element name="msgType">shipment</xsl:element>
                    <xsl:element name="customerID">
                        <xsl:value-of select="current()/ExpenditureOrderForGoods/VN"/>
                    </xsl:element>
                    <xsl:element name="customer">
                        <xsl:value-of select="current()/ExpenditureOrderForGoods/Customer"/>
                    </xsl:element>
                    <xsl:apply-templates mode="shipment" select="current()"/>
                </xsl:when>

                <xsl:when test="AddingGoods != ''">
                    <xsl:element name="msgType">sku</xsl:element>
                    <xsl:element name="customerID">
                        <xsl:value-of select="current()/AddingGoods/VN"/>
                    </xsl:element>
                    <xsl:element name="product">
                        <xsl:apply-templates mode="addingGoods" select="current()"/>
                    </xsl:element>
                </xsl:when>
            </xsl:choose>
        </Shell>
    </xsl:template>

    <!--Test-->
    <xsl:template mode="customer" match="Customer">
        <xsl:element name="customerrrr">
            <xsl:value-of select="current()"/>
        </xsl:element>
    </xsl:template>

    <xsl:template mode="addingGoods" match="AddingGoods" xmlns="http://www.severtrans.com">
        <xsl:element name="article">
            <xsl:value-of select="current()/ARTICLE"/>
        </xsl:element>
        <xsl:element name="upc">
            <xsl:value-of select="current()/UPC"/>
        </xsl:element>
        <xsl:element name="name">
            <xsl:value-of select="current()/NAME"/>
        </xsl:element>
        <xsl:element name="uofm">
            <xsl:value-of select="current()/MEASURE"/>
        </xsl:element>
        <xsl:element name="storageLife">
            <xsl:value-of select="current()/PRODUCT_LIFE"/>
        </xsl:element>
        <xsl:element name="storageCondition">
            <xsl:value-of select="current()/STORAGE_POS"/>
        </xsl:element>
        <xsl:element name="productType">
            <xsl:value-of select="current()/BILLING_CLASS"/>
        </xsl:element>

        <!--        <ARTICLE>COGENT1</ARTICLE>
        <UPC>2000001720875</UPC>
        <NAME>Матрас</NAME>
        <MEASURE>шт</MEASURE>
        <PRODUCT_LIFE>180</PRODUCT_LIFE>
        <STORAGE_POS>НОРМ</STORAGE_POS>
        <BILLING_CLASS>НЗ</BILLING_CLASS>
-->
    </xsl:template>

    <xsl:template name="order" xmlns="http://www.severtrans.com">
        <xsl:element name="orderNo">
            <xsl:value-of select="current()/Number"/>
        </xsl:element>
        <xsl:element name="orderDate">
            <xsl:variable name="dt" select="current()/Date"/>
            <xsl:value-of select="st:myDateTime($dt)"/>
        </xsl:element>
        <xsl:element name="plannedDate">
            <xsl:choose>
                <xsl:when test="current()/PlannedDeliveryDate">
                    <xsl:variable name="dt" select="current()/PlannedDeliveryDate"/>
                    <xsl:value-of select="st:myDateTime($dt)"/>
                </xsl:when>
                <xsl:when test="current()/PlannedShipmentDate">
                    <xsl:variable name="dt" select="current()/PlannedShipmentDate"/>
                    <xsl:value-of select="st:myDateTime($dt)"/>
                </xsl:when>
            </xsl:choose>

        </xsl:element>
        <xsl:element name="orderType">
            <xsl:value-of select="current()/OrderType"/>
        </xsl:element>
        <xsl:element name="deliveryType">
            <xsl:value-of select="current()/TypeOfDelivery"/>
        </xsl:element>
        <xsl:element name="contractor">
            <xsl:element name="code">
                <xsl:choose>
                    <xsl:when test="current()/IDSupplier">
                        <xsl:value-of select="current()/IDSupplier"/>
                    </xsl:when>
                    <xsl:when test="current()/IDConsignee">
                        <xsl:value-of select="current()/IDConsignee"/>
                    </xsl:when>
                </xsl:choose>
            </xsl:element>

            <xsl:element name="name">
                <!--                <xsl:value-of select="current()/NameSupplier"/>-->
                <xsl:choose>
                    <xsl:when test="current()/NameSupplier">
                        <xsl:value-of select="current()/NameSupplier"/>
                    </xsl:when>
                    <xsl:when test="current()/NameConsignee">
                        <xsl:value-of select="current()/NameConsignee"/>
                    </xsl:when>
                </xsl:choose>
            </xsl:element>

            <xsl:element name="address">
                <xsl:value-of select="current()/AdressSupplier"/>
                <xsl:choose>
                    <xsl:when test="current()/AdressSupplier">
                        <xsl:value-of select="current()/AdressSupplier"/>
                    </xsl:when>
                    <xsl:when test="current()/AdressConsignee">
                        <xsl:value-of select="current()/AdressConsignee"/>
                    </xsl:when>
                </xsl:choose>
            </xsl:element>

        </xsl:element>

        <xsl:element name="vehicle">
            <xsl:element name="licencePlate">
                <xsl:value-of select="current()/NumberCar"/>
            </xsl:element>
            <xsl:element name="driver">
                <xsl:value-of select="current()/Driver"/>
            </xsl:element>
        </xsl:element>
    </xsl:template>

    <xsl:template mode="lineItems" match="Goods">
        <lineItem xmlns="http://www.severtrans.com">
            <xsl:element name="lineNumber">
                <xsl:value-of select="current()/LineNumber"/>
            </xsl:element>
            <xsl:element name="article">
                <xsl:value-of select="current()/Article"/>
            </xsl:element>
            <xsl:element name="name">
                <xsl:value-of select="current()/Name"/>
            </xsl:element>
            <xsl:element name="qty">
                <xsl:value-of select="current()/Count"/>
            </xsl:element>
            <xsl:element name="category">
                <xsl:value-of select="current()/Category"/>
            </xsl:element>
            <xsl:element name="mark2">
                <xsl:value-of select="current()/Mark2"/>
            </xsl:element>
            <xsl:element name="comment">
                <xsl:value-of select="current()/Comment"/>
            </xsl:element>
        </lineItem>
    </xsl:template>

    <xsl:template mode="delivery" match="ReceiptOrderForGoods">
        <deliveryOrder xmlns="http://www.severtrans.com">
            <xsl:call-template name="order"/>
            <!--
                        <xsl:element name="orderNo">
                            <xsl:value-of select="current()/Number"/>
                        </xsl:element>
                        <xsl:element name="orderDate">
                            &lt;!&ndash;            <xsl:value-of select="current()/Date"/>&ndash;&gt;
                            <xsl:text>2021-01-17T11:51:23.206+03:00</xsl:text>
                        </xsl:element>
                        <xsl:element name="plannedDate">
                            &lt;!&ndash;            <xsl:value-of select="current()/PlannedDeliveryDate"/>&ndash;&gt;
                            <xsl:text>2021-01-17T11:51:23.206+03:00</xsl:text>
                        </xsl:element>
                        <xsl:element name="orderType">
                            <xsl:value-of select="current()/OrderType"/>
                        </xsl:element>
                        <xsl:element name="deliveryType">
                            <xsl:value-of select="current()/TypeOfDelivery"/>
                        </xsl:element>
                        <xsl:element name="contractor">
                            <xsl:element name="code">
                                <xsl:value-of select="current()/IDSupplier"/>
                            </xsl:element>
                            <xsl:element name="name">
                                <xsl:value-of select="current()/NameSupplier"/>
                            </xsl:element>
                            <xsl:element name="adress">
                                <xsl:value-of select="current()/AdressSupplier"/>
                            </xsl:element>
                        </xsl:element>
                        <xsl:element name="vehicle">
                            <xsl:element name="licencePlate">
                                <xsl:value-of select="current()/NumberCar"/>
                            </xsl:element>
                            <xsl:element name="driver">
                                <xsl:value-of select="current()/Driver"/>
                            </xsl:element>
                        </xsl:element>
            -->
            <xsl:apply-templates mode="lineItems" select="current()/Goods"/>
        </deliveryOrder>
        <!--        <xsl:for-each select="current()/Goods">
            <lineItem>
                <xsl:element name="lineNumber">
                    <xsl:value-of select="current()/LineNumber"/>
                </xsl:element>
                <xsl:element name="article">
                    <xsl:value-of select="current()/Article"/>
                </xsl:element>
                <xsl:element name="name">
                    <xsl:value-of select="current()/Name"/>
                </xsl:element>
                <xsl:element name="qty">
                    <xsl:value-of select="current()/Count"/>
                </xsl:element>
                <xsl:element name="category">
                    <xsl:value-of select="current()/Category"/>
                </xsl:element>
                <xsl:element name="mark2">
                    <xsl:value-of select="current()/Mark2"/>
                </xsl:element>
                <xsl:element name="comment">
                    <xsl:value-of select="current()/Comment"/>
                </xsl:element>
            </lineItem>
        </xsl:for-each>
-->
    </xsl:template>

    <xsl:template mode="shipment" match="ExpenditureOrderForGoods">
        <deliveryOrder xmlns="http://www.severtrans.com">
            <xsl:call-template name="order"/>
            <xsl:apply-templates mode="lineItems" select="current()/Goods"/>
        </deliveryOrder>
    </xsl:template>
</xsl:stylesheet>
