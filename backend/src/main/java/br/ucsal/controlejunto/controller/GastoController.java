package br.ucsal.controlejunto.controller;

import br.ucsal.controlejunto.dto.GastoDTOs.*;
import br.ucsal.controlejunto.security.UsuarioLogado;
import br.ucsal.controlejunto.service.GastoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class GastoController {

    private final GastoService service;

    public GastoController(GastoService service) {
        this.service = service;
    }

    @GetMapping("/categorias")
    public List<CategoriaResponse> categorias() {
        return service.listarCategorias();
    }

    /** RF-010: sugere a categoria a partir da descrição. */
    @PostMapping("/categorias/sugestao")
    public SugestaoResponse sugerir(@Valid @RequestBody SugestaoRequest req) {
        return service.sugerirCategoria(req.descricao());
    }

    /**
     * RF-014: lista os gastos do mês, com filtros opcionais por categoria e por grupo.
     * origem: PESSOAL (padrão), GRUPOS ou TODOS. Com grupoId, lista só a parte do
     * usuário nas despesas daquele grupo.
     */
    @GetMapping("/gastos")
    public List<GastoResponse> listar(@RequestParam(required = false) Integer mes,
                                      @RequestParam(required = false) Integer ano,
                                      @RequestParam(required = false) Long categoriaId,
                                      @RequestParam(defaultValue = "PESSOAL") GastoService.Origem origem,
                                      @RequestParam(required = false) Long grupoId) {
        LocalDate hoje = LocalDate.now();
        return service.listar(UsuarioLogado.id(),
                mes != null ? mes : hoje.getMonthValue(),
                ano != null ? ano : hoje.getYear(),
                categoriaId, origem, grupoId);
    }

    @PostMapping("/gastos")
    @ResponseStatus(HttpStatus.CREATED)
    public GastoResponse criar(@Valid @RequestBody GastoRequest req) {
        return service.criar(UsuarioLogado.id(), req);
    }

    @PutMapping("/gastos/{id}")
    public GastoResponse atualizar(@PathVariable Long id, @Valid @RequestBody GastoRequest req) {
        return service.atualizar(UsuarioLogado.id(), id, req);
    }

    @DeleteMapping("/gastos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(UsuarioLogado.id(), id);
    }
}
