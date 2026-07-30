package org.superquinquin.report;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

/** Templates du rapport : {@code src/main/resources/templates/reports/dailyDlc.html}. */
@CheckedTemplate(basePath = "reports")
public class ReportTemplates {

    public static native TemplateInstance dailyDlc(ReportData data);
}
