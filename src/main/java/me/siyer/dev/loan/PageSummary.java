package me.siyer.dev.loan;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PageSummary {

    private Map<String, List<AmortisationValueHolder>> pageSummary;
    private int maxRecordCount;
    int pageNo;

    public PageSummary(final int pageNo, final int maxRecordCount, final Map<String, List<AmortisationValueHolder>> pageSummary){
        this.maxRecordCount=maxRecordCount;
        this.pageNo=pageNo;
        this.pageSummary=new LinkedHashMap<>(pageSummary);
    }

    public Map<String, List<AmortisationValueHolder>> getPageSummary() {
        return pageSummary;
    }

    public int getMaxRecordCount() {
        return maxRecordCount;
    }

    public int getPageNo() {
        return pageNo;
    }
}
