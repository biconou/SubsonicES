package com.github.biconou.subsonic.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by remi on 19/05/2016.
 */
public interface SubsonicESDomainObject {

  public String getESId();

  public void setESId(String ESId);

  public int getVersion();

  public void setVersion(int version);
}
