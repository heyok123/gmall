package com.atguigu.gmall.search.service.impl;

import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.service.SearchService;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    public void search(SearchParamVo searchParamVo) {

        try {
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"},this.searchSourceBuilder(searchParamVo));
            this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private SearchSourceBuilder searchSourceBuilder(SearchParamVo searchParamVo){

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        String keyword = searchParamVo.getKeyword();

        if (StringUtils.isBlank(keyword)){
            return sourceBuilder;
        }

        // 1. 构建搜索条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));
        sourceBuilder.query(boolQueryBuilder);


        // 1.1. 构建匹配查询

        // 1.2. 构建过滤条件
        // 1.2.1. 品牌过滤

        // 1.2.2. 分类过滤

        // 1.2.3. 价格区间过滤

        // 1.2.4. 库存过滤

        // 1.2.5. 规格参数的嵌套过滤

        // 2. 构建排序条件

        // 3. 构建分页条件

        // 4. 构建高亮

        // 5. 构建聚合
        // 5.1. 品牌聚合

        // 5.2. 分类聚合

        // 5.3. 规格参数的嵌套聚合

        System.out.println(sourceBuilder);

        return sourceBuilder;

    }
}
