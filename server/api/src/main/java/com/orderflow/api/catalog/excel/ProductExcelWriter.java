package com.orderflow.api.catalog.excel;

import static com.orderflow.api.catalog.excel.ProductExcelLayout.DOWNLOAD_HEADERS;

import com.orderflow.domain.catalog.ProductStatus;
import com.orderflow.infra.catalog.ProductSummary;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * 상품 엑셀 다운로드 생성 (US-CAT-03, api-spec 3.3.1·3.3.3) — SXSSF 스트리밍 쓰기
 * (윈도 100행만 메모리 유지). 업로드 열 레이아웃과 왕복 호환 — A~F는 그대로 재업로드 가능.
 */
@Component
public class ProductExcelWriter {

    private static final int STREAMING_WINDOW = 100;

    public void write(List<ProductSummary> products, OutputStream out) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(STREAMING_WINDOW)) {
            Sheet sheet = workbook.createSheet("상품");
            writeHeader(sheet);
            for (int i = 0; i < products.size(); i++) {
                writeProduct(sheet.createRow(i + 1), products.get(i));
            }
            workbook.write(out);
            workbook.dispose(); // SXSSF 임시 파일 정리
        } catch (IOException e) {
            throw new UncheckedIOException("엑셀 다운로드 생성 실패", e);
        }
    }

    private void writeHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        for (int col = 0; col < DOWNLOAD_HEADERS.size(); col++) {
            header.createCell(col).setCellValue(DOWNLOAD_HEADERS.get(col));
        }
    }

    private void writeProduct(Row row, ProductSummary product) {
        // 품목코드·바코드는 문자열 셀 — 숫자 셀이면 엑셀이 지수 표기로 표시한다
        row.createCell(ProductExcelLayout.COL_PRODUCT_CODE).setCellValue(product.productCode());
        row.createCell(ProductExcelLayout.COL_NAME).setCellValue(product.name());
        row.createCell(ProductExcelLayout.COL_BARCODE).setCellValue(product.barcode());
        row.createCell(ProductExcelLayout.COL_CATEGORY).setCellValue(product.category());
        row.createCell(ProductExcelLayout.COL_ORDER_UNIT).setCellValue(product.orderUnit());
        row.createCell(ProductExcelLayout.COL_UNIT_PRICE).setCellValue(product.unitPrice().longValueExact());
        row.createCell(6).setCellValue(product.limited() ? "Y" : "N");
        if (product.availableQty() != null) {
            row.createCell(7).setCellValue(product.availableQty());
        }
        row.createCell(8).setCellValue(product.status() == ProductStatus.ON_SALE ? "판매중" : "판매중지");
    }
}
