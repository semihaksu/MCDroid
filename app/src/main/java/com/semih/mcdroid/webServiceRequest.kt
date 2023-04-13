package com.semih.mcdroid

import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

class WebServiceRequest(private val xmlData: String) {
    fun getElementsByTagName(tagName: String): Array<Element> {
        val documentBuilderFactory = DocumentBuilderFactory.newInstance()
        val builder = documentBuilderFactory.newDocumentBuilder()
        val inputSource = InputSource(StringReader(xmlData))
        val document = builder.parse(inputSource)

        // Get the root element (DataSet)
        val dataSetElement = document.documentElement

        // Get the elements based on the specified tag name
        val elements = dataSetElement.getElementsByTagName(tagName)

        return Array(elements.length) { i -> elements.item(i) as Element }
    }
}