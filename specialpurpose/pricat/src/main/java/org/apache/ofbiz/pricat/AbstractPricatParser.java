/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.pricat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.poi.hssf.usermodel.HSSFDataFormatter;
import org.apache.poi.ss.usermodel.ClientAnchor.AnchorType;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.OFBizPricatUtil;
import org.apache.poi.xssf.usermodel.XSSFAnchor;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFCreationHelper;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.commons.fileupload.FileItem;

import org.apache.ofbiz.htmlreport.InterfaceReport;
import org.apache.ofbiz.order.finaccount.FinAccountHelper;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Abstract class of pricat parser.
 * 
 */
public abstract class AbstractPricatParser implements InterfacePricatParser {
	
	public static final String module = AbstractPricatParser.class.getName();
	
	protected LocalDispatcher dispatcher;
	
	protected Delegator delegator;
	
	protected List<FileItem> fileItems;
	
	protected File pricatFile;
	
	protected String userLoginId;
	
	protected GenericValue userLogin;
	
	protected String pricatFileVersion;
	
	protected String currencyId;
	
	protected Map<CellReference, String> errorMessages = new HashMap<CellReference, String>();
	
	protected HSSFDataFormatter formatter = new HSSFDataFormatter();
	
	protected Map<String, String[]> facilities = new HashMap<String, String[]>();
	
    protected HttpSession session;
    
    protected List<EntityCondition> basicCategoryConds;
    
    protected List<EntityCondition> basicBrandConds;
    
    protected String selectedPricatType = DEFAULT_PRICAT_TYPE;
    
    protected String selectedFacilityId;
    
    protected InterfaceReport report;
    
    protected Locale locale;
    
    protected long sequenceNum = -1L;

    public AbstractPricatParser(LocalDispatcher dispatcher, Delegator delegator, Locale locale, InterfaceReport report, Map<String, String[]> facilities, File pricatFile, GenericValue userLogin) {
    	this.dispatcher = dispatcher;
    	this.delegator = delegator;
    	this.locale = locale;
    	this.report = report;
    	this.userLogin = userLogin;
    	if (UtilValidate.isNotEmpty(userLogin)) {
    		this.userLoginId = userLogin.getString("userLoginId");
    	}
    	this.facilities = facilities;
    	this.pricatFile = pricatFile;
		initBasicConds(UtilMisc.toList(userLogin.getString("partyId")));
    }
    
	public void writeCommentsToFile(XSSFWorkbook workbook, XSSFSheet sheet) {
		report.println();
		report.print(UtilProperties.getMessage(resource, "WriteCommentsBackToExcel", locale), InterfaceReport.FORMAT_NOTE);
		FileOutputStream fos = null;
		XSSFCreationHelper factory = workbook.getCreationHelper();
		XSSFFont boldFont = workbook.createFont();
		boldFont.setFontName("Arial");
		boldFont.setBold(true);
		boldFont.setCharSet(134);
		boldFont.setFontHeightInPoints((short) 9);
		XSSFFont plainFont = workbook.createFont();
		plainFont.setFontName("Arial");
		plainFont.setCharSet(134);
		plainFont.setFontHeightInPoints((short) 9);
		
		XSSFSheet errorSheet = null;
		if (errorMessages.keySet().size() > 0) {
			String errorSheetName = UtilDateTime.nowDateString("yyyy-MM-dd HHmm") + " Errors";
			errorSheetName = WorkbookUtil.createSafeSheetName(errorSheetName);
			errorSheet = workbook.createSheet(errorSheetName);
			workbook.setSheetOrder(errorSheetName, 0);
			workbook.setActiveSheet(workbook.getSheetIndex(errorSheetName));
			XSSFDrawing drawingPatriarch = errorSheet.getDrawingPatriarch();
			if (drawingPatriarch == null) {
				drawingPatriarch = errorSheet.createDrawingPatriarch();
			}
			for (int i = 0; i <= getHeaderRowNo(); i++) {
				XSSFRow newRow = errorSheet.createRow(i);
				XSSFRow row = sheet.getRow(i);
				newRow.setHeight(row.getHeight());
				copyRow(row, newRow, factory, drawingPatriarch);
			}
			
			// copy merged regions
			for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
			    CellRangeAddress mergedRegion = sheet.getMergedRegion(i);
			    if (mergedRegion.getFirstRow() < getHeaderRowNo()) {
				    errorSheet.addMergedRegion(mergedRegion);
			    }
			}
			
			// copy images
			List<XSSFPictureData> pics = workbook.getAllPictures();
			List<XSSFShape> shapes = sheet.getDrawingPatriarch().getShapes();
			for (int i = 0; i < shapes.size(); i++) {
				XSSFShape shape = shapes.get(i);
				XSSFAnchor anchor = shape.getAnchor();
				if (shape instanceof XSSFPicture && anchor instanceof XSSFClientAnchor) {
					XSSFPicture pic = (XSSFPicture) shape;
					XSSFClientAnchor clientAnchor = (XSSFClientAnchor) anchor;
					if (clientAnchor.getRow1() < getHeaderRowNo()) {
						for (int j = 0; j < pics.size(); j++) {
							XSSFPictureData picture = pics.get(j);
							if (picture.getPackagePart().getPartName().equals(pic.getPictureData().getPackagePart().getPartName())) {
								drawingPatriarch.createPicture(clientAnchor, j);
							}
						}
					}
				}
			}
		}
		
