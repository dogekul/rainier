/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.service;

import com.rainier.opportunity.domain.ArtifactType;
import com.rainier.opportunity.domain.Opportunity;
import com.rainier.opportunity.domain.OpportunityArtifact;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

/**
 * Renders a 流转产出物 to a Word .docx via Apache POI (v0.0.45). Title + a metadata block
 * (类型/客户·商机/阶段/决策/作者/时间) + the content body. Returns raw bytes (never touches disk).
 */
final class DocxWriter {

  static byte[] write(OpportunityArtifact a, Opportunity o) {
    try (XWPFDocument doc = new XWPFDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      // Title
      XWPFParagraph title = doc.createParagraph();
      title.setAlignment(ParagraphAlignment.CENTER);
      XWPFRun tr = title.createRun();
      tr.setBold(true);
      tr.setFontSize(18);
      tr.setText(a.getTitle() == null ? "" : a.getTitle());

      // Metadata
      meta(doc, "类型", ArtifactType.label(a.getType()));
      meta(doc, "客户·商机", o == null ? "—" : (nz(o.getCustomerName()) + " · " + nz(o.getTitle())));
      meta(doc, "阶段", nz(a.getStageFrom()));
      if (a.getDecision() != null && !a.getDecision().isEmpty()) {
        meta(doc, "决策", a.getDecision());
      }
      if (a.getLink() != null && !a.getLink().isEmpty()) {
        meta(doc, "链接", a.getLink());
      }
      meta(doc, "作者", nz(a.getCreateBy()));
      meta(doc, "时间", a.getCreateTime() == null ? "—" : a.getCreateTime().toString());

      // Spacer
      doc.createParagraph();

      // Body — one paragraph per line (preserve the author's line breaks)
      String content = a.getContent() == null ? "" : a.getContent();
      for (String line : content.split("\n", -1)) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText(line);
      }

      doc.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("docx generation failed for artifact " + a.getId(), e);
    }
  }

  private static void meta(XWPFDocument doc, String label, String value) {
    XWPFParagraph p = doc.createParagraph();
    XWPFRun k = p.createRun();
    k.setBold(true);
    k.setText(label + "：");
    XWPFRun v = p.createRun();
    v.setText(value);
  }

  private static String nz(String s) {
    return s == null ? "—" : s;
  }

  private DocxWriter() {}
}
