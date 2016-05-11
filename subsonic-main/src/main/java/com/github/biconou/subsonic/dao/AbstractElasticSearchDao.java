package com.github.biconou.subsonic.dao;

import com.github.biconou.dao.ElasticSearchClient;

/**
 * Created by remi on 11/05/2016.
 */
public abstract class AbstractElasticSearchDao {

  private ElasticSearchClient elasticSearchClient = null;

  public ElasticSearchClient getElasticSearchClient() {
    return elasticSearchClient;
  }

  public void setElasticSearchClient(ElasticSearchClient elasticSearchClient) {
    this.elasticSearchClient = elasticSearchClient;
  }


}