		try {
			// set comments in the original sheet
			XSSFDrawing patriarch = sheet.getDrawingPatriarch();
			for (CellReference cell : errorMessages.keySet()) {
				if (cell != null && errorMessages.get(cell) != null) {
					XSSFComment comment = sheet.getCellComment(new CellAddress(cell.getRow(), cell.getCol()));
					boolean isNewComment = false;
					if (comment == null) {
						XSSFClientAnchor anchor = factory.createClientAnchor();
						anchor.setDx1(100);
						anchor.setDx2(100);
						anchor.setDy1(100);
						anchor.setDy2(100);
					    anchor.setCol1(cell.getCol());
					    anchor.setCol2(cell.getCol() + 4);
					    anchor.setRow1(cell.getRow());
					    anchor.setRow2(cell.getRow() + 4);
					    anchor.setAnchorType(AnchorType.DONT_MOVE_AND_RESIZE);

					    comment = patriarch.createCellComment(anchor);
						isNewComment = true;
					}
					XSSFRichTextString rts = factory.createRichTextString("OFBiz PriCat:\n");
					rts.applyFont(boldFont);
					rts.append(errorMessages.get(cell), plainFont);
					comment.setString(rts);
					comment.setAuthor("Apache OFBiz PriCat");
					if (isNewComment) {
						sheet.getRow(cell.getRow()).getCell(cell.getCol()).setCellComment(comment);
				        OFBizPricatUtil.formatCommentShape(sheet, cell);
					}
				}
			}
			
			// set comments in the new error sheet
			XSSFDrawing errorPatriarch = errorSheet.getDrawingPatriarch();
			int newRowNum = getHeaderRowNo() + 1;
			Map<Integer, Integer> rowMapping = new HashMap<Integer, Integer>();
			for (CellReference cell : errorMessages.keySet()) {
				if (cell != null && errorMessages.get(cell) != null) {
					XSSFRow row = sheet.getRow(cell.getRow());
					Integer rowNum = Integer.valueOf(row.getRowNum());
					int errorRow = newRowNum;
					if (rowMapping.containsKey(rowNum)) {
						errorRow = rowMapping.get(rowNum).intValue();
					} else {
						XSSFRow newRow = errorSheet.getRow(errorRow);
						if (newRow == null) {
							newRow = errorSheet.createRow(errorRow);
						}
						rowMapping.put(rowNum, Integer.valueOf(errorRow));
						newRow.setHeight(row.getHeight());
						copyRow(row, newRow, factory, errorPatriarch);
						newRowNum ++;
					}
				}
			}

			// write to file
			if (sequenceNum > 0L) {
				File commentedExcel = FileUtil.getFile(tempFilesFolder + userLoginId + "/" + sequenceNum + ".xlsx");
				fos = new FileOutputStream(commentedExcel);
				workbook.write(fos);
			} else {
				fos = new FileOutputStream(pricatFile);
				workbook.write(fos);
			}
			fos.flush();
			fos.close();
			workbook.close();
		} catch (FileNotFoundException e) {
			report.println(e);
			Debug.logError(e, module);
		} catch (IOException e) {
			report.println(e);
			Debug.logError(e, module);
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					Debug.logError(e, module);
				}
			}
			if (workbook != null) {
				try {
					workbook.close();
				} catch (IOException e) {
					Debug.logError(e, module);
				}
			}
		}
		report.println(UtilProperties.getMessage(resource, "ok", locale), InterfaceReport.FORMAT_OK);
		report.println();
	}

	private void copyRow(XSSFRow sourceRow, XSSFRow targetRow, XSSFCreationHelper factory, XSSFDrawing patriarch) {
		for (int j = 0; j < sourceRow.getPhysicalNumberOfCells(); j++) {
			XSSFCell cell = sourceRow.getCell(j);
			if (cell != null) {
				XSSFCell newCell = targetRow.createCell(j);
				int cellType = cell.getCellType();
				newCell.setCellType(cellType);
				switch (cellType) {
					case XSSFCell.CELL_TYPE_BOOLEAN:
						newCell.setCellValue(cell.getBooleanCellValue());
						break;
					case XSSFCell.CELL_TYPE_ERROR:
						newCell.setCellErrorValue(cell.getErrorCellValue());
						break;
					case XSSFCell.CELL_TYPE_FORMULA:
						newCell.setCellFormula(cell.getCellFormula());
						break;
					case XSSFCell.CELL_TYPE_NUMERIC:
						newCell.setCellValue(cell.getNumericCellValue());
						break;
					case XSSFCell.CELL_TYPE_STRING:
						newCell.setCellValue(cell.getRichStringCellValue());
						break;
					default:
						newCell.setCellValue(formatter.formatCellValue(cell));
				}
				if (cell.getCellComment() != null) {
					XSSFClientAnchor anchor = factory.createClientAnchor();
					anchor.setDx1(100);
					anchor.setDx2(100);
					anchor.setDy1(100);
					anchor.setDy2(100);
				    anchor.setCol1(newCell.getColumnIndex());
				    anchor.setCol2(newCell.getColumnIndex() + 4);
				    anchor.setRow1(newCell.getRowIndex());
				    anchor.setRow2(newCell.getRowIndex() + 4);
				    anchor.setAnchorType(AnchorType.DONT_MOVE_AND_RESIZE);

				    XSSFComment comment = patriarch.createCellComment(anchor);
				    comment.setString(cell.getCellComment().getString());
					newCell.setCellComment(comment);
				}
				newCell.setCellStyle(cell.getCellStyle());
				newCell.getSheet().setColumnWidth(newCell.getColumnIndex(), cell.getSheet().getColumnWidth(cell.getColumnIndex()));
			}
		}
	}

	public void initBasicConds(List<String> orgPartyIds) {
		basicCategoryConds = new ArrayList<EntityCondition>();
		basicCategoryConds.add(EntityCondition.makeCondition("isPublic", "N"));
		//basicCategoryConds.add(EntityCondition.makeCondition("isDefault", "Y"));
		
		basicBrandConds = new ArrayList<EntityCondition>();
		basicBrandConds.add(EntityCondition.makeCondition("isPublic", "N"));
		basicBrandConds.add(EntityCondition.makeCondition("productFeatureTypeId", "BRAND"));
		
		List<EntityCondition> partyIdConds = new ArrayList<EntityCondition>();
		for (String orgPartyId : orgPartyIds) {
			partyIdConds.add(EntityCondition.makeCondition("ownerPartyId", orgPartyId));
		}
		if (UtilValidate.isNotEmpty(partyIdConds)) {
			basicCategoryConds.add(EntityCondition.makeCondition(partyIdConds, EntityOperator.OR));
			basicBrandConds.add(EntityCondition.makeCondition(partyIdConds, EntityOperator.OR));
		}
	}

	public Map<String, Object> updateSkuPrice(String skuId, String ownerPartyId, BigDecimal memberPrice) {
		return ServiceUtil.returnSuccess();
	}

	public Map<String, Object> updateColorAndDimension(String productId, String ownerPartyId, String color, String dimension) {
		Map<String, Object> results = ServiceUtil.returnSuccess();
		results.put("colorId", "sampleColorId");
		results.put("dimensionId", "sampleDimensionId");
		return results;
	}

	public Map<String, Object> getDimensionIds(String productId, String ownerPartyId, String dimension) {
		Map<String, Object> results = ServiceUtil.returnSuccess();
		results.put("dimensionId", "sampleDimensionId");
		return results;
	}

	public Map<String, Object> getColorIds(String productId, String ownerPartyId, String color) {
		Map<String, Object> results = ServiceUtil.returnSuccess();
		results.put("foundColor", Boolean.TRUE);
		results.put("colorId", "sampleColorId");
		return results;
	}

	public String getBrandId(String brandName, String ownerPartyId) {
		return "sampleBrandId";
	}

	public boolean isNumOfSheetsOK(XSSFWorkbook workbook) {
		report.print(UtilProperties.getMessage(resource, "CheckPricatHasSheet", locale), InterfaceReport.FORMAT_NOTE);
		int sheets = workbook.getNumberOfSheets();
		if (sheets < 1) {
			report.println(UtilProperties.getMessage(resource, "PricatTableNoSheet", locale), InterfaceReport.FORMAT_ERROR);
			return false;
		} else if (sheets >= 1) {
			report.println(UtilProperties.getMessage(resource, "ok", locale), InterfaceReport.FORMAT_OK);
			report.println(UtilProperties.getMessage(resource, "PricatTableOnlyParse1stSheet", locale), InterfaceReport.FORMAT_WARNING);
		}
		return true;
	}

	/**
	 * Get data by version definition.
	 * 
	 * @param row
	 * @param colNames 
	 * @param size 
	 * @return
	 */
	public List<Object> getCellContents(XSSFRow row, List<Object[]> colNames, int size) {
		List<Object> results = new ArrayList<Object>();
		boolean foundError = false;
		if (isEmptyRow(row, size, true)) {
			return null;
		}
		for (int i = 0; i < size; i++) {
			XSSFCell cell = null;
			if (row.getPhysicalNumberOfCells() > i) {
				cell = row.getCell(i);
			}
			if (cell == null) {
				if (((Boolean) colNames.get(i)[2]).booleanValue()) {
					report.print(UtilProperties.getMessage(resource, "ErrorColCannotEmpty", new Object[] {colNames.get(i)[0]}, locale), InterfaceReport.FORMAT_WARNING);
					errorMessages.put(new CellReference(cell), UtilProperties.getMessage(resource, "ErrorColCannotEmpty", new Object[] {colNames.get(i)[0]}, locale));
					foundError = true;
					continue;
				} else {
					cell = row.createCell(i);
				}
			}
			int cellType = cell.getCellType();
			String cellValue = formatter.formatCellValue(cell);
			if (UtilValidate.isNotEmpty(cellValue)) {
				if (cellType == XSSFCell.CELL_TYPE_FORMULA) {
					cellValue = BigDecimal.valueOf(cell.getNumericCellValue()).setScale(FinAccountHelper.decimals, FinAccountHelper.rounding).toString();
					report.print(((i == 0)?"":", ") + cellValue, InterfaceReport.FORMAT_NOTE);
				} else {
					report.print(((i == 0)?"":", ") + cellValue, InterfaceReport.FORMAT_NOTE);
				}
			} else {
				report.print(((i == 0)?"":","), InterfaceReport.FORMAT_NOTE);
			}
			if (((Boolean) colNames.get(i)[2]).booleanValue() && UtilValidate.isEmpty(cellValue)) {
				report.print(UtilProperties.getMessage(resource, "ErrorColCannotEmpty", new Object[] {colNames.get(i)[0]}, locale), InterfaceReport.FORMAT_WARNING);
				errorMessages.put(new CellReference(cell), UtilProperties.getMessage(resource, "ErrorColCannotEmpty", new Object[] {colNames.get(i)[0]}, locale));
				foundError = true;
				results.add(null);
				continue;
			}
			if (((Boolean) colNames.get(i)[2]).booleanValue() && cellType != (int) colNames.get(i)[1]) {
				// String warningMessage = "";
				if ((int) colNames.get(i)[1] == XSSFCell.CELL_TYPE_STRING) {
					results.add(cellValue);
				} else if ((int) colNames.get(i)[1] == XSSFCell.CELL_TYPE_NUMERIC) {
					if (cell.getCellType() != XSSFCell.CELL_TYPE_STRING) {
						cell.setCellType(XSSFCell.CELL_TYPE_STRING);
					}
					try {
						results.add(BigDecimal.valueOf(Double.parseDouble(cell.getStringCellValue())).setScale(FinAccountHelper.decimals, FinAccountHelper.rounding));
					} catch (NumberFormatException e) {
						results.add(null);
						errorMessages.put(new CellReference(cell), UtilProperties.getMessage(resource, "ErrorParseValueToNumeric", locale));
					}
				}
			} else {
				if (UtilValidate.isEmpty(cellValue)) {
					results.add(null);
					continue;
				}
				if ((int) colNames.get(i)[1] == XSSFCell.CELL_TYPE_STRING) {
					if (cell.getCellType() == XSSFCell.CELL_TYPE_STRING) {
						results.add(cell.getStringCellValue());
					} else {
						results.add(cellValue);
					}
				} else if ((int) colNames.get(i)[1] == XSSFCell.CELL_TYPE_NUMERIC) {
					if (cell.getCellType() == XSSFCell.CELL_TYPE_STRING) {
						try {
							results.add(BigDecimal.valueOf(Double.valueOf(cell.getStringCellValue())));
						} catch (NumberFormatException e) {
							results.add(null);
							errorMessages.put(new CellReference(cell), UtilProperties.getMessage(resource, "ErrorParseValueToNumeric", locale));
						}
					} else if (cell.getCellType() == XSSFCell.CELL_TYPE_NUMERIC) {
						try {
							results.add(BigDecimal.valueOf(cell.getNumericCellValue()).setScale(FinAccountHelper.decimals, FinAccountHelper.rounding));
						} catch (NumberFormatException e) {
							results.add(null);
							errorMessages.put(new CellReference(cell), UtilProperties.getMessage(resource, "ErrorParseValueToNumeric", locale));
						}
					} else {
						try {
							results.add(BigDecimal.valueOf(Double.valueOf(cellValue)).setScale(FinAccountHelper.decimals, FinAccountHelper.rounding));
						} catch (NumberFormatException e) {
							results.add(null);
							errorMessages.put(new CellReference(cell), UtilProperties.getMessage(resource, "ErrorParseValueToNumeric", locale));
						}
					}
				}
			}
		}
		if (foundError) {
			return null;
		}
		return results;
	}

	public void setFacilityId(String selectedFacilityId) {
		this.selectedFacilityId = selectedFacilityId;
	}

	protected boolean isEmptyRow(XSSFRow row, int size, boolean display) {
		// check whether this row is empty
		if (UtilValidate.isEmpty(row)) {
			report.print(UtilProperties.getMessage(resource, "ExcelEmptyRow", locale), InterfaceReport.FORMAT_NOTE);
			return true;
		}
		boolean isEmptyRow = true;
		int physicalNumberOfCells = row.getPhysicalNumberOfCells();
		int i = 0;
		for (; i < size; i++) {
			XSSFCell cell = null;
			if (physicalNumberOfCells > i) {
				cell = row.getCell(i);
			}
			if (cell != null && UtilValidate.isNotEmpty(formatter.formatCellValue(cell)) && UtilValidate.isNotEmpty(formatter.formatCellValue(cell).trim())) {
				isEmptyRow = false;
				break;
			}
		}
		if (isEmptyRow) {
			if (display) {
				report.print(UtilProperties.getMessage(resource, "ExcelEmptyRow", locale), InterfaceReport.FORMAT_NOTE);
			}
			return true;
		} else if (!isEmptyRow && i > size) {
			if (display) {
				report.print(UtilProperties.getMessage(resource, "IgnoreDataOutOfRange", locale), InterfaceReport.FORMAT_NOTE);
			}
			return true;
		}
		return isEmptyRow;
	}
	
	protected abstract int getHeaderRowNo();
	

	public synchronized void endExcelImportHistory(String logFileName, String thruReasonId) {
		Thread currentThread = Thread.currentThread();
		String threadName = null;
		if (currentThread instanceof PricatParseExcelHtmlThread) {
			threadName = ((PricatParseExcelHtmlThread) currentThread).getUUID().toString();
		}
		if (UtilValidate.isEmpty(threadName)) {
			return;
		}
		try {
			GenericValue historyValue = null;
			if (sequenceNum < 1L) {
				historyValue = EntityUtil.getFirst(EntityUtil.filterByDate(delegator.findByAnd("ExcelImportHistory", 
													UtilMisc.toMap("userLoginId", userLoginId, "logFileName", logFileName), UtilMisc.toList("sequenceNum DESC"), false)));
			} else {
				historyValue = delegator.findOne("ExcelImportHistory", UtilMisc.toMap("userLoginId", userLoginId, "sequenceNum", (Long) sequenceNum), false);
			}
			Timestamp now = UtilDateTime.nowTimestamp();
			if (UtilValidate.isEmpty(historyValue)) {
				historyValue = delegator.makeValue("ExcelImportHistory", UtilMisc.toMap("sequenceNum", Long.valueOf(sequenceNum), "userLoginId", userLoginId,
													"fileName", pricatFile.getName(), "statusId", "EXCEL_IMPORTED", "fromDate", now,  
													"thruDate", now, "threadName", threadName, "logFileName", logFileName));
			} else {
				historyValue.set("statusId", "EXCEL_IMPORTED");
				historyValue.set("thruDate", now);
				if (pricatFile != null && pricatFile.exists()) {
					historyValue.set("fileName", pricatFile.getName());
				}
				historyValue.set("thruReasonId", thruReasonId);
			}
			delegator.createOrStore(historyValue);
		} catch (GenericEntityException e) {
			// do nothing
		}
	}
	
	public boolean hasErrorMessages() {
		return !errorMessages.keySet().isEmpty();
	}

	/**
	 * Check whether a commented file exists.
	 * 
	 * @param request
	 * @param sequenceNum
	 * @return
	 */
	public static boolean isCommentedExcelExists(HttpServletRequest request, Long sequenceNum) {
	    GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
		if (UtilValidate.isEmpty(sequenceNum) || UtilValidate.isEmpty(userLogin)) {
			Debug.logError("sequenceNum[" + sequenceNum + "] or userLogin is empty", module);
			return false;
		}
	    String userLoginId = userLogin.getString("userLoginId");
		Delegator delegator = (Delegator) request.getAttribute("delegator");
		GenericValue historyValue = null;
		try {
			historyValue = delegator.findOne("ExcelImportHistory", UtilMisc.toMap("userLoginId", userLoginId, "sequenceNum", Long.valueOf(sequenceNum)), false);
		} catch (NumberFormatException e) {
			Debug.logError(e.getMessage(), module);
			return false;
		} catch (GenericEntityException e) {
			Debug.logError(e.getMessage(), module);
			return false;
		}
		if (UtilValidate.isEmpty(historyValue)) {
			Debug.logError("No ExcelImportHistory value found by sequenceNum[" + sequenceNum + "] and userLoginId[" + userLoginId + "].", module);
			return false;
		}
		File file = FileUtil.getFile(tempFilesFolder + userLoginId + "/" + sequenceNum + ".xlsx");
		if (file.exists()) {
			return true;
		}
        return false;
	}

	protected void cleanupLogAndCommentedExcel() {
		try {
			report.print(UtilProperties.getMessage(resource, "CLEANUP_LOGANDEXCEL_BEGIN", locale), InterfaceReport.FORMAT_DEFAULT);
			List<GenericValue> historyValues = delegator.findByAnd("ExcelImportHistory", UtilMisc.toMap("userLoginId", userLoginId), UtilMisc.toList("sequenceNum DESC"), false);
			if (UtilValidate.isEmpty(historyValues) || historyValues.size() <= HISTORY_MAX_FILENUMBER) {
				report.print(UtilProperties.getMessage(resource, "HistoryLessThan", new Object[] {String.valueOf(HISTORY_MAX_FILENUMBER)}, locale), InterfaceReport.FORMAT_NOTE);
				report.println(" ... " + UtilProperties.getMessage(resource, "skipped", locale), InterfaceReport.FORMAT_NOTE);
			} else {
				report.print(" ... " + UtilProperties.getMessage(resource, "HistoryEntryToRemove", new Object[] {historyValues.size() - HISTORY_MAX_FILENUMBER}, locale), InterfaceReport.FORMAT_NOTE);
				List<GenericValue> valuesToRemove = new ArrayList<GenericValue>();
				for (int i = HISTORY_MAX_FILENUMBER; i < historyValues.size(); i++) {
					GenericValue historyValue = historyValues.get(i);
					valuesToRemove.add(historyValue);
					File excelFile = FileUtil.getFile(tempFilesFolder + userLoginId + "/" + historyValue.getLong("sequenceNum") + ".xlsx");
					if (excelFile.exists()) {
						try {
							excelFile.delete();
						} catch (SecurityException e) {
							Debug.logError(e.getMessage(), module);
							report.print(e.getMessage(), InterfaceReport.FORMAT_ERROR);
						}
					}
					File logFile = FileUtil.getFile(tempFilesFolder + userLoginId + "/" + historyValue.getLong("sequenceNum") + ".log");
					if (logFile.exists()) {
						try {
							logFile.delete();
						} catch (SecurityException e) {
							Debug.logError(e.getMessage(), module);
							report.print(e.getMessage(), InterfaceReport.FORMAT_ERROR);
						}
					}
				}
				delegator.removeAll(valuesToRemove);
				report.println(" ... " + UtilProperties.getMessage(resource, "ok", locale), InterfaceReport.FORMAT_OK);
			}
			report.println();
		} catch (GenericEntityException e) {
			Debug.logError(e.getMessage(), module);
		}
	}
}
