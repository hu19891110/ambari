/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.stack;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.stack.ServiceMetainfoXml;
import org.apache.ambari.server.state.stack.StackRoleCommandOrder;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates IO operations on a stack definition service directory.
 */
public abstract class ServiceDirectory extends StackDefinitionDirectory {
  /**
   * metrics file
   */
  private Map<String, File> metricsFileMap = new HashMap<String, File>();

  /**
   * advisor file
   */
  private File advisorFile;

  /**
   * alerts file
   */
  private File alertsFile;

  /**
   * theme file
   */
  private File themeFile;

  /**
   * kerberos descriptor file
   */
  private File kerberosDescriptorFile;

  /**
   * RCO file
   */
  private File rcoFile;

  /**
   * role command order
   */
  private StackRoleCommandOrder roleCommandOrder;

  /**
   * widgets descriptor file
   */
  private Map<String, File> widgetsDescriptorFileMap = new HashMap<String, File>();

  /**
   * package directory path
   */
  protected String packageDir;

  /**
   * upgrades directory path
   */
  protected File upgradesDir;

  /**
   * service metainfo file object representation
   */
  private ServiceMetainfoXml metaInfoXml;

  /**
   * services root directory name
   */
  public static final String SERVICES_FOLDER_NAME = "services";

  /**
   * package directory name
   */
  protected static final String PACKAGE_FOLDER_NAME = "package";

  /**
   * upgrades directory name
   */
  protected static final String UPGRADES_FOLDER_NAME = "upgrades";

  /**
   * service metainfo file name
   */
  private static final String SERVICE_METAINFO_FILE_NAME = "metainfo.xml";

  /**
   * stack definition file unmarshaller
   */
  ModuleFileUnmarshaller unmarshaller = new ModuleFileUnmarshaller();

  /**
   * logger instance
   */
  private final static Logger LOG = LoggerFactory.getLogger(ServiceDirectory.class);


  /**
   * Constructor.
   *
   * @param servicePath     path of the service directory
   * @throws AmbariException if unable to parse the service directory
   */
  public ServiceDirectory(String servicePath) throws AmbariException {
    super(servicePath);
    parsePath();
  }

  /**
   * Obtain the package directory path.
   *
   * @return package directory path
   */
  public String getPackageDir() {
    return packageDir;
  }

  /**
   * Obtain the upgrades directory path.
   *
   * @return upgrades directory path
   */
  public File getUpgradesDir() {
    return upgradesDir;
  }

  /**
   * Obtain the metrics file.
   *
   * @return metrics file
   */
  public File getMetricsFile(String serviceName) {
    return metricsFileMap.get(serviceName);
  }

  /**
   * Obtain the advisor file.
   *
   * @return advisor file
   */
  public File getAdvisorFile() {
    return advisorFile;
  }

  /**
   * Obtain the advisor name.
   *
   * @return advisor name
   */
  public abstract String getAdvisorName(String serviceName);

  /**
   * Obtain the alerts file.
   *
   * @return alerts file
   */
  public File getAlertsFile() {
    return alertsFile;
  }

  /**
   * Obtain theme file
   * @return theme file
   */
  public File getThemeFile() {
    return themeFile;
  }

  /**
   * Obtain the Kerberos Descriptor file.
   *
   * @return Kerberos Descriptor file
   */
  public File getKerberosDescriptorFile() {
    return kerberosDescriptorFile;
  }

  /**
   * Obtain the Widgets Descriptor file.
   *
   * @return Widgets Descriptor file
   */
  public File getWidgetsDescriptorFile(String serviceName) {
    return widgetsDescriptorFileMap.get(serviceName);
  }

  /**
   * Obtain the service metainfo file object representation.
   *
   * @return
   * Obtain the service metainfo file object representation
   */
  public ServiceMetainfoXml getMetaInfoFile() {
    return metaInfoXml;
  }

