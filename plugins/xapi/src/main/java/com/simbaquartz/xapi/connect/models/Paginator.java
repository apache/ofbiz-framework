/*
 *
 *  * *****************************************************************************************
 *  *  Copyright (c) SimbaQuartz  2016. - All Rights Reserved                                 *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited               *
 *  *  Proprietary and confidential                                                           *
 *  *  Written by Mandeep Sidhu <mandeep.sidhu@fidelissd.com>,  September, 2017                    *
 *  * ****************************************************************************************
 *
 */

package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Generic Pagination model for wrapping paginated records.
 * Created by mande on 9/9/2017.
 */
public class Paginator {
    private List<? extends Object> data;
    private int totalRecordsCount;
    private int currentPage;
    private int nextPage;
    private int previousPage;

    @JsonProperty("data")
    public List<? extends Object> getData() {return data;}
    public void setData(List<? extends Object> data) {this.data = data;}

    @JsonProperty("count")
    public int getTotalRecordsCount() {return totalRecordsCount;}
    public void setTotalRecordsCount(int totalRecordsCount) {this.totalRecordsCount = totalRecordsCount;}

    @JsonProperty("current_page")
    public int getCurrentPage() {return currentPage;}
    public void setCurrentPage(int currentPage) {this.currentPage = currentPage;}

    @JsonProperty("next_page")
    public int getNextPage() {return nextPage;}
    public void setNextPage(int nextPage) {this.nextPage = nextPage;}

    @JsonProperty("previous_page")
    public int getPreviousPage() {return previousPage;}
    public void setPreviousPage(int previousPage) {this.previousPage = previousPage;}

}
