package com.github.biconou.subsonic.rest;

import java.io.File;

/**
 * Created by remi on 07/05/2016.
 */
public class SecurityServiceMock extends net.sourceforge.subsonic.service.SecurityService {

  @Override
  public boolean isReadAllowed(File file) {
    return true;
  }
}