  /**
   * Obtain the object representation of the service role_command_order.json file
   *
   * @return object representation of the service role_command_order.json file
   */
  public StackRoleCommandOrder getRoleCommandOrder() {
    return roleCommandOrder;
  }

  /**
   * Parse the service directory.
   */
  protected void parsePath() throws AmbariException {
    calculateDirectories();
    parseMetaInfoFile();

    File af = new File(directory, AmbariMetaInfo.SERVICE_ALERT_FILE_NAME);
    alertsFile = af.exists() ? af : null;

    File kdf = new File(directory, AmbariMetaInfo.KERBEROS_DESCRIPTOR_FILE_NAME);
    kerberosDescriptorFile = kdf.exists() ? kdf : null;

    File rco = new File(directory, AmbariMetaInfo.RCO_FILE_NAME);
    if (rco.exists()) {
      rcoFile = rco;
      parseRoleCommandOrder();
    }

    if (metaInfoXml.getServices() != null) {
      for (ServiceInfo serviceInfo : metaInfoXml.getServices()) {
        File mf = new File(directory, serviceInfo.getMetricsFileName());
        metricsFileMap.put(serviceInfo.getName(), mf.exists() ? mf : null);

        File wdf = new File(directory, serviceInfo.getWidgetsFileName());
        widgetsDescriptorFileMap.put(serviceInfo.getName(), wdf.exists() ? wdf : null);
      }
    }

    File advFile = new File(directory, AmbariMetaInfo.SERVICE_ADVISOR_FILE_NAME);
    advisorFile = advFile.exists() ? advFile : null;

    File themeFile = new File(directory, AmbariMetaInfo.SERVICE_THEME_FILE_NAME);
    this.themeFile = themeFile.exists() ? themeFile : null;
  }

  /**
   * Calculate the service specific directories.
   */
  protected abstract void calculateDirectories();

  /**
   * Unmarshal the metainfo file into its object representation.
   *
   * @throws AmbariException if the metainfo file doesn't exist or
   *                         unable to unmarshal the metainfo file
   */
  protected void parseMetaInfoFile() throws AmbariException {
    File f = new File(getAbsolutePath() + File.separator + SERVICE_METAINFO_FILE_NAME);
    if (! f.exists()) {
      throw new AmbariException(String.format("Stack Definition Service at '%s' doesn't contain a metainfo.xml file",
          f.getAbsolutePath()));
    }

    try {
      metaInfoXml = unmarshaller.unmarshal(ServiceMetainfoXml.class, f);
    } catch (JAXBException e) {
      metaInfoXml = new ServiceMetainfoXml();
      metaInfoXml.setValid(false);
      String msg = String.format("Unable to parse service metainfo.xml file '%s' ", f.getAbsolutePath());
      metaInfoXml.addError(msg);
      LOG.warn(msg, e);
      metaInfoXml.setSchemaVersion(getAbsolutePath().replace(f.getParentFile().getParentFile().getParent()+File.separator, ""));
    }
  }

  /**
   * Parse role command order file
   */
  private void parseRoleCommandOrder() {
    if (rcoFile == null)
      return;

    try {
      ObjectMapper mapper = new ObjectMapper();
      TypeReference<Map<String, Object>> rcoElementTypeReference = new TypeReference<Map<String, Object>>() {};
      HashMap<String, Object> result = mapper.readValue(rcoFile, rcoElementTypeReference);
      LOG.info("Role command order info was loaded from file: {}", rcoFile.getAbsolutePath());

      roleCommandOrder = new StackRoleCommandOrder(result);

      if (LOG.isDebugEnabled() && rcoFile != null) {
        LOG.debug("Role Command Order for " + rcoFile.getAbsolutePath());
        roleCommandOrder.printRoleCommandOrder(LOG);
      }
    } catch (IOException e) {
      LOG.error(String.format("Can not read role command order info %s", rcoFile.getAbsolutePath()), e);
    }
  }

}
