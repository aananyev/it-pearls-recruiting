package com.company.itpearls.core;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

@Service(WebLoadService.NAME)
public class WebLoadServiceBean implements WebLoadService {
    @Override
    public String getCVWebPage(String cvURL) throws IOException {
        URL url = new URL(cvURL);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(url.openStream()));

        String inputLine;
        String retStr = "";
        while ((inputLine = in.readLine()) != null) {
            retStr += inputLine;
        } //Можно   накапливать в StringBuilder а потом присвоить перемной String результат накопления

        in.close();

        return retStr;
    }
}