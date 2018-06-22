package app.broccolina.solet;

import java.io.IOException;

public interface HttpSolet {

    void init(SoletConfig soletConfig);

    boolean isInitialized();

    void service(HttpSoletRequest request, HttpSoletResponse response) throws IOException;
}
