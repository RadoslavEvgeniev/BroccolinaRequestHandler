package app.broccolina.solet;

import java.io.IOException;

public interface HttpSolet {

    void init(SoletConfig soletConfig);

    void service(HttpSoletRequest request, HttpSoletResponse response) throws IOException;

    SoletConfig getSoletConfig();

//    void doGet(HttpSoletRequest request, HttpSoletResponse response);
//
//    void doPost(HttpSoletRequest request, HttpSoletResponse response);
//
//    void doPut(HttpSoletRequest request, HttpSoletResponse response);
//
//    void doDelete(HttpSoletRequest request, HttpSoletResponse response);
}
