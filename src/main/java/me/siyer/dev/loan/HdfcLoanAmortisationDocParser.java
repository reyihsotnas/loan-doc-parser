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
        File outputFile = new File("output.csv");
        if(outputFile.exists()) {
            outputFile.delete();
        }
        parser.initParsing(outputFile);

    }

    public void initParsing(final File outputFile) {
        final File amortisationScheduleFile = new File(HdfcLoanAmortisationDocParser.class.getClassLoader().getResource(AMORTISATION_FILE_NAME).getPath());
        parseAmortisationSchedule(amortisationScheduleFile, outputFile);
    }

    private void parseAmortisationSchedule(final File amortisationSchedule, final File outputFile) {
        Map<String, List<String>> parsedContent = new HashMap<>();
        final List<String> headerRecords = getHeaders();
        try (PDDocument doc = Loader.loadPDF(amortisationSchedule)) {

            logger.info("No. Pages: {}", doc.getPages().getCount());
            //int maxPages = doc.getPages().getCount();
            int maxPages = 1;
            for (int pageNo = 1; pageNo<=maxPages; pageNo++){
                PDPage page = doc.getPage(1);
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(pageNo);
                stripper.setEndPage(pageNo);

                String text = stripper.getText(doc);

                prettyPrintResults(parsePage(text, headerRecords, pageNo), headerRecords, outputFile, pageNo==1);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, List<String>> parsePage(final String pageContents, final List<String> headerRecords, final int pageNo) {
        final String pageStart = "Page " + pageNo + " of ";
        final Map<String, List<String>> parsedPageValues = new HashMap<>();
        final Map<String, List<String>> lastHeaderParsed = new HashMap<>();
        final List<String> pageContent = List.of(pageContents.split("\n"));
        List<String> tempCollection = new ArrayList<>();
        final String finalHeaderRecord = headerRecords.get(headerRecords.size()-1);
        boolean pageStartEncountered = false;
        boolean finalHeaderFound = false;
        String lastHeaderEvaluated="";
        String lastDerivedHeader="";

        for (String value : pageContent) {
            final String lineValue = value.trim();
            logger.info(" ---- lineContent: {}, pageStartEncontered: {}", lineValue, pageStartEncountered);
            if (!lineValue.contains(pageStart) && !pageStartEncountered) {
                logger.info(" ---- --- looping until page start :{} is encountered..", pageStart);
                continue;
            }
            if (lineValue.contains(pageStart) && !pageStartEncountered) {
                logger.info(" ---- --- Setting pageStartEncountered = true");
                pageStartEncountered = true;
                continue;
            }

            boolean headerFound = false;

            for (String header : headerRecords) {
                if(header.equals(lineValue)){

                } else if (header.contains(lineValue)) {
                    headerFound = true;
                    lastHeaderEvaluated=header;
                    lastDerivedHeader=lineValue;
                    logger.info("---- --- Header found. populating values : {} to header: {}, count: {}", tempCollection, header, tempCollection.size());
                    logger.info("---- ---- --- lastHeaderEvaluated: {}, lastHeaderLineParsed: {}",lastHeaderEvaluated, lastDerivedHeader);
                    if(tempCollection.size() > 0){
                        if(lastHeaderParsed.containsKey(header)){
                            lastHeaderParsed.get(header).addAll(new ArrayList<>(List.copyOf(tempCollection)));
                        } else {
                            lastHeaderParsed.put(header, new ArrayList(List.copyOf(tempCollection)));
                        }
                    } else {
                        lastHeaderParsed.put(header, new ArrayList<String>());
                    }

                    tempCollection.clear();
                    if(finalHeaderRecord.contains(header)) {
                        logger.info("Final Header: {} encountered. Breaking loop..", finalHeaderRecord);
                        finalHeaderFound = true;
                        break;
                    }
                    if(headerFound) {
                        logger.info("---- --- --- since a header was found in the last loop, existing ...");
                        break;
                    }
                    String completedHeader=lastDerivedHeader+lineValue;
                    logger.info("---- --- --- --- Complete Header Evaluated: {}", completedHeader);
                    //if(completedHeader.)

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
        return parsedPageValues;
    }

    private void prettyPrintResults(final Map<String, List<String>> resultRecords, final List<String> headerRecords, final File outputFile, boolean printHeaderRow) throws IOException {
        logger.info("---------------------------------------------------");
        System.out.println();
        try(Writer fileWriter = new BufferedWriter(new FileWriter(outputFile,true))) {
            //String line = resultRecords.keySet().toString();
            String headerStr = headerRecords.toString();
            headerStr=headerStr.replace("[","");
            headerStr=headerStr.replace("]","");
            System.out.println(headerStr);
            if (printHeaderRow) {
                fileWriter.append(headerStr);
            }
            fileWriter.append(System.lineSeparator());
            final List<String> tranTypeRecords = resultRecords.get(headerRecords.get(0));
            if (tranTypeRecords != null && tranTypeRecords.size() > 0) {
                for (int i = 0; i < tranTypeRecords.size(); i++) {
                    for (String header : headerRecords) {
                        List<String> recordValues = resultRecords.get(header);
                        if (recordValues == null || recordValues.size() <= i) {
                            System.out.print(",");
                            fileWriter.append(",");
                        } else {
                            String value = recordValues.get(i);
                            System.out.print(value + ",");
                            fileWriter.append(value + ",");
                        }
                    }
                    System.out.println();
                    fileWriter.append(System.lineSeparator());
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
