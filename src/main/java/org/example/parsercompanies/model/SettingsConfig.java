package org.example.parsercompanies.model;

import lombok.Data;

@Data
public class SettingsConfig {
    private Long pagesDeep;
    private Double parsingDelay;
    private boolean autoExcelOpen;
    private boolean onlyInOperation;
    private boolean partOfGovernmentProcurement;
    private boolean onlyMainOKVED;
    private boolean rememberParsingPosition;
    private String proxy;
    private String proxyLogin;
    private String proxyPassword;
    private java.util.List<String> cities;
    private java.util.List<String> regions;
}