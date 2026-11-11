package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.ProfitReportResponse;
import com.devsam.urbantrend.Service.Auth.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/profit")
    @PreAuthorize("hasAuthority('CAN_VIEW_PROFIT_REPORT')")
    public ProfitReportResponse getProfitReport(
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        LocalDateTime fromDate = LocalDateTime.parse(from);
        LocalDateTime toDate = LocalDateTime.parse(to);
        return reportService.getProfitReport(fromDate, toDate);
    }
}