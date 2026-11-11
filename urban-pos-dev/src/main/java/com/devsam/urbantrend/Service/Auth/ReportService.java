package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.ProfitReportResponse;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public interface ReportService {
    ProfitReportResponse getProfitReport(LocalDateTime from, LocalDateTime  to);
}
