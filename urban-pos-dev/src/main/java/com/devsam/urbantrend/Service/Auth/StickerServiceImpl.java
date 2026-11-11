package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.InventoryRequest;
import com.devsam.urbantrend.Dtos.StickerBatch;
import com.devsam.urbantrend.Entity.Product;
import com.devsam.urbantrend.Entity.StockUnit;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.StickerLayout;
import com.devsam.urbantrend.Enum.StockUnitStatus;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.ProductRepository;
import com.devsam.urbantrend.Repository.StockUnitRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfBoolean;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StickerServiceImpl implements StickerService {

    private static final int MAX_STICKERS_PER_BATCH = 5000;

    private static final float MM = 72f / 25.4f;
    // Roll label is 60mm wide x 40mm high (landscape). The page matches it exactly so the
    // printer neither scales nor rotates; content is drawn upright.
    private static final float ROLL_WIDTH = 60 * MM;
    private static final float ROLL_HEIGHT = 40 * MM;
    private static final float ROLL_SIDE_MARGIN = 2.5f * MM;
    // Thermal printers tend to clip the leading edge, so keep extra space at the top.
    private static final float ROLL_TOP_MARGIN = 4 * MM;
    private static final float ROLL_BOTTOM_MARGIN = 3 * MM;
    private static final int A4_COLUMNS = 3;
    private static final int A4_ROWS = 8;

    private final StockUnitRepository stockUnitRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    @Override
    @Transactional
    public StickerBatch restockWithStickers(InventoryRequest request, StickerLayout layout, User user) {
        int quantity = toWholeQuantity(request.getQuantity());
        inventoryService.restock(request, user);
        return createStickers(request.getProductId(), quantity, layout);
    }

    @Override
    @Transactional
    public StickerBatch createStickers(UUID productId, int quantity, StickerLayout layout) {
        if (quantity <= 0 || quantity > MAX_STICKERS_PER_BATCH) {
            throw new IllegalArgumentException("Sticker quantity must be between 1 and " + MAX_STICKERS_PER_BATCH);
        }

        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        UUID batchId = UUID.randomUUID();
        List<StockUnit> units = new ArrayList<>(quantity);

        for (Number next : stockUnitRepository.nextSerialNumbers(quantity)) {
            units.add(StockUnit.builder()
                    .product(product)
                    .serial(String.format("UT%08d", next.longValue()))
                    .batchId(batchId)
                    .status(StockUnitStatus.IN_STOCK)
                    .build());
        }

        stockUnitRepository.saveAll(units);
        return new StickerBatch(batchId, units.size(), render(units, layout));
    }

    @Override
    @Transactional(readOnly = true)
    public StickerBatch reprintBatch(UUID batchId, StickerLayout layout) {
        List<StockUnit> units = stockUnitRepository.findByBatchIdOrderBySerialAsc(batchId);
        if (units.isEmpty()) {
            throw new ResourceNotFoundException("Sticker batch not found");
        }
        return new StickerBatch(batchId, units.size(), render(units, layout));
    }

    private int toWholeQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Restock quantity must be greater than zero");
        }
        try {
            return quantity.intValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Stickers can only be printed for a whole number of units");
        }
    }

    private byte[] render(List<StockUnit> units, StickerLayout layout) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean roll = layout == StickerLayout.ROLL;

        Rectangle rollLabel = new Rectangle(ROLL_WIDTH, ROLL_HEIGHT);
        rollLabel.setRotation(0);

        Document document = roll
                ? new Document(rollLabel, 0, 0, 0, 0)
                : new Document(PageSize.A4, 18, 18, 18, 18);
        PdfWriter writer = PdfWriter.getInstance(document, out);
        if (roll) {
            // Ask the print dialog to keep the label at actual size and pick media by PDF size,
            // so drivers don't auto-rotate/scale the 60x40 page.
            writer.addViewerPreference(PdfName.PRINTSCALING, PdfName.NONE);
            writer.addViewerPreference(PdfName.PICKTRAYBYPDFSIZE, PdfBoolean.PDFTRUE);
        }
        document.open();

        PdfContentByte canvas = writer.getDirectContent();
        if (roll) {
            for (int i = 0; i < units.size(); i++) {
                if (i > 0) {
                    document.newPage();
                }
                drawRollLabel(units.get(i), canvas);
            }
        } else {
            float cellHeight = (PageSize.A4.getHeight() - 36) / A4_ROWS;
            PdfPTable table = new PdfPTable(A4_COLUMNS);
            table.setWidthPercentage(100);
            for (StockUnit unit : units) {
                table.addCell(stickerCell(unit, canvas, cellHeight));
            }
            table.completeRow();
            document.add(table);
        }

        document.close();
        return out.toByteArray();
    }

    private void drawRollLabel(StockUnit unit, PdfContentByte canvas) {
        Product product = unit.getProduct();
        float usableWidth = ROLL_WIDTH - 2 * ROLL_SIDE_MARGIN;
        float centerX = ROLL_WIDTH / 2;

        BaseFont bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD).getBaseFont();
        float nameSize = 9;
        float nameLeading = nameSize + 2;
        float priceSize = 11;

        // Product name at the top (one line on a wide label, two on a tall one)
        int maxNameLines = ROLL_HEIGHT > ROLL_WIDTH ? 2 : 1;
        List<String> nameLines = wrapText(product.getName(), bold, nameSize, usableWidth, maxNameLines);
        float y = ROLL_HEIGHT - ROLL_TOP_MARGIN - nameSize;
        canvas.beginText();
        canvas.setFontAndSize(bold, nameSize);
        for (String line : nameLines) {
            canvas.showTextAligned(Element.ALIGN_CENTER, line, centerX, y, 0);
            y -= nameLeading;
        }
        canvas.endText();
        float nameBottom = y + nameLeading - 3;

        // Price at the bottom
        String price = "KES " + new DecimalFormat("#,##0.##").format(product.getSellingPrice());
        canvas.beginText();
        canvas.setFontAndSize(bold, priceSize);
        canvas.showTextAligned(Element.ALIGN_CENTER, price, centerX, ROLL_BOTTOM_MARGIN, 0);
        canvas.endText();

        // Barcode (bars vertical, serial underneath) centred in the space between
        float bandBottom = ROLL_BOTTOM_MARGIN + priceSize + 2 * MM;
        float bandTop = nameBottom - 2 * MM;
        float bandHeight = bandTop - bandBottom;

        Barcode128 barcode = new Barcode128();
        barcode.setCode(unit.getSerial());
        barcode.setSize(9);
        barcode.setBaseline(9);
        barcode.setX(usableWidth / barcodeModules(barcode));
        barcode.setBarHeight(Math.min(22 * MM, bandHeight - barcode.getSize() - barcode.getBaseline()));
        Image barcodeImage = barcode.createImageWithBarcode(canvas, null, null);
        barcodeImage.scaleToFit(usableWidth, bandHeight);
        barcodeImage.setAbsolutePosition(
                centerX - barcodeImage.getScaledWidth() / 2,
                bandBottom + (bandHeight - barcodeImage.getScaledHeight()) / 2
        );
        canvas.addImage(barcodeImage);
    }

    // Width of the barcode in modules (narrow bars), so it can be sized to fill the label width.
    private float barcodeModules(Barcode128 barcode) {
        float x = barcode.getX();
        return barcode.getBarcodeSize().getWidth() / x;
    }

    private List<String> wrapText(String text, BaseFont font, float size, float maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }
        String[] words = text.trim().split("\\s+");
        StringBuilder current = new StringBuilder();
        int i = 0;
        while (i < words.length && lines.size() < maxLines - 1) {
            String candidate = current.isEmpty() ? words[i] : current + " " + words[i];
            if (font.getWidthPoint(candidate, size) <= maxWidth || current.isEmpty()) {
                current = new StringBuilder(candidate);
                i++;
            } else {
                lines.add(current.toString());
                current = new StringBuilder();
            }
        }
        StringBuilder last = new StringBuilder(current);
        for (; i < words.length; i++) {
            last.append(last.isEmpty() ? "" : " ").append(words[i]);
        }
        if (!last.isEmpty()) {
            lines.add(fitText(last.toString(), font, size, maxWidth));
        }
        for (int j = 0; j < lines.size(); j++) {
            lines.set(j, fitText(lines.get(j), font, size, maxWidth));
        }
        return lines;
    }

    private String fitText(String text, BaseFont font, float size, float maxWidth) {
        if (text == null) {
            return "";
        }
        if (font.getWidthPoint(text, size) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int end = text.length();
        while (end > 0 && font.getWidthPoint(text.substring(0, end) + ellipsis, size) > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    private PdfPCell stickerCell(StockUnit unit, PdfContentByte canvas, float height) {
        Product product = unit.getProduct();

        Barcode128 barcode = new Barcode128();
        barcode.setCode(unit.getSerial());
        barcode.setBarHeight(height * 0.38f);
        Image barcodeImage = barcode.createImageWithBarcode(canvas, null, null);
        barcodeImage.setAlignment(Element.ALIGN_CENTER);

        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(height);
        cell.setPadding(3);
        cell.setBorderWidth(0.25f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph name = new Paragraph(product.getName(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7));
        name.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(name);

        cell.addElement(barcodeImage);

        Paragraph price = new Paragraph(
                "KES " + new DecimalFormat("#,##0.##").format(product.getSellingPrice()),
                FontFactory.getFont(FontFactory.HELVETICA, 7)
        );
        price.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(price);

        return cell;
    }
}
