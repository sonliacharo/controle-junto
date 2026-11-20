package br.ucsal.controlejunto.controller;

import br.ucsal.controlejunto.dto.GrupoDTOs.*;
import br.ucsal.controlejunto.security.UsuarioLogado;
import br.ucsal.controlejunto.service.GrupoService;
import br.ucsal.controlejunto.service.NotificacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class GrupoController {

    private final GrupoService grupos;
    private final NotificacaoService notificacoes;

    public GrupoController(GrupoService grupos, NotificacaoService notificacoes) {
        this.grupos = grupos;
        this.notificacoes = notificacoes;
    }

    @GetMapping("/grupos")
    public List<GrupoResumoResponse> listar() {
        return grupos.listarMeusGrupos(UsuarioLogado.id());
    }

    @PostMapping("/grupos")
    @ResponseStatus(HttpStatus.CREATED)
    public GrupoDetalheResponse criar(@Valid @RequestBody GrupoRequest req) {
        return grupos.criar(UsuarioLogado.id(), req);
    }

    @GetMapping("/grupos/{id}")
    public GrupoDetalheResponse detalhar(@PathVariable Long id) {
        return grupos.detalhar(UsuarioLogado.id(), id);
    }

    @DeleteMapping("/grupos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        grupos.excluir(UsuarioLogado.id(), id);
    }

    @PostMapping("/grupos/{id}/participantes")
    public GrupoDetalheResponse adicionarParticipante(@PathVariable Long id,
                                                      @Valid @RequestBody ParticipanteRequest req) {
        return grupos.adicionarParticipante(UsuarioLogado.id(), id, req);
    }

    @DeleteMapping("/grupos/{id}/participantes/{usuarioId}")
    public GrupoDetalheResponse removerParticipante(@PathVariable Long id, @PathVariable Long usuarioId) {
        return grupos.removerParticipante(UsuarioLogado.id(), id, usuarioId);
    }

    @PostMapping("/grupos/{id}/despesas")
    @ResponseStatus(HttpStatus.CREATED)
    public GrupoDetalheResponse registrarDespesa(@PathVariable Long id, @Valid @RequestBody DespesaRequest req) {
        return grupos.registrarDespesa(UsuarioLogado.id(), id, req);
    }

    /** RN-002: só quem registrou a despesa pode editá-la. */
    @PutMapping("/grupos/{id}/despesas/{despesaId}")
    public GrupoDetalheResponse editarDespesa(@PathVariable Long id, @PathVariable Long despesaId,
                                              @Valid @RequestBody DespesaRequest req) {
        return grupos.editarDespesa(UsuarioLogado.id(), id, despesaId, req);
    }

    /** RN-002: só quem registrou a despesa pode excluí-la. */
    @DeleteMapping("/grupos/{id}/despesas/{despesaId}")
    public GrupoDetalheResponse excluirDespesa(@PathVariable Long id, @PathVariable Long despesaId) {
        return grupos.excluirDespesa(UsuarioLogado.id(), id, despesaId);
    }

    @PostMapping("/grupos/{id}/encerrar")
    public GrupoDetalheResponse encerrar(@PathVariable Long id) {
        return grupos.encerrar(UsuarioLogado.id(), id);
    }

    @PostMapping("/dividas/{id}/quitar")
    public GrupoDetalheResponse quitar(@PathVariable Long id) {
        return grupos.quitarDivida(UsuarioLogado.id(), id);
    }

    @GetMapping("/notificacoes")
    public List<NotificacaoResponse> notificacoes() {
        return notificacoes.listar(UsuarioLogado.id());
    }

    @PostMapping("/notificacoes/{id}/lida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarComoLida(@PathVariable Long id) {
        notificacoes.marcarComoLida(UsuarioLogado.id(), id);
    }
}
