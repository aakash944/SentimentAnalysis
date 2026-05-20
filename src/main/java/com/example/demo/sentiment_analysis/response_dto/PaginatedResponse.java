package com.example.demo.sentiment_analysis.response_dto;

import lombok.Data;

import java.util.List;
@Data
public class PaginatedResponse<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private boolean first;
    private boolean last;
    private boolean hasNext;
}
