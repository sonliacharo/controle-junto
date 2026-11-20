package br.ucsal.controlejunto.controller;

import br.ucsal.controlejunto.security.UsuarioLogado;
import br.ucsal.controlejunto.service.RelatorioService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class RelatorioController {

    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    /** RF-015: baixa o relatório do mês em PDF. */
    @GetMapping("/relatorios/mensal")
    public ResponseEntity<byte[]> mensal(@RequestParam(required = false) Integer mes,
                                         @RequestParam(required = false) Integer ano) {
        LocalDate hoje = LocalDate.now();
        int m = mes != null ? mes : hoje.getMonthValue();
        int a = ano != null ? ano : hoje.getYear();
        byte[] pdf = service.relatorioMensal(UsuarioLogado.id(), m, a);
        String arquivo = String.format("controlejunto-relatorio-%d-%02d.pdf", a, m);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(arquivo).build().toString())
                .body(pdf);
    }
}
