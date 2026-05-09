
package com.example.crm.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageResponse<T> {

    private List<T> list;

    private Long total;

    private Integer pageNum;

    private Integer pageSize;

    public PageResponse() {
    }

    public PageResponse(List<T> list, Long total, Integer pageNum, Integer pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }
}
