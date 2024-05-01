package me.siyer.dev.loan;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;
import java.util.*;

public class HdfcLoanAmortisationDocParser {

    private static final String HEADER_FILE = "headers.txt";

    private static final String AMORTISATION_FILE_NAME = "AMRT_DETAILS_694456.PDF";

    private static final Logger logger = LogManager.getLogger(HdfcLoanAmortisationDocParser.class);

    static {
        final String log4jXMLPath = HdfcLoanAmortisationDocParser.class.getClassLoader().getResource("log4j2.xml").getPath();
        System.setProperty("log4j.configurationFile", log4jXMLPath);
    }


    public static void main(String[] args) {
        HdfcLoanAmortisationDocParser parser = new HdfcLoanAmortisationDocParser();
        parser.parseAmortisationSchedule();
    }

    public void parseAmortisationSchedule() {
        final File amortisationScheduleFile = new File(HdfcLoanAmortisationDocParser.class.getClassLoader().getResource(AMORTISATION_FILE_NAME).getPath());
        final File outputFile = new File("output.csv");
        if(outputFile.exists()) {
            logger.info("Output file exists at :{}. Deleting it.",outputFile.getPath());
            outputFile.delete();
        }
        logger.info("Parsing input PDF:{}, to generate CSV:{}",amortisationScheduleFile.getPath(), outputFile.getPath());
        parseAmortisationSchedule(amortisationScheduleFile, outputFile);
    }

