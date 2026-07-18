package com.cutflow.service;

import com.cutflow.dto.planodecorte.ChapaUtilizadaResponse;
import com.cutflow.dto.planodecorte.PlanoDeCorteResponse;
import com.cutflow.dto.planodecorte.PosicionamentoResponse;
import com.cutflow.dto.planodecorte.SobraResponse;
import com.cutflow.entity.Projeto;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gera o PDF do plano de corte para uso na oficina, sem depender de internet
 * no momento do corte (doc secao 3.6, requisito nao funcional). Isolado do
 * PlanoDeCorteService de proposito, no mesmo padrao do BudgetPdfService do
 * FlowOps: montagem de documento e' uma responsabilidade bem diferente de
 * regra de negocio, e OpenPDF (com.lowagie.text) nao deveria vazar para o
 * resto da camada de servico.
 */
@Service
public class PlanoDeCortePdfService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));

    public byte[] generate(Projeto projeto, PlanoDeCorteResponse plano) {
        Document document = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font subtitleFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
            Font labelFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 10);
            Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            Font chapaTitleFont = new Font(Font.HELVETICA, 12, Font.BOLD);

            document.add(new Paragraph("CutFlow — Plano de Corte", titleFont));
            document.add(new Paragraph(projeto.getNome()
                    + (projeto.getCliente() != null ? " — " + projeto.getCliente() : ""), subtitleFont));
            document.add(new Paragraph(" "));

            document.add(labeledLine("Gerado em:", DATE_FORMAT.format(plano.geradoEm()), labelFont, normalFont));
            document.add(labeledLine("Chapas necessárias:", String.valueOf(plano.totalChapasUtilizadas()), labelFont, normalFont));
            document.add(labeledLine("Aproveitamento:", plano.percentualAproveitamento() + "%", labelFont, normalFont));
            document.add(labeledLine("Desperdício:", plano.percentualDesperdicio() + "%", labelFont, normalFont));
            document.add(new Paragraph(" "));

            for (ChapaUtilizadaResponse chapa : plano.chapas()) {
                document.add(new Paragraph("Chapa %d — %dx%dmm, %dmm"
                        .formatted(chapa.numeroChapa(), chapa.larguraMm(), chapa.alturaMm(), chapa.espessuraMm()),
                        chapaTitleFont));
                document.add(new Paragraph("Aproveitamento: %s%%".formatted(chapa.percentualAproveitamento()), normalFont));
                document.add(new Paragraph(" "));

                document.add(tabelaPecas(chapa.posicionamentos(), headerFont, normalFont));

                if (!chapa.sobras().isEmpty()) {
                    document.add(new Paragraph(" "));
                    document.add(new Paragraph("Sobras aproveitáveis desta chapa:", labelFont));
                    document.add(tabelaSobras(chapa.sobras(), headerFont, normalFont));
                }

                document.add(new Paragraph(" "));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar PDF do plano de corte", e);
        } finally {
            document.close();
        }

        return out.toByteArray();
    }

    private PdfPTable tabelaPecas(List<PosicionamentoResponse> posicionamentos, Font headerFont, Font normalFont) {
        PdfPTable table = new PdfPTable(new float[]{0.7f, 3f, 1.3f, 1.3f, 1f});
        table.setWidthPercentage(100);

        addHeaderCell(table, "#", headerFont);
        addHeaderCell(table, "Peça", headerFont);
        addHeaderCell(table, "Largura x Altura", headerFont);
        addHeaderCell(table, "Posição (x, y)", headerFont);
        addHeaderCell(table, "Girada", headerFont);

        for (PosicionamentoResponse p : posicionamentos) {
            table.addCell(new PdfPCell(new Phrase(String.valueOf(p.numeroEtiqueta()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(p.nomePeca(), normalFont)));
            table.addCell(new PdfPCell(new Phrase("%dx%dmm".formatted(p.larguraMm(), p.alturaMm()), normalFont)));
            table.addCell(new PdfPCell(new Phrase("%d, %d".formatted(p.xMm(), p.yMm()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(Boolean.TRUE.equals(p.rotacionada()) ? "Sim" : "Não", normalFont)));
        }
        return table;
    }

    private PdfPTable tabelaSobras(List<SobraResponse> sobras, Font headerFont, Font normalFont) {
        PdfPTable table = new PdfPTable(new float[]{2f, 2f});
        table.setWidthPercentage(50);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);

        addHeaderCell(table, "Medida", headerFont);
        addHeaderCell(table, "Posição (x, y)", headerFont);

        for (SobraResponse s : sobras) {
            table.addCell(new PdfPCell(new Phrase("%dx%dmm".formatted(s.larguraMm(), s.alturaMm()), normalFont)));
            table.addCell(new PdfPCell(new Phrase("%d, %d".formatted(s.xMm(), s.yMm()), normalFont)));
        }
        return table;
    }

    private Paragraph labeledLine(String label, String value, Font labelFont, Font normalFont) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + " ", labelFont));
        p.add(new Chunk(value, normalFont));
        return p;
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(180, 83, 9)); // tom amber, coerente com a identidade CutFlow
        cell.setPadding(5);
        table.addCell(cell);
    }
}
