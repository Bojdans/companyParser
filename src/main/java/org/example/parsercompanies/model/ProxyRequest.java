package org.example.parsercompanies.model;

import lombok.Data;

@Data
public class ProxyRequest {
    private String host;
    private int port;
    private String username;
    private String password;
}
