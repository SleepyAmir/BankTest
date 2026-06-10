package com.springbank.transaction.read.service;

import com.springbank.transaction.read.entity.Transaction;
import com.springbank.transaction.read.repository.TransactionRepository;
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
public class TransactionExportService {

    private final TransactionRepository transactionRepository;

    public Page<Transaction> searchTransactions(Long userId, Long accountId, String status, String type, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (accountId != null) {
                Predicate fromAcc = cb.equal(root.get("fromAccountId"), accountId);
                Predicate toAcc = cb.equal(root.get("toAccountId"), accountId);
                predicates.add(cb.or(fromAcc, toAcc));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (type != null && !type.isEmpty()) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            }
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return transactionRepository.findAll(spec, pageable);
    }

    public ByteArrayInputStream exportToExcel(Long userId, Long accountId, String status, String type, LocalDateTime start, LocalDateTime end) {
        Page<Transaction> txs = searchTransactions(userId, accountId, status, type, start, end, Pageable.unpaged());
        return generateExcel(txs.getContent());
    }

    private ByteArrayInputStream generateExcel(List<Transaction> txs) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Transactions");

            Row headerRow = sheet.createRow(0);
            String[] columns = {"ID", "Tracking Code", "Type", "Status", "Amount", "Currency", "From Account", "To Account", "User ID", "Created At"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
            }

            int rowIdx = 1;
            for (Transaction tx : txs) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(tx.getId() != null ? tx.getId().toString() : "");
                row.createCell(1).setCellValue(tx.getTrackingCode() != null ? tx.getTrackingCode() : "");
                row.createCell(2).setCellValue(tx.getType() != null ? tx.getType().toString() : "");
                row.createCell(3).setCellValue(tx.getStatus() != null ? tx.getStatus().toString() : "");
                row.createCell(4).setCellValue(tx.getAmount() != null ? tx.getAmount().toString() : "");
                row.createCell(5).setCellValue(tx.getCurrency() != null ? tx.getCurrency() : "");
                row.createCell(6).setCellValue(tx.getFromAccountId() != null ? tx.getFromAccountId().toString() : "");
                row.createCell(7).setCellValue(tx.getToAccountId() != null ? tx.getToAccountId().toString() : "");
                row.createCell(8).setCellValue(tx.getUserId() != null ? tx.getUserId().toString() : "");
                row.createCell(9).setCellValue(tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : "");
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            log.error("Error exporting transactions to Excel", e);
            throw new RuntimeException("Failed to export Excel", e);
        }
    }

    // Run on the 1st day of every month at 00:00:00
    @Scheduled(cron = "0 0 0 1 * ?")
    public void generateMonthlyReport() {
        log.info("Generating automated monthly transaction report...");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfLastMonth = now.minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfLastMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).minusSeconds(1);

        ByteArrayInputStream excelData = exportToExcel(null, null, null, null, startOfLastMonth, endOfLastMonth);

        File dir = new File("/home/user/reports");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = "TransactionReport_" + startOfLastMonth.format(DateTimeFormatter.ofPattern("yyyy_MM")) + ".xlsx";
        File file = new File(dir, fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = excelData.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
            log.info("Monthly transaction report saved successfully at: {}", file.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to save monthly transaction report", e);
        }
    }
}
