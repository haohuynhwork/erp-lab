package com.erplab.report.http;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erplab.report.application.DashboardQueries;
import com.erplab.report.application.DashboardQueries.Dashboard;
import com.erplab.report.application.ExcelExports;
import com.erplab.report.application.ExcelExports.Report;
import com.erplab.report.application.InvoicePdf;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "report")
@RestController
@RequestMapping("/reports")
class ReportController {

	private static final MediaType XLSX = MediaType
		.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private final DashboardQueries dashboard;

	private final ExcelExports excel;

	private final InvoicePdf invoicePdf;

	ReportController(DashboardQueries dashboard, ExcelExports excel, InvoicePdf invoicePdf) {
		this.dashboard = dashboard;
		this.excel = excel;
		this.invoicePdf = invoicePdf;
	}

	@GetMapping("/dashboard")
	@PreAuthorize("hasAuthority('reports:read')")
	Dashboard dashboard() {
		return dashboard.dashboard();
	}

	/** @param report stock, sales-orders, purchase-orders, invoices, trial-balance */
	@GetMapping("/exports/{report}")
	@PreAuthorize("hasAuthority('reports:read')")
	ResponseEntity<byte[]> export(@PathVariable String report) {
		var r = java.util.Arrays.stream(Report.values())
			.filter(x -> x.name().equalsIgnoreCase(report.replace('-', '_')))
			.findFirst()
			.orElseThrow(() -> new com.erplab.common.domain.NotFoundException("Report", report));
		return file(excel.export(r), r.fileName(), XLSX);
	}

	@GetMapping("/invoices/{id}.pdf")
	@PreAuthorize("hasAuthority('sales:read') or hasAuthority('accounting:read')")
	ResponseEntity<byte[]> invoicePdf(@PathVariable Long id) {
		return file(invoicePdf.render(id), invoicePdf.fileName(id), MediaType.APPLICATION_PDF);
	}

	private static ResponseEntity<byte[]> file(byte[] body, String name, MediaType type) {
		return ResponseEntity.ok()
			.contentType(type)
			.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
			.body(body);
	}

}
