package cl.cmq.salud.repository.local

import cl.cmq.salud.model.Documento
import cl.cmq.salud.model.TipoDocumento
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

/** PDF de una página con fuentes estándar. No depende de servicios, red ni un motor de impresión. */
internal object DocumentoEjemploPdf {
    private val codificacion = charset("windows-1252")

    fun escribir(documento: Documento, destino: File) {
        val pagina = Pagina()
        with(pagina) {
            rectangulo(0f, 0f, 595f, 105f, "0 0.502 0.537")
            texto("CMQ SALUD", 44f, 50f, 26f, "1 1 1", true)
            texto("GESTIÓN DOCUMENTAL - ANTECEDENTES DEL FUNCIONARIO", 44f, 78f, 10f, "1 1 1")
            texto("COPIA DE MUESTRA", 44f, 132f, 10f, TURQUESA, true)
            var y = parrafo(documento.titulo, 153f, 23f, true) + 22f
            rectangulo(36f, y - 4f, 523f, 158f, "0.933 0.969 0.969")
            listOf("Titular" to documento.titular, "Centro" to documento.centro,
                "Tipo" to documento.tipo.etiqueta, "Fecha de registro" to documento.fechaCarga,
                "Referencia" to "CMQ-DEMO-${documento.id.toString().padStart(4, '0')}").forEach { (etiqueta, valor) ->
                texto(etiqueta, 50f, y + 18f, 10f, GRIS, true)
                texto(valor, 165f, y + 18f, 11f)
                y += 29f
            }
            y += 38f
            y = parrafo(when (documento.tipo) {
                TipoDocumento.CERTIFICADO -> "Constancia de capacitación"
                TipoDocumento.CONTRATO -> "Antecedente laboral"
                TipoDocumento.ANTECEDENTE -> "Registro administrativo"
            }, y, 15f, true) + 15f
            val contenido = when (documento.tipo) {
                TipoDocumento.CERTIFICADO -> "Se deja constancia de la participación de ${documento.titular} en una actividad de capacitación de ejemplo sobre inducción y gestión documental, asociada a ${documento.centro}."
                TipoDocumento.CONTRATO -> "El presente ejemplar representa un antecedente de la relación laboral de ${documento.titular}, asociado a ${documento.centro}. Contiene información ilustrativa para la consulta y revisión de documentos dentro de la aplicación."
                TipoDocumento.ANTECEDENTE -> "Se registra este antecedente administrativo a nombre de ${documento.titular}, perteneciente a ${documento.centro}, para su incorporación al expediente documental y posterior revisión."
            }
            y = parrafo(contenido, y) + 20f
            parrafo("Este archivo acompaña al registro «${documento.titulo}». Su aprobación u observaciones pueden consultarse en el detalle y en el historial de la aplicación.", y)
            linea(44f, 690f, 290f, 690f, TURQUESA)
            texto("Unidad de Gestión Documental", 44f, 711f, 11f, GRAFITO, true)
            texto("CMQ Salud - Ejemplar académico", 44f, 730f, 10f, GRIS)
            linea(44f, 770f, 551f, 770f, "0.878 0.914 0.918")
            texto("DOCUMENTO DE DEMOSTRACIÓN - SIN VALIDEZ LABORAL NI CLÍNICA", 44f, 791f, 9f, GRIS)
            texto("1 / 1", 525f, 811f, 9f, GRIS)
        }
        guardar(pagina.contenido.toString(), destino)
    }

    private class Pagina {
        val contenido = StringBuilder()
        fun rectangulo(x: Float, y: Float, ancho: Float, alto: Float, color: String) {
            contenido.append("$color rg ${n(x)} ${n(842f - y - alto)} ${n(ancho)} ${n(alto)} re f\n")
        }
        fun linea(x1: Float, y1: Float, x2: Float, y2: Float, color: String) {
            contenido.append("$color RG 1 w ${n(x1)} ${n(842f - y1)} m ${n(x2)} ${n(842f - y2)} l S\n")
        }
        fun texto(valor: String, x: Float, y: Float, tamano: Float, color: String = GRAFITO, negrita: Boolean = false) {
            val fuente = if (negrita) "F2" else "F1"
            contenido.append("BT /$fuente ${n(tamano)} Tf $color rg 1 0 0 1 ${n(x)} ${n(842f - y)} Tm (${escapar(valor)}) Tj ET\n")
        }
        fun parrafo(valor: String, y: Float, tamano: Float = 12f, negrita: Boolean = false): Float {
            val lineas = mutableListOf<String>()
            var actual = ""
            valor.split(Regex("\\s+")).forEach { palabra ->
                val candidata = if (actual.isEmpty()) palabra else "$actual $palabra"
                if (ancho(candidata, tamano, negrita) > 507f && actual.isNotEmpty()) {
                    lineas += actual; actual = palabra
                } else actual = candidata
                // También partir títulos sin espacios, manteniendo el texto dentro de la hoja.
                while (ancho(actual, tamano, negrita) > 507f && actual.length > 1) {
                    var corte = actual.length - 1
                    while (corte > 1 && ancho(actual.take(corte), tamano, negrita) > 507f) corte--
                    lineas += actual.take(corte); actual = actual.drop(corte)
                }
            }
            if (actual.isNotEmpty()) lineas += actual
            lineas.forEachIndexed { i, linea -> texto(linea, 44f, y + tamano + i * (tamano + 5f), tamano, negrita = negrita) }
            return y + lineas.size * (tamano + 5f)
        }
    }

    private fun escapar(texto: String): String = buildString {
        texto.toByteArray(codificacion).forEach { byte ->
            val valor = byte.toInt() and 255
            when (valor) {
                40, 41, 92 -> { append('\\'); append(valor.toChar()) }
                in 32..126 -> append(valor.toChar())
                else -> { append('\\'); append(valor.toString(8).padStart(3, '0')) }
            }
        }
    }
    private fun ancho(texto: String, tamano: Float, negrita: Boolean): Float {
        val metricas = if (negrita) HelveticaMetricas.negrita else HelveticaMetricas.normal
        return texto.toByteArray(codificacion).sumOf { metricas[it.toInt() and 255] } * tamano / 1000f
    }
    private fun n(valor: Float) = String.format(Locale.US, "%.2f", valor)

    private fun guardar(contenido: String, destino: File) {
        val datos = contenido.toByteArray(Charsets.US_ASCII)
        val objetos = listOf(
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> /Contents 6 0 R >>",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>",
            "<< /Length ${datos.size} >>\nstream\n$contenido" + "endstream"
        )
        val salida = ByteArrayOutputStream()
        fun escribir(valor: String) { salida.write(valor.toByteArray(Charsets.US_ASCII)) }
        escribir("%PDF-1.4\n")
        val posiciones = mutableListOf(0)
        objetos.forEachIndexed { i, objeto ->
            posiciones += salida.size()
            escribir("${i + 1} 0 obj\n$objeto\nendobj\n")
        }
        val inicioXref = salida.size()
        escribir("xref\n0 ${objetos.size + 1}\n0000000000 65535 f \n")
        posiciones.drop(1).forEach { escribir("${it.toString().padStart(10, '0')} 00000 n \n") }
        escribir("trailer\n<< /Size ${objetos.size + 1} /Root 1 0 R >>\nstartxref\n$inicioXref\n%%EOF\n")
        destino.outputStream().use { salida.writeTo(it) }
    }
    private const val TURQUESA = "0 0.502 0.537"
    private const val GRAFITO = "0.133 0.169 0.188"
    private const val GRIS = "0.369 0.424 0.451"
}
