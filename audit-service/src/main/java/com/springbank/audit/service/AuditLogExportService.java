package com.springbank.audit.service;

import com.springbank.audit.entity.AuditLog;
import com.springbank.audit.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableScheduling
public class AuditLogExportService {

    private final AuditLogRepository auditLogRepository;

    public Page<AuditLog> searchLogs(String username, String action, String entityType, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (username != null && !username.isEmpty()) {
                predicates.add(cb.equal(root.get("actorUsername"), username));
            }
            if (action != null && !action.isEmpty()) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (entityType != null && !entityType.isEmpty()) {
                predicates.add(cb.equal(root.get("entityType"), entityType));
            }
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), start));
            }
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), end));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return auditLogRepository.findAll(spec, pageable);
    }

    public ByteArrayInputStream exportToExcel(String username, String action, String entityType, LocalDateTime start, LocalDateTime end) {
        Page<AuditLog> logs = searchLogs(username, action, entityType, start, end, Pageable.unpaged());
        return generateExcel(logs.getContent());
    }

    private ByteArrayInputStream generateExcel(List<AuditLog> logs) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Audit Logs");

            // Header
            Row headerRow = sheet.createRow(0);
            String[] columns = {"ID", "Actor Username", "Action", "Entity Type", "Entity ID", "IP Address", "Timestamp", "Reason"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
            }

            // Data
            int rowIdx = 1;
            for (AuditLog logEntry : logs) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(logEntry.getId() != null ? logEntry.getId().toString() : "");
                row.createCell(1).setCellValue(logEntry.getActorUsername() != null ? logEntry.getActorUsername() : "");
                row.createCell(2).setCellValue(logEntry.getAction() != null ? logEntry.getAction() : "");
                row.createCell(3).setCellValue(logEntry.getEntityType() != null ? logEntry.getEntityType() : "");
                row.createCell(4).setCellValue(logEntry.getEntityId() != null ? logEntry.getEntityId().toString() : "");
                row.createCell(5).setCellValue(logEntry.getIpAddress() != null ? logEntry.getIpAddress() : "");
                row.createCell(6).setCellValue(logEntry.getTimestamp() != null ? logEntry.getTimestamp().toString() : "");
                row.createCell(7).setCellValue(logEntry.getReason() != null ? logEntry.getReason() : "");
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            log.error("Error exporting audit logs to Excel", e);
            throw new RuntimeException("Failed to export Excel", e);
        }
    }

    // Run on the 1st day of every month at 00:00:00
    @Scheduled(cron = "0 0 0 1 * ?")
    public void generateMonthlyReport() {
        log.info("Generating automated monthly audit log report...");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfLastMonth = now.minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfLastMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).minusSeconds(1);

        ByteArrayInputStream excelData = exportToExcel(null, null, null, startOfLastMonth, endOfLastMonth);

        File dir = new File("/home/user/reports");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = "AuditReport_" + startOfLastMonth.format(DateTimeFormatter.ofPattern("yyyy_MM")) + ".xlsx";
        File file = new File(dir, fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = excelData.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
            log.info("Monthly audit log report saved successfully at: {}", file.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to save monthly audit log report", e);
        }
    }
}
