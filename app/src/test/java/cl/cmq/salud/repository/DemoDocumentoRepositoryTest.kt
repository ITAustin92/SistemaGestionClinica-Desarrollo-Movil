package cl.cmq.salud.repository

import cl.cmq.salud.model.*
import org.junit.Assert.*
import org.junit.Test

class DemoDocumentoRepositoryTest {
    @Test fun funcionarioNoPuedeConsultarOtroExpediente() {
        val repository = DemoDocumentoRepository()
        val funcionario = repository.usuarios.first()
        assertTrue(repository.documentosVisibles(funcionario).all { it.titularId == funcionario.id })
        assertNull(repository.buscar(funcionario, 4))
    }
    @Test fun revisorSoloPuedeConsultarSuCentro() {
        val repository = DemoDocumentoRepository()
        assertNull(repository.buscar(repository.usuarios.last(), 5))
    }
    @Test fun rechazoSinMotivoNoModificaElDocumento() {
        val repository = DemoDocumentoRepository()
        val revisor = repository.usuarios.last()
        assertThrows(IllegalArgumentException::class.java) { repository.revisar(revisor, 1, EstadoDocumento.RECHAZADO, "   ") }
        assertEquals(EstadoDocumento.PENDIENTE, repository.buscar(revisor, 1)?.estado)
    }
    @Test fun decisionTerminalNoSeSobrescribe() {
        val repository = DemoDocumentoRepository()
        val revisor = repository.usuarios.last()
        repository.revisar(revisor, 1, EstadoDocumento.APROBADO, "")
        assertThrows(IllegalArgumentException::class.java) { repository.revisar(revisor, 1, EstadoDocumento.RECHAZADO, "Otro motivo") }
        assertEquals(EstadoDocumento.APROBADO, repository.buscar(revisor, 1)?.estado)
        assertEquals(2, repository.buscar(revisor, 1)?.historial?.size)
    }
    @Test fun altaValidaApareceSoloEnElAmbitoAutorizado() {
        val repository = DemoDocumentoRepository()
        val funcionario = repository.usuarios[0]
        val id = repository.registrar(funcionario, "Nuevo certificado", TipoDocumento.CERTIFICADO, "", Adjunto("demo.pdf", "PDF"))
        assertNotNull(repository.buscar(funcionario, id))
        assertNull(repository.buscar(repository.usuarios[1], id))
        assertNotNull(repository.buscar(repository.usuarios.last(), id))
    }
}
