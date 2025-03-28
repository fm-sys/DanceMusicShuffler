package org.example;

import org.example.api.AuthorizationCodePKCEFlow;

public class Main {
    public static void main(String[] args) {

        AuthorizationCodePKCEFlow authorizationCodePKCEFlow = new AuthorizationCodePKCEFlow(() ->
                System.out.println("Authorization code flow finished!"));

        authorizationCodePKCEFlow.start();

    }
}