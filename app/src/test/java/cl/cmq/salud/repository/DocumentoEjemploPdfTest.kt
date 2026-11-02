package cl.cmq.salud.repository

import cl.cmq.salud.repository.local.DocumentoEjemploPdf
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class DocumentoEjemploPdfTest {
    @Test fun ejemplosInicialesSonPdfConTablaDeReferenciasValida() {
        val directorio = File(requireNotNull(System.getProperty("cmq.test.pdfReportDir"))).apply { mkdirs() }
        DemoDocumentoRepository().documentos.value.forEach { documento ->
            val pdf = File(directorio, "documento_${documento.id}.pdf")
            DocumentoEjemploPdf.escribir(documento, pdf)
            val texto = pdf.readText(Charsets.US_ASCII)
            assertTrue(texto.startsWith("%PDF-1.4"))
            assertTrue(texto.endsWith("%%EOF\n"))
            assertTrue(texto.contains("/Count 1"))
            val inicio = texto.substringAfterLast("startxref\n").substringBefore('\n').toInt()
            assertEquals("xref", texto.substring(inicio, inicio + 4))
            assertTrue(texto.contains("CMQ-DEMO-${documento.id.toString().padStart(4, '0')}"))
        }
    }

    @Test fun textoDeUsuarioEscapaDelimitadoresYConservaAcentos() {
        val pdf = File.createTempFile("ejemplo_escapado", ".pdf")
        try {
            val documento = DemoDocumentoRepository().documentos.value.first().copy(titulo = "Contrato (copia) \\ archivo")
            DocumentoEjemploPdf.escribir(documento, pdf)
            val texto = pdf.readText(Charsets.US_ASCII)
            assertTrue(texto.contains("Contrato \\(copia\\) \\\\ archivo"))
            assertTrue(texto.contains("\\301ngel Quintero"))
        } finally { pdf.delete() }
    }
}
