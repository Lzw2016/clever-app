<?xml version="1.0" encoding="UTF-8" standalone="yes"?><?mso-application progid="Word.Document"?>
<pkg:package xmlns:pkg="http://schemas.microsoft.com/office/2006/xmlPackage">
    <pkg:part pkg:name="/_rels/.rels" pkg:contentType="application/vnd.openxmlformats-package.relationships+xml" pkg:padding="512">
        <pkg:xmlData>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
                <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/custom-properties" Target="docProps/custom.xml"/>
            </Relationships>
        </pkg:xmlData>
    </pkg:part>
    <pkg:part pkg:name="/word/document.xml" pkg:contentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml">
        <pkg:xmlData>
            <w:document
                    xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006"
                    xmlns:o="urn:schemas-microsoft-com:office:office"
                    xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math"
                    xmlns:v="urn:schemas-microsoft-com:vml"
                    xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                    xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml"
                    xmlns:w15="http://schemas.microsoft.com/office/word/2012/wordml"
                    mc:Ignorable="w14 w15 w16se w16cid w16 w16cex wp14">
                <w:body>
                    <w:p w14:paraId="7AD67E4B" w14:textId="77777777" w:rsidR="005153C0" w:rsidRDefault="005153C0">
                        <w:pPr>
                            <w:pStyle w:val="1"/>
                            <w:jc w:val="center"/>
                        </w:pPr>
                        <w:r>
                            <w:rPr>
                                <w:rFonts w:hint="eastAsia"/>
                            </w:rPr>
                            <w:t>${title!'数据库表结构文档'}</w:t>
                        </w:r>
                    </w:p>
                    <w:p w14:paraId="11B80088" w14:textId="77777777" w:rsidR="005153C0" w:rsidRPr="006012B2" w:rsidRDefault="005153C0">
                        <w:pPr>
                            <w:jc w:val="left"/>
                            <w:rPr>
                                <w:sz w:val="18"/>
                                <w:szCs w:val="18"/>
                            </w:rPr>
                        </w:pPr>
                        <w:r w:rsidRPr="00CD0CA2">
                            <w:rPr>
                                <w:rFonts w:hint="eastAsia"/>
                                <w:b/>
                                <w:bCs/>
                                <w:sz w:val="18"/>
                                <w:szCs w:val="18"/>
                            </w:rPr>
                            <w:t>数据库名：</w:t>
                        </w:r>
                        <w:r w:rsidRPr="006012B2">
                            <w:rPr>
                                <w:rFonts w:hint="eastAsia"/>
                                <w:sz w:val="18"/>
                                <w:szCs w:val="18"/>
                            </w:rPr>
                            <w:t>${database!''}</w:t>
                        </w:r>
                    </w:p>
                    <#if version?trim?length gt 2>
                    <w:p w14:paraId="5AF9097E" w14:textId="77777777" w:rsidR="005153C0" w:rsidRPr="006012B2" w:rsidRDefault="005153C0">
                        <w:pPr>
                            <w:jc w:val="left"/>
                            <w:rPr>
                                <w:sz w:val="18"/>
                                <w:szCs w:val="18"/>
                            </w:rPr>
                        </w:pPr>
                        <w:r w:rsidRPr="00CD0CA2">
                            <w:rPr>
                                <w:rFonts w:hint="eastAsia"/>
                                <w:b/>
                                <w:bCs/>
                                <w:sz w:val="18"/>
                                <w:szCs w:val="18"/>
                            </w:rPr>
                            <w:t>文档版本：</w:t>
                        </w:r>
                        <w:r w:rsidRPr="006012B2">
                            <w:rPr>
                                <w:rFonts w:hint="eastAsia"/>
                                <w:sz w:val="18"/>
                                <w:szCs w:val="18"/>
                            </w:rPr>
                            <w:t>${version!''}</w:t>
                        </w:r>
                    </w:p>
                    </#if>
                    <#if description?trim?length gt 2>
                    <w:p w14:paraId="1EEE076F" w14:textId="77777777" w:rsidR="005153C0" w:rsidRPr="006012B2" w:rsidRDefault="005153C0">
                        <w:pPr>
                            <w:jc w:val="left"/>
                            <w:rPr>
                                <w:rFonts w:hint="eastAsia"/>
                                <w:sz w:val="18"/>
                                <w:szCs w:val="18"/>
                            </w:rPr>
                        </w:pPr>
                        <w:r w:rsidRPr="00CD0CA2">
                            <w:rPr>
                                <w:rFonts w:hint="eastAsia"/>
                                <w:b/>
                                <w:bCs/>
                                <w:sz w:val="18"/>
                                <w:szCs w:val="18"/>
                            </w:rPr>
                            <w:t>文档描述：</w:t>
                        </w:r>
                        <w:r w:rsidRPr="006012B2">
                            <w:rPr>
                                <w:rFonts w:hint="eastAsia"/>
                                <w:sz w:val="18"/>
                                <w:szCs w:val="18"/>
                            </w:rPr>
                            <w:t>${description!''}</w:t>
                        </w:r>
                    </w:p>
                    </#if>
                    <#--表-->
                    <#list tables>
                    <#items as t>
                    <w:p w14:paraId="2C9FECF1" w14:textId="77777777" w:rsidR="005153C0" w:rsidRPr="00CB1EBF" w:rsidRDefault="00F13AF0" w:rsidP="00C040D4">
                        <w:pPr>
                            <w:pStyle w:val="2"/>
                            <w:jc w:val="left"/>
                            <w:rPr>
                                <w:rFonts w:ascii="宋体" w:eastAsia="宋体" w:hAnsi="宋体"/>
                                <w:sz w:val="18"/>
                                <w:szCs w:val="18"/>
                            </w:rPr>
                        </w:pPr>
                        <w:bookmarkStart w:id="${t?index}" w:name="${'_'+t.tableName!''}"/>
                        <w:bookmarkEnd w:id="${t?index}"/>
                        <w:r w:rsidRPr="00CB1EBF">
                            <w:rPr>
                                <w:rFonts w:ascii="宋体" w:eastAsia="宋体" w:hAnsi="宋体" w:hint="eastAsia"/>
                                <w:sz w:val="21"/>
                                <w:szCs w:val="21"/>
                            </w:rPr>
                            <w:t>表</w:t>
                        </w:r>
                        <w:r w:rsidR="00B73767" w:rsidRPr="00CB1EBF">
                            <w:rPr>
                                <w:rFonts w:ascii="宋体" w:eastAsia="宋体" w:hAnsi="宋体" w:hint="eastAsia"/>
                                <w:sz w:val="18"/>
                                <w:szCs w:val="18"/>
                            </w:rPr>
                            <w:t xml:space="preserve"/>
                        </w:r>
                        <w:r w:rsidR="005153C0" w:rsidRPr="00CB1EBF">
                            <w:rPr>
                                <w:rFonts w:ascii="宋体" w:eastAsia="宋体" w:hAnsi="宋体" w:hint="eastAsia"/>
                                <w:sz w:val="21"/>
                                <w:szCs w:val="21"/>
                            </w:rPr>
                            <#if (t.remarks??)&&(t.remarks?length gt 1)>
                            <w:t>${t.tableName!''} (${t.remarks})</w:t>
                            <#else>
                            <w:t>${t.tableName!''}</w:t>
                            </#if>
                        </w:r>
                    </w:p>
                    <#--页面设置-->
                    <w:tbl>
                        <w:tblPr>
                            <w:tblW w:w="10400" w:type="dxa"/>
                            <w:jc w:val="center"/>
                            <w:tblInd w:w="0" w:type="dxa"/>
                            <w:tblBorders>
                                <w:top w:val="single" w:sz="4" w:space="0" w:color="auto"/>
                                <w:left w:val="single" w:sz="4" w:space="0" w:color="auto"/>
                                <w:bottom w:val="single" w:sz="4" w:space="0" w:color="auto"/>
                                <w:right w:val="single" w:sz="4" w:space="0" w:color="auto"/>
                                <w:insideH w:val="single" w:sz="4" w:space="0" w:color="auto"/>
                                <w:insideV w:val="single" w:sz="4" w:space="0" w:color="auto"/>
                            </w:tblBorders>
                            <w:tblLook w:val="0000" w:firstRow="0" w:lastRow="0" w:firstColumn="0" w:lastColumn="0" w:noHBand="0" w:noVBand="0"/>
                        </w:tblPr>
                        <w:tblGrid>
                            <w:gridCol w:w="1152"/>
                            <w:gridCol w:w="1153"/>
                            <w:gridCol w:w="1153"/>
                            <w:gridCol w:w="1153"/>
                            <w:gridCol w:w="1153"/>
                            <w:gridCol w:w="1153"/>
                            <w:gridCol w:w="1153"/>
                            <w:gridCol w:w="1153"/>
                            <w:gridCol w:w="1177"/>
                        </w:tblGrid>
                        <w:tr w:rsidR="005153C0" w14:paraId="35F8E939" w14:textId="77777777" w:rsidTr="00406645">
                            <w:trPr>
                                <w:trHeight w:val="250"/>
                                <w:jc w:val="center"/>
                            </w:trPr>
                            <w:tc>
                                <w:tcPr>
                                    <w:tcW w:w="1152" w:type="dxa"/>
                                    <w:shd w:val="clear" w:color="auto" w:fill="E7E6E6"/>
                                </w:tcPr>
                                <w:p w14:paraId="0E519532" w14:textId="77777777" w:rsidR="005153C0" w:rsidRPr="006012B2" w:rsidRDefault="005153C0">
                                    <w:pPr>
                                        <w:jc w:val="center"/>
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                    </w:pPr>
                                    <w:r w:rsidRPr="006012B2">
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体" w:hint="eastAsia"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                        <w:t>编号</w:t>
                                    </w:r>
                                </w:p>
                            </w:tc>
                            <w:tc>
                                <w:tcPr>
                                    <w:tcW w:w="1153" w:type="dxa"/>
                                    <w:shd w:val="clear" w:color="auto" w:fill="E7E6E6"/>
                                </w:tcPr>
                                <w:p w14:paraId="6AD5691E" w14:textId="77777777" w:rsidR="005153C0" w:rsidRPr="006012B2" w:rsidRDefault="005153C0">
                                    <w:pPr>
                                        <w:jc w:val="center"/>
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                    </w:pPr>
                                    <w:r w:rsidRPr="006012B2">
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体" w:hint="eastAsia"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                        <w:t>名称</w:t>
                                    </w:r>
                                </w:p>
                            </w:tc>
                            <w:tc>
                                <w:tcPr>
                                    <w:tcW w:w="1153" w:type="dxa"/>
                                    <w:shd w:val="clear" w:color="auto" w:fill="E7E6E6"/>
                                </w:tcPr>
                                <w:p w14:paraId="182E8874" w14:textId="77777777" w:rsidR="005153C0" w:rsidRPr="006012B2" w:rsidRDefault="005153C0">
                                    <w:pPr>
                                        <w:jc w:val="center"/>
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                    </w:pPr>
                                    <w:r w:rsidRPr="006012B2">
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体" w:hint="eastAsia"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                        <w:t>数据类型</w:t>
                                    </w:r>
                                </w:p>
                            </w:tc>
                            <w:tc>
                                <w:tcPr>
                                    <w:tcW w:w="1153" w:type="dxa"/>
                                    <w:shd w:val="clear" w:color="auto" w:fill="E7E6E6"/>
                                </w:tcPr>
                                <w:p w14:paraId="3F87B560" w14:textId="77777777" w:rsidR="005153C0" w:rsidRPr="006012B2" w:rsidRDefault="005153C0">
                                    <w:pPr>
                                        <w:jc w:val="center"/>
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                    </w:pPr>
                                    <w:r w:rsidRPr="006012B2">
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体" w:hint="eastAsia"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                        <w:t>小数位</w:t>
                                    </w:r>
                                </w:p>
                            </w:tc>
                            <w:tc>
                                <w:tcPr>
                                    <w:tcW w:w="1153" w:type="dxa"/>
                                    <w:shd w:val="clear" w:color="auto" w:fill="E7E6E6"/>
                                </w:tcPr>
                                <w:p w14:paraId="5445F911" w14:textId="77777777" w:rsidR="005153C0" w:rsidRPr="006012B2" w:rsidRDefault="005153C0">
                                    <w:pPr>
                                        <w:jc w:val="center"/>
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                    </w:pPr>
                                    <w:r w:rsidRPr="006012B2">
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体" w:hint="eastAsia"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                        <w:t>允许空值</w:t>
                                    </w:r>
                                </w:p>
                            </w:tc>
                            <w:tc>
                                <w:tcPr>
                                    <w:tcW w:w="1153" w:type="dxa"/>
                                    <w:shd w:val="clear" w:color="auto" w:fill="E7E6E6"/>
                                </w:tcPr>
                                <w:p w14:paraId="1B70EF2C" w14:textId="77777777" w:rsidR="005153C0" w:rsidRPr="006012B2" w:rsidRDefault="005153C0">
                                    <w:pPr>
                                        <w:jc w:val="center"/>
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                    </w:pPr>
                                    <w:r w:rsidRPr="006012B2">
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体" w:hint="eastAsia"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                        <w:t>主键</w:t>
                                    </w:r>
                                </w:p>
                            </w:tc>
                            <w:tc>
                                <w:tcPr>
                                    <w:tcW w:w="1153" w:type="dxa"/>
                                    <w:shd w:val="clear" w:color="auto" w:fill="E7E6E6"/>
                                </w:tcPr>
                                <w:p w14:paraId="56943756" w14:textId="77777777" w:rsidR="005153C0" w:rsidRPr="006012B2" w:rsidRDefault="005153C0">
                                    <w:pPr>
                                        <w:jc w:val="center"/>
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                    </w:pPr>
                                    <w:r w:rsidRPr="006012B2">
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体" w:hint="eastAsia"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                        <w:t>默认值</w:t>
                                    </w:r>
                                </w:p>
                            </w:tc>
                            <w:tc>
                                <w:tcPr>
                                    <w:tcW w:w="1177" w:type="dxa"/>
                                    <w:shd w:val="clear" w:color="auto" w:fill="E7E6E6"/>
                                </w:tcPr>
                                <w:p w14:paraId="0F1BBB9B" w14:textId="77777777" w:rsidR="005153C0" w:rsidRPr="006012B2" w:rsidRDefault="005153C0">
                                    <w:pPr>
                                        <w:jc w:val="center"/>
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                    </w:pPr>
                                    <w:r w:rsidRPr="006012B2">
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体" w:hint="eastAsia"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                        <w:t>说明</w:t>
                                    </w:r>
                                </w:p>
                            </w:tc>
                        </w:tr>
                        <#--列-->
                        <#list t.columns>
                        <#items as c>
                        <w:tr w:rsidR="005153C0" w14:paraId="6AEC5B4C" w14:textId="77777777">
                            <w:trPr>
                                <w:trHeight w:val="359"/>
                                <w:jc w:val="center"/>
                            </w:trPr>
                            <w:tc>
                                <w:tcPr>
                                    <w:tcW w:w="1152" w:type="dxa"/>
                                </w:tcPr>
                                <w:p w14:paraId="2918073A" w14:textId="77777777" w:rsidR="005153C0" w:rsidRPr="006012B2" w:rsidRDefault="005153C0">
                                    <w:pPr>
                                        <w:jc w:val="center"/>
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                    </w:pPr>
                                    <w:r w:rsidRPr="006012B2">
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体" w:hint="eastAsia"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                        <w:t>${c?index+1}</w:t>
                                    </w:r>
                                </w:p>
                            </w:tc>
                            <w:tc>
                                <w:tcPr>
                                    <w:tcW w:w="1153" w:type="dxa"/>
                                </w:tcPr>
                                <w:p w14:paraId="19D9F58B" w14:textId="77777777" w:rsidR="005153C0" w:rsidRPr="006012B2" w:rsidRDefault="005153C0">
                                    <w:pPr>
                                        <w:jc w:val="left"/>
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                    </w:pPr>
                                    <w:r w:rsidRPr="006012B2">
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体" w:hint="eastAsia"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                        <w:t>${c.columnName!''}</w:t>
                                    </w:r>
                                </w:p>
                            </w:tc>
                            <w:tc>
                                <w:tcPr>
                                    <w:tcW w:w="1153" w:type="dxa"/>
                                </w:tcPr>
                                <w:p w14:paraId="59AD952F" w14:textId="77777777" w:rsidR="005153C0" w:rsidRPr="006012B2" w:rsidRDefault="005153C0">
                                    <w:pPr>
                                        <w:jc w:val="center"/>
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                    </w:pPr>
                                    <w:r w:rsidRPr="006012B2">
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体" w:hint="eastAsia"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                        <w:t>${c.columnType!''}</w:t>
                                    </w:r>
                                </w:p>
                            </w:tc>
                            <w:tc>
                                <w:tcPr>
                                    <w:tcW w:w="1153" w:type="dxa"/>
                                </w:tcPr>
                                <w:p w14:paraId="22D1598F" w14:textId="77777777" w:rsidR="005153C0" w:rsidRPr="006012B2" w:rsidRDefault="005153C0">
                                    <w:pPr>
                                        <w:jc w:val="center"/>
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                    </w:pPr>
                                    <w:r w:rsidRPr="006012B2">
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体" w:hint="eastAsia"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                        <w:t>${c.decimalDigits!'0'}</w:t>
                                    </w:r>
                                </w:p>
                            </w:tc>
                            <w:tc>
                                <w:tcPr>
                                    <w:tcW w:w="1153" w:type="dxa"/>
                                </w:tcPr>
                                <w:p w14:paraId="54658183" w14:textId="77777777" w:rsidR="005153C0" w:rsidRPr="006012B2" w:rsidRDefault="005153C0">
                                    <w:pPr>
                                        <w:jc w:val="center"/>
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                    </w:pPr>
                                    <w:r w:rsidRPr="006012B2">
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体" w:hint="eastAsia"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                        <w:t>${c.nullable!''}</w:t>
                                    </w:r>
                                </w:p>
                            </w:tc>
                            <w:tc>
                                <w:tcPr>
                                    <w:tcW w:w="1153" w:type="dxa"/>
                                </w:tcPr>
                                <w:p w14:paraId="55267221" w14:textId="77777777" w:rsidR="005153C0" w:rsidRPr="006012B2" w:rsidRDefault="005153C0">
                                    <w:pPr>
                                        <w:jc w:val="center"/>
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                    </w:pPr>
                                    <w:r w:rsidRPr="006012B2">
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体" w:hint="eastAsia"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                        <w:t>${c.primaryKey!''}</w:t>
                                    </w:r>
                                </w:p>
                            </w:tc>
                            <w:tc>
                                <w:tcPr>
                                    <w:tcW w:w="1153" w:type="dxa"/>
                                </w:tcPr>
                                <w:p w14:paraId="3EF53946" w14:textId="77777777" w:rsidR="005153C0" w:rsidRPr="006012B2" w:rsidRDefault="005153C0">
                                    <w:pPr>
                                        <w:jc w:val="center"/>
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                    </w:pPr>
                                    <w:r w:rsidRPr="006012B2">
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体" w:hint="eastAsia"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                        <w:t>${c.columnDef!''}</w:t>
                                    </w:r>
                                </w:p>
                            </w:tc>
                            <w:tc>
                                <w:tcPr>
                                    <w:tcW w:w="1177" w:type="dxa"/>
                                </w:tcPr>
                                <w:p w14:paraId="60A311BD" w14:textId="77777777" w:rsidR="005153C0" w:rsidRPr="006012B2" w:rsidRDefault="005153C0">
                                    <w:pPr>
                                        <w:jc w:val="center"/>
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                    </w:pPr>
                                    <w:r w:rsidRPr="006012B2">
                                        <w:rPr>
                                            <w:rFonts w:ascii="宋体" w:hAnsi="宋体" w:cs="宋体" w:hint="eastAsia"/>
                                            <w:sz w:val="18"/>
                                            <w:szCs w:val="18"/>
                                        </w:rPr>
                                        <w:t>${c.remarks!''}</w:t>
                                    </w:r>
                                </w:p>
                            </w:tc>
                        </w:tr>
                        </#items>
                        </#list>
                    </w:tbl>
                    </#items>
                    </#list>
                    <w:sectPr w:rsidR="005153C0" w:rsidSect="00F9005A">
                        <w:pgSz w:w="11906" w:h="16838"/>
                        <w:pgMar w:top="720" w:right="720" w:bottom="720" w:left="720" w:header="851" w:footer="992" w:gutter="0"/>
                        <w:cols w:space="720"/>
                        <w:docGrid w:type="lines" w:linePitch="312"/>
                    </w:sectPr>
                </w:body>
            </w:document>
        </pkg:xmlData>
    </pkg:part>
    <pkg:part pkg:name="/word/_rels/document.xml.rels" pkg:contentType="application/vnd.openxmlformats-package.relationships+xml" pkg:padding="256">
        <pkg:xmlData>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/webSettings" Target="webSettings.xml"/>
                <Relationship Id="rId7" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="theme/theme1.xml"/>
                <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings" Target="settings.xml"/>
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                <Relationship Id="rId6" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/fontTable" Target="fontTable.xml"/>
                <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/endnotes" Target="endnotes.xml"/>
                <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/footnotes" Target="footnotes.xml"/>
            </Relationships>
        </pkg:xmlData>
    </pkg:part>
    <pkg:part pkg:name="/word/footnotes.xml" pkg:contentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footnotes+xml">
        <pkg:xmlData>
            <w:footnotes
                    xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006"
                    xmlns:o="urn:schemas-microsoft-com:office:office"
                    xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math"
                    xmlns:v="urn:schemas-microsoft-com:vml"
                    xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                    xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml"
                    xmlns:w15="http://schemas.microsoft.com/office/word/2012/wordml"
                    mc:Ignorable="w14 w15 w16se w16cid w16 w16cex wp14">
                <w:footnote w:type="separator" w:id="-1">
                    <w:p w14:paraId="6A89F9E1" w14:textId="77777777" w:rsidR="0072008A" w:rsidRDefault="0072008A">
                        <w:r>
                            <w:separator/>
                        </w:r>
                    </w:p>
                </w:footnote>
                <w:footnote w:type="continuationSeparator" w:id="0">
                    <w:p w14:paraId="32BF6ABC" w14:textId="77777777" w:rsidR="0072008A" w:rsidRDefault="0072008A">
                        <w:r>
                            <w:continuationSeparator/>
                        </w:r>
                    </w:p>
                </w:footnote>
            </w:footnotes>
        </pkg:xmlData>
    </pkg:part>
    <pkg:part pkg:name="/word/endnotes.xml" pkg:contentType="application/vnd.openxmlformats-officedocument.wordprocessingml.endnotes+xml">
        <pkg:xmlData>
            <w:endnotes
                    xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006"
                    xmlns:o="urn:schemas-microsoft-com:office:office"
                    xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math"
                    xmlns:v="urn:schemas-microsoft-com:vml"
                    xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                    xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml"
                    xmlns:w15="http://schemas.microsoft.com/office/word/2012/wordml"
                    mc:Ignorable="w14 w15 w16se w16cid w16 w16cex wp14">
                <w:endnote w:type="separator" w:id="-1">
                    <w:p w14:paraId="4C221E26" w14:textId="77777777" w:rsidR="0072008A" w:rsidRDefault="0072008A">
                        <w:r>
                            <w:separator/>
                        </w:r>
                    </w:p>
                </w:endnote>
                <w:endnote w:type="continuationSeparator" w:id="0">
                    <w:p w14:paraId="0C8480D2" w14:textId="77777777" w:rsidR="0072008A" w:rsidRDefault="0072008A">
                        <w:r>
                            <w:continuationSeparator/>
                        </w:r>
                    </w:p>
                </w:endnote>
            </w:endnotes>
        </pkg:xmlData>
    </pkg:part>
    <pkg:part pkg:name="/word/theme/theme1.xml" pkg:contentType="application/vnd.openxmlformats-officedocument.theme+xml">
        <pkg:xmlData>
            <a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="Office 主题">
                <a:themeElements>
                    <a:clrScheme name="Office">
                        <a:dk1>
                            <a:sysClr val="windowText" lastClr="000000"/>
                        </a:dk1>
                        <a:lt1>
                            <a:sysClr val="window" lastClr="FFFFFF"/>
                        </a:lt1>
                        <a:dk2>
                            <a:srgbClr val="44546A"/>
                        </a:dk2>
                        <a:lt2>
                            <a:srgbClr val="E7E6E6"/>
                        </a:lt2>
                        <a:accent1>
                            <a:srgbClr val="4472C4"/>
                        </a:accent1>
                        <a:accent2>
                            <a:srgbClr val="ED7D31"/>
                        </a:accent2>
                        <a:accent3>
                            <a:srgbClr val="A5A5A5"/>
                        </a:accent3>
                        <a:accent4>
                            <a:srgbClr val="FFC000"/>
                        </a:accent4>
                        <a:accent5>
                            <a:srgbClr val="5B9BD5"/>
                        </a:accent5>
                        <a:accent6>
                            <a:srgbClr val="70AD47"/>
                        </a:accent6>
                        <a:hlink>
                            <a:srgbClr val="0563C1"/>
                        </a:hlink>
                        <a:folHlink>
                            <a:srgbClr val="954F72"/>
                        </a:folHlink>
                    </a:clrScheme>
                    <a:fontScheme name="Office">
                        <a:majorFont>
                            <a:latin typeface="等线 Light" panose="020F0302020204030204"/>
                            <a:ea typeface=""/>
                            <a:cs typeface=""/>
                            <a:font script="Jpan" typeface="游ゴシック Light"/>
                            <a:font script="Hang" typeface="맑은 고딕"/>
                            <a:font script="Hans" typeface="等线 Light"/>
                            <a:font script="Hant" typeface="新細明體"/>
                            <a:font script="Arab" typeface="Times New Roman"/>
                            <a:font script="Hebr" typeface="Times New Roman"/>
                            <a:font script="Thai" typeface="Angsana New"/>
                            <a:font script="Ethi" typeface="Nyala"/>
                            <a:font script="Beng" typeface="Vrinda"/>
                            <a:font script="Gujr" typeface="Shruti"/>
                            <a:font script="Khmr" typeface="MoolBoran"/>
                            <a:font script="Knda" typeface="Tunga"/>
                            <a:font script="Guru" typeface="Raavi"/>
                            <a:font script="Cans" typeface="Euphemia"/>
                            <a:font script="Cher" typeface="Plantagenet Cherokee"/>
                            <a:font script="Yiii" typeface="Microsoft Yi Baiti"/>
                            <a:font script="Tibt" typeface="Microsoft Himalaya"/>
                            <a:font script="Thaa" typeface="MV Boli"/>
                            <a:font script="Deva" typeface="Mangal"/>
                            <a:font script="Telu" typeface="Gautami"/>
                            <a:font script="Taml" typeface="Latha"/>
                            <a:font script="Syrc" typeface="Estrangelo Edessa"/>
                            <a:font script="Orya" typeface="Kalinga"/>
                            <a:font script="Mlym" typeface="Kartika"/>
                            <a:font script="Laoo" typeface="DokChampa"/>
                            <a:font script="Sinh" typeface="Iskoola Pota"/>
                            <a:font script="Mong" typeface="Mongolian Baiti"/>
                            <a:font script="Viet" typeface="Times New Roman"/>
                            <a:font script="Uigh" typeface="Microsoft Uighur"/>
                            <a:font script="Geor" typeface="Sylfaen"/>
                            <a:font script="Armn" typeface="Arial"/>
                            <a:font script="Bugi" typeface="Leelawadee UI"/>
                            <a:font script="Bopo" typeface="Microsoft JhengHei"/>
                            <a:font script="Java" typeface="Javanese Text"/>
                            <a:font script="Lisu" typeface="Segoe UI"/>
                            <a:font script="Mymr" typeface="Myanmar Text"/>
                            <a:font script="Nkoo" typeface="Ebrima"/>
                            <a:font script="Olck" typeface="Nirmala UI"/>
                            <a:font script="Osma" typeface="Ebrima"/>
                            <a:font script="Phag" typeface="Phagspa"/>
                            <a:font script="Syrn" typeface="Estrangelo Edessa"/>
                            <a:font script="Syrj" typeface="Estrangelo Edessa"/>
                            <a:font script="Syre" typeface="Estrangelo Edessa"/>
                            <a:font script="Sora" typeface="Nirmala UI"/>
                            <a:font script="Tale" typeface="Microsoft Tai Le"/>
                            <a:font script="Talu" typeface="Microsoft New Tai Lue"/>
                            <a:font script="Tfng" typeface="Ebrima"/>
                        </a:majorFont>
                        <a:minorFont>
                            <a:latin typeface="等线" panose="020F0502020204030204"/>
                            <a:ea typeface=""/>
                            <a:cs typeface=""/>
                            <a:font script="Jpan" typeface="游明朝"/>
                            <a:font script="Hang" typeface="맑은 고딕"/>
                            <a:font script="Hans" typeface="等线"/>
                            <a:font script="Hant" typeface="新細明體"/>
                            <a:font script="Arab" typeface="Arial"/>
                            <a:font script="Hebr" typeface="Arial"/>
                            <a:font script="Thai" typeface="Cordia New"/>
                            <a:font script="Ethi" typeface="Nyala"/>
                            <a:font script="Beng" typeface="Vrinda"/>
                            <a:font script="Gujr" typeface="Shruti"/>
                            <a:font script="Khmr" typeface="DaunPenh"/>
                            <a:font script="Knda" typeface="Tunga"/>
                            <a:font script="Guru" typeface="Raavi"/>
                            <a:font script="Cans" typeface="Euphemia"/>
                            <a:font script="Cher" typeface="Plantagenet Cherokee"/>
                            <a:font script="Yiii" typeface="Microsoft Yi Baiti"/>
                            <a:font script="Tibt" typeface="Microsoft Himalaya"/>
                            <a:font script="Thaa" typeface="MV Boli"/>
                            <a:font script="Deva" typeface="Mangal"/>
                            <a:font script="Telu" typeface="Gautami"/>
                            <a:font script="Taml" typeface="Latha"/>
                            <a:font script="Syrc" typeface="Estrangelo Edessa"/>
                            <a:font script="Orya" typeface="Kalinga"/>
                            <a:font script="Mlym" typeface="Kartika"/>
                            <a:font script="Laoo" typeface="DokChampa"/>
                            <a:font script="Sinh" typeface="Iskoola Pota"/>
                            <a:font script="Mong" typeface="Mongolian Baiti"/>
                            <a:font script="Viet" typeface="Arial"/>
                            <a:font script="Uigh" typeface="Microsoft Uighur"/>
                            <a:font script="Geor" typeface="Sylfaen"/>
                            <a:font script="Armn" typeface="Arial"/>
                            <a:font script="Bugi" typeface="Leelawadee UI"/>
                            <a:font script="Bopo" typeface="Microsoft JhengHei"/>
                            <a:font script="Java" typeface="Javanese Text"/>
                            <a:font script="Lisu" typeface="Segoe UI"/>
                            <a:font script="Mymr" typeface="Myanmar Text"/>
                            <a:font script="Nkoo" typeface="Ebrima"/>
                            <a:font script="Olck" typeface="Nirmala UI"/>
                            <a:font script="Osma" typeface="Ebrima"/>
                            <a:font script="Phag" typeface="Phagspa"/>
                            <a:font script="Syrn" typeface="Estrangelo Edessa"/>
                            <a:font script="Syrj" typeface="Estrangelo Edessa"/>
                            <a:font script="Syre" typeface="Estrangelo Edessa"/>
                            <a:font script="Sora" typeface="Nirmala UI"/>
                            <a:font script="Tale" typeface="Microsoft Tai Le"/>
                            <a:font script="Talu" typeface="Microsoft New Tai Lue"/>
                            <a:font script="Tfng" typeface="Ebrima"/>
                        </a:minorFont>
                    </a:fontScheme>
                    <a:fmtScheme name="Office">
                        <a:fillStyleLst>
                            <a:solidFill>
                                <a:schemeClr val="phClr"/>
                            </a:solidFill>
                            <a:gradFill rotWithShape="1">
                                <a:gsLst>
                                    <a:gs pos="0">
                                        <a:schemeClr val="phClr">
                                            <a:lumMod val="110000"/>
                                            <a:satMod val="105000"/>
                                            <a:tint val="67000"/>
                                        </a:schemeClr>
                                    </a:gs>
                                    <a:gs pos="50000">
                                        <a:schemeClr val="phClr">
                                            <a:lumMod val="105000"/>
                                            <a:satMod val="103000"/>
                                            <a:tint val="73000"/>
                                        </a:schemeClr>
                                    </a:gs>
                                    <a:gs pos="100000">
                                        <a:schemeClr val="phClr">
                                            <a:lumMod val="105000"/>
                                            <a:satMod val="109000"/>
                                            <a:tint val="81000"/>
                                        </a:schemeClr>
                                    </a:gs>
                                </a:gsLst>
                                <a:lin ang="5400000" scaled="0"/>
                            </a:gradFill>
                            <a:gradFill rotWithShape="1">
                                <a:gsLst>
                                    <a:gs pos="0">
                                        <a:schemeClr val="phClr">
                                            <a:satMod val="103000"/>
                                            <a:lumMod val="102000"/>
                                            <a:tint val="94000"/>
                                        </a:schemeClr>
                                    </a:gs>
                                    <a:gs pos="50000">
                                        <a:schemeClr val="phClr">
                                            <a:satMod val="110000"/>
                                            <a:lumMod val="100000"/>
                                            <a:shade val="100000"/>
                                        </a:schemeClr>
                                    </a:gs>
                                    <a:gs pos="100000">
                                        <a:schemeClr val="phClr">
                                            <a:lumMod val="99000"/>
                                            <a:satMod val="120000"/>
                                            <a:shade val="78000"/>
                                        </a:schemeClr>
                                    </a:gs>
                                </a:gsLst>
                                <a:lin ang="5400000" scaled="0"/>
                            </a:gradFill>
                        </a:fillStyleLst>
                        <a:lnStyleLst>
                            <a:ln w="6350" cap="flat" cmpd="sng" algn="ctr">
                                <a:solidFill>
                                    <a:schemeClr val="phClr"/>
                                </a:solidFill>
                                <a:prstDash val="solid"/>
                                <a:miter lim="800000"/>
                            </a:ln>
                            <a:ln w="12700" cap="flat" cmpd="sng" algn="ctr">
                                <a:solidFill>
                                    <a:schemeClr val="phClr"/>
                                </a:solidFill>
                                <a:prstDash val="solid"/>
                                <a:miter lim="800000"/>
                            </a:ln>
                            <a:ln w="19050" cap="flat" cmpd="sng" algn="ctr">
                                <a:solidFill>
                                    <a:schemeClr val="phClr"/>
                                </a:solidFill>
                                <a:prstDash val="solid"/>
                                <a:miter lim="800000"/>
                            </a:ln>
                        </a:lnStyleLst>
                        <a:effectStyleLst>
                            <a:effectStyle>
                                <a:effectLst/>
                            </a:effectStyle>
                            <a:effectStyle>
                                <a:effectLst/>
                            </a:effectStyle>
                            <a:effectStyle>
                                <a:effectLst>
                                    <a:outerShdw blurRad="57150" dist="19050" dir="5400000" algn="ctr" rotWithShape="0">
                                        <a:srgbClr val="000000">
                                            <a:alpha val="63000"/>
                                        </a:srgbClr>
                                    </a:outerShdw>
                                </a:effectLst>
                            </a:effectStyle>
                        </a:effectStyleLst>
                        <a:bgFillStyleLst>
                            <a:solidFill>
                                <a:schemeClr val="phClr"/>
                            </a:solidFill>
                            <a:solidFill>
                                <a:schemeClr val="phClr">
                                    <a:tint val="95000"/>
                                    <a:satMod val="170000"/>
                                </a:schemeClr>
                            </a:solidFill>
                            <a:gradFill rotWithShape="1">
                                <a:gsLst>
                                    <a:gs pos="0">
                                        <a:schemeClr val="phClr">
                                            <a:tint val="93000"/>
                                            <a:satMod val="150000"/>
                                            <a:shade val="98000"/>
                                            <a:lumMod val="102000"/>
                                        </a:schemeClr>
                                    </a:gs>
                                    <a:gs pos="50000">
                                        <a:schemeClr val="phClr">
                                            <a:tint val="98000"/>
                                            <a:satMod val="130000"/>
                                            <a:shade val="90000"/>
                                            <a:lumMod val="103000"/>
                                        </a:schemeClr>
                                    </a:gs>
                                    <a:gs pos="100000">
                                        <a:schemeClr val="phClr">
                                            <a:shade val="63000"/>
                                            <a:satMod val="120000"/>
                                        </a:schemeClr>
                                    </a:gs>
                                </a:gsLst>
                                <a:lin ang="5400000" scaled="0"/>
                            </a:gradFill>
                        </a:bgFillStyleLst>
                    </a:fmtScheme>
                </a:themeElements>
                <a:objectDefaults/>
                <a:extraClrSchemeLst/>
                <a:extLst>
                    <a:ext uri="{05A4C25C-085E-4340-85A3-A5531E510DB2}">
                        <thm15:themeFamily xmlns:thm15="http://schemas.microsoft.com/office/thememl/2012/main" name="Office Theme" id="{62F939B6-93AF-4DB8-9C6B-D6C7DFDC589F}" vid="{4A3C46E8-61CC-4603-A589-7422A47A8E4A}"/>
                    </a:ext>
                </a:extLst>
            </a:theme>
        </pkg:xmlData>
    </pkg:part>
    <pkg:part pkg:name="/word/settings.xml" pkg:contentType="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml">
        <pkg:xmlData>
            <w:settings xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006"
                    xmlns:o="urn:schemas-microsoft-com:office:office"
                    xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math"
                    xmlns:v="urn:schemas-microsoft-com:vml"
                    xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                    xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml"
                    xmlns:w15="http://schemas.microsoft.com/office/word/2012/wordml"
                    mc:Ignorable="w14 w15 w16se w16cid w16 w16cex">
                <w:zoom w:percent="140"/>
                <w:bordersDoNotSurroundHeader/>
                <w:bordersDoNotSurroundFooter/>
                <w:proofState w:spelling="clean" w:grammar="clean"/>
                <w:doNotTrackMoves/>
                <w:defaultTabStop w:val="420"/>
                <w:drawingGridVerticalSpacing w:val="156"/>
                <w:displayHorizontalDrawingGridEvery w:val="0"/>
                <w:displayVerticalDrawingGridEvery w:val="2"/>
                <w:characterSpacingControl w:val="compressPunctuation"/>
                <w:footnotePr>
                    <w:footnote w:id="-1"/>
                    <w:footnote w:id="0"/>
                </w:footnotePr>
                <w:endnotePr>
                    <w:endnote w:id="-1"/>
                    <w:endnote w:id="0"/>
                </w:endnotePr>
                <w:compat>
                    <w:spaceForUL/>
                    <w:balanceSingleByteDoubleByteWidth/>
                    <w:doNotLeaveBackslashAlone/>
                    <w:ulTrailSpace/>
                    <w:doNotExpandShiftReturn/>
                    <w:adjustLineHeightInTable/>
                    <w:useFELayout/>
                    <w:useNormalStyleForList/>
                    <w:doNotUseIndentAsNumberingTabStop/>
                    <w:useAltKinsokuLineBreakRules/>
                    <w:allowSpaceOfSameStyleInTable/>
                    <w:doNotSuppressIndentation/>
                    <w:doNotAutofitConstrainedTables/>
                    <w:autofitToFirstFixedWidthCell/>
                    <w:displayHangulFixedWidth/>
                    <w:splitPgBreakAndParaMark/>
                    <w:doNotVertAlignCellWithSp/>
                    <w:doNotBreakConstrainedForcedTable/>
                    <w:doNotVertAlignInTxbx/>
                    <w:useAnsiKerningPairs/>
                    <w:cachedColBalance/>
                    <w:compatSetting w:name="compatibilityMode" w:uri="http://schemas.microsoft.com/office/word" w:val="11"/>
                    <w:compatSetting w:name="allowHyphenationAtTrackBottom" w:uri="http://schemas.microsoft.com/office/word" w:val="1"/>
                    <w:compatSetting w:name="useWord2013TrackBottomHyphenation" w:uri="http://schemas.microsoft.com/office/word" w:val="1"/>
                </w:compat>
                <w:rsids>
                    <w:rsidRoot w:val="006B6BAC"/>
                    <w:rsid w:val="00006FDD"/>
                    <w:rsid w:val="00011742"/>
                    <w:rsid w:val="00090B82"/>
                    <w:rsid w:val="00120AC9"/>
                    <w:rsid w:val="00194C59"/>
                    <w:rsid w:val="001A00A3"/>
                    <w:rsid w:val="001B2F16"/>
                    <w:rsid w:val="001C760D"/>
                    <w:rsid w:val="001D1BA4"/>
                    <w:rsid w:val="001F7F9D"/>
                    <w:rsid w:val="00207EF9"/>
                    <w:rsid w:val="00291595"/>
                    <w:rsid w:val="002C09E5"/>
                    <w:rsid w:val="00300C65"/>
                    <w:rsid w:val="003446F6"/>
                    <w:rsid w:val="0037067B"/>
                    <w:rsid w:val="00373A92"/>
                    <w:rsid w:val="003E1A72"/>
                    <w:rsid w:val="004018CE"/>
                    <w:rsid w:val="00405DA1"/>
                    <w:rsid w:val="00406645"/>
                    <w:rsid w:val="00492AA4"/>
                    <w:rsid w:val="0049404D"/>
                    <w:rsid w:val="004A2B0C"/>
                    <w:rsid w:val="005153C0"/>
                    <w:rsid w:val="00526115"/>
                    <w:rsid w:val="00554FC1"/>
                    <w:rsid w:val="00581A6E"/>
                    <w:rsid w:val="006012B2"/>
                    <w:rsid w:val="00652AB7"/>
                    <w:rsid w:val="006B6BAC"/>
                    <w:rsid w:val="0071181D"/>
                    <w:rsid w:val="0072008A"/>
                    <w:rsid w:val="007C29DD"/>
                    <w:rsid w:val="007D2EF9"/>
                    <w:rsid w:val="007E5B44"/>
                    <w:rsid w:val="00813026"/>
                    <w:rsid w:val="00836808"/>
                    <w:rsid w:val="008732C2"/>
                    <w:rsid w:val="00886F3A"/>
                    <w:rsid w:val="008A15A9"/>
                    <w:rsid w:val="008C65D0"/>
                    <w:rsid w:val="008E5711"/>
                    <w:rsid w:val="0091460F"/>
                    <w:rsid w:val="00921076"/>
                    <w:rsid w:val="00923C54"/>
                    <w:rsid w:val="00927D92"/>
                    <w:rsid w:val="00976D0D"/>
                    <w:rsid w:val="009779F7"/>
                    <w:rsid w:val="00987F90"/>
                    <w:rsid w:val="009A05DA"/>
                    <w:rsid w:val="009C1221"/>
                    <w:rsid w:val="009C2E32"/>
                    <w:rsid w:val="009E5EEA"/>
                    <w:rsid w:val="00A12B70"/>
                    <w:rsid w:val="00A178B0"/>
                    <w:rsid w:val="00A20989"/>
                    <w:rsid w:val="00AF2A8C"/>
                    <w:rsid w:val="00B035E9"/>
                    <w:rsid w:val="00B55D72"/>
                    <w:rsid w:val="00B57CE5"/>
                    <w:rsid w:val="00B73767"/>
                    <w:rsid w:val="00BB5F19"/>
                    <w:rsid w:val="00C040D4"/>
                    <w:rsid w:val="00C156E1"/>
                    <w:rsid w:val="00C3734F"/>
                    <w:rsid w:val="00CB1EBF"/>
                    <w:rsid w:val="00CD0CA2"/>
                    <w:rsid w:val="00D01B94"/>
                    <w:rsid w:val="00D508BB"/>
                    <w:rsid w:val="00D9163D"/>
                    <w:rsid w:val="00DB3B33"/>
                    <w:rsid w:val="00DD0437"/>
                    <w:rsid w:val="00DD6366"/>
                    <w:rsid w:val="00DF278B"/>
                    <w:rsid w:val="00EA0CE1"/>
                    <w:rsid w:val="00EA5BFD"/>
                    <w:rsid w:val="00EB783A"/>
                    <w:rsid w:val="00EC592F"/>
                    <w:rsid w:val="00F05527"/>
                    <w:rsid w:val="00F13AF0"/>
                    <w:rsid w:val="00F22A01"/>
                    <w:rsid w:val="00F836EF"/>
                    <w:rsid w:val="00F9005A"/>
                    <w:rsid w:val="00F9647E"/>
                    <w:rsid w:val="00FC7EA7"/>
                    <w:rsid w:val="01F165F2"/>
                    <w:rsid w:val="020F6014"/>
                    <w:rsid w:val="0B795A0E"/>
                    <w:rsid w:val="0CF9065E"/>
                    <w:rsid w:val="12D719B9"/>
                    <w:rsid w:val="186B5653"/>
                    <w:rsid w:val="1FDD5F9C"/>
                    <w:rsid w:val="261D235F"/>
                    <w:rsid w:val="2DDF3793"/>
                    <w:rsid w:val="36DF4541"/>
                    <w:rsid w:val="40E00744"/>
                    <w:rsid w:val="42B74BAD"/>
                    <w:rsid w:val="434F6875"/>
                    <w:rsid w:val="482E7D1D"/>
                    <w:rsid w:val="522C2596"/>
                    <w:rsid w:val="532E03DD"/>
                    <w:rsid w:val="53AB0C8E"/>
                    <w:rsid w:val="567F0764"/>
                    <w:rsid w:val="58E02D25"/>
                    <w:rsid w:val="5EB31DB5"/>
                    <w:rsid w:val="60B27DD0"/>
                    <w:rsid w:val="63702E15"/>
                    <w:rsid w:val="655955D6"/>
                    <w:rsid w:val="65595A0A"/>
                    <w:rsid w:val="679346C8"/>
                    <w:rsid w:val="68444C3D"/>
                    <w:rsid w:val="68794446"/>
                    <w:rsid w:val="6FB02165"/>
                    <w:rsid w:val="76BC2F74"/>
                    <w:rsid w:val="7C642244"/>
                </w:rsids>
                <m:mathPr>
                    <m:mathFont m:val="Cambria Math"/>
                    <m:brkBin m:val="before"/>
                    <m:brkBinSub m:val="--"/>
                    <m:smallFrac m:val="0"/>
                    <m:dispDef/>
                    <m:lMargin m:val="0"/>
                    <m:rMargin m:val="0"/>
                    <m:defJc m:val="centerGroup"/>
                    <m:wrapIndent m:val="1440"/>
                    <m:intLim m:val="subSup"/>
                    <m:naryLim m:val="undOvr"/>
                </m:mathPr>
                <w:themeFontLang w:val="en-US" w:eastAsia="zh-CN"/>
                <w:clrSchemeMapping w:bg1="light1" w:t1="dark1" w:bg2="light2" w:t2="dark2" w:accent1="accent1" w:accent2="accent2" w:accent3="accent3" w:accent4="accent4" w:accent5="accent5" w:accent6="accent6" w:hyperlink="hyperlink" w:followedHyperlink="followedHyperlink"/>
                <w:doNotIncludeSubdocsInStats/>
                <w:shapeDefaults>
                    <o:shapedefaults v:ext="edit" spidmax="1026" fillcolor="white">
                        <v:fill color="white"/>
                    </o:shapedefaults>
                    <o:shapelayout v:ext="edit">
                        <o:idmap v:ext="edit" data="1"/>
                    </o:shapelayout>
                </w:shapeDefaults>
                <w:decimalSymbol w:val="."/>
                <w:listSeparator w:val=","/>
                <w14:docId w14:val="1BE47DC8"/>
                <w15:chartTrackingRefBased/>
                <w15:docId w15:val="{C6DD5580-9111-4D4E-9572-2937C1D43720}"/>
            </w:settings>
        </pkg:xmlData>
    </pkg:part>
    <pkg:part pkg:name="/word/styles.xml" pkg:contentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml">
        <pkg:xmlData>
            <w:styles xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006"
                    xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                    xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml"
                    xmlns:w15="http://schemas.microsoft.com/office/word/2012/wordml"
                    mc:Ignorable="w14 w15 w16se w16cid w16 w16cex">
                <w:docDefaults>
                    <w:rPrDefault>
                        <w:rPr>
                            <w:rFonts w:ascii="Calibri" w:eastAsia="宋体" w:hAnsi="Calibri" w:cs="Times New Roman"/>
                            <w:lang w:val="en-US" w:eastAsia="zh-CN" w:bidi="ar-SA"/>
                        </w:rPr>
                    </w:rPrDefault>
                    <w:pPrDefault/>
                </w:docDefaults>
                <w:latentStyles w:defLockedState="0" w:defUIPriority="0" w:defSemiHidden="0" w:defUnhideWhenUsed="0" w:defQFormat="0" w:count="376">
                    <w:lsdException w:name="Normal" w:qFormat="1"/>
                    <w:lsdException w:name="heading 1" w:qFormat="1"/>
                    <w:lsdException w:name="heading 2" w:qFormat="1"/>
                    <w:lsdException w:name="heading 3" w:semiHidden="1" w:unhideWhenUsed="1" w:qFormat="1"/>
                    <w:lsdException w:name="heading 4" w:semiHidden="1" w:unhideWhenUsed="1" w:qFormat="1"/>
                    <w:lsdException w:name="heading 5" w:semiHidden="1" w:unhideWhenUsed="1" w:qFormat="1"/>
                    <w:lsdException w:name="heading 6" w:semiHidden="1" w:unhideWhenUsed="1" w:qFormat="1"/>
                    <w:lsdException w:name="heading 7" w:semiHidden="1" w:unhideWhenUsed="1" w:qFormat="1"/>
                    <w:lsdException w:name="heading 8" w:semiHidden="1" w:unhideWhenUsed="1" w:qFormat="1"/>
                    <w:lsdException w:name="heading 9" w:semiHidden="1" w:unhideWhenUsed="1" w:qFormat="1"/>
                    <w:lsdException w:name="header" w:uiPriority="99"/>
                    <w:lsdException w:name="footer" w:uiPriority="99"/>
                    <w:lsdException w:name="caption" w:semiHidden="1" w:unhideWhenUsed="1" w:qFormat="1"/>
                    <w:lsdException w:name="Title" w:qFormat="1"/>
                    <w:lsdException w:name="Default Paragraph Font" w:semiHidden="1" w:uiPriority="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Subtitle" w:qFormat="1"/>
                    <w:lsdException w:name="Strong" w:qFormat="1"/>
                    <w:lsdException w:name="Emphasis" w:qFormat="1"/>
                    <w:lsdException w:name="HTML Top of Form" w:semiHidden="1" w:uiPriority="99" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="HTML Bottom of Form" w:semiHidden="1" w:uiPriority="99" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Normal Table" w:semiHidden="1" w:uiPriority="99" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="No List" w:semiHidden="1" w:uiPriority="99" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Outline List 1" w:semiHidden="1" w:uiPriority="99" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Outline List 2" w:semiHidden="1" w:uiPriority="99" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Outline List 3" w:semiHidden="1" w:uiPriority="99" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Simple 1" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Simple 2" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Simple 3" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Classic 1" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Classic 2" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Classic 3" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Classic 4" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Colorful 1" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Colorful 2" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Colorful 3" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Columns 1" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Columns 2" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Columns 3" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Columns 4" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Columns 5" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Grid 1" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Grid 2" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Grid 3" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Grid 4" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Grid 5" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Grid 6" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Grid 7" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Grid 8" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table List 1" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table List 2" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table List 3" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table List 4" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table List 5" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table List 6" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table List 7" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table List 8" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table 3D effects 1" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table 3D effects 2" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table 3D effects 3" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Contemporary" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Elegant" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Professional" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Subtle 1" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Subtle 2" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Web 1" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Web 2" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Web 3" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Table Grid" w:qFormat="1"/>
                    <w:lsdException w:name="Table Theme" w:semiHidden="1" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Placeholder Text" w:semiHidden="1" w:uiPriority="99" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="No Spacing" w:uiPriority="99" w:qFormat="1"/>
                    <w:lsdException w:name="Light Shading" w:uiPriority="60"/>
                    <w:lsdException w:name="Light List" w:uiPriority="61"/>
                    <w:lsdException w:name="Light Grid" w:uiPriority="62"/>
                    <w:lsdException w:name="Medium Shading 1" w:uiPriority="63"/>
                    <w:lsdException w:name="Medium Shading 2" w:uiPriority="64"/>
                    <w:lsdException w:name="Medium List 1" w:uiPriority="65"/>
                    <w:lsdException w:name="Medium List 2" w:uiPriority="66"/>
                    <w:lsdException w:name="Medium Grid 1" w:uiPriority="67"/>
                    <w:lsdException w:name="Medium Grid 2" w:uiPriority="68"/>
                    <w:lsdException w:name="Medium Grid 3" w:uiPriority="69"/>
                    <w:lsdException w:name="Dark List" w:uiPriority="70"/>
                    <w:lsdException w:name="Colorful Shading" w:uiPriority="71"/>
                    <w:lsdException w:name="Colorful List" w:uiPriority="72"/>
                    <w:lsdException w:name="Colorful Grid" w:uiPriority="73"/>
                    <w:lsdException w:name="Light Shading Accent 1" w:uiPriority="60"/>
                    <w:lsdException w:name="Light List Accent 1" w:uiPriority="61"/>
                    <w:lsdException w:name="Light Grid Accent 1" w:uiPriority="62"/>
                    <w:lsdException w:name="Medium Shading 1 Accent 1" w:uiPriority="63"/>
                    <w:lsdException w:name="Medium Shading 2 Accent 1" w:uiPriority="64"/>
                    <w:lsdException w:name="Medium List 1 Accent 1" w:uiPriority="65"/>
                    <w:lsdException w:name="Revision" w:semiHidden="1" w:uiPriority="99" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="List Paragraph" w:uiPriority="99" w:qFormat="1"/>
                    <w:lsdException w:name="Quote" w:uiPriority="99" w:qFormat="1"/>
                    <w:lsdException w:name="Intense Quote" w:uiPriority="99" w:qFormat="1"/>
                    <w:lsdException w:name="Medium List 2 Accent 1" w:uiPriority="66"/>
                    <w:lsdException w:name="Medium Grid 1 Accent 1" w:uiPriority="67"/>
                    <w:lsdException w:name="Medium Grid 2 Accent 1" w:uiPriority="68"/>
                    <w:lsdException w:name="Medium Grid 3 Accent 1" w:uiPriority="69"/>
                    <w:lsdException w:name="Dark List Accent 1" w:uiPriority="70"/>
                    <w:lsdException w:name="Colorful Shading Accent 1" w:uiPriority="71"/>
                    <w:lsdException w:name="Colorful List Accent 1" w:uiPriority="72"/>
                    <w:lsdException w:name="Colorful Grid Accent 1" w:uiPriority="73"/>
                    <w:lsdException w:name="Light Shading Accent 2" w:uiPriority="60"/>
                    <w:lsdException w:name="Light List Accent 2" w:uiPriority="61"/>
                    <w:lsdException w:name="Light Grid Accent 2" w:uiPriority="62"/>
                    <w:lsdException w:name="Medium Shading 1 Accent 2" w:uiPriority="63"/>
                    <w:lsdException w:name="Medium Shading 2 Accent 2" w:uiPriority="64"/>
                    <w:lsdException w:name="Medium List 1 Accent 2" w:uiPriority="65"/>
                    <w:lsdException w:name="Medium List 2 Accent 2" w:uiPriority="66"/>
                    <w:lsdException w:name="Medium Grid 1 Accent 2" w:uiPriority="67"/>
                    <w:lsdException w:name="Medium Grid 2 Accent 2" w:uiPriority="68"/>
                    <w:lsdException w:name="Medium Grid 3 Accent 2" w:uiPriority="69"/>
                    <w:lsdException w:name="Dark List Accent 2" w:uiPriority="70"/>
                    <w:lsdException w:name="Colorful Shading Accent 2" w:uiPriority="71"/>
                    <w:lsdException w:name="Colorful List Accent 2" w:uiPriority="72"/>
                    <w:lsdException w:name="Colorful Grid Accent 2" w:uiPriority="73"/>
                    <w:lsdException w:name="Light Shading Accent 3" w:uiPriority="60"/>
                    <w:lsdException w:name="Light List Accent 3" w:uiPriority="61"/>
                    <w:lsdException w:name="Light Grid Accent 3" w:uiPriority="62"/>
                    <w:lsdException w:name="Medium Shading 1 Accent 3" w:uiPriority="63"/>
                    <w:lsdException w:name="Medium Shading 2 Accent 3" w:uiPriority="64"/>
                    <w:lsdException w:name="Medium List 1 Accent 3" w:uiPriority="65"/>
                    <w:lsdException w:name="Medium List 2 Accent 3" w:uiPriority="66"/>
                    <w:lsdException w:name="Medium Grid 1 Accent 3" w:uiPriority="67"/>
                    <w:lsdException w:name="Medium Grid 2 Accent 3" w:uiPriority="68"/>
                    <w:lsdException w:name="Medium Grid 3 Accent 3" w:uiPriority="69"/>
                    <w:lsdException w:name="Dark List Accent 3" w:uiPriority="70"/>
                    <w:lsdException w:name="Colorful Shading Accent 3" w:uiPriority="71"/>
                    <w:lsdException w:name="Colorful List Accent 3" w:uiPriority="72"/>
                    <w:lsdException w:name="Colorful Grid Accent 3" w:uiPriority="73"/>
                    <w:lsdException w:name="Light Shading Accent 4" w:uiPriority="60"/>
                    <w:lsdException w:name="Light List Accent 4" w:uiPriority="61"/>
                    <w:lsdException w:name="Light Grid Accent 4" w:uiPriority="62"/>
                    <w:lsdException w:name="Medium Shading 1 Accent 4" w:uiPriority="63"/>
                    <w:lsdException w:name="Medium Shading 2 Accent 4" w:uiPriority="64"/>
                    <w:lsdException w:name="Medium List 1 Accent 4" w:uiPriority="65"/>
                    <w:lsdException w:name="Medium List 2 Accent 4" w:uiPriority="66"/>
                    <w:lsdException w:name="Medium Grid 1 Accent 4" w:uiPriority="67"/>
                    <w:lsdException w:name="Medium Grid 2 Accent 4" w:uiPriority="68"/>
                    <w:lsdException w:name="Medium Grid 3 Accent 4" w:uiPriority="69"/>
                    <w:lsdException w:name="Dark List Accent 4" w:uiPriority="70"/>
                    <w:lsdException w:name="Colorful Shading Accent 4" w:uiPriority="71"/>
                    <w:lsdException w:name="Colorful List Accent 4" w:uiPriority="72"/>
                    <w:lsdException w:name="Colorful Grid Accent 4" w:uiPriority="73"/>
                    <w:lsdException w:name="Light Shading Accent 5" w:uiPriority="60"/>
                    <w:lsdException w:name="Light List Accent 5" w:uiPriority="61"/>
                    <w:lsdException w:name="Light Grid Accent 5" w:uiPriority="62"/>
                    <w:lsdException w:name="Medium Shading 1 Accent 5" w:uiPriority="63"/>
                    <w:lsdException w:name="Medium Shading 2 Accent 5" w:uiPriority="64"/>
                    <w:lsdException w:name="Medium List 1 Accent 5" w:uiPriority="65"/>
                    <w:lsdException w:name="Medium List 2 Accent 5" w:uiPriority="66"/>
                    <w:lsdException w:name="Medium Grid 1 Accent 5" w:uiPriority="67"/>
                    <w:lsdException w:name="Medium Grid 2 Accent 5" w:uiPriority="68"/>
                    <w:lsdException w:name="Medium Grid 3 Accent 5" w:uiPriority="69"/>
                    <w:lsdException w:name="Dark List Accent 5" w:uiPriority="70"/>
                    <w:lsdException w:name="Colorful Shading Accent 5" w:uiPriority="71"/>
                    <w:lsdException w:name="Colorful List Accent 5" w:uiPriority="72"/>
                    <w:lsdException w:name="Colorful Grid Accent 5" w:uiPriority="73"/>
                    <w:lsdException w:name="Light Shading Accent 6" w:uiPriority="60"/>
                    <w:lsdException w:name="Light List Accent 6" w:uiPriority="61"/>
                    <w:lsdException w:name="Light Grid Accent 6" w:uiPriority="62"/>
                    <w:lsdException w:name="Medium Shading 1 Accent 6" w:uiPriority="63"/>
                    <w:lsdException w:name="Medium Shading 2 Accent 6" w:uiPriority="64"/>
                    <w:lsdException w:name="Medium List 1 Accent 6" w:uiPriority="65"/>
                    <w:lsdException w:name="Medium List 2 Accent 6" w:uiPriority="66"/>
                    <w:lsdException w:name="Medium Grid 1 Accent 6" w:uiPriority="67"/>
                    <w:lsdException w:name="Medium Grid 2 Accent 6" w:uiPriority="68"/>
                    <w:lsdException w:name="Medium Grid 3 Accent 6" w:uiPriority="69"/>
                    <w:lsdException w:name="Dark List Accent 6" w:uiPriority="70"/>
                    <w:lsdException w:name="Colorful Shading Accent 6" w:uiPriority="71"/>
                    <w:lsdException w:name="Colorful List Accent 6" w:uiPriority="72"/>
                    <w:lsdException w:name="Colorful Grid Accent 6" w:uiPriority="73"/>
                    <w:lsdException w:name="Subtle Emphasis" w:uiPriority="19" w:qFormat="1"/>
                    <w:lsdException w:name="Intense Emphasis" w:uiPriority="21" w:qFormat="1"/>
                    <w:lsdException w:name="Subtle Reference" w:uiPriority="31" w:qFormat="1"/>
                    <w:lsdException w:name="Intense Reference" w:uiPriority="32" w:qFormat="1"/>
                    <w:lsdException w:name="Book Title" w:uiPriority="33" w:qFormat="1"/>
                    <w:lsdException w:name="Bibliography" w:semiHidden="1" w:uiPriority="37" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="TOC Heading" w:semiHidden="1" w:uiPriority="39" w:unhideWhenUsed="1" w:qFormat="1"/>
                    <w:lsdException w:name="Plain Table 1" w:uiPriority="41"/>
                    <w:lsdException w:name="Plain Table 2" w:uiPriority="42"/>
                    <w:lsdException w:name="Plain Table 3" w:uiPriority="43"/>
                    <w:lsdException w:name="Plain Table 4" w:uiPriority="44"/>
                    <w:lsdException w:name="Plain Table 5" w:uiPriority="45"/>
                    <w:lsdException w:name="Grid Table Light" w:uiPriority="40"/>
                    <w:lsdException w:name="Grid Table 1 Light" w:uiPriority="46"/>
                    <w:lsdException w:name="Grid Table 2" w:uiPriority="47"/>
                    <w:lsdException w:name="Grid Table 3" w:uiPriority="48"/>
                    <w:lsdException w:name="Grid Table 4" w:uiPriority="49"/>
                    <w:lsdException w:name="Grid Table 5 Dark" w:uiPriority="50"/>
                    <w:lsdException w:name="Grid Table 6 Colorful" w:uiPriority="51"/>
                    <w:lsdException w:name="Grid Table 7 Colorful" w:uiPriority="52"/>
                    <w:lsdException w:name="Grid Table 1 Light Accent 1" w:uiPriority="46"/>
                    <w:lsdException w:name="Grid Table 2 Accent 1" w:uiPriority="47"/>
                    <w:lsdException w:name="Grid Table 3 Accent 1" w:uiPriority="48"/>
                    <w:lsdException w:name="Grid Table 4 Accent 1" w:uiPriority="49"/>
                    <w:lsdException w:name="Grid Table 5 Dark Accent 1" w:uiPriority="50"/>
                    <w:lsdException w:name="Grid Table 6 Colorful Accent 1" w:uiPriority="51"/>
                    <w:lsdException w:name="Grid Table 7 Colorful Accent 1" w:uiPriority="52"/>
                    <w:lsdException w:name="Grid Table 1 Light Accent 2" w:uiPriority="46"/>
                    <w:lsdException w:name="Grid Table 2 Accent 2" w:uiPriority="47"/>
                    <w:lsdException w:name="Grid Table 3 Accent 2" w:uiPriority="48"/>
                    <w:lsdException w:name="Grid Table 4 Accent 2" w:uiPriority="49"/>
                    <w:lsdException w:name="Grid Table 5 Dark Accent 2" w:uiPriority="50"/>
                    <w:lsdException w:name="Grid Table 6 Colorful Accent 2" w:uiPriority="51"/>
                    <w:lsdException w:name="Grid Table 7 Colorful Accent 2" w:uiPriority="52"/>
                    <w:lsdException w:name="Grid Table 1 Light Accent 3" w:uiPriority="46"/>
                    <w:lsdException w:name="Grid Table 2 Accent 3" w:uiPriority="47"/>
                    <w:lsdException w:name="Grid Table 3 Accent 3" w:uiPriority="48"/>
                    <w:lsdException w:name="Grid Table 4 Accent 3" w:uiPriority="49"/>
                    <w:lsdException w:name="Grid Table 5 Dark Accent 3" w:uiPriority="50"/>
                    <w:lsdException w:name="Grid Table 6 Colorful Accent 3" w:uiPriority="51"/>
                    <w:lsdException w:name="Grid Table 7 Colorful Accent 3" w:uiPriority="52"/>
                    <w:lsdException w:name="Grid Table 1 Light Accent 4" w:uiPriority="46"/>
                    <w:lsdException w:name="Grid Table 2 Accent 4" w:uiPriority="47"/>
                    <w:lsdException w:name="Grid Table 3 Accent 4" w:uiPriority="48"/>
                    <w:lsdException w:name="Grid Table 4 Accent 4" w:uiPriority="49"/>
                    <w:lsdException w:name="Grid Table 5 Dark Accent 4" w:uiPriority="50"/>
                    <w:lsdException w:name="Grid Table 6 Colorful Accent 4" w:uiPriority="51"/>
                    <w:lsdException w:name="Grid Table 7 Colorful Accent 4" w:uiPriority="52"/>
                    <w:lsdException w:name="Grid Table 1 Light Accent 5" w:uiPriority="46"/>
                    <w:lsdException w:name="Grid Table 2 Accent 5" w:uiPriority="47"/>
                    <w:lsdException w:name="Grid Table 3 Accent 5" w:uiPriority="48"/>
                    <w:lsdException w:name="Grid Table 4 Accent 5" w:uiPriority="49"/>
                    <w:lsdException w:name="Grid Table 5 Dark Accent 5" w:uiPriority="50"/>
                    <w:lsdException w:name="Grid Table 6 Colorful Accent 5" w:uiPriority="51"/>
                    <w:lsdException w:name="Grid Table 7 Colorful Accent 5" w:uiPriority="52"/>
                    <w:lsdException w:name="Grid Table 1 Light Accent 6" w:uiPriority="46"/>
                    <w:lsdException w:name="Grid Table 2 Accent 6" w:uiPriority="47"/>
                    <w:lsdException w:name="Grid Table 3 Accent 6" w:uiPriority="48"/>
                    <w:lsdException w:name="Grid Table 4 Accent 6" w:uiPriority="49"/>
                    <w:lsdException w:name="Grid Table 5 Dark Accent 6" w:uiPriority="50"/>
                    <w:lsdException w:name="Grid Table 6 Colorful Accent 6" w:uiPriority="51"/>
                    <w:lsdException w:name="Grid Table 7 Colorful Accent 6" w:uiPriority="52"/>
                    <w:lsdException w:name="List Table 1 Light" w:uiPriority="46"/>
                    <w:lsdException w:name="List Table 2" w:uiPriority="47"/>
                    <w:lsdException w:name="List Table 3" w:uiPriority="48"/>
                    <w:lsdException w:name="List Table 4" w:uiPriority="49"/>
                    <w:lsdException w:name="List Table 5 Dark" w:uiPriority="50"/>
                    <w:lsdException w:name="List Table 6 Colorful" w:uiPriority="51"/>
                    <w:lsdException w:name="List Table 7 Colorful" w:uiPriority="52"/>
                    <w:lsdException w:name="List Table 1 Light Accent 1" w:uiPriority="46"/>
                    <w:lsdException w:name="List Table 2 Accent 1" w:uiPriority="47"/>
                    <w:lsdException w:name="List Table 3 Accent 1" w:uiPriority="48"/>
                    <w:lsdException w:name="List Table 4 Accent 1" w:uiPriority="49"/>
                    <w:lsdException w:name="List Table 5 Dark Accent 1" w:uiPriority="50"/>
                    <w:lsdException w:name="List Table 6 Colorful Accent 1" w:uiPriority="51"/>
                    <w:lsdException w:name="List Table 7 Colorful Accent 1" w:uiPriority="52"/>
                    <w:lsdException w:name="List Table 1 Light Accent 2" w:uiPriority="46"/>
                    <w:lsdException w:name="List Table 2 Accent 2" w:uiPriority="47"/>
                    <w:lsdException w:name="List Table 3 Accent 2" w:uiPriority="48"/>
                    <w:lsdException w:name="List Table 4 Accent 2" w:uiPriority="49"/>
                    <w:lsdException w:name="List Table 5 Dark Accent 2" w:uiPriority="50"/>
                    <w:lsdException w:name="List Table 6 Colorful Accent 2" w:uiPriority="51"/>
                    <w:lsdException w:name="List Table 7 Colorful Accent 2" w:uiPriority="52"/>
                    <w:lsdException w:name="List Table 1 Light Accent 3" w:uiPriority="46"/>
                    <w:lsdException w:name="List Table 2 Accent 3" w:uiPriority="47"/>
                    <w:lsdException w:name="List Table 3 Accent 3" w:uiPriority="48"/>
                    <w:lsdException w:name="List Table 4 Accent 3" w:uiPriority="49"/>
                    <w:lsdException w:name="List Table 5 Dark Accent 3" w:uiPriority="50"/>
                    <w:lsdException w:name="List Table 6 Colorful Accent 3" w:uiPriority="51"/>
                    <w:lsdException w:name="List Table 7 Colorful Accent 3" w:uiPriority="52"/>
                    <w:lsdException w:name="List Table 1 Light Accent 4" w:uiPriority="46"/>
                    <w:lsdException w:name="List Table 2 Accent 4" w:uiPriority="47"/>
                    <w:lsdException w:name="List Table 3 Accent 4" w:uiPriority="48"/>
                    <w:lsdException w:name="List Table 4 Accent 4" w:uiPriority="49"/>
                    <w:lsdException w:name="List Table 5 Dark Accent 4" w:uiPriority="50"/>
                    <w:lsdException w:name="List Table 6 Colorful Accent 4" w:uiPriority="51"/>
                    <w:lsdException w:name="List Table 7 Colorful Accent 4" w:uiPriority="52"/>
                    <w:lsdException w:name="List Table 1 Light Accent 5" w:uiPriority="46"/>
                    <w:lsdException w:name="List Table 2 Accent 5" w:uiPriority="47"/>
                    <w:lsdException w:name="List Table 3 Accent 5" w:uiPriority="48"/>
                    <w:lsdException w:name="List Table 4 Accent 5" w:uiPriority="49"/>
                    <w:lsdException w:name="List Table 5 Dark Accent 5" w:uiPriority="50"/>
                    <w:lsdException w:name="List Table 6 Colorful Accent 5" w:uiPriority="51"/>
                    <w:lsdException w:name="List Table 7 Colorful Accent 5" w:uiPriority="52"/>
                    <w:lsdException w:name="List Table 1 Light Accent 6" w:uiPriority="46"/>
                    <w:lsdException w:name="List Table 2 Accent 6" w:uiPriority="47"/>
                    <w:lsdException w:name="List Table 3 Accent 6" w:uiPriority="48"/>
                    <w:lsdException w:name="List Table 4 Accent 6" w:uiPriority="49"/>
                    <w:lsdException w:name="List Table 5 Dark Accent 6" w:uiPriority="50"/>
                    <w:lsdException w:name="List Table 6 Colorful Accent 6" w:uiPriority="51"/>
                    <w:lsdException w:name="List Table 7 Colorful Accent 6" w:uiPriority="52"/>
                    <w:lsdException w:name="Mention" w:semiHidden="1" w:uiPriority="99" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Smart Hyperlink" w:semiHidden="1" w:uiPriority="99" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Hashtag" w:semiHidden="1" w:uiPriority="99" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Unresolved Mention" w:semiHidden="1" w:uiPriority="99" w:unhideWhenUsed="1"/>
                    <w:lsdException w:name="Smart Link" w:semiHidden="1" w:uiPriority="99" w:unhideWhenUsed="1"/>
                </w:latentStyles>
                <w:style w:type="paragraph" w:default="1" w:styleId="a">
                    <w:name w:val="Normal"/>
                    <w:qFormat/>
                    <w:pPr>
                        <w:widowControl w:val="0"/>
                        <w:jc w:val="both"/>
                    </w:pPr>
                    <w:rPr>
                        <w:kern w:val="2"/>
                        <w:sz w:val="21"/>
                        <w:szCs w:val="24"/>
                    </w:rPr>
                </w:style>
                <w:style w:type="paragraph" w:styleId="1">
                    <w:name w:val="heading 1"/>
                    <w:basedOn w:val="a"/>
                    <w:next w:val="a"/>
                    <w:qFormat/>
                    <w:pPr>
                        <w:keepNext/>
                        <w:keepLines/>
                        <w:spacing w:before="340" w:after="330" w:line="576" w:lineRule="auto"/>
                        <w:outlineLvl w:val="0"/>
                    </w:pPr>
                    <w:rPr>
                        <w:b/>
                        <w:kern w:val="44"/>
                        <w:sz w:val="44"/>
                    </w:rPr>
                </w:style>
                <w:style w:type="paragraph" w:styleId="2">
                    <w:name w:val="heading 2"/>
                    <w:basedOn w:val="a"/>
                    <w:next w:val="a"/>
                    <w:qFormat/>
                    <w:pPr>
                        <w:keepNext/>
                        <w:keepLines/>
                        <w:spacing w:before="260" w:after="260" w:line="413" w:lineRule="auto"/>
                        <w:outlineLvl w:val="1"/>
                    </w:pPr>
                    <w:rPr>
                        <w:rFonts w:ascii="Arial" w:eastAsia="黑体" w:hAnsi="Arial"/>
                        <w:b/>
                        <w:sz w:val="32"/>
                    </w:rPr>
                </w:style>
                <w:style w:type="character" w:default="1" w:styleId="a0">
                    <w:name w:val="Default Paragraph Font"/>
                    <w:uiPriority w:val="1"/>
                    <w:unhideWhenUsed/>
                </w:style>
                <w:style w:type="table" w:default="1" w:styleId="a1">
                    <w:name w:val="Normal Table"/>
                    <w:uiPriority w:val="99"/>
                    <w:unhideWhenUsed/>
                    <w:tblPr>
                        <w:tblCellMar>
                            <w:top w:w="0" w:type="dxa"/>
                            <w:left w:w="108" w:type="dxa"/>
                            <w:bottom w:w="0" w:type="dxa"/>
                            <w:right w:w="108" w:type="dxa"/>
                        </w:tblCellMar>
                    </w:tblPr>
                </w:style>
                <w:style w:type="numbering" w:default="1" w:styleId="a2">
                    <w:name w:val="No List"/>
                    <w:uiPriority w:val="99"/>
                    <w:semiHidden/>
                    <w:unhideWhenUsed/>
                </w:style>
                <w:style w:type="character" w:styleId="a3">
                    <w:name w:val="Hyperlink"/>
                    <w:rPr>
                        <w:color w:val="0563C1"/>
                        <w:u w:val="single"/>
                    </w:rPr>
                </w:style>
                <w:style w:type="character" w:customStyle="1" w:styleId="a4">
                    <w:name w:val="页眉 字符"/>
                    <w:link w:val="a5"/>
                    <w:uiPriority w:val="99"/>
                    <w:rPr>
                        <w:kern w:val="2"/>
                        <w:sz w:val="18"/>
                        <w:szCs w:val="18"/>
                    </w:rPr>
                </w:style>
                <w:style w:type="character" w:styleId="a6">
                    <w:name w:val="Unresolved Mention"/>
                    <w:uiPriority w:val="99"/>
                    <w:unhideWhenUsed/>
                    <w:rPr>
                        <w:color w:val="605E5C"/>
                        <w:shd w:val="clear" w:color="auto" w:fill="E1DFDD"/>
                    </w:rPr>
                </w:style>
                <w:style w:type="character" w:customStyle="1" w:styleId="a7">
                    <w:name w:val="页脚 字符"/>
                    <w:link w:val="a8"/>
                    <w:uiPriority w:val="99"/>
                    <w:rPr>
                        <w:kern w:val="2"/>
                        <w:sz w:val="18"/>
                        <w:szCs w:val="18"/>
                    </w:rPr>
                </w:style>
                <w:style w:type="paragraph" w:styleId="a8">
                    <w:name w:val="footer"/>
                    <w:basedOn w:val="a"/>
                    <w:link w:val="a7"/>
                    <w:uiPriority w:val="99"/>
                    <w:pPr>
                        <w:tabs>
                            <w:tab w:val="center" w:pos="4153"/>
                            <w:tab w:val="right" w:pos="8306"/>
                        </w:tabs>
                        <w:snapToGrid w:val="0"/>
                        <w:jc w:val="left"/>
                    </w:pPr>
                    <w:rPr>
                        <w:sz w:val="18"/>
                        <w:szCs w:val="18"/>
                    </w:rPr>
                </w:style>
                <w:style w:type="paragraph" w:styleId="a5">
                    <w:name w:val="header"/>
                    <w:basedOn w:val="a"/>
                    <w:link w:val="a4"/>
                    <w:uiPriority w:val="99"/>
                    <w:pPr>
                        <w:pBdr>
                            <w:bottom w:val="single" w:sz="6" w:space="1" w:color="auto"/>
                        </w:pBdr>
                        <w:tabs>
                            <w:tab w:val="center" w:pos="4153"/>
                            <w:tab w:val="right" w:pos="8306"/>
                        </w:tabs>
                        <w:snapToGrid w:val="0"/>
                        <w:jc w:val="center"/>
                    </w:pPr>
                    <w:rPr>
                        <w:sz w:val="18"/>
                        <w:szCs w:val="18"/>
                    </w:rPr>
                </w:style>
                <w:style w:type="table" w:styleId="a9">
                    <w:name w:val="Table Grid"/>
                    <w:basedOn w:val="a1"/>
                    <w:qFormat/>
                    <w:pPr>
                        <w:widowControl w:val="0"/>
                        <w:jc w:val="both"/>
                    </w:pPr>
                    <w:tblPr>
                        <w:tblBorders>
                            <w:top w:val="single" w:sz="4" w:space="0" w:color="auto"/>
                            <w:left w:val="single" w:sz="4" w:space="0" w:color="auto"/>
                            <w:bottom w:val="single" w:sz="4" w:space="0" w:color="auto"/>
                            <w:right w:val="single" w:sz="4" w:space="0" w:color="auto"/>
                            <w:insideH w:val="single" w:sz="4" w:space="0" w:color="auto"/>
                            <w:insideV w:val="single" w:sz="4" w:space="0" w:color="auto"/>
                        </w:tblBorders>
                    </w:tblPr>
                </w:style>
            </w:styles>
        </pkg:xmlData>
    </pkg:part>
    <pkg:part pkg:name="/word/webSettings.xml" pkg:contentType="application/vnd.openxmlformats-officedocument.wordprocessingml.webSettings+xml">
        <pkg:xmlData>
            <w:webSettings xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006"
                    xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                    xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml"
                    xmlns:w15="http://schemas.microsoft.com/office/word/2012/wordml"
                    mc:Ignorable="w14 w15 w16se w16cid w16 w16cex">
                <w:encoding w:val="x-cp20936"/>
                <w:optimizeForBrowser/>
                <w:allowPNG/>
            </w:webSettings>
        </pkg:xmlData>
    </pkg:part>
    <pkg:part pkg:name="/word/fontTable.xml" pkg:contentType="application/vnd.openxmlformats-officedocument.wordprocessingml.fontTable+xml">
        <pkg:xmlData>
            <w:fonts xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006"
                    xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                    xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml"
                    xmlns:w15="http://schemas.microsoft.com/office/word/2012/wordml"
                    mc:Ignorable="w14 w15 w16se w16cid w16 w16cex">
                <w:font w:name="Calibri">
                    <w:panose1 w:val="020F0502020204030204"/>
                    <w:charset w:val="00"/>
                    <w:family w:val="swiss"/>
                    <w:pitch w:val="variable"/>
                    <w:sig w:usb0="E4002EFF" w:usb1="C000247B" w:usb2="00000009" w:usb3="00000000" w:csb0="000001FF" w:csb1="00000000"/>
                </w:font>
                <w:font w:name="宋体">
                    <w:altName w:val="SimSun"/>
                    <w:panose1 w:val="02010600030101010101"/>
                    <w:charset w:val="86"/>
                    <w:family w:val="auto"/>
                    <w:pitch w:val="variable"/>
                    <w:sig w:usb0="00000003" w:usb1="288F0000" w:usb2="00000016" w:usb3="00000000" w:csb0="00040001" w:csb1="00000000"/>
                </w:font>
                <w:font w:name="Times New Roman">
                    <w:panose1 w:val="02020603050405020304"/>
                    <w:charset w:val="00"/>
                    <w:family w:val="roman"/>
                    <w:pitch w:val="variable"/>
                    <w:sig w:usb0="E0002EFF" w:usb1="C000785B" w:usb2="00000009" w:usb3="00000000" w:csb0="000001FF" w:csb1="00000000"/>
                </w:font>
                <w:font w:name="Arial">
                    <w:panose1 w:val="020B0604020202020204"/>
                    <w:charset w:val="00"/>
                    <w:family w:val="swiss"/>
                    <w:pitch w:val="variable"/>
                    <w:sig w:usb0="E0002EFF" w:usb1="C000785B" w:usb2="00000009" w:usb3="00000000" w:csb0="000001FF" w:csb1="00000000"/>
                </w:font>
                <w:font w:name="黑体">
                    <w:altName w:val="SimHei"/>
                    <w:panose1 w:val="02010609060101010101"/>
                    <w:charset w:val="86"/>
                    <w:family w:val="modern"/>
                    <w:pitch w:val="fixed"/>
                    <w:sig w:usb0="800002BF" w:usb1="38CF7CFA" w:usb2="00000016" w:usb3="00000000" w:csb0="00040001" w:csb1="00000000"/>
                </w:font>
                <w:font w:name="等线 Light">
                    <w:panose1 w:val="02010600030101010101"/>
                    <w:charset w:val="86"/>
                    <w:family w:val="auto"/>
                    <w:pitch w:val="variable"/>
                    <w:sig w:usb0="A00002BF" w:usb1="38CF7CFA" w:usb2="00000016" w:usb3="00000000" w:csb0="0004000F" w:csb1="00000000"/>
                </w:font>
                <w:font w:name="等线">
                    <w:altName w:val="DengXian"/>
                    <w:panose1 w:val="02010600030101010101"/>
                    <w:charset w:val="86"/>
                    <w:family w:val="auto"/>
                    <w:pitch w:val="variable"/>
                    <w:sig w:usb0="A00002BF" w:usb1="38CF7CFA" w:usb2="00000016" w:usb3="00000000" w:csb0="0004000F" w:csb1="00000000"/>
                </w:font>
            </w:fonts>
        </pkg:xmlData>
    </pkg:part>
    <pkg:part pkg:name="/docProps/core.xml" pkg:contentType="application/vnd.openxmlformats-package.core-properties+xml" pkg:padding="256">
        <pkg:xmlData>
            <cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>${title!'数据库表结构文档'}</dc:title>
                <dc:subject/>
                <dc:creator>screw</dc:creator>
                <cp:keywords/>
                <cp:lastModifiedBy>screw</cp:lastModifiedBy>
                <cp:revision>2</cp:revision>
            </cp:coreProperties>
        </pkg:xmlData>
    </pkg:part>
    <pkg:part pkg:name="/docProps/app.xml" pkg:contentType="application/vnd.openxmlformats-officedocument.extended-properties+xml" pkg:padding="256">
        <pkg:xmlData>
            <Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
                <Template>Normal.dotm</Template>
                <TotalTime>0</TotalTime>
                <Pages>1</Pages>
                <Words>17</Words>
                <Characters>100</Characters>
                <Application>Microsoft Office Word</Application>
                <DocSecurity>0</DocSecurity>
                <Lines>1</Lines>
                <Paragraphs>1</Paragraphs>
                <ScaleCrop>false</ScaleCrop>
                <Company/>
                <LinksUpToDate>false</LinksUpToDate>
                <CharactersWithSpaces>116</CharactersWithSpaces>
                <SharedDoc>false</SharedDoc>
                <HyperlinksChanged>false</HyperlinksChanged>
                <AppVersion>16.0000</AppVersion>
            </Properties>
        </pkg:xmlData>
    </pkg:part>
    <pkg:part pkg:name="/docProps/custom.xml" pkg:contentType="application/vnd.openxmlformats-officedocument.custom-properties+xml" pkg:padding="256">
        <pkg:xmlData>
            <Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/custom-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
                <property fmtid="{D5CDD505-2E9C-101B-9397-08002B2CF9AE}" pid="2" name="KSOProductBuildVer">
                    <vt:lpwstr>2052-11.1.0.9740</vt:lpwstr>
                </property>
            </Properties>
        </pkg:xmlData>
    </pkg:part>
</pkg:package>