    private void parseAmortisationSchedule(final File amortisationSchedule, final File outputFile) {

        final List<PageSummary> parsedContent = new LinkedList<>();
        final List<String> headerRecords = getHeaders();
        try (PDDocument doc = Loader.loadPDF(amortisationSchedule)) {

            logger.info("No. Pages: {}", doc.getPages().getCount());
            int initialPage=0;
            //int maxPages = 5;
            final int maxPages = doc.getPages().getCount();
            for (int pageNo = initialPage; pageNo<maxPages; pageNo++){
                logger.info(" --- processing page: {}", pageNo);
                PDPage page = doc.getPage(pageNo);
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(pageNo);
                stripper.setEndPage(pageNo);

                String text = stripper.getText(doc);

                parsedContent.add(parsePage(text, headerRecords, pageNo));
                //prettyPrintResults(parsePage(text, headerRecords, pageNo), headerRecords, outputFile, pageNo==1);
            }
        prettyPrintResults(parsedContent,headerRecords,outputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private PageSummary parsePage(final String pageContents, final List<String> passedHeaderRecords, final int pageNo) {
        final String pageStart = "Page " + pageNo + " of ";
        final PageSummary pageSummary = null;
        final Map<String, List<AmortisationValueHolder>> parsedPageValues = new LinkedHashMap<>();
        //final Map<String, List<String>> lastHeaderParsed = new HashMap<>();
        final List<String> pageContent = List.of(pageContents.split("\n"));
        List<String> tempCollection = new ArrayList<>();
        final List<String> headerRecords = new ArrayList<>(passedHeaderRecords);
        final String finalHeaderRecord = headerRecords.get(headerRecords.size()-1);
        boolean pageStartEncountered = false;
        boolean finalHeaderFound = false;
        List<AmortisationValueHolder> backupTempValue = new LinkedList<>();
        int maxRecords = 0;
        for (String value : pageContent) {
            final String lineValue = value.trim();
            logger.info(" --- --- lineContent: {}, pageStartEncontered: {}", lineValue, pageStartEncountered);
            if (!lineValue.contains(pageStart) && !pageStartEncountered) {
                logger.info(" --- --- --- looping until page start :{} is encountered..", pageStart);
                continue;
            }
            if (lineValue.contains(pageStart) && !pageStartEncountered) {
                logger.info(" --- --- Setting pageStartEncountered = true");
                pageStartEncountered = true;
                continue;
            }

            boolean headerFound = false;
            boolean partialHeaderFound = false;
            List<AmortisationValueHolder> lastTempValue = null;


            for (String header : headerRecords) {
                boolean valueDerived=false;
                // atleast partial header value is present in the current line
               if (header.contains(lineValue)) {
                   logger.info(" --- --- --- Header found. creating VO with values :{} , header:{} to evaluatedHeader: {}, count: {}", tempCollection, header, lineValue, tempCollection.size());
                   lastTempValue=AmortisationValueHolder.parsedValues(header,lineValue,tempCollection);
                   maxRecords = maxRecords<tempCollection.size()? tempCollection.size() : maxRecords;
                   // Exact header is found. Add to the list.
                   if(header.equals(lineValue)) {
                       headerFound = true;
                       logger.info(" --- --- --- --- Exact Header found: {}. Adding elements:{} of count: {}", header, tempCollection, tempCollection.size());

                       if(tempCollection.size() > 0){

                           if(parsedPageValues.containsKey(header)){
                               logger.info(" --- --- --- --- --- added element: {} for header:{} to the parsed values", lastTempValue,header);
                               parsedPageValues.get(header).addAll(new LinkedList<>(lastTempValue));
                           } else {
                               logger.info(" --- --- --- --- --- created new mapping element: {} for header:{} to the parsed values", lastTempValue,header);
                               parsedPageValues.put(header, new LinkedList<>(lastTempValue));
                           }
                       } else {
                           logger.info(" --- --- --- --- --- Collection is empty.empty element for header:{} to the parsed values", header);
                           parsedPageValues.put(header, new LinkedList<>());
                       }
                       backupTempValue.clear();
                       valueDerived = true;
                       logger.info(" ... ... Cleared out backup temp value ...");
                       headerRecords.remove(header);
                   } else {
                       logger.info(" --- --- --- --- Partial Header found. creating VO with values :{} , header:{} to evaluatedHeader: {}, count: {}", tempCollection, header, lineValue, tempCollection.size());
                       headerFound = true;
                       if (backupTempValue != null && backupTempValue.size()>0) {
                           final String newHeaderValue=backupTempValue.get(0).getEvaluatedHeader()+" " +lineValue;
                            logger.info(" --- --- --- --- --- new header derived: {} , current header: {}", newHeaderValue, header);
                           if(header.equals(newHeaderValue)) {
                               if(parsedPageValues.containsKey(header)){
                                   logger.info(" --- --- --- --- --- added element: {} for header:{} to the parsed values", backupTempValue,header);
                                   parsedPageValues.get(header).addAll(new LinkedList<>(backupTempValue));
                               } else {
                                   logger.info(" --- --- --- --- --- created new mapping element: {} for header:{} to the parsed values", backupTempValue,header);
                                   parsedPageValues.put(header, new LinkedList(backupTempValue));
                               }
                               valueDerived=true;
                               backupTempValue.clear();
                               logger.info(" ... ... Cleared out backup temp value ...");
                               headerRecords.remove(header);
                           }
                       } else {
                           logger.info("This is first instance of the backup. LastTempValue backed up to backupTempValue ...");
                           backupTempValue.addAll(lastTempValue);
                       }
                   }
                    tempCollection.clear();
                    if(finalHeaderRecord.contains(header)) {
                        logger.info("Final Header: {} encountered. Breaking loop..", finalHeaderRecord);
                        finalHeaderFound = true;
                        break;
                    }
                    if (valueDerived) {
                        logger.info(" --- --- Exiting the loop for checking for Headers.");
                        break;
                    }
                }
            }


            if(finalHeaderFound) {
                break;
            }
            if (!headerFound) {
                logger.info(" ---- ---- --- Adding {} to temp header", lineValue);
                tempCollection.add(lineValue);
            }
        }
        return new PageSummary(pageNo,maxRecords,parsedPageValues);
    }

    private void prettyPrintResults(final List<PageSummary> pageSummary, final List<String> headerRecords, final File outputFile) throws IOException {
        logger.info("---------------------------------------------------");
        System.out.println();
        try(Writer fileWriter = new BufferedWriter(new FileWriter(outputFile,true))) {
            //String line = resultRecords.keySet().toString();
            String headerStr = headerRecords.toString();
            headerStr=headerStr.replace("[","");
            headerStr=headerStr.replace("]","");
            System.out.println(headerStr);

                fileWriter.append(headerStr);

            fileWriter.append(System.lineSeparator());
            if (pageSummary != null && pageSummary.size() > 0) {

                for (final PageSummary page : pageSummary) {
                    for (int i = 0; i < page.getMaxRecordCount(); i++) {
                        final Map<String, List<AmortisationValueHolder>> pageRecords =  page.getPageSummary();
                            for (String header : headerRecords) {
                                List<AmortisationValueHolder> recordValues = pageRecords.get(header);
                                if (recordValues == null || recordValues.size() <= i) {
                                    System.out.print(",");
                                    fileWriter.append(",");
                                } else {
                                    AmortisationValueHolder value = recordValues.get(i);
                                    System.out.print(value.getValue() + ",");
                                    fileWriter.append(value.getValue() + ",");
                                }
                            }
                            System.out.println();
                            fileWriter.append(System.lineSeparator());
                    }
                }
            }
        }
    }


    private List<String> getHeaders(){
        List<String> headerList = new LinkedList<>();
        try(BufferedReader reader = new BufferedReader(new FileReader(getClass().getClassLoader().getResource(HEADER_FILE).getPath()))){
            String headerLine = reader.readLine();
            for(String header : headerLine.split(",")) {
                headerList.add(header.trim());
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return headerList;
    }
}
